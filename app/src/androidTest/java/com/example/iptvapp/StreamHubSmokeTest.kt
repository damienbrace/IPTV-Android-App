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
        composeRule.onNodeWithText("StreamHub TV").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.GuideNav).performClick()
        composeRule.onNodeWithText("TV Guide").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.SearchNav).performClick()
        composeRule.onNodeWithText("Search").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.PlaylistsNav).performClick()
        composeRule.onNodeWithText("Playlists").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.SettingsNav).performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
