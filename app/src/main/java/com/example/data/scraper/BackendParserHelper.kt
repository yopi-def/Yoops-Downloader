package com.example.data.scraper

import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

fun parseStandardBackendResponse(json: JSONObject, originalUrl: String, fallbackPlatform: MediaPlatform): MediaInfo {
    if (json.optBoolean("error", false) || json.optString("status") == "error") {
        val errorMsg = json.optString("message", json.optString("error", "Ekstraksi backend gagal"))
        throw ScraperError.ExtractionFailed(errorMsg)
    }

    val title = json.optString("title", "Media Download").ifBlank { "Media Download" }
    val author = json.optString("author", "Creator").ifBlank { "Creator" }
    val thumbnail = json.optString("thumbnail", "")
    val duration = json.optLong("duration", 0L)
    val desc = json.optString("description", "")
    val baseFilename = FileUtils.sanitizeFilename(title, "media_download")

    val downloads = mutableListOf<DownloadOption>()
    val downloadsArray = json.optJSONArray("downloads")

    if (downloadsArray != null && downloadsArray.length() > 0) {
        for (i in 0 until downloadsArray.length()) {
            val opt = downloadsArray.getJSONObject(i)
            val typeStr = opt.optString("type", "VIDEO")
            val type = try { MediaType.valueOf(typeStr.uppercase()) } catch (_: Exception) { MediaType.VIDEO }
            val format = opt.optString("format", if (type == MediaType.AUDIO) "MP3" else "MP4")
            val quality = opt.optString("quality", "HD")
            val url = opt.optString("url", "")
            val size = opt.optLong("size", -1L)
            val source = opt.optString("source", "Backend")

            if (url.isNotBlank()) {
                val ext = format.lowercase()
                downloads.add(
                    DownloadOption(
                        id = "opt_$i",
                        type = type,
                        format = format,
                        quality = quality,
                        url = url,
                        filename = "${baseFilename}_$quality.$ext",
                        size = size,
                        source = source
                    )
                )
            }
        }
    } else if (json.has("url")) {
        val directUrl = json.getString("url")
        downloads.add(
            DownloadOption(
                id = "direct_opt",
                type = MediaType.VIDEO,
                format = "MP4",
                quality = "Direct",
                url = directUrl,
                filename = "${baseFilename}.mp4",
                source = "Backend"
            )
        )
    }

    if (downloads.isEmpty()) {
        throw ScraperError.ExtractionFailed("Backend tidak mengembalikan opsi unduhan yang valid")
    }

    return MediaInfo(
        platform = fallbackPlatform,
        originalUrl = originalUrl,
        title = title,
        author = author,
        thumbnail = thumbnail,
        duration = if (duration > 0) duration else null,
        description = desc,
        mediaType = downloads.first().type,
        downloads = downloads
    )
}
