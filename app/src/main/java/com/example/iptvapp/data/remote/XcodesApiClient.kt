package com.example.iptvapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class XcodesConnectionStatus(
    val username: String,
    val status: String,
    val authenticated: Boolean,
    val activeConnections: Int?,
    val maxConnections: Int?,
    val expiresAtEpochSeconds: Long?
)

class XcodesApiClient {
    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<XcodesConnectionStatus> = withContext(Dispatchers.IO) {
        runCatching {
            require(serverUrl.isNotBlank()) { "Server URL is required" }
            require(username.isNotBlank()) { "Username is required" }
            require(password.isNotBlank()) { "Password is required" }

            val endpoint = buildPlayerApiUrl(serverUrl, username, password)
            val connection = (endpoint.toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "StreamHubTV/1.0")
            }

            try {
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use(BufferedReader::readText)
                if (responseCode !in 200..299) {
                    error("Server returned HTTP $responseCode")
                }

                parseConnectionStatus(body).also { status ->
                    if (!status.authenticated || !status.status.equals("Active", ignoreCase = true)) {
                        error("Account status is ${status.status.ifBlank { "inactive" }}")
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun buildPlayerApiUrl(serverUrl: String, username: String, password: String): URI {
        val normalizedBase = serverUrl.trim().trimEnd('/')
        val baseWithScheme = if ("://" in normalizedBase) normalizedBase else "http://$normalizedBase"
        val encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8.name())
        val encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.name())
        return URI.create("$baseWithScheme/player_api.php?username=$encodedUsername&password=$encodedPassword")
    }

    private fun parseConnectionStatus(body: String): XcodesConnectionStatus {
        val root = JSONObject(body)
        val userInfo = root.optJSONObject("user_info") ?: error("Missing user_info in XCODES response")
        return XcodesConnectionStatus(
            username = userInfo.optString("username"),
            status = userInfo.optString("status"),
            authenticated = userInfo.optInt("auth", 0) == 1,
            activeConnections = userInfo.optNullableInt("active_cons"),
            maxConnections = userInfo.optNullableInt("max_connections"),
            expiresAtEpochSeconds = userInfo.optNullableLong("exp_date")
        )
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        val value = optString(name, "")
        return value.toIntOrNull()
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        val value = optString(name, "")
        return value.toLongOrNull()
    }
}
