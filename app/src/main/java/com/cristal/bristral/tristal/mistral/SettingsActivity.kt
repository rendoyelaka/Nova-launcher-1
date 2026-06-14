package com.cristal.bristral.tristal.mistral

import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cristal.bristral.tristal.mistral.utils.AppPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnChangeWallpaper: Button
    private lateinit var btnClearFavorites: Button
    private lateinit var btnSetDefault: Button
    private lateinit var switchShowClock: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var btnBack: ImageButton
    private lateinit var tvVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadSettings()
    }

    private fun initViews() {
        btnChangeWallpaper = findViewById(R.id.btn_change_wallpaper)
        btnClearFavorites = findViewById(R.id.btn_clear_favorites)
        btnSetDefault = findViewById(R.id.btn_set_default_launcher)
        switchShowClock = findViewById(R.id.switch_show_clock)
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        btnBack = findViewById(R.id.btn_back_settings)
        tvVersion = findViewById(R.id.tv_version)

        btnBack.setOnClickListener { finish() }

        btnChangeWallpaper.setOnClickListener {
            val wallpaperManager = WallpaperManager.getInstance(this)
            startActivity(wallpaperManager.getCropAndSetWallpaperIntent(null)
                ?: Intent(Intent.ACTION_SET_WALLPAPER))
        }

        btnClearFavorites.setOnClickListener {
            AppPreferences.clearFavoriteApps()
            Toast.makeText(this, "Favorites cleared", Toast.LENGTH_SHORT).show()
        }

        btnSetDefault.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        }

        switchShowClock.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setShowClock(isChecked)
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setDarkMode(isChecked)
            recreate()
        }

        try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version ${pkgInfo.versionName}"
        } catch (e: Exception) {
            tvVersion.text = "Version 1.0"
        }
    }

    private fun loadSettings() {
        switchShowClock.isChecked = AppPreferences.isShowClock()
        switchDarkMode.isChecked = AppPreferences.isDarkMode()
    }
}
