package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MoriApplication
import com.example.data.repository.DownloadRepository
import com.example.data.repository.MediaRepository
import com.example.domain.model.DownloadOption
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaPlatform
import com.example.domain.model.ScraperError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val mediaInfo: MediaInfo) : HomeUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : HomeUiState()
}

class HomeViewModel(
    private val mediaRepository: MediaRepository = MoriApplication.instance.mediaRepository,
    private val downloadRepository: DownloadRepository = MoriApplication.instance.downloadRepository
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _detectedPlatform = MutableStateFlow(MediaPlatform.UNKNOWN)
    val detectedPlatform: StateFlow<MediaPlatform> = _detectedPlatform.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _snackBarEvent = MutableSharedFlow<String>()
    val snackBarEvent: SharedFlow<String> = _snackBarEvent.asSharedFlow()

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (newUrl.isNotBlank()) {
            _detectedPlatform.value = mediaRepository.detectPlatform(newUrl)
        } else {
            _detectedPlatform.value = MediaPlatform.UNKNOWN
        }
    }

    fun clearUrl() {
        _urlInput.value = ""
        _detectedPlatform.value = MediaPlatform.UNKNOWN
        _uiState.value = HomeUiState.Idle
    }

    fun analyzeUrl() {
        val url = _urlInput.value.trim()
        if (url.isBlank()) {
            _uiState.value = HomeUiState.Error("Masukkan link media terlebih dahulu")
            return
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val result = mediaRepository.extractMedia(url)

            result.onSuccess { info ->
                _uiState.value = HomeUiState.Success(info)
                _detectedPlatform.value = info.platform
            }.onFailure { ex ->
                val errorMsg = if (ex is ScraperError) {
                    ex.getUserMessage()
                } else {
                    ex.localizedMessage ?: "Gagal menganalisis link"
                }
                _uiState.value = HomeUiState.Error(errorMsg)
            }
        }
    }

    fun startDownload(mediaInfo: MediaInfo, option: DownloadOption) {
        viewModelScope.launch {
            val taskId = downloadRepository.startDownload(mediaInfo, option)
            _snackBarEvent.emit("Unduhan ditambahkan ke antrean")
        }
    }
}
