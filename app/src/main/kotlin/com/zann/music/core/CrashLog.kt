package com.zann.music.core

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves the stack trace of a crash to a plain text file so it can be shared without a computer:
 * Android/data/com.zann.music/files/crash-log.txt (reachable from any file manager on Android 10).
 * It only records, then hands the crash on to the system, so behaviour is otherwise unchanged.
 */
object CrashLog {
    private const val FILE_NAME = "crash-log.txt"
    private const val MAX_BYTES = 200_000L

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(app: Context, thread: Thread, error: Throwable) {
        val dir = app.getExternalFilesDir(null) ?: app.filesDir
        val file = File(dir, FILE_NAME)
        if (file.exists() && file.length() > MAX_BYTES) file.delete()
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val report = buildString {
            append("==== ZANN MUSIC crash ====\n")
            append("time: ").append(time).append('\n')
            append("device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("thread: ").append(thread.name).append("\n\n")
            append(trace).append("\n\n")
        }
        file.appendText(report)
    }
}
