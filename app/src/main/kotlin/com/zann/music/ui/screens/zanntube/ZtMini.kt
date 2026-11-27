package com.zann.music.ui.screens.zanntube

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.math.roundToInt

object ZtMini {
    val videoId = mutableStateOf<String?>(null)
    val start = mutableStateOf(0)
    val title = mutableStateOf("")
    val channel = mutableStateOf("")
    val playing = mutableStateOf(true)
    val failed = mutableStateOf(false)

    fun open(id: String, t: Int, ti: String, ch: String) {
        start.value = t
        title.value = ti
        channel.value = ch
        playing.value = true
        failed.value = false
        videoId.value = id
    }

    fun close() {
        videoId.value = null
    }
}

class ZtMiniBridge {
    @JavascriptInterface
    fun onState(s: Int) {
        ZtMini.playing.value = (s == 1 || s == 3)
    }

    @JavascriptInterface
    fun onError(c: Int) {
        ZtMini.failed.value = true
    }
}

private fun ztWatchId(url: String): String? {
    if (!url.contains("youtube.com/watch")) return null
    return Regex("[?&]v=([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
}

private const val ZT_PROBE = """(function(){var v=document.querySelector('video');var o=document.querySelector('ytm-slim-owner-renderer');return JSON.stringify({t:v?v.currentTime:0,p:v?v.paused:true,e:v?v.ended:false,ti:document.title,ch:((o&&o.innerText)||'').split('\n')[0]});})()"""

private fun ztMiniHtml(id: String, start: Int): String = """<html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{width:100%;height:100%}</style></head><body><div id="p"></div><script src="https://www.youtube.com/iframe_api"></script><script>
var pl;
function onYouTubeIframeAPIReady(){pl=new YT.Player('p',{width:'100%',height:'100%',videoId:'$id',playerVars:{autoplay:1,playsinline:1,controls:0,start:$start,rel:0,modestbranding:1,fs:0,disablekb:1,origin:'https://www.youtube.com'},events:{onReady:function(e){e.target.playVideo();},onStateChange:function(e){ZtMiniBridge.onState(e.data);},onError:function(e){ZtMiniBridge.onError(e.data);}}});}
function ztToggle(){if(!pl)return;if(pl.getPlayerState()==1)pl.pauseVideo();else pl.playVideo();}
function ztTime(){return(pl&&pl.getCurrentTime)?pl.getCurrentTime():0;}
</script></body></html>"""

@Composable
private fun ZtMiniTracker(webView: WebView?) {
    LaunchedEffect(webView) {
        var lastId: String? = null
        var lastT = 0
        var lastTitle = ""
        var lastCh = ""
        var lastPlaying = false
        while (true) {
            val w = webView
            if (w != null) {
                val id = ztWatchId(w.url ?: "")
                if (id != null) {
                    if (ZtMini.videoId.value != null) ZtMini.close()
                    lastId = id
                    w.evaluateJavascript(ZT_PROBE) { r ->
                        try {
                            val s = JSONTokener(r).nextValue() as? String
                            if (s != null) {
                                val o = JSONObject(s)
                                lastT = o.optDouble("t", 0.0).toInt()
                                lastPlaying = !o.optBoolean("p", true) && !o.optBoolean("e", false)
                                lastTitle = o.optString("ti", "").removeSuffix(" - YouTube")
                                lastCh = o.optString("ch", "")
                            }
                        } catch (e: Exception) {
                        }
                    }
                } else if (lastId != null) {
                    if (lastPlaying) {
                        w.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause();})", null)
                        ZtMini.open(lastId!!, lastT, lastTitle, lastCh)
                    }
                    lastId = null
                }
            }
            delay(1000)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZtMiniPlayer(webView: WebView?) {
    ZtMiniTracker(webView)
    val id = ZtMini.videoId.value ?: return
    val density = LocalDensity.current
    var ox by remember(id) { mutableStateOf(0f) }
    var oy by remember(id) { mutableStateOf(with(density) { -96.dp.toPx() }) }
    val holder = remember(id) { arrayOfNulls<WebView>(1) }
    val playing = ZtMini.playing.value

    fun expand() {
        val go = { t: Int ->
            webView?.loadUrl("https://m.youtube.com/watch?v=$id&t=${t}s")
            ZtMini.close()
        }
        val m = holder[0]
        if (m == null) {
            go(ZtMini.start.value)
        } else {
            m.evaluateJavascript("ztTime()") { r ->
                go((r?.toDoubleOrNull() ?: ZtMini.start.value.toDouble()).toInt())
            }
        }
    }

    LaunchedEffect(ZtMini.failed.value) { if (ZtMini.failed.value) expand() }
    DisposableEffect(id) {
        onDispose {
            holder[0]?.destroy()
            holder[0] = null
        }
    }

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(ox.roundToInt(), oy.roundToInt()),
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .width(300.dp)
                .clip(shape)
                .background(Color(0xFF1C1B1F))
                .border(1.dp, Color(0x40FFFFFF), shape),
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                            addJavascriptInterface(ZtMiniBridge(), "ZtMiniBridge")
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(
                                "https://www.youtube.com", ztMiniHtml(id, ZtMini.start.value),
                                "text/html", "utf-8", null,
                            )
                            holder[0] = this
                        }
                    },
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { holder[0]?.evaluateJavascript("ztToggle()", null) })
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                ox += drag.x
                                oy += drag.y
                            }
                        },
                )
                ZtMiniBtn(Modifier.align(Alignment.TopStart).padding(8.dp), { expand() }) { zmExpand(Color.White) }
                ZtMiniBtn(Modifier.align(Alignment.TopEnd).padding(8.dp), { ZtMini.close() }) { zmClose(Color.White) }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            ox += drag.x
                            oy += drag.y
                        }
                    }
                    .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        ZtMini.title.value.ifEmpty { "ZannTube" },
                        color = Color(0xFFE6E0E9), fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        ZtMini.channel.value.ifEmpty { "Mini player" },
                        color = Color(0xFFCAC4D0), fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A4458))
                        .clickable { holder[0]?.evaluateJavascript("ztToggle()", null) },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(22.dp)) { if (playing) zmPause(Color(0xFFD0BCFF)) else zmPlay(Color(0xFFD0BCFF)) }
                }
            }
        }
    }
}

@Composable
private fun ZtMiniBtn(modifier: Modifier, onClick: () -> Unit, draw: DrawScope.() -> Unit) {
    Box(
        modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x99303033))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) { draw() }
    }
}

private fun DrawScope.zmClose(c: Color) {
    val w = size.width
    drawLine(c, Offset(w * .15f, w * .15f), Offset(w * .85f, w * .85f), strokeWidth = w * .14f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * .85f, w * .15f), Offset(w * .15f, w * .85f), strokeWidth = w * .14f, cap = StrokeCap.Round)
}

private fun DrawScope.zmExpand(c: Color) {
    val w = size.width
    val a = w * .1f
    val b = w * .42f
    val e = w * .9f
    val f = w * .58f
    val sw = w * .13f
    drawLine(c, Offset(a, a), Offset(b, a), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(a, a), Offset(a, b), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(e, a), Offset(f, a), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(e, a), Offset(e, b), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(a, e), Offset(b, e), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(a, e), Offset(a, f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(e, e), Offset(f, e), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(e, e), Offset(e, f), strokeWidth = sw, cap = StrokeCap.Round)
}

private fun DrawScope.zmPause(c: Color) {
    val w = size.width
    val h = size.height
    drawRoundRect(c, Offset(w * .15f, h * .1f), Size(w * .26f, h * .8f), CornerRadius(w * .06f))
    drawRoundRect(c, Offset(w * .59f, h * .1f), Size(w * .26f, h * .8f), CornerRadius(w * .06f))
}

private fun DrawScope.zmPlay(c: Color) {
    val w = size.width
    val h = size.height
    val p = Path().apply {
        moveTo(w * .2f, h * .08f); lineTo(w * .92f, h * .5f); lineTo(w * .2f, h * .92f); close()
    }
    drawPath(p, c)
}
