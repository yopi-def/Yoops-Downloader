package com.example.utils

import java.net.URI

object UrlCleaner {
    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "igsh", "si", "feature", "t", "ref", "ref_src", "_r"
    )

    fun clean(rawUrl: String): String {
        var trimmed = rawUrl.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }

        return try {
            val uri = URI(trimmed)
            val host = uri.host ?: return trimmed
            val path = uri.path ?: ""
            val query = uri.query

            val cleanQuery = if (!query.isNullOrBlank()) {
                query.split("&")
                    .filterNot { param ->
                        val key = param.substringBefore("=").lowercase()
                        TRACKING_PARAMS.contains(key)
                    }
                    .joinToString("&")
            } else ""

            val scheme = uri.scheme ?: "https"
            if (cleanQuery.isBlank()) {
                "$scheme://$host$path"
            } else {
                "$scheme://$host$path?$cleanQuery"
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.length < 5) return false
        return try {
            val uri = URI(if (!trimmed.startsWith("http")) "https://$trimmed" else trimmed)
            !uri.host.isNullOrBlank() && uri.host.contains(".")
        } catch (_: Exception) {
            false
        }
    }
}
