package com.example.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MoriApplication
import com.example.data.repository.DownloadRepository
import com.example.domain.model.DownloadStatus
import com.example.domain.model.DownloadTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val repository: DownloadRepository = MoriApplication.instance.downloadRepository
) : ViewModel() {

    val tasks: StateFlow<List<DownloadTask>> = repository.tasks

    fun pauseTask(taskId: String) {
        repository.pauseDownload(taskId)
    }

    fun resumeTask(taskId: String) {
        repository.resumeDownload(taskId)
    }

    fun cancelTask(taskId: String) {
        repository.cancelDownload(taskId)
    }

    fun retryTask(taskId: String) {
        repository.retryDownload(taskId)
    }

    fun removeTask(taskId: String) {
        repository.removeTask(taskId)
    }

    fun clearCompleted() {
        repository.clearCompletedTasks()
    }
}
