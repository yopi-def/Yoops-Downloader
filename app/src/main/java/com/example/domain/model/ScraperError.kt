package com.example.domain.model

enum class ErrorType {
    INVALID_URL,
    UNSUPPORTED_PLATFORM,
    EXTRACTION_FAILED,
    SOURCE_UNAVAILABLE,
    RATE_LIMITED,
    NETWORK_ERROR,
    TIMEOUT,
    MEDIA_NOT_FOUND,
    DOWNLOAD_FAILED,
    STORAGE_ERROR,
    UNKNOWN_ERROR
}

sealed class ScraperError(
    val type: ErrorType,
    val messageId: String,
    val messageEn: String,
    val causeException: Throwable? = null
) : Exception(messageId, causeException) {

    class InvalidUrl(msg: String = "Link tautan tidak valid") :
        ScraperError(ErrorType.INVALID_URL, msg, "Invalid media URL")

    class UnsupportedPlatform(msg: String = "Platform belum didukung") :
        ScraperError(ErrorType.UNSUPPORTED_PLATFORM, msg, "Platform is not supported yet")

    class ExtractionFailed(msg: String = "Gagal mengekstrak media dari platform", cause: Throwable? = null) :
        ScraperError(ErrorType.EXTRACTION_FAILED, msg, "Failed to extract media", cause)

    class SourceUnavailable(msg: String = "Server extraction sedang tidak tersedia", cause: Throwable? = null) :
        ScraperError(ErrorType.SOURCE_UNAVAILABLE, msg, "Extraction source is currently unavailable", cause)

    class RateLimited(msg: String = "Batas permintaan tercapai, silakan coba beberapa saat lagi") :
        ScraperError(ErrorType.RATE_LIMITED, msg, "Rate limit reached, please try again later")

    class NetworkError(msg: String = "Koneksi internet bermasalah. Periksa jaringan Anda.", cause: Throwable? = null) :
        ScraperError(ErrorType.NETWORK_ERROR, msg, "Network error. Please check your connection.", cause)

    class Timeout(msg: String = "Waktu permintaan habis. Server tidak merespons.") :
        ScraperError(ErrorType.TIMEOUT, msg, "Request timed out. Server did not respond.")

    class MediaNotFound(msg: String = "Media tidak dapat ditemukan atau telah dihapus") :
        ScraperError(ErrorType.MEDIA_NOT_FOUND, msg, "Media not found or was deleted")

    class DownloadFailed(msg: String = "Gagal mengunduh file media", cause: Throwable? = null) :
        ScraperError(ErrorType.DOWNLOAD_FAILED, msg, "Failed to download media file", cause)

    class StorageError(msg: String = "Gagal menyimpan file ke penyimpanan perangkat", cause: Throwable? = null) :
        ScraperError(ErrorType.STORAGE_ERROR, msg, "Failed to save file to device storage", cause)

    class UnknownError(msg: String = "Terjadi kesalahan yang tidak diketahui", cause: Throwable? = null) :
        ScraperError(ErrorType.UNKNOWN_ERROR, msg, "An unknown error occurred", cause)

    fun getUserMessage(isEnglish: Boolean = false): String {
        return if (isEnglish) messageEn else messageId
    }
}
