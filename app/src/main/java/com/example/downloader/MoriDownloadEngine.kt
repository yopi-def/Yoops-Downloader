package com.example.downloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryEntity
import com.example.data.local.SettingsDataStore
import com.example.domain.model.DownloadOption
import com.example.domain.model.DownloadStatus
import com.example.domain.model.DownloadTask
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaType
import com.example.utils.FileUtils
import com.example.utils.Formatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MoriDownloadEngine(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val notificationHelper: DownloadNotificationHelper
) {
    private val TAG = "MoriDownloadEngine"
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun enqueueDownload(mediaInfo: MediaInfo, option: DownloadOption): String {
        val taskId = UUID.randomUUID().toString()
        val newTask = DownloadTask(
            id = taskId,
            mediaInfo = mediaInfo,
            selectedOption = option,
            status = DownloadStatus.QUEUED,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = option.size.coerceAtLeast(0L),
            createdAt = System.currentTimeMillis()
        )

        _tasks.value = _tasks.value + newTask
        processQueue()
        return taskId
    }

    fun pauseDownload(taskId: String) {
        val job = activeJobs[taskId]
        job?.cancel()
        activeJobs.remove(taskId)

        updateTask(taskId) {
            it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
        }
        processQueue()
    }

    fun resumeDownload(taskId: String) {
        val task = _tasks.value.firstOrNull { it.id == taskId } ?: return
        if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
            updateTask(taskId) {
                it.copy(status = DownloadStatus.QUEUED, errorMessage = null)
            }
            processQueue()
        }
    }

    fun cancelDownload(taskId: String) {
        val job = activeJobs[taskId]
        job?.cancel()
        activeJobs.remove(taskId)

        updateTask(taskId) {
            it.copy(status = DownloadStatus.CANCELLED, speedBytesPerSec = 0L)
        }
        notificationHelper.cancelNotification(taskId.hashCode())
        processQueue()
    }

    fun retryDownload(taskId: String) {
        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.QUEUED,
                progress = 0f,
                downloadedBytes = 0L,
                speedBytesPerSec = 0L,
                errorMessage = null
            )
        }
        processQueue()
    }

    fun removeTask(taskId: String) {
        cancelDownload(taskId)
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
    }

    fun clearCompleted() {
        _tasks.value = _tasks.value.filter {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED
        }
    }

    @Synchronized
    private fun processQueue() {
        val maxConcurrent = settingsDataStore.settings.value.concurrentDownloads
        val activeCount = _tasks.value.count { it.status == DownloadStatus.DOWNLOADING }

        if (activeCount >= maxConcurrent) return

        val slotsAvailable = maxConcurrent - activeCount
        val queuedTasks = _tasks.value
            .filter { it.status == DownloadStatus.QUEUED }
            .take(slotsAvailable)

        for (task in queuedTasks) {
            startDownloadJob(task)
        }
    }

    private fun startDownloadJob(task: DownloadTask) {
        val taskId = task.id
        updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }

        val job = engineScope.launch {
            try {
                executeDownloadStream(task)
            } catch (ce: CancellationException) {
                // Job was paused or cancelled intentionally
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for task $taskId", e)
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.localizedMessage ?: "Gagal mengunduh file",
                        speedBytesPerSec = 0L
                    )
                }
                notificationHelper.showErrorNotification(
                    taskId.hashCode(),
                    task.mediaInfo.title,
                    e.localizedMessage ?: "Terjadi kesalahan"
                )
            } finally {
                activeJobs.remove(taskId)
                processQueue()
            }
        }
        activeJobs[taskId] = job
    }

    private suspend fun executeDownloadStream(task: DownloadTask) = withContext(Dispatchers.IO) {
        val taskId = task.id
        val option = task.selectedOption
        val mediaInfo = task.mediaInfo
        val filename = option.filename
        val mimeType = FileUtils.getMimeType(filename)

        val requestBuilder = Request.Builder()
            .url(option.url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")

        if (task.downloadedBytes > 0) {
            requestBuilder.header("Range", "bytes=${task.downloadedBytes}-")
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful && response.code != 206) {
            val errCode = response.code
            response.close()
            throw IllegalStateException("Server mengembalikan status HTTP $errCode")
        }

        val body = response.body ?: throw IllegalStateException("Respons unduhan kosong")
        val contentLen = body.contentLength()
        val totalBytes = if (contentLen > 0) {
            if (task.downloadedBytes > 0) task.downloadedBytes + contentLen else contentLen
        } else task.totalBytes

        val (targetUri, outputStream) = prepareOutputStream(filename, mimeType, mediaInfo.mediaType)

        var downloaded = task.downloadedBytes
        val buffer = ByteArray(32 * 1024)
        var bytesRead: Int
        val inputStream: InputStream = body.byteStream()

        var lastUpdateTime = System.currentTimeMillis()
        var lastBytes = downloaded

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateTime
                if (elapsed >= 500) {
                    val bytesDelta = downloaded - lastBytes
                    val speed = if (elapsed > 0) (bytesDelta * 1000) / elapsed else 0L
                    val progress = if (totalBytes > 0) (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

                    lastUpdateTime = now
                    lastBytes = downloaded

                    updateTask(taskId) {
                        it.copy(
                            progress = progress,
                            downloadedBytes = downloaded,
                            totalBytes = totalBytes,
                            speedBytesPerSec = speed
                        )
                    }

                    if (settingsDataStore.settings.value.notificationsEnabled) {
                        val percent = (progress * 100).toInt()
                        notificationHelper.showProgressNotification(
                            taskId.hashCode(),
                            mediaInfo.title,
                            percent,
                            Formatters.formatSpeed(speed)
                        )
                    }
                }
            }

            outputStream.flush()
        } finally {
            try { outputStream.close() } catch (_: Exception) {}
            try { inputStream.close() } catch (_: Exception) {}
            try { response.close() } catch (_: Exception) {}
        }

        // Complete!
        val completedTask = task.copy(
            status = DownloadStatus.COMPLETED,
            progress = 1f,
            downloadedBytes = downloaded,
            totalBytes = downloaded,
            speedBytesPerSec = 0L,
            localUri = targetUri.toString(),
            completedAt = System.currentTimeMillis()
        )

        updateTask(taskId) { completedTask }

        // Save to Room History
        val historyItem = HistoryEntity(
            title = mediaInfo.title,
            author = mediaInfo.author,
            platform = mediaInfo.platform.displayName,
            originalUrl = mediaInfo.originalUrl,
            downloadUrl = option.url,
            thumbnailUrl = mediaInfo.thumbnail,
            localUri = targetUri.toString(),
            filePath = targetUri.path,
            fileSize = downloaded,
            mimeType = mimeType,
            format = option.format,
            quality = option.quality,
            status = "COMPLETED",
            timestamp = System.currentTimeMillis()
        )
        database.historyDao().insertHistory(historyItem)

        if (settingsDataStore.settings.value.notificationsEnabled) {
            notificationHelper.cancelNotification(taskId.hashCode())
            notificationHelper.showCompletedNotification(taskId.hashCode(), mediaInfo.title)
        }
    }

    private fun prepareOutputStream(
        filename: String,
        mimeType: String,
        mediaType: MediaType
    ): Pair<Uri, OutputStream> {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collectionUri = when (mediaType) {
                MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                MediaType.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                MediaType.IMAGE, MediaType.CAROUSEL -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val relativePath = when (mediaType) {
                MediaType.VIDEO -> Environment.DIRECTORY_MOVIES + "/MORI"
                MediaType.AUDIO -> Environment.DIRECTORY_MUSIC + "/MORI"
                MediaType.IMAGE, MediaType.CAROUSEL -> Environment.DIRECTORY_PICTURES + "/MORI"
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }

            val uri = resolver.insert(collectionUri, values)
                ?: throw IllegalStateException("Gagal membuat entri penyimpanan MediaStore")

            val stream = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("Gagal membuka stream output MediaStore")

            return Pair(uri, stream)
        } else {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val moriDir = File(downloadDir, "MORI").apply { if (!exists()) mkdirs() }
            val file = File(moriDir, filename)
            val uri = Uri.fromFile(file)
            return Pair(uri, FileOutputStream(file))
        }
    }

    private fun updateTask(taskId: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) block(it) else it
        }
    }
}
