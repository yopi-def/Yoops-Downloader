package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import org.json.JSONObject

class ThreadsScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.THREADS

    override fun canHandle(url: String): Boolean {
        return url.lowercase().contains("threads.net")
    }

    override fun getSupportedSources(): List<String> = listOf("Cobalt", "DirectOEmbed", "Backend")

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
                put("platform", "threads")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.THREADS)
        }

        try {
            extractWithCobalt(url, backendUrl)
        } catch (e: Exception) {
            if (e is ScraperError) throw e
            throw ScraperError.ExtractionFailed("Gagal mengekstrak media dari Threads", e)
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

        val downloadUrl = json.optString("url")
        if (downloadUrl.isBlank()) throw ScraperError.ExtractionFailed("Tidak ada video/gambar yang ditemukan pada postingan Threads")

        val filename = json.optString("filename", "threads_media.mp4")
        val isVideo = filename.endsWith(".mp4")

        return MediaInfo(
            platform = MediaPlatform.THREADS,
            originalUrl = url,
            title = "Postingan Threads",
            author = "Threads User",
            thumbnail = if (!isVideo) downloadUrl else "",
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
            downloads = listOf(
                DownloadOption(
                    id = "threads_cobalt_opt",
                    type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                    format = if (isVideo) "MP4" else "JPG",
                    quality = "Original",
                    url = downloadUrl,
                    filename = filename,
                    source = "Cobalt"
                )
            )
        )
    }
}
