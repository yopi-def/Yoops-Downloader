package com.example.domain.model

enum class MediaType(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    IMAGE("Image"),
    CAROUSEL("Carousel");

    companion object {
        fun fromMimeOrExtension(format: String): MediaType {
            val upper = format.uppercase()
            return when {
                upper.contains("MP4") || upper.contains("WEBM") || upper.contains("MKV") || upper.contains("MOV") || upper.contains("VIDEO") -> VIDEO
                upper.contains("MP3") || upper.contains("M4A") || upper.contains("WAV") || upper.contains("FLAC") || upper.contains("AUDIO") || upper.contains("AAC") -> AUDIO
                upper.contains("JPG") || upper.contains("JPEG") || upper.contains("PNG") || upper.contains("WEBP") || upper.contains("IMAGE") -> IMAGE
                else -> VIDEO
            }
        }
    }
}
