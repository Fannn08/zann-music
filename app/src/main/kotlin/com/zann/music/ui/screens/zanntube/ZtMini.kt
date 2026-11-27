package com.zann.music.ui.screens.zanntube

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
    val ready = mutableStateOf(false)
    @Volatile var openedAt = 0L

    fun open(id: String, t: Int, ti: String, ch: String) {
        if (videoId.value == id) return
        openedAt = android.os.SystemClock.uptimeMillis()
        start.value = t
        title.value = ti
        channel.value = ch
        playing.value = true
        failed.value = false
        ready.value = false
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
        ZtMini.ready.value = true
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

private fun ztMiniUrl(id: String, start: Int): String = "https://m.youtube.com/watch?v=" + id + "&t=" + start + "s"

/**
 * Mini player = halaman tonton YouTube asli di WebView kecil, dengan semua tampilan disembunyikan kecuali videonya.
 * Cookie dan login sama dengan layar utama, jadi tidak ada layar hitam seperti iframe embed.
 */
private const val ZT_MINI_JS = """(function(){
if(window.__ztmini)return;window.__ztmini=1;
function over(n,fake){try{Object.defineProperty(document,n,{configurable:true,get:function(){return fake;}});}catch(e){}}
over('hidden',false);over('webkitHidden',false);over('visibilityState','visible');over('webkitVisibilityState','visible');
['visibilitychange','webkitvisibilitychange'].forEach(function(n){window.addEventListener(n,function(e){e.stopImmediatePropagation();},true);document.addEventListener(n,function(e){e.stopImmediatePropagation();},true);});
var css='html,body{background:#000 !important;overflow:hidden !important;}'
+'body *{visibility:hidden !important;}'
+'.html5-video-player,.html5-video-player *{visibility:visible !important;}'
+'.html5-video-player{position:fixed !important;left:0 !important;top:0 !important;right:0 !important;bottom:0 !important;width:100vw !important;height:100vh !important;max-width:none !important;max-height:none !important;margin:0 !important;z-index:2147483647 !important;background:#000 !important;}'
+'.html5-video-container{position:absolute !important;left:0 !important;top:0 !important;width:100% !important;height:100% !important;}'
+'video{position:absolute !important;left:0 !important;top:0 !important;width:100% !important;height:100% !important;object-fit:contain !important;}'
+'.ytp-chrome-top,.ytp-chrome-bottom,.ytp-gradient-top,.ytp-gradient-bottom,.ytp-pause-overlay,.ytp-ce-element,.ytp-endscreen-content,.ytp-watermark,.ytp-unmute,.ytp-unmute-inner,.player-controls-content,.player-controls-background,.player-controls-top,.player-controls-bottom,ytm-custom-control,.ytp-ad-overlay-container,.ytp-ad-module{display:none !important;}';
function addStyle(){
 if(document.getElementById('zt-mini-css'))return;
 var s=document.createElement('style');s.id='zt-mini-css';s.textContent=css;
 (document.head||document.documentElement).appendChild(s);}
function fixAnc(p){
 var a=p.parentElement;
 while(a&&a!==document.documentElement){
  try{a.style.setProperty('transform','none','important');a.style.setProperty('contain','none','important');a.style.setProperty('filter','none','important');}catch(e){}
  a=a.parentElement;}}
function vd(){return document.querySelector('video');}
window.ztToggle=function(){
 var v=vd();if(!v)return;
 if(v.paused){var r=v.play();if(r&&r.catch)r.catch(function(){});}else v.pause();};
window.ztTime=function(){var v=vd();return v?v.currentTime:0;};
var started=false,last=0,fixedEl=null;
function tick(){
 try{
  addStyle();
  var p=document.querySelector('.html5-video-player');
  if(p&&fixedEl!==p){fixedEl=p;fixAnc(p);}
  var v=vd();if(!v)return;
  var ad=p&&(p.classList.contains('ad-showing')||p.classList.contains('ad-interrupting'));
  if(!ad&&v.readyState>0){if(v.muted)v.muted=false;if(v.volume===0)v.volume=1;}
  if(!started){
   if(v.paused&&v.readyState>=2){var r=v.play();if(r&&r.catch)r.catch(function(){});}
   if(!v.paused)started=true;}
  var s=v.paused?2:1;
  if(s!==last){last=s;try{ZtMiniBridge.onState(s);}catch(e){}}
 }catch(e){}
}
setInterval(tick,400);
tick();
})();
"""

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
                    if (ZtMini.videoId.value != null && android.os.SystemClock.uptimeMillis() - ZtMini.openedAt > 1500) ZtMini.close()
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
fun BoxScope.ZtMiniPlayer(webView: WebView?) {
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

    DisposableEffect(id) {
        onDispose {
            holder[0]?.let { it.stopLoading(); it.loadUrl("about:blank"); it.destroy() }
            holder[0] = null
        }
    }

    run {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset { IntOffset(ox.roundToInt(), oy.roundToInt()) }
                .width(300.dp)
                .background(Color(0xFF1C1B1F), shape)
                .border(1.dp, Color(0x40FFFFFF), shape)
                .padding(6.dp),
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.userAgentString = settings.userAgentString.replace("; wv", "")
                            setBackgroundColor(android.graphics.Color.BLACK)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            addJavascriptInterface(ZtMiniBridge(), "ZtMiniBridge")
                            webViewClient = object : WebViewClient() {
                                private fun inject(v: WebView) {
                                    v.evaluateJavascript(ZT_ADS_JS, null)
                                    v.evaluateJavascript(ZT_MINI_JS, null)
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { inject(view) }

                                override fun onPageCommitVisible(view: WebView, url: String?) { inject(view) }

                                override fun onPageFinished(view: WebView, url: String?) { inject(view) }

                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? {
                                    return if (ZtAdBlock.isAd(request.url)) ZtAdBlock.empty() else super.shouldInterceptRequest(view, request)
                                }
                            }
                            loadUrl(ztMiniUrl(id, ZtMini.start.value))
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
                        if (!ZtMini.ready.value) "Memuat video..." else ZtMini.channel.value.ifEmpty { "Mini player" },
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
