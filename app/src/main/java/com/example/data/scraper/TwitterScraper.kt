package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject
import java.net.URI

class TwitterScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.TWITTER

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("twitter.com") || lower.contains("x.com")
    }

    override fun getSupportedSources(): List<String> = listOf("VxTwitter", "Cobalt", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        val preferred = sourcePreference ?: "VxTwitter"

        if (preferred == "Backend" && !backendUrl.isNullOrBlank()) {
            return@runCatching extractFromBackend(url, backendUrl)
        }

        try {
            extractWithVxTwitter(url)
        } catch (e: Exception) {
            try {
                extractWithCobalt(url, backendUrl)
            } catch (fallbackEx: Exception) {
                if (e is ScraperError) throw e
                throw ScraperError.ExtractionFailed("Gagal mengekstrak media dari Twitter/X", fallbackEx)
            }
        }
    }

    private suspend fun extractWithVxTwitter(url: String): MediaInfo {
        val uri = URI(url)
        val path = uri.path.trim('/')
        val segments = path.split('/')
        val statusIndex = segments.indexOf("status")
        if (statusIndex == -1 || statusIndex + 1 >= segments.size) {
            throw ScraperError.InvalidUrl("Format URL tweet tidak dikenali")
        }

        val tweetId = segments[statusIndex + 1]
        val user = if (statusIndex > 0) segments[statusIndex - 1] else "i"
        val vxApiUrl = "https://api.vxtwitter.com/$user/status/$tweetId"

        val response = client.get(vxApiUrl)
        val json = JSONObject(response)

        val text = json.optString("text", "Postingan X").take(100)
        val authorName = json.optString("user_name", "Pengguna X")
        val baseFilename = FileUtils.sanitizeFilename(text, "twitter_$tweetId")

        val mediaArray = json.optJSONArray("media_extended")
        val downloads = mutableListOf<DownloadOption>()
        var mainThumbnail = ""
        var detectedType = MediaType.VIDEO

        if (mediaArray != null && mediaArray.length() > 0) {
            for (i in 0 until mediaArray.length()) {
                val media = mediaArray.getJSONObject(i)
                val type = media.optString("type")
                val mediaUrl = media.optString("url")
                val thumb = media.optString("thumbnail_url")
                if (mainThumbnail.isBlank() && thumb.isNotBlank()) mainThumbnail = thumb

                if (type == "video" || type == "gif") {
                    detectedType = MediaType.VIDEO
                    downloads.add(
                        DownloadOption(
                            id = "tw_vid_$i",
                            type = MediaType.VIDEO,
                            format = "MP4",
                            quality = if (type == "gif") "GIF (MP4)" else "HD Video",
                            url = mediaUrl,
                            filename = "${baseFilename}_video.mp4",
                            source = "VxTwitter"
                        )
                    )
                } else if (type == "image") {
                    detectedType = if (mediaArray.length() > 1) MediaType.CAROUSEL else MediaType.IMAGE
                    downloads.add(
                        DownloadOption(
                            id = "tw_img_$i",
                            type = MediaType.IMAGE,
                            format = "JPG",
                            quality = "Original",
                            url = mediaUrl,
                            filename = "${baseFilename}_image_${i + 1}.jpg",
                            source = "VxTwitter"
                        )
                    )
                }
            }
        }

        if (downloads.isEmpty()) {
            throw ScraperError.MediaNotFound("Tidak ditemukan video atau gambar pada cuitan ini")
        }

        return MediaInfo(
            platform = MediaPlatform.TWITTER,
            originalUrl = url,
            title = text,
            author = authorName,
            thumbnail = mainThumbnail,
            mediaType = detectedType,
            downloads = downloads
        )
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
        if (downloadUrl.isBlank()) throw ScraperError.ExtractionFailed("Cobalt tidak menemukan video di tweet ini")

        val filename = json.optString("filename", "twitter_media.mp4")
        return MediaInfo(
            platform = MediaPlatform.TWITTER,
            originalUrl = url,
            title = "Postingan Twitter/X",
            author = "Pengguna X",
            thumbnail = "",
            mediaType = MediaType.VIDEO,
            downloads = listOf(
                DownloadOption(
                    id = "cobalt_tw_video",
                    type = MediaType.VIDEO,
                    format = "MP4",
                    quality = "HD",
                    url = downloadUrl,
                    filename = filename,
                    source = "Cobalt"
                )
            )
        )
    }

    private suspend fun extractFromBackend(url: String, backendUrl: String): MediaInfo {
        val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
        val requestJson = JSONObject().apply {
            put("url", url)
            put("platform", "twitter")
        }.toString()
        val response = client.postJson(endpoint, requestJson)
        return parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.TWITTER)
    }
}
