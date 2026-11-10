package com.gentlemanjoel.jot

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apiAssetUrl: String?,
    val browserUrl: String?,
    val notes: String,
)

object Updater {

    /** Single source of truth lives in Prefs so Settings and the updater can't drift. */
    fun savedToken(context: Context): String = Prefs.token(context)

    fun saveToken(context: Context, token: String) = Prefs.setToken(context, token)

    /** Returns the latest release if it is newer than the installed build, else null. */
    suspend fun check(context: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = savedToken(context)
            val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Jot-Android")
                if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            val code = conn.responseCode
            if (code == 404) {
                throw IllegalStateException(
                    "No release found. If the repo is private, add a GitHub token in Settings."
                )
            }
            if (code !in 200..299) {
                throw IllegalStateException("GitHub returned HTTP $code")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            conn.disconnect()

            val tag = json.optString("tag_name").removePrefix("v").trim()
            val notes = json.optString("body").take(1200)
            val assets = json.optJSONArray("assets")

            var apiUrl: String? = null
            var browserUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apiUrl = a.optString("url")
                        browserUrl = a.optString("browser_download_url")
                        break
                    }
                }
            }

            if (tag.isBlank() || isNewer(tag, BuildConfig.VERSION_NAME).not()) {
                null
            } else {
                UpdateInfo(tag, apiUrl, browserUrl, notes)
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val token = savedToken(context)
            val useApi = token.isNotBlank() && info.apiAssetUrl != null
            val target = info.apiAssetUrl.takeIf { useApi } ?: info.browserUrl
                ?: throw IllegalStateException("Release has no APK attached.")

            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "Jot-${info.version}.apk")

            var current = URL(target)
            var redirects = 0
            var conn: HttpURLConnection
            while (true) {
                conn = (current.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "Jot-Android")
                    setRequestProperty("Accept", "application/octet-stream")
                    if (useApi && current.host == "api.github.com") {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
                val code = conn.responseCode
                if (code in 300..399 && redirects < 5) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    current = URL(loc)
                    redirects++
                    continue
                }
                if (code !in 200..299) throw IllegalStateException("Download failed: HTTP $code")
                break
            }

            val total = conn.contentLength.toLong()
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(16 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        done += read
                        if (total > 0) onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            conn.disconnect()
            out
        }
    }

    fun installApk(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun releasesPageUrl(): String =
        "https://github.com/${BuildConfig.GITHUB_REPO}/releases/latest"

    /** true when [candidate] is a higher version than [installed]. */
    fun isNewer(candidate: String, installed: String): Boolean {
        val a = candidate.split('.', '-').mapNotNull { it.toIntOrNull() }
        val b = installed.split('.', '-').mapNotNull { it.toIntOrNull() }
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
