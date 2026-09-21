package com.example.data.scraper

import com.example.data.local.SettingsDataStore
import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.ScraperError
import com.example.utils.UrlCleaner

class ScraperManager(
    private val client: ScraperNetworkClient,
    private val settingsDataStore: SettingsDataStore
) {
    private val scrapers: List<MediaScraper> = listOf(
        TikTokScraper(client),
        YouTubeScraper(client),
        TwitterScraper(client),
        InstagramScraper(client),
        FacebookScraper(client),
        PinterestScraper(client),
        ThreadsScraper(client),
        DouyinScraper(client),
        RedNoteScraper(client),
        BilibiliScraper(client),
        PixivScraper(client),
        SpotifyScraper(client),
        AppleMusicScraper(client),
        BandcampScraper(client)
    )

    fun cleanUrl(rawUrl: String): String = UrlCleaner.clean(rawUrl)

    fun detectPlatform(url: String): MediaPlatform {
        val cleaned = UrlCleaner.clean(url)
        val scraper = scrapers.firstOrNull { it.canHandle(cleaned) }
        return scraper?.getPlatform() ?: MediaPlatform.fromUrl(cleaned)
    }

    fun getScraperForUrl(url: String): MediaScraper? {
        val cleaned = UrlCleaner.clean(url)
        return scrapers.firstOrNull { it.canHandle(cleaned) }
    }

    fun getAllSupportedPlatforms(): List<MediaPlatform> {
        return scrapers.map { it.getPlatform() }
    }

    suspend fun extract(rawUrl: String): Result<MediaInfo> {
        if (!UrlCleaner.isValidUrl(rawUrl)) {
            return Result.failure(ScraperError.InvalidUrl())
        }

        val cleanedUrl = UrlCleaner.clean(rawUrl)
        val platform = detectPlatform(cleanedUrl)

        if (platform == MediaPlatform.UNKNOWN) {
            return Result.failure(ScraperError.UnsupportedPlatform())
        }

        val scraper = getScraperForUrl(cleanedUrl)
            ?: return Result.failure(ScraperError.UnsupportedPlatform())

        val settings = settingsDataStore.settings.value
        val preferredSource = when (platform) {
            MediaPlatform.TIKTOK -> settings.preferredTikTokSource
            MediaPlatform.YOUTUBE -> settings.preferredYouTubeSource
            MediaPlatform.TWITTER -> settings.preferredTwitterSource
            MediaPlatform.INSTAGRAM -> settings.preferredInstagramSource
            else -> null
        }

        return scraper.extract(
            url = cleanedUrl,
            sourcePreference = preferredSource,
            backendUrl = settings.customBackendUrl.ifBlank { null }
        )
    }
}
