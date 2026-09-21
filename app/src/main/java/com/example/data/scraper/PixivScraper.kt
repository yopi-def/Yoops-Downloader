package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

class PixivScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.PIXIV

    override fun canHandle(url: String): Boolean {
        return url.lowercase().contains("pixiv.net")
    }

    override fun getSupportedSources(): List<String> = listOf("Phixiv", "DirectOEmbed", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        if (!backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "pixiv")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.PIXIV)
        }

        try {
            val oembedUrl = "https://embed.pixiv.net/oembed.php?url=$url"
            val response = client.get(oembedUrl)
            val json = JSONObject(response)

            val title = json.optString("title", "Karya Pixiv")
            val author = json.optString("author_name", "Artis Pixiv")
            val thumb = json.optString("thumbnail_url", "")

            if (thumb.isBlank()) throw ScraperError.MediaNotFound("Ilustrasi Pixiv tidak ditemukan")

            val baseName = FileUtils.sanitizeFilename(title, "pixiv_art")
            MediaInfo(
                platform = MediaPlatform.PIXIV,
                originalUrl = url,
                title = title,
                author = author,
                thumbnail = thumb,
                mediaType = MediaType.IMAGE,
                downloads = listOf(
                    DownloadOption(
                        id = "pixiv_img",
                        type = MediaType.IMAGE,
                        format = "JPG",
                        quality = "High Quality Artwork",
                        url = thumb,
                        filename = "${baseName}.jpg",
                        source = "DirectOEmbed"
                    )
                )
            )
        } catch (e: Exception) {
            if (e is ScraperError) throw e
            throw ScraperError.ExtractionFailed("Gagal mengekstrak Pixiv. Akses Pixiv memerlukan login atau backend scraper service.", e)
        }
    }
}
