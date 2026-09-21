package com.example.data.network

import android.util.Log
import com.example.domain.model.ScraperError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class ScraperNetworkClient(
    timeoutSeconds: Long = 30L
) {
    private val TAG = "MoriNetworkClient"

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        userAgent: String = DEFAULT_USER_AGENT
    ): String = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,application/json,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")

        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        val request = requestBuilder.build()
        try {
            val response: Response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                when (code) {
                    404 -> throw ScraperError.MediaNotFound()
                    429 -> throw ScraperError.RateLimited()
                    503, 502 -> throw ScraperError.SourceUnavailable()
                    else -> throw ScraperError.ExtractionFailed("HTTP $code dari server target")
                }
            }
            response.body?.string() ?: throw ScraperError.ExtractionFailed("Respons kosong dari server")
        } catch (e: SocketTimeoutException) {
            throw ScraperError.Timeout()
        } catch (e: IOException) {
            throw ScraperError.NetworkError(cause = e)
        } catch (e: ScraperError) {
            throw e
        } catch (e: Exception) {
            throw ScraperError.UnknownError(cause = e)
        }
    }

    suspend fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap(),
        userAgent: String = DEFAULT_USER_AGENT
    ): String = withContext(Dispatchers.IO) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                when (code) {
                    429 -> throw ScraperError.RateLimited()
                    503, 502 -> throw ScraperError.SourceUnavailable()
                    else -> throw ScraperError.ExtractionFailed("HTTP $code saat menghubungi extraction provider")
                }
            }
            response.body?.string() ?: throw ScraperError.ExtractionFailed("Respons kosong dari server")
        } catch (e: SocketTimeoutException) {
            throw ScraperError.Timeout()
        } catch (e: IOException) {
            throw ScraperError.NetworkError(cause = e)
        } catch (e: ScraperError) {
            throw e
        } catch (e: Exception) {
            throw ScraperError.UnknownError(cause = e)
        }
    }

    suspend fun getFileSize(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()
            val response = okHttpClient.newCall(request).execute()
            val len = response.header("Content-Length")?.toLongOrNull() ?: -1L
            response.close()
            len
        } catch (_: Exception) {
            -1L
        }
    }
}
