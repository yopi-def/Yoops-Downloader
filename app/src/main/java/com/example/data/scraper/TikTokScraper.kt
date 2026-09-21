package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject
import java.net.URLEncoder

class TikTokScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.TIKTOK

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("tiktok.com") || lower.contains("vt.tiktok.com") || lower.contains("vm.tiktok.com")
    }

    override fun getSupportedSources(): List<String> = listOf("TikWM", "Cobalt", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        val preferred = sourcePreference ?: "TikWM"

        if (preferred == "Backend" && !backendUrl.isNullOrBlank()) {
            return@runCatching extractFromBackend(url, backendUrl)
        }

        // Try Primary Source (TikWM)
        try {
            extractWithTikWm(url)
        } catch (e: Exception) {
            // Fallback Source: Cobalt
            try {
                extractWithCobalt(url, backendUrl)
            } catch (fallbackEx: Exception) {
                if (e is ScraperError) throw e
                throw ScraperError.ExtractionFailed("Gagal mengekstrak TikTok melalui semua source yang tersedia", fallbackEx)
            }
        }
    }

    private suspend fun extractWithTikWm(url: String): MediaInfo {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val apiUrl = "https://www.tikwm.com/api/?url=$encodedUrl&hd=1"
        val response = client.get(apiUrl, userAgent = ScraperNetworkClient.MOBILE_USER_AGENT)

        val json = JSONObject(response)
        val code = json.optInt("code", -1)
        if (code != 0) {
            val msg = json.optString("msg", "Gagal memproses link TikTok")
            throw ScraperError.ExtractionFailed("TikWM: $msg")
        }

        val data = json.optJSONObject("data") ?: throw ScraperError.MediaNotFound("Konten TikTok tidak ditemukan")
        val title = data.optString("title", "TikTok Video").ifBlank { "TikTok Video" }
        val authorObj = data.optJSONObject("author")
        val author = authorObj?.optString("nickname", "TikTok User") ?: "TikTok User"
        val thumbnail = data.optString("cover", "")
        val duration = data.optLong("duration", 0L)

        val downloads = mutableListOf<DownloadOption>()
        val baseFilename = FileUtils.sanitizeFilename(title, "tiktok_video")

        val hdPlay = data.optString("hdplay", "")
        val play = data.optString("play", "")
        val music = data.optString("music", "")
        val images = data.optJSONArray("images")

        var mediaType = MediaType.VIDEO

        if (images != null && images.length() > 0) {
            mediaType = MediaType.CAROUSEL
            for (i in 0 until images.length()) {
                val imgUrl = images.getString(i)
                downloads.add(
                    DownloadOption(
                        id = "tiktok_img_$i",
                        type = MediaType.IMAGE,
                        format = "JPG",
                        quality = "Slide ${i + 1}",
                        url = imgUrl,
                        filename = "${baseFilename}_slide_${i + 1}.jpg",
                        source = "TikWM"
                    )
                )
            }
        } else {
            if (hdPlay.isNotBlank()) {
                val fullUrl = if (hdPlay.startsWith("http")) hdPlay else "https://www.tikwm.com$hdPlay"
                downloads.add(
                    DownloadOption(
                        id = "tiktok_hd",
                        type = MediaType.VIDEO,
                        format = "MP4",
                        quality = "HD (No Watermark)",
                        url = fullUrl,
                        filename = "${baseFilename}_hd.mp4",
                        source = "TikWM"
                    )
                )
            }

            if (play.isNotBlank()) {
                val fullUrl = if (play.startsWith("http")) play else "https://www.tikwm.com$play"
                downloads.add(
                    DownloadOption(
                        id = "tiktok_sd",
                        type = MediaType.VIDEO,
                        format = "MP4",
                        quality = "Standard (No Watermark)",
                        url = fullUrl,
                        filename = "${baseFilename}.mp4",
                        source = "TikWM"
                    )
                )
            }
        }

        if (music.isNotBlank()) {
            val musicUrl = if (music.startsWith("http")) music else "https://www.tikwm.com$music"
            downloads.add(
                DownloadOption(
                    id = "tiktok_audio",
                    type = MediaType.AUDIO,
                    format = "MP3",
                    quality = "Audio Only",
                    url = musicUrl,
                    filename = "${baseFilename}_audio.mp3",
                    source = "TikWM"
                )
            )
        }

        if (downloads.isEmpty()) {
            throw ScraperError.ExtractionFailed("Tidak ditemukan media yang dapat diunduh")
        }

        return MediaInfo(
            platform = MediaPlatform.TIKTOK,
            originalUrl = url,
            title = title,
            author = author,
            thumbnail = thumbnail,
            duration = if (duration > 0) duration else null,
            description = title,
            mediaType = mediaType,
            downloads = downloads
        )
    }

    private suspend fun extractWithCobalt(url: String, backendUrl: String?): MediaInfo {
        val endpoint = if (!backendUrl.isNullOrBlank()) {
            "$backendUrl/api/json"
        } else {
            "https://api.cobalt.tools/api/json"
        }

        val requestJson = JSONObject().apply {
            put("url", url)
            put("vQuality", "max")
            put("filenamePattern", "basic")
        }.toString()

        val response = client.postJson(
            url = endpoint,
            jsonBody = requestJson,
            headers = mapOf("Accept" to "application/json")
        )

        val json = JSONObject(response)
        val status = json.optString("status")

        if (status == "error") {
            val text = json.optJSONObject("text")?.optString("error") ?: "Gagal mengekstrak melalui Cobalt"
            throw ScraperError.ExtractionFailed(text)
        }

        val downloadUrl = json.optString("url")
        if (downloadUrl.isBlank()) {
            throw ScraperError.ExtractionFailed("Cobalt tidak mengembalikan URL unduhan")
        }

        val filename = json.optString("filename", "tiktok_video.mp4")
        val downloads = listOf(
            DownloadOption(
                id = "cobalt_video",
                type = MediaType.VIDEO,
                format = "MP4",
                quality = "Best Quality",
                url = downloadUrl,
                filename = filename,
                source = "Cobalt"
            )
        )

        return MediaInfo(
            platform = MediaPlatform.TIKTOK,
            originalUrl = url,
            title = "TikTok Video",
            author = "TikTok User",
            thumbnail = "",
            mediaType = MediaType.VIDEO,
            downloads = downloads
        )
    }

    private suspend fun extractFromBackend(url: String, backendUrl: String): MediaInfo {
        val cleanEndpoint = backendUrl.trimEnd('/') + "/api/extract"
        val requestJson = JSONObject().apply {
            put("url", url)
            put("platform", "tiktok")
        }.toString()

        val response = client.postJson(cleanEndpoint, requestJson)
        val json = JSONObject(response)
        return parseStandardBackendResponse(json, url, MediaPlatform.TIKTOK)
    }
}
