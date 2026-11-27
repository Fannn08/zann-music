package com.zann.music.ui.screens.zanntube

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import com.zann.music.core.Http
import com.zann.music.data.settings.AudioQuality
import com.zann.music.data.youtube.ResolvedStream
import com.zann.music.data.youtube.StreamResolver
import java.io.IOException
import java.io.OutputStream
import okhttp3.Request

object ZtDownload {
    private val main = Handler(Looper.getMainLooper())
    private const val CHUNK = 4L * 1024 * 1024

    private fun toast(ctx: Context, msg: String) {
        main.post { Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
    }

    fun start(ctx: Context, id: String, title: String, audioOnly: Boolean) {
        toast(ctx, "Menyiapkan unduhan... biarkan aplikasi tetap terbuka")
        Thread {
            var uri: Uri? = null
            try {
                val rs: ResolvedStream = if (audioOnly) {
                    StreamResolver.resolveAudio(id, AudioQuality.HIGH)
                } else {
                    StreamResolver.resolveVideo(id) ?: throw IOException("Tidak ada stream video")
                }
                var mime = rs.mimeType.substringBefore(';').trim()
                if (mime.isBlank()) mime = if (audioOnly) "audio/mp4" else "video/mp4"
                val ext = when {
                    mime.contains("webm") -> "webm"
                    audioOnly -> "m4a"
                    else -> "mp4"
                }
                val safe = title.replace(Regex("[\\\\/:*?\"<>|\\n\\r]"), " ").trim().take(80).ifBlank { id }
                val name = "$safe.$ext"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ZannTube")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val target = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Gagal membuat file")
                uri = target
                val stream = ctx.contentResolver.openOutputStream(target) ?: throw IOException("Gagal menulis file")
                stream.use { copyRanged(rs, it) }
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                ctx.contentResolver.update(target, done, null, null)
                toast(ctx, "Selesai: Download/ZannTube/$name")
            } catch (e: Exception) {
                uri?.let { runCatching { ctx.contentResolver.delete(it, null, null) } }
                toast(ctx, "Unduhan gagal: " + (e.message ?: "kesalahan"))
            }
        }.start()
    }

    private fun copyRanged(rs: ResolvedStream, out: OutputStream) {
        var pos = 0L
        var total = -1L
        var finished = false
        val buf = ByteArray(64 * 1024)
        while (!finished) {
            val rb = Request.Builder().url(rs.url)
            rs.headers.forEach { (k, v) -> rb.header(k, v) }
            rb.header("Range", "bytes=" + pos + "-" + (pos + CHUNK - 1))
            Http.base.newCall(rb.build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("HTTP " + r.code)
                val body = r.body ?: throw IOException("Respons kosong")
                if (r.code == 200) {
                    body.byteStream().use { ins -> ins.copyTo(out, 64 * 1024) }
                    finished = true
                } else {
                    total = r.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                        ?: throw IOException("Ukuran file tidak diketahui")
                    var n = 0L
                    body.byteStream().use { ins ->
                        while (true) {
                            val k = ins.read(buf)
                            if (k < 0) break
                            out.write(buf, 0, k)
                            n += k
                        }
                    }
                    pos += n
                    if (n == 0L || pos >= total) finished = true
                }
            }
        }
    }
}
