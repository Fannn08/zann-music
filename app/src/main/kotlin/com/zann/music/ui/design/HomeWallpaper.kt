package com.zann.music.ui.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.max

/** The picture behind the home screen: copied into app storage so it survives the picker's permission ending. */
object HomeWallpaper {
    private const val FILE = "home_background.jpg"
    private const val MAX_EDGE = 1600

    fun file(context: Context): File = File(context.filesDir, FILE)

    private const val VIDEO_PREFIX = "home_background_video_"
    private const val MAX_VIDEO_BYTES = 100L * 1024 * 1024

    /** The chosen picture, or null when the background is a video (or nothing is chosen). */
    fun image(context: Context): File? = file(context).takeIf { it.isFile && it.length() > 0 }

    /** The chosen video. Every import gets a fresh name so the player's cache never serves a stale one. */
    fun video(context: Context): File? = context.filesDir
        .listFiles { f -> f.name.startsWith(VIDEO_PREFIX) && f.length() > 0 }
        ?.maxByOrNull { it.lastModified() }

    /** A picture or a video is chosen. */
    fun exists(context: Context): Boolean = image(context) != null || video(context) != null

    fun remove(context: Context) {
        runCatching { file(context).delete() }
        removeVideos(context)
    }

    private fun removeVideos(context: Context) {
        context.filesDir.listFiles { f -> f.name.startsWith(VIDEO_PREFIX) }?.forEach { runCatching { it.delete() } }
    }

    /** Copies a video into app storage (silent loop behind the home screen). False if unreadable or over 100 MB. */
    fun importVideo(context: Context, uri: Uri): Boolean = runCatching {
        val target = File(context.filesDir, "$VIDEO_PREFIX${System.currentTimeMillis()}.mp4")
        var copied = 0L
        val input = context.contentResolver.openInputStream(uri) ?: return@runCatching false
        input.use { source ->
            target.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = source.read(buffer)
                    if (n < 0) break
                    copied += n
                    if (copied > MAX_VIDEO_BYTES) {
                        target.delete()
                        return@runCatching false
                    }
                    out.write(buffer, 0, n)
                }
            }
        }
        if (copied == 0L) { target.delete(); return@runCatching false }
        context.filesDir.listFiles { f -> f.name.startsWith(VIDEO_PREFIX) && f != target }?.forEach { runCatching { it.delete() } }
        file(context).delete()
        true
    }.getOrDefault(false)

    /** Decodes [uri] down to a screen-sized JPEG. Returns false when the picture could not be read. */
    fun import(context: Context, uri: Uri): Boolean = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching false
        val degrees = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val bitmap = if (degrees == 0f) decoded else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
                .also { if (it !== decoded) decoded.recycle() }
        }
        val temp = File(context.filesDir, "$FILE.tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        val target = file(context)
        target.delete()
        val saved = temp.renameTo(target)
        if (saved) removeVideos(context)
        saved
    }.getOrDefault(false)
}
