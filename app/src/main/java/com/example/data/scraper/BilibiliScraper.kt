package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

class BilibiliScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.BILIBILI

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("bilibili.com") || lower.contains("b23.tv")
    }

    override fun getSupportedSources(): List<String> = listOf("BilibiliAPI", "Cobalt", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        if (!backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "bilibili")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.BILIBILI)
        }

        val bvid = extractBvid(url)
        var title = "Bilibili Video"
        var author = "Bilibili Creator"
        var thumb = ""
        var duration: Long? = null

        if (bvid != null) {
            try {
                val api = "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
                val res = client.get(api, userAgent = ScraperNetworkClient.DEFAULT_USER_AGENT)
                val json = JSONObject(res)
                if (json.optInt("code") == 0) {
                    val data = json.getJSONObject("data")
                    title = data.optString("title", title)
                    author = data.optJSONObject("owner")?.optString("name", author) ?: author
                    thumb = data.optString("pic", "")
                    val dur = data.optLong("duration", 0L)
                    if (dur > 0) duration = dur
                }
            } catch (_: Exception) {}
        }

        try {
            val cobaltRes = client.postJson(
                url = "https://api.cobalt.tools/api/json",
                jsonBody = JSONObject().apply {
                    put("url", url)
                    put("vQuality", "720")
                    put("filenamePattern", "basic")
                }.toString()
            )

            val cJson = JSONObject(cobaltRes)
            val downloadUrl = cJson.optString("url")
            if (downloadUrl.isNotBlank()) {
                val filename = cJson.optString("filename", "${FileUtils.sanitizeFilename(title, "bilibili")}.mp4")
                return@runCatching MediaInfo(
                    platform = MediaPlatform.BILIBILI,
                    originalUrl = url,
                    title = title,
                    author = author,
                    thumbnail = thumb,
                    duration = duration,
                    mediaType = MediaType.VIDEO,
                    downloads = listOf(
                        DownloadOption(
                            id = "bili_cobalt_vid",
                            type = MediaType.VIDEO,
                            format = "MP4",
                            quality = "720p HD",
                            url = downloadUrl,
                            filename = filename,
                            source = "Cobalt"
                        )
                    )
                )
            }
        } catch (_: Exception) {}

        throw ScraperError.ExtractionFailed("Gagal mengekstrak video Bilibili. Gunakan backend service untuk bypass proteksi anti-hotlink Bilibili.")
    }

    private fun extractBvid(url: String): String? {
        val regex = "BV[0-9a-zA-Z]{10}".toRegex()
        return regex.find(url)?.value
    }
}
