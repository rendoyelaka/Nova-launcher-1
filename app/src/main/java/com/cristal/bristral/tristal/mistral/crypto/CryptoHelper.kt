package com.cristal.bristral.tristal.mistral.crypto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * CryptoHelper — AES-256-GCM decryption + HMAC-SHA256 verification.
 *
 * Encrypted payload format (produced by bot):
 *   [12 bytes IV] [N bytes ciphertext] [16 bytes GCM auth tag]
 *
 * All keys are base64-encoded strings patched into dex by bot.
 */
object CryptoHelper {

    private const val AES_ALGORITHM   = "AES/GCM/NoPadding"
    private const val HMAC_ALGORITHM  = "HmacSHA256"
    private const val GCM_IV_LENGTH   = 12   // 96-bit nonce
    private const val GCM_TAG_LENGTH  = 128  // bits

    /**
     * Decrypt AES-256-GCM encrypted payload bytes.
     * @param encryptedData  raw bytes: [12B IV | ciphertext+tag]
     * @param keyB64         base64-encoded 32-byte AES key (patched by bot)
     * @return decrypted plaintext bytes or null on failure
     */
    fun decrypt(encryptedData: ByteArray, keyB64: String): ByteArray? {
        return try {
            val keyBytes = Base64.decode(keyB64, Base64.NO_WRAP)
            if (keyBytes.size != 32) return null

            val iv          = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText  = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

            val secretKey   = SecretKeySpec(keyBytes, "AES")
            val gcmSpec     = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher      = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verify HMAC-SHA256 of encrypted payload.
     * Must be called BEFORE decryption.
     * @param data       the encrypted payload bytes
     * @param hmacB64    expected HMAC base64 (patched by bot)
     * @param keyB64     base64-encoded AES key (used as HMAC key too)
     * @return true if HMAC matches — payload is untampered
     */
    fun verifyHmac(data: ByteArray, hmacB64: String, keyB64: String): Boolean {
        return try {
            val keyBytes     = Base64.decode(keyB64, Base64.NO_WRAP)
            val expectedHmac = Base64.decode(hmacB64, Base64.NO_WRAP)
            val secretKey    = SecretKeySpec(keyBytes, HMAC_ALGORITHM)
            val mac          = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(secretKey)
            val computedHmac = mac.doFinal(data)
            constantTimeEquals(computedHmac, expectedHmac)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Constant-time byte array comparison — prevents timing attacks.
     */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
