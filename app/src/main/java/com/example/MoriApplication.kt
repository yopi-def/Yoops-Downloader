package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.network.ScraperNetworkClient
import com.example.data.repository.DownloadRepository
import com.example.data.repository.MediaRepository
import com.example.data.scraper.ScraperManager
import com.example.downloader.DownloadNotificationHelper
import com.example.downloader.MoriDownloadEngine

class MoriApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var networkClient: ScraperNetworkClient
        private set

    lateinit var scraperManager: ScraperManager
        private set

    lateinit var notificationHelper: DownloadNotificationHelper
        private set

    lateinit var downloadEngine: MoriDownloadEngine
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        networkClient = ScraperNetworkClient(timeoutSeconds = 30L)
        scraperManager = ScraperManager(networkClient, settingsDataStore)
        notificationHelper = DownloadNotificationHelper(this)
        downloadEngine = MoriDownloadEngine(this, database, settingsDataStore, notificationHelper)
        mediaRepository = MediaRepository(scraperManager)
        downloadRepository = DownloadRepository(this, database, downloadEngine)
    }

    companion object {
        lateinit var instance: MoriApplication
            private set
    }
}
