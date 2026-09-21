package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.AppSettings
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsDataStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mori_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString("theme", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val theme = try { ThemeMode.valueOf(themeStr) } catch (_: Exception) { ThemeMode.DARK }

        return AppSettings(
            themeMode = theme,
            language = prefs.getString("language", "ID") ?: "ID",
            wifiOnly = prefs.getBoolean("wifi_only", false),
            concurrentDownloads = prefs.getInt("concurrent_downloads", 2),
            autoSaveToGallery = prefs.getBoolean("auto_save_gallery", true),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            requestTimeoutSeconds = prefs.getInt("request_timeout", 30),
            retryCount = prefs.getInt("retry_count", 2),
            debugLogging = prefs.getBoolean("debug_logging", false),
            customBackendUrl = prefs.getString("custom_backend_url", "") ?: "",
            preferredTikTokSource = prefs.getString("pref_tiktok_source", "TikWM") ?: "TikWM",
            preferredYouTubeSource = prefs.getString("pref_youtube_source", "Cobalt") ?: "Cobalt",
            preferredTwitterSource = prefs.getString("pref_twitter_source", "VxTwitter") ?: "VxTwitter",
            preferredInstagramSource = prefs.getString("pref_instagram_source", "Cobalt") ?: "Cobalt"
        )
    }

    fun updateTheme(themeMode: ThemeMode) {
        prefs.edit().putString("theme", themeMode.name).apply()
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    fun updateLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _settings.value = _settings.value.copy(language = lang)
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean("wifi_only", wifiOnly).apply()
        _settings.value = _settings.value.copy(wifiOnly = wifiOnly)
    }

    fun updateConcurrentDownloads(count: Int) {
        prefs.edit().putInt("concurrent_downloads", count).apply()
        _settings.value = _settings.value.copy(concurrentDownloads = count)
    }

    fun updateAutoSave(autoSave: Boolean) {
        prefs.edit().putBoolean("auto_save_gallery", autoSave).apply()
        _settings.value = _settings.value.copy(autoSaveToGallery = autoSave)
    }

    fun updateNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun updateRequestTimeout(seconds: Int) {
        prefs.edit().putInt("request_timeout", seconds).apply()
        _settings.value = _settings.value.copy(requestTimeoutSeconds = seconds)
    }

    fun updateRetryCount(count: Int) {
        prefs.edit().putInt("retry_count", count).apply()
        _settings.value = _settings.value.copy(retryCount = count)
    }

    fun updateDebugLogging(enabled: Boolean) {
        prefs.edit().putBoolean("debug_logging", enabled).apply()
        _settings.value = _settings.value.copy(debugLogging = enabled)
    }

    fun updateCustomBackendUrl(url: String) {
        prefs.edit().putString("custom_backend_url", url).apply()
        _settings.value = _settings.value.copy(customBackendUrl = url)
    }

    fun updatePreferredSource(platform: String, source: String) {
        when (platform.uppercase()) {
            "TIKTOK" -> {
                prefs.edit().putString("pref_tiktok_source", source).apply()
                _settings.value = _settings.value.copy(preferredTikTokSource = source)
            }
            "YOUTUBE" -> {
                prefs.edit().putString("pref_youtube_source", source).apply()
                _settings.value = _settings.value.copy(preferredYouTubeSource = source)
            }
            "TWITTER" -> {
                prefs.edit().putString("pref_twitter_source", source).apply()
                _settings.value = _settings.value.copy(preferredTwitterSource = source)
            }
            "INSTAGRAM" -> {
                prefs.edit().putString("pref_instagram_source", source).apply()
                _settings.value = _settings.value.copy(preferredInstagramSource = source)
            }
        }
    }
}
