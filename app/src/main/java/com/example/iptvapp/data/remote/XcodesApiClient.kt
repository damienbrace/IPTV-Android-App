package com.example.iptvapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.zip.GZIPInputStream
import javax.xml.parsers.SAXParserFactory

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

data class XcodesVodStream(
    val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String?,
    val containerExtension: String?
)

data class XcodesSeriesStream(
    val seriesId: Int,
    val name: String,
    val categoryId: String,
    val cover: String?
)

data class XcodesEpgProgram(
    val streamId: Int,
    val title: String,
    val description: String?,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long
)

data class XcodesXmltvProgram(
    val channelId: String,
    val channelName: String?,
    val title: String,
    val description: String?,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long
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
                                streamIcon = item.normalizedString("stream_icon"),
                                epgChannelId = item.normalizedString("epg_channel_id")
                            )
                        )
                    }
                }
            }.filter { it.name.isNotBlank() }
        }
    }

    suspend fun fetchVodCategories(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<XcodesCategory>> = fetchCategories(serverUrl, username, password, "get_vod_categories")

    suspend fun fetchSeriesCategories(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<XcodesCategory>> = fetchCategories(serverUrl, username, password, "get_series_categories")

    suspend fun fetchVodStreams(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<XcodesVodStream>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = getBody(buildPlayerApiUrl(serverUrl, username, password, "get_vod_streams"))
            val streams = org.json.JSONArray(body)
            buildList {
                for (index in 0 until streams.length()) {
                    val item = streams.getJSONObject(index)
                    val streamId = item.optInt("stream_id", -1)
                    if (streamId > 0) {
                        add(
                            XcodesVodStream(
                                streamId = streamId,
                                name = item.optString("name"),
                                categoryId = item.optString("category_id"),
                                streamIcon = item.normalizedString("stream_icon"),
                                containerExtension = item.normalizedString("container_extension")
                            )
                        )
                    }
                }
            }.filter { it.name.isNotBlank() }
        }
    }

    suspend fun fetchSeriesStreams(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<XcodesSeriesStream>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = getBody(buildPlayerApiUrl(serverUrl, username, password, "get_series"))
            val streams = org.json.JSONArray(body)
            buildList {
                for (index in 0 until streams.length()) {
                    val item = streams.getJSONObject(index)
                    val seriesId = item.optInt("series_id", -1)
                    if (seriesId > 0) {
                        add(
                            XcodesSeriesStream(
                                seriesId = seriesId,
                                name = item.optString("name"),
                                categoryId = item.optString("category_id"),
                                cover = item.normalizedString("cover")
                            )
                        )
                    }
                }
            }.filter { it.name.isNotBlank() }
        }
    }

    suspend fun fetchShortEpg(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        limit: Int = 4
    ): Result<List<XcodesEpgProgram>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = getBody(
                buildPlayerApiUrl(
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    action = "get_short_epg&stream_id=$streamId&limit=$limit"
                )
            )
            parseShortEpgBody(body, streamId)
        }
    }

    suspend fun fetchXmltvPrograms(
        serverUrl: String,
        username: String,
        password: String,
        targetChannelIds: Set<String>,
        targetChannelNames: Set<String>,
        windowStartEpochMillis: Long,
        windowEndEpochMillis: Long
    ): Result<List<XcodesXmltvProgram>> = withContext(Dispatchers.IO) {
        runCatching {
            if (targetChannelIds.isEmpty() && targetChannelNames.isEmpty()) return@runCatching emptyList()
            val connection = (buildXmltvUrl(serverUrl, username, password).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 45_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "StreamHubTV/1.0")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    error("XMLTV returned HTTP $responseCode")
                }
                parseXmltv(
                    source = InputSource(connection.decodedInputStream()),
                    targetChannelIds = targetChannelIds,
                    targetChannelNames = targetChannelNames,
                    windowStartEpochMillis = windowStartEpochMillis,
                    windowEndEpochMillis = windowEndEpochMillis
                )
            } finally {
                connection.disconnect()
            }
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

    fun buildMovieStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        containerExtension: String?
    ): String {
        val extension = containerExtension
            ?.trim()
            ?.trimStart('.')
            ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?: "mp4"
        return "${normalizeServerUrl(serverUrl)}/movie/${encode(username)}/${encode(password)}/$streamId.$extension"
    }

    fun buildLogoUrl(serverUrl: String, logoUrl: String?): String? {
        val value = logoUrl?.trim()
            ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?: return null
        val serverBase = normalizeServerUrl(serverUrl)
        val url = when {
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true) -> value
            value.startsWith("//") -> "${URI.create(serverBase).scheme}:$value"
            value.startsWith("/") -> "$serverBase$value"
            else -> "$serverBase/$value"
        }
        return url.replace(" ", "%20")
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

    private fun buildXmltvUrl(serverUrl: String, username: String, password: String): URI {
        return URI.create(
            "${normalizeServerUrl(serverUrl)}/xmltv.php?username=${encode(username)}&password=${encode(password)}"
        )
    }

    private suspend fun fetchCategories(
        serverUrl: String,
        username: String,
        password: String,
        action: String
    ): Result<List<XcodesCategory>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = getBody(buildPlayerApiUrl(serverUrl, username, password, action))
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

    internal fun parseConnectionStatus(body: String): XcodesConnectionStatus {
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

    internal fun parseShortEpgBody(body: String, streamId: Int): List<XcodesEpgProgram> {
        val root = JSONObject(body)
        val listings = root.optJSONArray("epg_listings") ?: return emptyList()
        return buildList {
            for (index in 0 until listings.length()) {
                val item = listings.getJSONObject(index)
                val start = item.optNullableLong("start_timestamp") ?: continue
                val stop = item.optNullableLong("stop_timestamp") ?: continue
                val title = item.optString("title").decodeMaybeBase64()
                if (!title.isUsableEpgTitle()) continue
                add(
                    XcodesEpgProgram(
                        streamId = streamId,
                        title = title,
                        description = item.optString("description").decodeMaybeBase64().ifBlank { null },
                        startsAtEpochMillis = start * 1_000L,
                        endsAtEpochMillis = stop * 1_000L
                    )
                )
            }
        }
    }

    internal fun parseXmltvBody(
        body: String,
        targetChannelIds: Set<String>,
        targetChannelNames: Set<String> = emptySet(),
        windowStartEpochMillis: Long,
        windowEndEpochMillis: Long
    ): List<XcodesXmltvProgram> {
        return parseXmltv(
            source = InputSource(StringReader(body)),
            targetChannelIds = targetChannelIds,
            targetChannelNames = targetChannelNames,
            windowStartEpochMillis = windowStartEpochMillis,
            windowEndEpochMillis = windowEndEpochMillis
        )
    }

    private fun parseXmltv(
        source: InputSource,
        targetChannelIds: Set<String>,
        targetChannelNames: Set<String>,
        windowStartEpochMillis: Long,
        windowEndEpochMillis: Long
    ): List<XcodesXmltvProgram> {
        val programs = mutableListOf<XcodesXmltvProgram>()
        val exactTargetChannelIds = targetChannelIds.filter { it.isNotBlank() }.toSet()
        val targetGuideKeys = (targetChannelIds + targetChannelNames)
            .map { it.normalizedGuideKey() }
            .filter { it.isNotBlank() }
            .toSet()
        val targetGuideKeysByToken = targetGuideKeys.groupBy { key -> key.substringBefore(' ') }
        val xmltvChannelNamesById = mutableMapOf<String, String>()
        val matchedXmltvChannelIds = mutableSetOf<String>()
        val parserFactory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
        }
        parserFactory.newSAXParser().parse(
            source,
            object : DefaultHandler() {
                private var includeProgramme = false
                private var channelId = ""
                private var startsAt = 0L
                private var endsAt = 0L
                private var title = ""
                private var description: String? = null
                private var inChannel = false
                private var xmltvChannelId = ""
                private var xmltvChannelDisplayName = ""
                private var activeTextElement: String? = null
                private val text = StringBuilder()

                override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                    when (xmlName(localName, qName)) {
                        "channel" -> {
                            inChannel = true
                            xmltvChannelId = attributes.getValue("id").orEmpty()
                            xmltvChannelDisplayName = ""
                        }
                        "display-name" -> if (inChannel) {
                            activeTextElement = "display-name"
                            text.clear()
                        }
                        "programme" -> {
                            channelId = attributes.getValue("channel").orEmpty()
                            startsAt = attributes.getValue("start")?.parseXmltvTime() ?: 0L
                            endsAt = attributes.getValue("stop")?.parseXmltvTime() ?: 0L
                            title = ""
                            description = null
                            includeProgramme = channelId in exactTargetChannelIds ||
                                channelId in matchedXmltvChannelIds ||
                                channelId.normalizedGuideKey().matchesGuideTarget(targetGuideKeys, targetGuideKeysByToken)
                            includeProgramme = includeProgramme &&
                                startsAt > 0L &&
                                endsAt > startsAt &&
                                endsAt >= windowStartEpochMillis &&
                                startsAt <= windowEndEpochMillis
                        }
                        "title",
                        "desc" -> if (includeProgramme) {
                            activeTextElement = xmlName(localName, qName)
                            text.clear()
                        }
                    }
                }

                override fun characters(ch: CharArray, start: Int, length: Int) {
                    if (activeTextElement != null) {
                        text.append(ch, start, length)
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    when (xmlName(localName, qName)) {
                        "display-name" -> {
                            if (activeTextElement == "display-name") {
                                xmltvChannelDisplayName = text.toString().trim()
                            }
                            activeTextElement = null
                        }
                        "channel" -> {
                            if (xmltvChannelId.isNotBlank()) {
                                if (xmltvChannelDisplayName.isNotBlank()) {
                                    xmltvChannelNamesById[xmltvChannelId] = xmltvChannelDisplayName
                                }
                                if (xmltvChannelId in exactTargetChannelIds ||
                                    xmltvChannelId.normalizedGuideKey().matchesGuideTarget(targetGuideKeys, targetGuideKeysByToken) ||
                                    xmltvChannelDisplayName.normalizedGuideKey().matchesGuideTarget(targetGuideKeys, targetGuideKeysByToken)
                                ) {
                                    matchedXmltvChannelIds += xmltvChannelId
                                }
                            }
                            inChannel = false
                            activeTextElement = null
                        }
                        "title" -> {
                            if (activeTextElement == "title") {
                                title = text.toString().trim()
                            }
                            activeTextElement = null
                        }
                        "desc" -> {
                            if (activeTextElement == "desc") {
                                description = text.toString().trim().ifBlank { null }
                            }
                            activeTextElement = null
                        }
                        "programme" -> {
                            if (includeProgramme && title.isUsableEpgTitle()) {
                                programs += XcodesXmltvProgram(
                                    channelId = channelId,
                                    channelName = xmltvChannelNamesById[channelId],
                                    title = title,
                                    description = description,
                                    startsAtEpochMillis = startsAt,
                                    endsAtEpochMillis = endsAt
                                )
                            }
                            includeProgramme = false
                            activeTextElement = null
                        }
                    }
                }
            }
        )
        return programs
    }

    private fun xmlName(localName: String?, qName: String?): String {
        return (localName?.takeIf { it.isNotBlank() } ?: qName.orEmpty())
            .substringAfter(':')
            .lowercase()
    }

    private fun HttpURLConnection.decodedInputStream(): InputStream {
        val stream = BufferedInputStream(inputStream).apply {
            mark(GZIP_HEADER_SIZE)
        }
        val firstByte = stream.read()
        val secondByte = stream.read()
        stream.reset()
        val isGzip = contentEncoding.equals("gzip", ignoreCase = true) ||
            (firstByte == GZIP_MAGIC_FIRST_BYTE && secondByte == GZIP_MAGIC_SECOND_BYTE)
        return if (isGzip) {
            GZIPInputStream(stream)
        } else {
            stream
        }
    }

    private fun String.parseXmltvTime(): Long? {
        val match = XmltvTimeRegex.find(trim()) ?: return null
        val localDateTime = LocalDateTime.parse(match.groupValues[1], XmltvDateTimeFormatter)
        val offset = match.groupValues.getOrNull(2)
            ?.takeIf { it.isNotBlank() }
            ?.let { ZoneOffset.of("${it.take(3)}:${it.takeLast(2)}") }
        return if (offset != null) {
            OffsetDateTime.of(localDateTime, offset).toInstant().toEpochMilli()
        } else {
            localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        }
    }

    private fun String.normalizedGuideKey(): String {
        return lowercase()
            .replace("&", " and ")
            .replace(Regex("\\b(uk|us|usa|ca|au|nz|ie)\\s*\\|"), " ")
            .replace(Regex("\\b(fhd|hd|uhd|sd|hevc|h265|50fps|60fps)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.matchesGuideTarget(
        targetGuideKeys: Set<String>,
        targetGuideKeysByToken: Map<String, List<String>>
    ): Boolean {
        if (isBlank()) return false
        if (this in targetGuideKeys) return true
        val candidates = split(' ')
            .asSequence()
            .flatMap { targetGuideKeysByToken[it].orEmpty().asSequence() }
            .distinct()
        return candidates.any { candidate ->
            candidate.length >= MIN_FUZZY_GUIDE_KEY_LENGTH &&
                (contains(candidate) || candidate.contains(this))
        }
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        val value = optString(name, "")
        return value.toIntOrNull()
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        val value = optString(name, "")
        return value.toLongOrNull()
    }

    private fun JSONObject.normalizedString(name: String): String? {
        return optString(name)
            .trim()
            .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
    }

    private fun String.decodeMaybeBase64(): String {
        return runCatching {
            String(Base64.getDecoder().decode(this), Charsets.UTF_8)
        }.getOrElse { this }.trim()
    }

    private fun String.isUsableEpgTitle(): Boolean {
        val normalized = lowercase().replace(Regex("\\s+"), " ").trim()
        return normalized.isNotBlank() &&
            normalized !in setOf(
                "epg",
                "no epg",
                "no info",
                "no information",
                "no programme information",
                "program information not available",
                "programme information not available",
                "not available",
                "n/a",
                "na"
            )
    }

    private companion object {
        const val GZIP_HEADER_SIZE = 2
        const val GZIP_MAGIC_FIRST_BYTE = 0x1f
        const val GZIP_MAGIC_SECOND_BYTE = 0x8b
        const val MIN_FUZZY_GUIDE_KEY_LENGTH = 8
        val XmltvDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val XmltvTimeRegex = Regex("^(\\d{14})(?:\\s*([+-]\\d{2}:?\\d{2}))?.*")
    }
}
