package com.example.utils

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {
    fun sanitizeFilename(name: String, fallback: String = "mori_download"): String {
        val sanitized = name.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
        return if (sanitized.isBlank()) fallback else sanitized.take(120)
    }

    fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    fun uriExists(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = uri.path?.let { File(it) }
                file?.exists() == true && file.length() > 0
            } else {
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            }
        } catch (_: Exception) {
            false
        }
    }
}
