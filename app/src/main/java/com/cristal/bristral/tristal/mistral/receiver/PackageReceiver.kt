package com.cristal.bristral.tristal.mistral.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cristal.bristral.tristal.mistral.MainActivity

class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REMOVED -> {
                // Notify home screen to refresh app list
                val refresh = Intent(context, MainActivity::class.java)
                refresh.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                refresh.putExtra("refresh_apps", true)
            }
        }
    }
}
