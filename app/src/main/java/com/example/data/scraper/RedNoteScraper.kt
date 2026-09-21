package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import org.json.JSONObject

class RedNoteScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.REDNOTE

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("xiaohongshu.com") || lower.contains("xhslink.com")
    }

    override fun getSupportedSources(): List<String> = listOf("Cobalt", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        if (!backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "rednote")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.REDNOTE)
        }

        try {
            val endpoint = "https://api.cobalt.tools/api/json"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("vQuality", "max")
                put("filenamePattern", "basic")
            }.toString()

            val response = client.postJson(endpoint, requestJson)
            val json = JSONObject(response)

            val downloadUrl = json.optString("url")
            if (downloadUrl.isBlank()) throw ScraperError.ExtractionFailed("RedNote membutuhkan backend service untuk ekstraksi langsung")

            val filename = json.optString("filename", "rednote_media.mp4")
            val isVideo = filename.endsWith(".mp4")

            MediaInfo(
                platform = MediaPlatform.REDNOTE,
                originalUrl = url,
                title = "RedNote (Xiaohongshu) Media",
                author = "RedNote Creator",
                thumbnail = if (!isVideo) downloadUrl else "",
                mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                downloads = listOf(
                    DownloadOption(
                        id = "rednote_opt",
                        type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                        format = if (isVideo) "MP4" else "JPG",
                        quality = "HD",
                        url = downloadUrl,
                        filename = filename,
                        source = "Cobalt"
                    )
                )
            )
        } catch (e: Exception) {
            if (e is ScraperError) throw e
            throw ScraperError.ExtractionFailed("Platform RedNote memerlukan Backend Service atau server extraction yang aktif.", e)
        }
    }
}
