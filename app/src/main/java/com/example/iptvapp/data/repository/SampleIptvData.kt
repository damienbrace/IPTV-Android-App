package com.example.iptvapp.data.repository

import androidx.compose.ui.graphics.Color
import com.example.iptvapp.data.model.Channel

object SampleIptvData {
    val channels = listOf(
        Channel("seven-news", 1, "Seven News", "7", Color(0xFFE92B2B), "News", "7:00 - 8:00pm", 0.58f, sampleStream()),
        Channel("nine-news", 2, "9 News", "9", Color(0xFF2F9CFF), "News", "7:00 - 8:00pm", 0.58f, sampleStream()),
        Channel("ten-news", 3, "10 News First", "10", Color(0xFF286CFF), "News", "7:00 - 8:00pm", 0.58f, sampleStream()),
        Channel("abc-news", 4, "ABC News", "ABC", Color(0xFFF2F5FA), "News", "7:00 - 8:00pm", 0.54f, sampleStream(), favorite = true),
        Channel("sbs-world-news", 5, "SBS World News", "SBS", Color(0xFFF2F5FA), "News", "7:00 - 8:00pm", 0.57f, sampleStream(), favorite = true),
        Channel("sky-news-live", 6, "Sky News Live", "sky", Color(0xFFE4E8F0), "News", "7:00 - 8:00pm", 0.52f, sampleStream()),
        Channel("espn-live", 7, "ESPN Live", "ESPN", Color(0xFFFF3838), "Sports", "6:30 - 8:30pm", 0.64f, sampleStream()),
        Channel("fox-sports-503", 8, "Fox Sports 503", "FOX", Color(0xFFF2F5FA), "Sports", "7:00 - 9:00pm", 0.47f, sampleStream()),
        Channel("nickelodeon", 9, "Nickelodeon", "nick", Color(0xFFFF981F), "Kids", "7:00 - 8:00pm", 0.33f, sampleStream()),
        Channel("discovery-channel", 10, "Discovery Channel", "D", Color(0xFFDDE3EB), "Lifestyle", "7:00 - 8:00pm", 0.42f, sampleStream())
    )

    private fun sampleStream(): String {
        return "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"
    }
}
