package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryEntity
import com.example.downloader.MoriDownloadEngine
import com.example.domain.model.DownloadOption
import com.example.domain.model.DownloadTask
import com.example.domain.model.MediaInfo
import com.example.utils.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class DownloadRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val downloadEngine: MoriDownloadEngine
) {
    val tasks: StateFlow<List<DownloadTask>> = downloadEngine.tasks

    val history: Flow<List<HistoryEntity>> = database.historyDao().getAllHistory()

    fun startDownload(mediaInfo: MediaInfo, option: DownloadOption): String {
        return downloadEngine.enqueueDownload(mediaInfo, option)
    }

    fun pauseDownload(taskId: String) {
        downloadEngine.pauseDownload(taskId)
    }

    fun resumeDownload(taskId: String) {
        downloadEngine.resumeDownload(taskId)
    }

    fun cancelDownload(taskId: String) {
        downloadEngine.cancelDownload(taskId)
    }

    fun retryDownload(taskId: String) {
        downloadEngine.retryDownload(taskId)
    }

    fun removeTask(taskId: String) {
        downloadEngine.removeTask(taskId)
    }

    fun clearCompletedTasks() {
        downloadEngine.clearCompleted()
    }

    suspend fun deleteHistory(id: Long) {
        database.historyDao().deleteHistoryById(id)
    }

    suspend fun clearAllHistory() {
        database.historyDao().clearAllHistory()
    }

    fun doesFileExist(uriString: String?): Boolean {
        return FileUtils.uriExists(context, uriString)
    }
}
