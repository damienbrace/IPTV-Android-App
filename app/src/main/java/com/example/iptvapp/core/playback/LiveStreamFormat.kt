package com.example.iptvapp.core.playback

enum class LiveStreamFormat(val label: String) {
    HLS("HLS"),
    MPEG_TS("MPEG-TS")
}

private val liveStreamExtensionPattern = Regex(
    pattern = """\.(m3u8|ts)(?=([?#]|$))""",
    option = RegexOption.IGNORE_CASE
)

internal fun resolveLiveStreamUrl(
    streamUrl: String,
    format: LiveStreamFormat
): String {
    val extension = when (format) {
        LiveStreamFormat.HLS -> ".m3u8"
        LiveStreamFormat.MPEG_TS -> ".ts"
    }
    return liveStreamExtensionPattern.replace(streamUrl, extension)
}

internal fun supportsLiveStreamFormatSwitch(streamUrl: String): Boolean {
    return liveStreamExtensionPattern.containsMatchIn(streamUrl)
}
