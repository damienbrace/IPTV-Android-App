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
    fun blankDecodedDescriptionBecomesNull() {
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

        assertEquals("Live Program", programs.first().title)
        assertNull(programs.first().description)
    }
}
