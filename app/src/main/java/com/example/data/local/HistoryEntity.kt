package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val platform: String,
    val originalUrl: String,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val localUri: String,
    val filePath: String?,
    val fileSize: Long,
    val mimeType: String,
    val format: String,
    val quality: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)
