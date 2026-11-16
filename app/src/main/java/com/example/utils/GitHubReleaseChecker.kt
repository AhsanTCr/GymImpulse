package com.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseResult(
    val version: String,
    val releaseDate: String,
    val downloadUrl: String?,
    val releasePageUrl: String?,
    val highlights: List<String>
)

/**
 * Queries GitHub Releases REST API (`/repos/<owner>/<repo>/releases/latest`)
 * without extra dependencies or restricted permissions to power Gymimpulse in-app APK updates.
 */
object GitHubReleaseChecker {

    suspend fun fetchLatestRelease(repoSlug: String): GitHubReleaseResult? = withContext(Dispatchers.IO) {
        val cleanSlug = repoSlug.trim().removePrefix("https://github.com/").trim('/')
        if (!cleanSlug.contains('/')) return@withContext null

        val endpoint = "https://api.github.com/repos/$cleanSlug/releases/latest"
        val conn = (URL(endpoint).openConnection() as? HttpURLConnection) ?: return@withContext null
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Gymimpulse-Android-Updater")

            if (conn.responseCode !in 200..299) return@withContext null
            val bodyText = conn.inputStream.bufferedReader().use { it.readText() }
            parseReleaseJson(bodyText)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseReleaseJson(jsonText: String): GitHubReleaseResult? {
        val rawTag = extractJsonStringField(jsonText, "tag_name")?.trim().orEmpty()
        val cleanVersion = rawTag.removePrefix("v").removePrefix("V").trim()
        if (cleanVersion.isBlank()) return null

        val publishedRaw = extractJsonStringField(jsonText, "published_at")?.trim().orEmpty()
        val releaseDate = if (publishedRaw.length >= 10) publishedRaw.take(10) else "Latest Release"
        val htmlUrl = extractJsonStringField(jsonText, "html_url")?.trim()?.takeIf { it.isNotBlank() }

        val assetUrls = extractAllJsonStringFields(jsonText, "browser_download_url")
        val apkDownloadUrl = assetUrls.firstOrNull { it.endsWith(".apk", ignoreCase = true) }

        val rawBody = extractJsonStringField(jsonText, "body").orEmpty()
        val bodyNotes = rawBody
            .lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .filter { it.isNotBlank() && !it.startsWith("**Full Changelog**") }
            .take(5)

        return GitHubReleaseResult(
            version = cleanVersion,
            releaseDate = releaseDate,
            downloadUrl = apkDownloadUrl ?: htmlUrl,
            releasePageUrl = htmlUrl,
            highlights = bodyNotes
        )
    }

    fun isNewerVersion(remote: String, installed: String): Boolean {
        val rParts = remote.trim().removePrefix("v").split(".").map {
            it.takeWhile(Char::isDigit).toIntOrNull() ?: 0
        }
        val iParts = installed.trim().removePrefix("v").split(".").map {
            it.takeWhile(Char::isDigit).toIntOrNull() ?: 0
        }
        val maxLen = maxOf(rParts.size, iParts.size)
        for (idx in 0 until maxLen) {
            val r = rParts.getOrElse(idx) { 0 }
            val i = iParts.getOrElse(idx) { 0 }
            if (r > i) return true
            if (r < i) return false
        }
        return false
    }

    private fun extractJsonStringField(json: String, fieldName: String): String? =
        extractAllJsonStringFields(json, fieldName).firstOrNull()

    private fun extractAllJsonStringFields(json: String, fieldName: String): List<String> {
        val results = mutableListOf<String>()
        val keyPattern = Regex(""""${Regex.escape(fieldName)}"\s*:\s*"""")
        for (match in keyPattern.findAll(json)) {
            var idx = match.range.last + 1
            val sb = StringBuilder()
            while (idx < json.length) {
                val ch = json[idx]
                if (ch == '"') break
                if (ch == '\\' && idx + 1 < json.length) {
                    val next = json[idx + 1]
                    when (next) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        '\\' -> sb.append('\\')
                        '"' -> sb.append('"')
                        '/' -> sb.append('/')
                        else -> sb.append(next)
                    }
                    idx += 2
                } else {
                    sb.append(ch)
                    idx++
                }
            }
            results.add(sb.toString())
        }
        return results
    }
}
