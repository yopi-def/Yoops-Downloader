package com.example.domain.model

data class DownloadOption(
    val id: String,
    val type: MediaType,
    val format: String,
    val quality: String,
    val resolution: String? = null,
    val url: String,
    val filename: String,
    val size: Long = -1L,
    val requiresConversion: Boolean = false,
    val source: String
)
