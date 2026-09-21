package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

class PinterestScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.PINTEREST

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("pinterest.com") || lower.contains("pin.it")
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
                put("platform", "pinterest")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.PINTEREST)
        }

        try {
            extractWithCobalt(url, backendUrl)
        } catch (e: Exception) {
            try {
                extractWithOEmbed(url)
            } catch (fallbackEx: Exception) {
                if (e is ScraperError) throw e
                throw ScraperError.ExtractionFailed("Gagal mengekstrak pin Pinterest", fallbackEx)
            }
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
        if (downloadUrl.isBlank()) throw ScraperError.ExtractionFailed("Tidak ada file media yang ditemukan di Pinterest")

        val filename = json.optString("filename", "pinterest_media.jpg")
        val isVideo = filename.endsWith(".mp4")

        return MediaInfo(
            platform = MediaPlatform.PINTEREST,
            originalUrl = url,
            title = "Pinterest Media",
            author = "Pinterest Creator",
            thumbnail = if (!isVideo) downloadUrl else "",
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
            downloads = listOf(
                DownloadOption(
                    id = "pin_cobalt_opt",
                    type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                    format = if (isVideo) "MP4" else "JPG",
                    quality = "Original Quality",
                    url = downloadUrl,
                    filename = filename,
                    source = "Cobalt"
                )
            )
        )
    }

    private suspend fun extractWithOEmbed(url: String): MediaInfo {
        val oembedUrl = "https://www.pinterest.com/oembed.json?url=$url"
        val response = client.get(oembedUrl)
        val json = JSONObject(response)

        val title = json.optString("title", "Pin Pinterest")
        val author = json.optString("author_name", "Pinterest")
        val thumb = json.optString("thumbnail_url", "")
        if (thumb.isBlank()) throw ScraperError.MediaNotFound("Gambar Pinterest tidak ditemukan")

        val highRes = thumb.replace("/236x/", "/originals/").replace("/474x/", "/originals/").replace("/736x/", "/originals/")
        val baseFilename = FileUtils.sanitizeFilename(title, "pinterest_pin")

        return MediaInfo(
            platform = MediaPlatform.PINTEREST,
            originalUrl = url,
            title = title,
            author = author,
            thumbnail = highRes,
            mediaType = MediaType.IMAGE,
            downloads = listOf(
                DownloadOption(
                    id = "pin_orig_img",
                    type = MediaType.IMAGE,
                    format = "JPG",
                    quality = "High Resolution",
                    url = highRes,
                    filename = "${baseFilename}.jpg",
                    source = "DirectOEmbed"
                )
            )
        )
    }
}
