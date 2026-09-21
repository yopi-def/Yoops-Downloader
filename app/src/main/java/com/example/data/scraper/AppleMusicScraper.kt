package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

class AppleMusicScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.APPLE_MUSIC

    override fun canHandle(url: String): Boolean {
        return url.lowercase().contains("music.apple.com")
    }

    override fun getSupportedSources(): List<String> = listOf("AppleOEmbed", "Cobalt", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        if (!backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "applemusic")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.APPLE_MUSIC)
        }

        val trackId = url.substringAfterLast("i=").substringBefore("&").substringBefore("?")
        val title = "Apple Music Track"
        val author = "Apple Music Artist"
        var artworkUrl = ""
        val downloads = mutableListOf<DownloadOption>()

        if (trackId.isNotBlank() && trackId.all { it.isDigit() }) {
            try {
                val itunesApi = "https://itunes.apple.com/lookup?id=$trackId"
                val res = client.get(itunesApi)
                val json = JSONObject(res)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val track = results.getJSONObject(0)
                    val trackName = track.optString("trackName", title)
                    val artistName = track.optString("artistName", author)
                    val rawArtwork = track.optString("artworkUrl100", "")
                    val hqArtwork = rawArtwork.replace("100x100bb.jpg", "1000x1000bb.jpg")
                    artworkUrl = hqArtwork
                    val previewUrl = track.optString("previewUrl", "")
                    val baseName = FileUtils.sanitizeFilename("$artistName - $trackName", "apple_music")

                    if (previewUrl.isNotBlank()) {
                        downloads.add(
                            DownloadOption(
                                id = "apple_preview_m4a",
                                type = MediaType.AUDIO,
                                format = "M4A",
                                quality = "HQ Preview Audio",
                                url = previewUrl,
                                filename = "${baseName}_preview.m4a",
                                source = "AppleOEmbed"
                            )
                        )
                    }

                    if (hqArtwork.isNotBlank()) {
                        downloads.add(
                            DownloadOption(
                                id = "apple_cover_art",
                                type = MediaType.IMAGE,
                                format = "JPG",
                                quality = "Hi-Res Artwork (1000x1000)",
                                url = hqArtwork,
                                filename = "${baseName}_artwork.jpg",
                                source = "AppleOEmbed"
                            )
                        )
                    }

                    return@runCatching MediaInfo(
                        platform = MediaPlatform.APPLE_MUSIC,
                        originalUrl = url,
                        title = trackName,
                        author = artistName,
                        thumbnail = hqArtwork,
                        mediaType = MediaType.AUDIO,
                        downloads = downloads
                    )
                }
            } catch (_: Exception) {}
        }

        throw ScraperError.ExtractionFailed("Gagal mengekstrak lagu Apple Music. Gunakan backend service untuk autentikasi Apple Music.")
    }
}
