package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MoriApplication
import com.example.data.local.SettingsDataStore
import com.example.data.network.ScraperNetworkClient
import com.example.domain.model.AppSettings
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request

class SettingsViewModel(
    private val dataStore: SettingsDataStore = MoriApplication.instance.settingsDataStore,
    private val networkClient: ScraperNetworkClient = MoriApplication.instance.networkClient
) : ViewModel() {

    val settings: StateFlow<AppSettings> = dataStore.settings

    private val _backendTestStatus = MutableStateFlow<String?>(null)
    val backendTestStatus: StateFlow<String?> = _backendTestStatus.asStateFlow()

    private val _isTestingBackend = MutableStateFlow(false)
    val isTestingBackend: StateFlow<Boolean> = _isTestingBackend.asStateFlow()

    fun updateTheme(themeMode: ThemeMode) {
        dataStore.updateTheme(themeMode)
    }

    fun updateLanguage(lang: String) {
        dataStore.updateLanguage(lang)
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        dataStore.updateWifiOnly(wifiOnly)
    }

    fun updateConcurrentDownloads(count: Int) {
        dataStore.updateConcurrentDownloads(count)
    }

    fun updateAutoSave(autoSave: Boolean) {
        dataStore.updateAutoSave(autoSave)
    }

    fun updateNotifications(enabled: Boolean) {
        dataStore.updateNotifications(enabled)
    }

    fun updateCustomBackendUrl(url: String) {
        dataStore.updateCustomBackendUrl(url.trim())
    }

    fun updatePlatformSource(platform: String, source: String) {
        dataStore.updatePreferredSource(platform, source)
    }

    fun testBackendConnection() {
        val url = settings.value.customBackendUrl.trim()
        if (url.isBlank()) {
            _backendTestStatus.value = "Masukkan URL backend terlebih dahulu"
            return
        }

        viewModelScope.launch {
            _isTestingBackend.value = true
            _backendTestStatus.value = "Menghubungi server..."
            try {
                val pingUrl = if (url.endsWith("/")) "${url}api/health" else "$url/api/health"
                val request = Request.Builder().url(pingUrl).build()
                val response = networkClient.okHttpClient.newCall(request).execute()
                val code = response.code
                response.close()
                if (response.isSuccessful || code == 404) {
                    _backendTestStatus.value = "Koneksi berhasil! Server merespons (HTTP $code)"
                } else {
                    _backendTestStatus.value = "Server merespons dengan HTTP $code"
                }
            } catch (e: Exception) {
                _backendTestStatus.value = "Gagal terhubung: ${e.localizedMessage ?: "Network error"}"
            } finally {
                _isTestingBackend.value = false
            }
        }
    }
}
