package com.example.ui.screens.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MoriApplication
import com.example.data.local.HistoryEntity
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: DownloadRepository = MoriApplication.instance.downloadRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPlatformFilter = MutableStateFlow<String?>(null)
    val selectedPlatformFilter: StateFlow<String?> = _selectedPlatformFilter.asStateFlow()

    val historyItems: StateFlow<List<HistoryEntity>> = combine(
        repository.history,
        _searchQuery,
        _selectedPlatformFilter
    ) { list, query, platform ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.author.contains(query, ignoreCase = true) ||
                item.platform.contains(query, ignoreCase = true)

            val matchesPlatform = platform == null || item.platform.equals(platform, ignoreCase = true)

            matchesQuery && matchesPlatform
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onPlatformFilterSelected(platform: String?) {
        _selectedPlatformFilter.value = if (_selectedPlatformFilter.value == platform) null else platform
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun doesFileExist(uriString: String?): Boolean {
        return repository.doesFileExist(uriString)
    }

    fun openMedia(context: Context, item: HistoryEntity): Boolean {
        return try {
            val uri = Uri.parse(item.localUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareMedia(context: Context, item: HistoryEntity): Boolean {
        return try {
            val uri = Uri.parse(item.localUri)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${item.title} - ${item.originalUrl}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Media").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }
}
