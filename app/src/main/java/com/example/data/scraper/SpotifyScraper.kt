package com.example.data.scraper

import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.MediaType
import com.example.domain.model.ScraperError
import com.example.utils.FileUtils
import org.json.JSONObject

class SpotifyScraper(
    private val client: ScraperNetworkClient
) : MediaScraper {

    override fun getPlatform(): MediaPlatform = MediaPlatform.SPOTIFY

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("spotify.com") || lower.contains("spotify.link")
    }

    override fun getSupportedSources(): List<String> = listOf("SpotifyOEmbed", "Cobalt", "Backend")

    override suspend fun extract(
        url: String,
        sourcePreference: String?,
        backendUrl: String?
    ): Result<MediaInfo> = runCatching {
        if (!backendUrl.isNullOrBlank()) {
            val endpoint = "${backendUrl.trimEnd('/')}/api/extract"
            val requestJson = JSONObject().apply {
                put("url", url)
                put("platform", "spotify")
            }.toString()
            val response = client.postJson(endpoint, requestJson)
            return@runCatching parseStandardBackendResponse(JSONObject(response), url, MediaPlatform.SPOTIFY)
        }

        // Fetch oEmbed metadata (album artwork, artist, title)
        val oembedUrl = "https://open.spotify.com/oembed?url=$url"
        val response = client.get(oembedUrl)
        val json = JSONObject(response)

        val title = json.optString("title", "Lagu Spotify")
        val author = json.optString("author_name", "Artis Spotify")
        val thumbnail = json.optString("thumbnail_url", "")
        val baseName = FileUtils.sanitizeFilename("$author - $title", "spotify_track")

        val downloads = mutableListOf<DownloadOption>()

        // Cover artwork download option
        if (thumbnail.isNotBlank()) {
            downloads.add(
                DownloadOption(
                    id = "spotify_cover_art",
                    type = MediaType.IMAGE,
                    format = "JPG",
                    quality = "Album Artwork (HQ)",
                    url = thumbnail,
                    filename = "${baseName}_cover.jpg",
                    source = "SpotifyOEmbed"
                )
            )
        }

        // Try extracting audio via Cobalt or Backend
        try {
            val cobaltRes = client.postJson(
                url = "https://api.cobalt.tools/api/json",
                jsonBody = JSONObject().apply {
                    put("url", url)
                    put("downloadMode", "audio")
                    put("audioFormat", "mp3")
                }.toString()
            )
            val cJson = JSONObject(cobaltRes)
            val audioUrl = cJson.optString("url")
            if (audioUrl.isNotBlank()) {
                downloads.add(
                    0,
                    DownloadOption(
                        id = "spotify_audio_mp3",
                        type = MediaType.AUDIO,
                        format = "MP3",
                        quality = "320kbps Audio",
                        url = audioUrl,
                        filename = "${baseName}.mp3",
                        source = "Cobalt"
                    )
                )
            }
        } catch (_: Exception) {}

        if (downloads.isEmpty()) {
            throw ScraperError.ExtractionFailed("Gagal mengekstrak metadata dari tautan Spotify")
        }

        MediaInfo(
            platform = MediaPlatform.SPOTIFY,
            originalUrl = url,
            title = title,
            author = author,
            thumbnail = thumbnail,
            mediaType = if (downloads.any { it.type == MediaType.AUDIO }) MediaType.AUDIO else MediaType.IMAGE,
            downloads = downloads
        )
    }
}
