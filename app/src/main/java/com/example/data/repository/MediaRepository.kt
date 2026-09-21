package com.example.data.repository

import com.example.data.scraper.ScraperManager
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform

class MediaRepository(
    private val scraperManager: ScraperManager
) {
    fun cleanUrl(rawUrl: String): String = scraperManager.cleanUrl(rawUrl)

    fun detectPlatform(url: String): MediaPlatform = scraperManager.detectPlatform(url)

    fun getSupportedPlatforms(): List<MediaPlatform> = scraperManager.getAllSupportedPlatforms()

    suspend fun extractMedia(url: String): Result<MediaInfo> {
        return scraperManager.extract(url)
    }
}
