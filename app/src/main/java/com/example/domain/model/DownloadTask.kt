package com.example.domain.model

data class DownloadTask(
    val id: String,
    val mediaInfo: MediaInfo,
    val selectedOption: DownloadOption,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val localUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
