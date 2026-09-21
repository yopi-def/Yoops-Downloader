package com.example.domain.model

enum class DownloadStatus(val labelId: String, val labelEn: String) {
    QUEUED("Dalam Antrean", "Queued"),
    EXTRACTING("Mengekstrak", "Extracting"),
    DOWNLOADING("Mengunduh", "Downloading"),
    PAUSED("Dijeda", "Paused"),
    COMPLETED("Selesai", "Completed"),
    FAILED("Gagal", "Failed"),
    CANCELLED("Dibatalkan", "Cancelled")
}
