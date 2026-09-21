package com.example.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: String = "ID", // "ID" or "EN"
    val wifiOnly: Boolean = false,
    val concurrentDownloads: Int = 2,
    val autoSaveToGallery: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val requestTimeoutSeconds: Int = 30,
    val retryCount: Int = 2,
    val debugLogging: Boolean = false,
    val customBackendUrl: String = "",
    val preferredTikTokSource: String = "TikWM",
    val preferredYouTubeSource: String = "Cobalt",
    val preferredTwitterSource: String = "VxTwitter",
    val preferredInstagramSource: String = "Cobalt"
)

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}
