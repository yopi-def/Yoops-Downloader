package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

class InstagramScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.INSTAGRAM

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("instagram.com") || lower.contains("instagr.am")
    }

    override fun getSupportedSources(): List<String> = listOf("Cobalt", "FastDL", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        val preferred = sourcePreference ?: "Cobalt"

        if (preferred == "Backend" && !backendUrl.isNullOrBlank()) {
            return@runCatching extractFromBackend(url, backendUrl)
        }

        try {
            extractWithCobalt(url, backendUrl)
        } catch (e: Exception) {
            try {
                extractWithFastDl(url)
            } catch (fallbackEx: Exception) {
                if (e is ScraperError) throw e
                throw ScraperError.ExtractionFailed("Gagal mengekstrak Instagram. Pastikan akun tidak privat atau gunakan Backend service.", fallbackEx)
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

        val status = json.optString("status")
        if (status == "error") {
            val err = json.optJSONObject("text")?.optString("error") ?: "Gagal mengekstrak media Instagram"
            throw ScraperError.ExtractionFailed(err)
        }

        val picker = json.optJSONArray("picker")
        val downloads = mutableListOf<DownloadOption>()

        if (picker != null && picker.length() > 0) {
            for (i in 0 until picker.length()) {
                val item = picker.getJSONObject(i)
                val type = item.optString("type")
                val itemUrl = item.optString("url")
                val thumb = item.optString("thumb")

                val mediaType = if (type == "photo") MediaType.IMAGE else MediaType.VIDEO
                val ext = if (type == "photo") "jpg" else "mp4"

                downloads.add(
                    DownloadOption(
                        id = "ig_picker_$i",
                        type = mediaType,
                        format = ext.uppercase(),
                        quality = "Slide ${i + 1}",
                        url = itemUrl,
                        filename = "instagram_slide_${i + 1}.$ext",
                        source = "Cobalt"
                    )
                )
            }

            return MediaInfo(
                platform = MediaPlatform.INSTAGRAM,
                originalUrl = url,
                title = "Instagram Carousel",
                author = "Instagram User",
                thumbnail = picker.getJSONObject(0).optString("thumb", ""),
                mediaType = MediaType.CAROUSEL,
                downloads = downloads
            )
        }

        val singleUrl = json.optString("url")
        if (singleUrl.isBlank()) throw ScraperError.ExtractionFailed("Cobalt tidak mengembalikan link unduhan")

        val filename = json.optString("filename", "instagram_media.mp4")
        val isVideo = filename.endsWith(".mp4")

        return MediaInfo(
            platform = MediaPlatform.INSTAGRAM,
            originalUrl = url,
            title = "Instagram Media",
            author = "Instagram User",
            thumbnail = "",
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
            downloads = listOf(
                DownloadOption(
                    id = "ig_cobalt_single",
                    type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                    format = if (isVideo) "MP4" else "JPG",
                    quality = "Original",
                    url = singleUrl,
                    filename = filename,
                    source = "Cobalt"
                )
            )
        )
    }

    private suspend fun extractWithFastDl(url: String): MediaInfo {
        // FastDL public endpoint or OEmbed fallback
        val oembedUrl = "https://api.instagram.com/oembed/?url=$url"
        val response = client.get(oembedUrl)
        val json = JSONObject(response)

        val title = json.optString("title", "Instagram Post")
        val author = json.optString("author_name", "Instagram User")
        val thumbnail = json.optString("thumbnail_url", "")

        throw ScraperError.SourceUnavailable("Media memerlukan otentikasi atau backend service untuk tautan video langsung")
    }

    private suspend fun extractFromBackend(url: String, backendUrl: String): MediaInfo {
        val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
        val requestJson = JSONObject().apply {
            put("url", url)
            put("platform", "instagram")
        }.toString()
        val response = client.postJson(endpoint, requestJson)
        return parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.INSTAGRAM)
    }
}
