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

class DouyinScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.DOUYIN

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("douyin.com") || lower.contains("iesdouyin.com")
    }

    override fun getSupportedSources(): List<String> = listOf("TikWM", "Cobalt", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        val preferred = sourcePreference ?: "TikWM"

        if (preferred == "Backend" && !backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "douyin")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.DOUYIN)
        }

        try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val apiUrl = "https://www.tikwm.com/api/?url=$encoded&hd=1"
            val response = client.get(apiUrl, userAgent = ScraperNetworkClient.MOBILE_USER_AGENT)
            val json = JSONObject(response)

            if (json.optInt("code", -1) != 0) {
                throw ScraperError.ExtractionFailed("Gagal membaca link Douyin")
            }

            val data = json.getJSONObject("data")
            val title = data.optString("title", "Douyin Video")
            val author = data.optJSONObject("author")?.optString("nickname", "Douyin User") ?: "Douyin User"
            val thumbnail = data.optString("cover", "")
            val play = data.optString("play", "")
            val hdPlay = data.optString("hdplay", "")
            val baseName = FileUtils.sanitizeFilename(title, "douyin_video")

            val downloads = mutableListOf<DownloadOption>()
            val mainUrl = if (hdPlay.isNotBlank()) hdPlay else play
            if (mainUrl.isNotBlank()) {
                val fullUrl = if (mainUrl.startsWith("http")) mainUrl else "https://www.tikwm.com$mainUrl"
                downloads.add(
                    DownloadOption(
                        id = "douyin_vid_hd",
                        type = MediaType.VIDEO,
                        format = "MP4",
                        quality = "Watermark-Free HD",
                        url = fullUrl,
                        filename = "${baseName}_hd.mp4",
                        source = "TikWM"
                    )
                )
            }

            if (downloads.isEmpty()) throw ScraperError.MediaNotFound("Tidak ada URL video yang dapat diunduh")

            MediaInfo(
                platform = MediaPlatform.DOUYIN,
                originalUrl = url,
                title = title,
                author = author,
                thumbnail = thumbnail,
                mediaType = MediaType.VIDEO,
                downloads = downloads
            )
        } catch (e: Exception) {
            if (e is ScraperError) throw e
            throw ScraperError.ExtractionFailed("Gagal mengekstrak Douyin", e)
        }
    }
}
