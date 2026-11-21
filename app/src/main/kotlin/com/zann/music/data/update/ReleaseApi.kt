package com.zann.music.data.update

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ReleaseInfo(
    val version: AppVersion,
    val tag: String,
    val name: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val pageUrl: String,
    val checksumUrl: String? = null,
)

object ReleaseApi {
    const val REPO = "Yenalie/VYORA"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"
    private const val LATEST = "https://api.github.com/repos/$REPO/releases/latest"

    val client: OkHttpClient = com.zann.music.core.Http.with(10, 30)

    // ZANN MUSIC: pengecekan update dimatikan karena repo asli (VYORA) bukan milik build ini.
    // Untuk mengaktifkannya lagi: isi REPO dengan repo GitHub sendiri dan tulis ulang latest() agar memanggil LATEST.
    @Throws(IOException::class)
    fun latest(): ReleaseInfo? = null

    fun parse(json: String): ReleaseInfo? {
        val root = JSONObject(json)
        if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
        val tag = root.optString("tag_name")
        val version = AppVersion.parse(tag) ?: return null
        val assets = root.optJSONArray("assets") ?: return null
        var best: JSONObject? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
            if (best == null || asset.optString("name").contains("release", ignoreCase = true)) best = asset
        }
        val apk = best ?: return null
        val checksumName = apk.optString("name") + ".sha256"
        var checksumUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name").equals(checksumName, ignoreCase = true)) {
                checksumUrl = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
            }
        }
        return ReleaseInfo(
            version = version,
            tag = tag,
            name = root.optString("name").ifBlank { tag },
            notes = cleanNotes(root.optString("body")),
            apkUrl = apk.optString("browser_download_url").takeIf { it.startsWith("https://") } ?: return null,
            apkName = apk.optString("name"),
            apkSize = apk.optLong("size"),
            pageUrl = root.optString("html_url").ifBlank { RELEASES_PAGE },
            checksumUrl = checksumUrl,
        )
    }

    internal fun cleanNotes(body: String): String = body
        .replace("\r\n", "\n")
        .lines()
        .filterNot { it.trimStart().startsWith(">") }
        .joinToString("\n") { line ->
            line.trimEnd()
                .replace(Regex("^#{1,6}\\s*"), "")
                .replace(Regex("^\\s*[-*]\\s+"), "• ")
                .replace("**", "")
                .replace("`", "")
        }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
