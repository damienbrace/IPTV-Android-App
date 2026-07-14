package com.example.iptvapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class StreamHubSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationShowsPrimaryScreens() {
        composeRule.onNodeWithText("Live TV").assertIsDisplayed()
        composeRule.onNodeWithTag("${TestTags.GroupRowPrefix}all-channels").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.SearchNav).performClick()
        composeRule.onNodeWithText("Search").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.PlaylistsNav).performClick()
        composeRule.onNodeWithText("Playlists").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.SettingsNav).performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun playlistAddFormOpensFromPlaylistsScreen() {
        composeRule.onNodeWithTag(TestTags.PlaylistsNav).performClick()
        composeRule.onNodeWithTag(TestTags.AddPlaylistAction).performClick()

        composeRule.onNodeWithTag(TestTags.AddPlaylistScreen).assertIsDisplayed()
        composeRule.onNodeWithText("XCODES Details").assertIsDisplayed()
        composeRule.onNodeWithText("Test Connection").assertIsDisplayed()
    }

    @Test
    fun liveChannelOpensPlayerAndReturns() {
        composeRule.onNodeWithTag("${TestTags.GroupRowPrefix}all-channels").performClick()
        composeRule.onNodeWithTag("${TestTags.ChannelRowPrefix}seven-news").performClick()

        composeRule.onNodeWithTag(TestTags.PlayerScreen).assertIsDisplayed()
        composeRule.onNodeWithText("LIVE").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.PlayerBack).performClick()
        composeRule.onNodeWithTag("${TestTags.ChannelRowPrefix}seven-news").assertIsDisplayed()
    }

    @Test
    fun systemBackReturnsToPreviousPrimaryScreen() {
        composeRule.onNodeWithTag(TestTags.SearchNav).performClick()
        composeRule.onNodeWithTag(TestTags.PlaylistsNav).performClick()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("Recent Searches").assertIsDisplayed()
    }

    @Test
    fun settingsShowsDiagnosticsSection() {
        composeRule.onNodeWithTag(TestTags.SettingsNav).performClick()

        composeRule.onNodeWithText("Diagnostics").assertIsDisplayed()
        composeRule.onNodeWithText("Playback diagnostics will appear after a stream starts.").assertIsDisplayed()
    }
}
