package com.example.data.scraper

import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform

interface MediaScraper {
    fun getPlatform(): MediaPlatform
    fun canHandle(url: String): Boolean
    fun getSupportedSources(): List<String>
    suspend fun extract(
        url: String,
        sourcePreference: String? = null,
        backendUrl: String? = null
    ): Result<MediaInfo>
}
