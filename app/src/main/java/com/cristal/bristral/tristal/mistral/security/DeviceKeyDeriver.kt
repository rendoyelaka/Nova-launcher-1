package com.cristal.bristral.tristal.mistral.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest

/**
 * DeviceKeyDeriver — derives a device-bound key component.
 *
 * Combines: ANDROID_ID + device model + build fingerprint + bot salt
 * Hashed with SHA-256 → 32-byte device-specific key material.
 *
 * This is XORed with the AES key from dex to produce the final key.
 * Result: payload only decrypts on THIS specific device.
 *
 * DEVICE_SALT is a random 32-byte salt patched by bot per client build.
 * Without the correct salt the derived key is wrong → decryption fails.
 */
object DeviceKeyDeriver {

    // ── Patched by bot ─────────────────────────────────────────
    private const val DEVICE_SALT = "DEVICE_SALT_PLACEHOLDER_CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC"
    // ───────────────────────────────────────────────────────────

    /**
     * Derive device-bound key and XOR with the base AES key.
     * @param context   Android context
     * @param baseKeyB64 base64 AES key from dex (patched by bot)
     * @return final 32-byte key base64, or baseKeyB64 on failure
     */
    fun deriveKey(context: Context, baseKeyB64: String): String {
        return try {
            // Guard: if salt not patched by bot, skip device binding
            if (DEVICE_SALT.contains("PLACEHOLDER") || DEVICE_SALT.isBlank()) {
                return baseKeyB64
            }

            val baseKey  = Base64.decode(baseKeyB64, Base64.NO_WRAP)
            val salt     = Base64.decode(DEVICE_SALT, Base64.NO_WRAP)
            val deviceId = getDeviceIdentifier(context)

            // Hash: SHA-256(deviceId + salt)
            val digest   = MessageDigest.getInstance("SHA-256")
            digest.update(deviceId.toByteArray(Charsets.UTF_8))
            digest.update(salt)
            val deviceHash = digest.digest()  // 32 bytes

            // XOR base key with device hash → final key
            val finalKey = ByteArray(32)
            for (i in 0 until 32) {
                finalKey[i] = (baseKey[i].toInt() xor deviceHash[i].toInt()).toByte()
            }

            Base64.encodeToString(finalKey, Base64.NO_WRAP)

        } catch (e: Exception) {
            // On failure return base key unchanged
            baseKeyB64
        }
    }

    /**
     * Build stable device identifier string.
     * Uses ANDROID_ID + model + manufacturer.
     * Stable across reboots, does not require phone permissions.
     */
    private fun getDeviceIdentifier(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""
        } catch (e: Exception) { "" }

        val model        = Build.MODEL        ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val fingerprint  = Build.FINGERPRINT  ?: ""

        return "$androidId|$model|$manufacturer|$fingerprint"
    }
}
