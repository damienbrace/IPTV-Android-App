package com.example.iptvapp.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class XcodesApiClientTest {
    private val client = XcodesApiClient()

    @Test
    fun buildsLiveStreamUrlWithSchemeAndEncodedCredentials() {
        val url = client.buildLiveStreamUrl(
            serverUrl = "server.example.com:8080/",
            username = "user name",
            password = "p@ss word",
            streamId = 42
        )

        assertEquals(
            "http://server.example.com:8080/live/user+name/p%40ss+word/42.m3u8",
            url
        )
    }

    @Test
    fun buildsMovieStreamUrlWithContainerExtension() {
        val url = client.buildMovieStreamUrl(
            serverUrl = "server.example.com:8080/",
            username = "user name",
            password = "p@ss word",
            streamId = 99,
            containerExtension = "mkv"
        )

        assertEquals(
            "http://server.example.com:8080/movie/user+name/p%40ss+word/99.mkv",
            url
        )
    }

    @Test
    fun movieStreamUrlDefaultsToMp4() {
        val url = client.buildMovieStreamUrl(
            serverUrl = "server.example.com",
            username = "user",
            password = "pass",
            streamId = 100,
            containerExtension = null
        )

        assertEquals("http://server.example.com/movie/user/pass/100.mp4", url)
    }

    @Test
    fun buildsLogoUrlFromRelativeProviderPath() {
        val url = client.buildLogoUrl(
            serverUrl = "server.example.com:8080/",
            logoUrl = "/images/channel logo.png"
        )

        assertEquals("http://server.example.com:8080/images/channel%20logo.png", url)
    }

    @Test
    fun blankLogoUrlBecomesNull() {
        assertNull(client.buildLogoUrl("server.example.com", "null"))
        assertNull(client.buildLogoUrl("server.example.com", " "))
    }

    @Test
    fun parsesConnectionStatus() {
        val status = client.parseConnectionStatus(
            """
            {
              "user_info": {
                "username": "user123",
                "status": "Active",
                "auth": 1,
                "active_cons": "1",
                "max_connections": "3",
                "exp_date": "1893456000"
              }
            }
            """.trimIndent()
        )

        assertEquals("user123", status.username)
        assertEquals("Active", status.status)
        assertEquals(true, status.authenticated)
        assertEquals(1, status.activeConnections)
        assertEquals(3, status.maxConnections)
        assertEquals(1_893_456_000L, status.expiresAtEpochSeconds)
    }

    @Test
    fun parsesShortEpgAndDecodesBase64Text() {
        val title = Base64.getEncoder().encodeToString("Morning News".toByteArray())
        val description = Base64.getEncoder().encodeToString("Top stories".toByteArray())
        val programs = client.parseShortEpgBody(
            """
            {
              "epg_listings": [
                {
                  "title": "$title",
                  "description": "$description",
                  "start_timestamp": "1710000000",
                  "stop_timestamp": "1710003600"
                }
              ]
            }
            """.trimIndent(),
            streamId = 7
        )

        assertEquals(1, programs.size)
        assertEquals(7, programs.first().streamId)
        assertEquals("Morning News", programs.first().title)
        assertEquals("Top stories", programs.first().description)
        assertEquals(1_710_000_000_000L, programs.first().startsAtEpochMillis)
        assertEquals(1_710_003_600_000L, programs.first().endsAtEpochMillis)
    }

    @Test
    fun blankDecodedTitleIsIgnored() {
        val programs = client.parseShortEpgBody(
            """
            {
              "epg_listings": [
                {
                  "title": "",
                  "description": "",
                  "start_timestamp": "1710000000",
                  "stop_timestamp": "1710003600"
                }
              ]
            }
            """.trimIndent(),
            streamId = 9
        )

        assertEquals(0, programs.size)
    }

    @Test
    fun placeholderEpgTitleIsIgnored() {
        val title = Base64.getEncoder().encodeToString("EPG".toByteArray())
        val programs = client.parseShortEpgBody(
            """
            {
              "epg_listings": [
                {
                  "title": "$title",
                  "description": "placeholder",
                  "start_timestamp": "1710000000",
                  "stop_timestamp": "1710003600"
                }
              ]
            }
            """.trimIndent(),
            streamId = 9
        )

        assertEquals(0, programs.size)
    }

    @Test
    fun parsesXmltvProgramsForTargetChannelsOnly() {
        val programs = client.parseXmltvBody(
            """
            <tv>
              <programme channel="sky.sports.main" start="20260607160000 +0000" stop="20260607170000 +0000">
                <title>Live Premier League</title>
                <desc>Match coverage</desc>
              </programme>
              <programme channel="other.channel" start="20260607160000 +0000" stop="20260607170000 +0000">
                <title>Should not import</title>
              </programme>
            </tv>
            """.trimIndent(),
            targetChannelIds = setOf("sky.sports.main"),
            windowStartEpochMillis = 1_780_844_000_000L,
            windowEndEpochMillis = 1_780_856_000_000L
        )

        assertEquals(1, programs.size)
        assertEquals("sky.sports.main", programs.first().channelId)
        assertNull(programs.first().channelName)
        assertEquals("Live Premier League", programs.first().title)
        assertEquals("Match coverage", programs.first().description)
        assertEquals(1_780_848_000_000L, programs.first().startsAtEpochMillis)
        assertEquals(1_780_851_600_000L, programs.first().endsAtEpochMillis)
    }

    @Test
    fun parsesXmltvProgramsMatchedByDisplayName() {
        val programs = client.parseXmltvBody(
            """
            <tv>
              <channel id="provider-sky-main-event">
                <display-name>UK | Sky Sports Main Event UHD</display-name>
              </channel>
              <programme channel="provider-sky-main-event" start="20260607160000 +0000" stop="20260607170000 +0000">
                <title>Formula 1 Live</title>
              </programme>
            </tv>
            """.trimIndent(),
            targetChannelIds = emptySet(),
            targetChannelNames = setOf("Sky Sports Main Event"),
            windowStartEpochMillis = 1_780_844_000_000L,
            windowEndEpochMillis = 1_780_856_000_000L
        )

        assertEquals(1, programs.size)
        assertEquals("provider-sky-main-event", programs.first().channelId)
        assertEquals("UK | Sky Sports Main Event UHD", programs.first().channelName)
        assertEquals("Formula 1 Live", programs.first().title)
    }

    @Test
    fun parsesXmltvTimeWithoutOffsetAsUtc() {
        val programs = client.parseXmltvBody(
            """
            <tv>
              <programme channel="sky.sports.main" start="20260607160000" stop="20260607170000">
                <title>Formula 1 Live</title>
              </programme>
            </tv>
            """.trimIndent(),
            targetChannelIds = setOf("sky.sports.main"),
            windowStartEpochMillis = 1_780_844_000_000L,
            windowEndEpochMillis = 1_780_856_000_000L
        )

        assertEquals(1, programs.size)
        assertEquals(1_780_848_000_000L, programs.first().startsAtEpochMillis)
        assertEquals(1_780_851_600_000L, programs.first().endsAtEpochMillis)
    }

    @Test
    fun xmltvPlaceholderTitleIsIgnored() {
        val programs = client.parseXmltvBody(
            """
            <tv>
              <programme channel="sky.sports.main" start="20260607160000 +0000" stop="20260607170000 +0000">
                <title>EPG</title>
              </programme>
            </tv>
            """.trimIndent(),
            targetChannelIds = setOf("sky.sports.main"),
            windowStartEpochMillis = 1_780_844_000_000L,
            windowEndEpochMillis = 1_780_856_000_000L
        )

        assertEquals(0, programs.size)
    }
}
