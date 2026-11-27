package com.zann.music.ui.screens.zanntube

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.zann.music.core.Http
import okhttp3.Request
import org.json.JSONArray

class ZtBridge(private val ctx: Context, private val web: WebView) {
    private val main = Handler(Looper.getMainLooper())
    private val idRe = Regex("^[A-Za-z0-9_-]{11}$")

    init {
        ZtBg.attach(ctx, web)
    }

    @JavascriptInterface
    fun minimize(id: String, t: Int, title: String, channel: String) {
        if (!idRe.matches(id)) return
        main.post { ZtMini.open(id, t, title, channel) }
    }

    @JavascriptInterface
    fun download(id: String, title: String, audioOnly: Boolean) {
        if (!idRe.matches(id)) return
        ZtDownload.start(ctx.applicationContext, id, title, audioOnly)
    }

    @JavascriptInterface
    fun bgState(id: String, playing: Boolean, title: String, channel: String, pos: Int, dur: Int, rate: Double, pip: Boolean) {
        if (id.isNotEmpty() && !idRe.matches(id)) return
        val r = if (rate > 0.0 && rate < 16.0) rate.toFloat() else 1f
        main.post { ZtBg.update(id, playing, title.take(200), channel.take(100), pos, dur, r, pip) }
    }

    @JavascriptInterface
    fun sponsor(id: String) {
        if (!idRe.matches(id)) return
        Thread {
            try {
                val url = "https://sponsor.ajay.app/api/skipSegments?videoID=" + id +
                    "&categories=%5B%22sponsor%22%2C%22selfpromo%22%2C%22interaction%22%2C%22intro%22%2C%22outro%22%5D"
                Http.base.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    val text = if (r.isSuccessful) r.body?.string() else null
                    val arr = JSONArray(text ?: "[]")
                    val out = JSONArray()
                    for (i in 0 until arr.length()) {
                        val seg = arr.getJSONObject(i).optJSONArray("segment")
                        if (seg != null && seg.length() == 2) out.put(seg)
                    }
                    main.post { web.evaluateJavascript("window.__ztSb('" + id + "'," + out.toString() + ")", null) }
                }
            } catch (e: Exception) {
                // tanpa data SponsorBlock, video tetap jalan biasa
            }
        }.start()
    }
}
