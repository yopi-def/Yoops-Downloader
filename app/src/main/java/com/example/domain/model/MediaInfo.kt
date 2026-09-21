package com.example.domain.model

data class MediaInfo(
    val platform: MediaPlatform,
    val originalUrl: String,
    val title: String,
    val author: String,
    val thumbnail: String,
    val duration: Long? = null,
    val description: String? = null,
    val mediaType: MediaType,
    val downloads: List<DownloadOption>,
    val extractedAt: Long = System.currentTimeMillis()
)
