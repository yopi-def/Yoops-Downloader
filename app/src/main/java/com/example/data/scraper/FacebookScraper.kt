package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import org.json.JSONObject

class FacebookScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.FACEBOOK

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com")
    }

    override fun getSupportedSources(): List<String> = listOf("Cobalt", "SnapSave", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        val preferred = sourcePreference ?: "Cobalt"

        if (preferred == "Backend" && !backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "facebook")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.FACEBOOK)
        }

        try {
            extractWithCobalt(url, backendUrl)
        } catch (e: Exception) {
            if (e is ScraperError) throw e
            throw ScraperError.ExtractionFailed("Gagal mengekstrak video Facebook. Pastikan video publik.", e)
        }
    }

    private suspend fun extractWithCobalt(url: String, backendUrl: String?): MediaInfo {
        val endpoint = if (!backendUrl.isNullOrBlank()) {
            "${backendUrl.trimEnd('/')}/api/json"
        } else {
            "https://api.cobalt.tools/api/json"
        }

        val requestJson = JSONObject().apply {
            put("url", url)
            put("vQuality", "max")
            put("filenamePattern", "basic")
        }.toString()

        val response = client.postJson(endpoint, requestJson)
        val json = JSONObject(response)

        val status = json.optString("status")
        if (status == "error") {
            val err = json.optJSONObject("text")?.optString("error") ?: "Gagal mengekstrak video Facebook"
            throw ScraperError.ExtractionFailed(err)
        }

        val downloadUrl = json.optString("url")
        if (downloadUrl.isBlank()) throw ScraperError.ExtractionFailed("Cobalt tidak memberikan URL unduhan")

        val filename = json.optString("filename", "facebook_video.mp4")

        return MediaInfo(
            platform = MediaPlatform.FACEBOOK,
            originalUrl = url,
            title = "Video Facebook",
            author = "Facebook User",
            thumbnail = "",
            mediaType = MediaType.VIDEO,
            downloads = listOf(
                DownloadOption(
                    id = "fb_cobalt_vid",
                    type = MediaType.VIDEO,
                    format = "MP4",
                    quality = "HD Video",
                    url = downloadUrl,
                    filename = filename,
                    source = "Cobalt"
                )
            )
        )
    }
}
