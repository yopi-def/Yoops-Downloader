package com.example.domain.model

enum class MediaPlatform(
    val displayName: String,
    val domains: List<String>,
    val defaultSources: List<String>
) {
    TIKTOK("TikTok", listOf("tiktok.com", "vm.tiktok.com", "vt.tiktok.com", "m.tiktok.com"), listOf("TikWM", "Cobalt", "Backend")),
    YOUTUBE("YouTube", listOf("youtube.com", "youtu.be", "m.youtube.com"), listOf("Cobalt", "Invidious", "Backend")),
    INSTAGRAM("Instagram", listOf("instagram.com", "instagr.am"), listOf("Cobalt", "SnapInsta", "Backend")),
    TWITTER("Twitter / X", listOf("twitter.com", "x.com"), listOf("VxTwitter", "Cobalt", "Backend")),
    FACEBOOK("Facebook", listOf("facebook.com", "fb.watch", "fb.com"), listOf("Cobalt", "SnapSave", "Backend")),
    PINTEREST("Pinterest", listOf("pinterest.com", "pin.it"), listOf("DirectOEmbed", "Cobalt", "Backend")),
    THREADS("Threads", listOf("threads.net"), listOf("DirectOEmbed", "Cobalt", "Backend")),
    DOUYIN("Douyin", listOf("douyin.com", "iesdouyin.com"), listOf("TikWM", "Cobalt", "Backend")),
    REDNOTE("RedNote", listOf("xiaohongshu.com", "xhslink.com"), listOf("Cobalt", "Backend")),
    BILIBILI("Bilibili", listOf("bilibili.com", "b23.tv"), listOf("BilibiliAPI", "Cobalt", "Backend")),
    PIXIV("Pixiv", listOf("pixiv.net"), listOf("Phixiv", "DirectOEmbed", "Backend")),
    SPOTIFY("Spotify", listOf("open.spotify.com", "spotify.link"), listOf("SpotifyOEmbed", "Cobalt", "Backend")),
    APPLE_MUSIC("Apple Music", listOf("music.apple.com"), listOf("AppleOEmbed", "Cobalt", "Backend")),
    BANDCAMP("Bandcamp", listOf("bandcamp.com"), listOf("BandcampScraper", "Backend")),
    UNKNOWN("Unknown", emptyList(), emptyList());

    companion object {
        fun fromUrl(url: String): MediaPlatform {
            val lower = url.lowercase().trim()
            return entries.firstOrNull { platform ->
                platform != UNKNOWN && platform.domains.any { domain ->
                    lower.contains("://$domain") || lower.contains(".${domain}") || lower.contains("/$domain") || lower.startsWith(domain)
                }
            } ?: UNKNOWN
        }
    }
}
