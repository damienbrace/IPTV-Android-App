package com.example.iptvapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
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

data class XcodesCategory(
    val id: String,
    val name: String
)

data class XcodesLiveStream(
    val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String?,
    val epgChannelId: String?
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

            val body = getBody(buildPlayerApiUrl(serverUrl, username, password))
            parseConnectionStatus(body).also { status ->
                if (!status.authenticated || !status.status.equals("Active", ignoreCase = true)) {
                    error("Account status is ${status.status.ifBlank { "inactive" }}")
                }
            }
        }
    }

    suspend fun fetchLiveCategories(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<XcodesCategory>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = getBody(buildPlayerApiUrl(serverUrl, username, password, "get_live_categories"))
            val categories = org.json.JSONArray(body)
            buildList {
                for (index in 0 until categories.length()) {
                    val item = categories.getJSONObject(index)
                    add(
                        XcodesCategory(
                            id = item.optString("category_id"),
                            name = item.optString("category_name")
                        )
                    )
                }
            }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
        }
    }

    suspend fun fetchLiveStreams(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<XcodesLiveStream>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = getBody(buildPlayerApiUrl(serverUrl, username, password, "get_live_streams"))
            val streams = org.json.JSONArray(body)
            buildList {
                for (index in 0 until streams.length()) {
                    val item = streams.getJSONObject(index)
                    val streamId = item.optInt("stream_id", -1)
                    if (streamId > 0) {
                        add(
                            XcodesLiveStream(
                                streamId = streamId,
                                name = item.optString("name"),
                                categoryId = item.optString("category_id"),
                                streamIcon = item.optString("stream_icon").ifBlank { null },
                                epgChannelId = item.optString("epg_channel_id").ifBlank { null }
                            )
                        )
                    }
                }
            }.filter { it.name.isNotBlank() }
        }
    }

    fun buildLiveStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int
    ): String {
        return "${normalizeServerUrl(serverUrl)}/live/${encode(username)}/${encode(password)}/$streamId.m3u8"
    }

    private fun buildPlayerApiUrl(
        serverUrl: String,
        username: String,
        password: String,
        action: String? = null
    ): URI {
        val actionQuery = action?.let { "&action=$it" } ?: ""
        return URI.create(
            "${normalizeServerUrl(serverUrl)}/player_api.php?username=${encode(username)}&password=${encode(password)}$actionQuery"
        )
    }

    private fun getBody(endpoint: URI): String {
        val connection = (endpoint.toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "StreamHubTV/1.0")
        }

        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use(BufferedReader::readText)
            if (responseCode !in 200..299) {
                error("Server returned HTTP $responseCode")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeServerUrl(serverUrl: String): String {
        val normalizedBase = serverUrl.trim().trimEnd('/')
        return if ("://" in normalizedBase) normalizedBase else "http://$normalizedBase"
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
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
