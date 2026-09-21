package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

class BandcampScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.BANDCAMP

    override fun canHandle(url: String): Boolean {
        return url.lowercase().contains("bandcamp.com")
    }

    override fun getSupportedSources(): List<String> = listOf("BandcampScraper", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        if (!backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "bandcamp")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.BANDCAMP)
        }

        val html = client.get(url)

        val titleMatch = Regex("""<meta property="og:title" content="([^"]+)">""").find(html)
        val title = titleMatch?.groupValues?.get(1)?.replace("&amp;", "&") ?: "Lagu Bandcamp"

        val imgMatch = Regex("""<meta property="og:image" content="([^"]+)">""").find(html)
        val artwork = imgMatch?.groupValues?.get(1) ?: ""

        // Extract mp3 stream: "mp3-128":"https:\/\/t4.bcbits.com\/..."
        val streamMatch = Regex(""""mp3-128"\s*:\s*"([^"]+)"""").find(html)
        val rawStream = streamMatch?.groupValues?.get(1)?.replace("\\/", "/") ?: ""

        val downloads = mutableListOf<DownloadOption>()
        val baseName = FileUtils.sanitizeFilename(title, "bandcamp_track")

        if (rawStream.isNotBlank()) {
            downloads.add(
                DownloadOption(
                    id = "bc_mp3_128",
                    type = MediaType.AUDIO,
                    format = "MP3",
                    quality = "128kbps Stream",
                    url = rawStream,
                    filename = "${baseName}.mp3",
                    source = "BandcampScraper"
                )
            )
        }

        if (artwork.isNotBlank()) {
            downloads.add(
                DownloadOption(
                    id = "bc_artwork",
                    type = MediaType.IMAGE,
                    format = "JPG",
                    quality = "Album Cover (Original)",
                    url = artwork,
                    filename = "${baseName}_cover.jpg",
                    source = "BandcampScraper"
                )
            )
        }

        if (downloads.isEmpty()) {
            throw ScraperError.ExtractionFailed("Gagal menemukan stream audio dari halaman Bandcamp")
        }

        MediaInfo(
            platform = MediaPlatform.BANDCAMP,
            originalUrl = url,
            title = title,
            author = "Bandcamp Artist",
            thumbnail = artwork,
            mediaType = MediaType.AUDIO,
            downloads = downloads
        )
    }
}
