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

class YouTubeScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.YOUTUBE

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    override fun getSupportedSources(): List<String> = listOf("Cobalt", "Invidious", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        val preferred = sourcePreference ?: "Cobalt"

        if (preferred == "Backend" && !backendUrl.isNullOrBlank()) {
            return@runCatching extractFromBackend(url, backendUrl)
        }

        // Try primary (Cobalt)
        try {
            extractWithCobalt(url, backendUrl)
        } catch (e: Exception) {
            // Fallback to Invidious
            try {
                extractWithInvidious(url)
            } catch (fallbackEx: Exception) {
                if (e is ScraperError) throw e
                throw ScraperError.ExtractionFailed("Gagal mengekstrak YouTube. Coba konfigurasi custom backend jika kuota public cobalt tercapai.", fallbackEx)
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
            put("vQuality", "720")
            put("youtubeVideoCodec", "h264")
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
            val errorText = json.optJSONObject("text")?.optString("error") ?: "Gagal mengambil data dari Cobalt"
            throw ScraperError.ExtractionFailed(errorText)
        }

        val downloadUrl = json.optString("url")
        if (downloadUrl.isBlank()) {
            throw ScraperError.ExtractionFailed("Cobalt tidak memberikan URL unduhan")
        }

        val filename = json.optString("filename", "youtube_video.mp4")
        val title = filename.substringBeforeLast(".")

        val downloads = listOf(
            DownloadOption(
                id = "yt_cobalt_720",
                type = MediaType.VIDEO,
                format = "MP4",
                quality = "720p HD",
                url = downloadUrl,
                filename = filename,
                source = "Cobalt"
            )
        )

        return MediaInfo(
            platform = MediaPlatform.YOUTUBE,
            originalUrl = url,
            title = title,
            author = "YouTube Creator",
            thumbnail = extractYouTubeThumbnail(url),
            mediaType = MediaType.VIDEO,
            downloads = downloads
        )
    }

    private suspend fun extractWithInvidious(url: String): MediaInfo {
        val videoId = extractVideoId(url) ?: throw ScraperError.InvalidUrl("ID video YouTube tidak valid")
        val invidiousInstances = listOf(
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://yt.artemislena.eu"
        )

        var lastException: Exception? = null
        for (instance in invidiousInstances) {
            try {
                val apiUrl = "$instance/api/v1/videos/$videoId"
                val response = client.get(apiUrl)
                val json = JSONObject(response)

                val title = json.optString("title", "YouTube Video")
                val author = json.optString("author", "YouTube Channel")
                val lengthSeconds = json.optLong("lengthSeconds", 0L)
                val baseFilename = FileUtils.sanitizeFilename(title, "yt_$videoId")

                val formatStreams = json.optJSONArray("formatStreams")
                val downloads = mutableListOf<DownloadOption>()

                if (formatStreams != null) {
                    for (i in 0 until formatStreams.length()) {
                        val stream = formatStreams.getJSONObject(i)
                        val streamUrl = stream.optString("url")
                        val resolution = stream.optString("resolution", "720p")
                        val container = stream.optString("container", "mp4").uppercase()

                        if (streamUrl.isNotBlank()) {
                            downloads.add(
                                DownloadOption(
                                    id = "invidious_stream_$i",
                                    type = MediaType.VIDEO,
                                    format = container,
                                    quality = resolution,
                                    url = streamUrl,
                                    filename = "${baseFilename}_$resolution.${container.lowercase()}",
                                    source = "Invidious"
                                )
                            )
                        }
                    }
                }

                if (downloads.isNotEmpty()) {
                    return MediaInfo(
                        platform = MediaPlatform.YOUTUBE,
                        originalUrl = url,
                        title = title,
                        author = author,
                        thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        duration = if (lengthSeconds > 0) lengthSeconds else null,
                        mediaType = MediaType.VIDEO,
                        downloads = downloads
                    )
                }
            } catch (ex: Exception) {
                lastException = ex
            }
        }

        throw lastException ?: ScraperError.ExtractionFailed("Invidious instances tidak merespons")
    }

    private fun extractVideoId(url: String): String? {
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: ""
            if (host.contains("youtu.be")) {
                uri.path.trim('/')
            } else if (host.contains("youtube.com")) {
                if (uri.path.startsWith("/shorts/")) {
                    uri.path.removePrefix("/shorts/").trim('/')
                } else {
                    uri.query?.split("&")
                        ?.firstOrNull { it.startsWith("v=") }
                        ?.substringAfter("v=")
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractYouTubeThumbnail(url: String): String {
        val id = extractVideoId(url)
        return if (!id.isNullOrBlank()) "https://img.youtube.com/vi/$id/hqdefault.jpg" else ""
    }

    private suspend fun extractFromBackend(url: String, backendUrl: String): MediaInfo {
        val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
        val requestJson = JSONObject().apply {
            put("url", url)
            put("platform", "youtube")
        }.toString()

        val response = client.postJson(endpoint, requestJson)
        return parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.YOUTUBE)
    }
}
