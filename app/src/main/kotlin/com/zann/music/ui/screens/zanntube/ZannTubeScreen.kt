package com.zann.music.ui.screens.zanntube

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zann.music.playback.PlayerManager

private const val HOME_URL = "https://m.youtube.com/?hl=id"
private val TubeBlack = Color(0xFF0F0F0F)
private val TubeRed = Color(0xFFFF0033)

/** Hosts that stay inside ZannTube (YouTube itself and Google's login/consent pages). */
private val INTERNAL_HOSTS = listOf(
    "youtube.com", "youtu.be", "google.com", "gstatic.com", "googleusercontent.com", "ggpht.com", "googlevideo.com",
)

/**
 * ZannTube: the real YouTube mobile site (Beranda, Shorts, Langganan, Notifikasi, Anda), with the ad clean-up and
 * fullscreen player a ReVanced-style client gives you. No NewPipe involved here.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZannTubeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(100) }
    var failed by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var musicPausedForVideo by remember { mutableStateOf(false) }

    fun setFullscreen(on: Boolean) {
        val act = activity ?: return
        val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
        if (on) {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(act.window, false)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun exitFullscreen() {
        customCallback?.onCustomViewHidden()
        customCallback = null
        customView = null
        setFullscreen(false)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (customView != null) setFullscreen(false)
            CookieManager.getInstance().flush()
            ZtBg.detach()
        }
    }

    BackHandler {
        val web = webView
        when {
            customView != null -> exitFullscreen()
            web != null && web.canGoBack() -> web.goBack()
            else -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(TubeBlack)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (progress < 100) {
                Box(Modifier.fillMaxWidth(progress / 100f).height(2.dp).background(TubeRed))
            } else {
                Box(Modifier.height(2.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setBackgroundColor(android.graphics.Color.parseColor("#0F0F0F"))
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportMultipleWindows(false)
                                // Without the "; wv" marker YouTube and Google treat this as a normal mobile browser.
                                userAgentString = userAgentString.replace("; wv", "")
                            }
                            val wv = this; CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                setAcceptThirdPartyCookies(wv, true)
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val uri = request.url
                                    val scheme = uri.scheme.orEmpty()
                                    if (scheme != "http" && scheme != "https") {
                                        // intent:// "open in the YouTube app" and friends stay out.
                                        return true
                                    }
                                    val host = uri.host.orEmpty()
                                    if (INTERNAL_HOSTS.any { host == it || host.endsWith(".$it") }) return false
                                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                    return true
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    failed = false
                                    view.evaluateJavascript(ZT_ADS_JS, null)
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
            view?.evaluateJavascript(ZT_HIDE_BAR_JS, null)
            view?.evaluateJavascript(ZT_EXTRA_JS, null)
                                    view.evaluateJavascript(CLEANUP_SCRIPT, null)
                                    canGoBack = view.canGoBack()
                                }

                                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                    canGoBack = view.canGoBack()
                                    view.evaluateJavascript(CLEANUP_SCRIPT, null)
                                    // A video is about to play here: the music steps aside instead of talking over it.
                                    val watching = url?.contains("/watch") == true || url?.contains("/shorts/") == true
                                    if (watching && !musicPausedForVideo) {
                                        musicPausedForVideo = true
                                        PlayerManager.pause()
                                    } else if (!watching) {
                                        musicPausedForVideo = false
                                    }
                                }

                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? {
                                    return if (ZtAdBlock.isAd(request.url)) ZtAdBlock.empty() else super.shouldInterceptRequest(view, request)
                                }

                                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                    if (request.isForMainFrame) failed = true
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                    customCallback?.onCustomViewHidden()
                                    customView = view
                                    customCallback = callback
                                    setFullscreen(true)
                                }

                                override fun onHideCustomView() {
                                    customView = null
                                    customCallback = null
                                    setFullscreen(false)
                                }
                            }
                            addJavascriptInterface(ZtBridge(ctx, this), "ZtBridge")
                            loadUrl(HOME_URL)
                            webView = this
                        }
                    },
                    onRelease = { view ->
                        view.stopLoading()
                        view.loadUrl("about:blank")
                        view.destroy()
                        if (webView === view) webView = null
                    },
                )
                if (failed) {
                    Column(
                        Modifier.fillMaxSize().background(TubeBlack).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Text("Tidak bisa membuka YouTube. Cek koneksi internetmu.", color = Color.White, textAlign = TextAlign.Center)
                        Button(
                            onClick = { failed = false; webView?.reload() },
                            modifier = Modifier.padding(top = 16.dp),
                        ) { Text("Coba lagi") }
                    }
                }
            }
            ZtChatDialog(webView)
            if (!ZtBg.pip.value) ZtBottomBar(webView, { onBack() }, false)
            if (false)
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().background(TubeBlack),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            ) {
                listOf(
                    "Beranda" to "https://m.youtube.com/?hl=id",
                    "Shorts" to "https://m.youtube.com/shorts",
                    "Langganan" to "https://m.youtube.com/feed/subscriptions?hl=id",
                    "Anda" to "https://m.youtube.com/feed/you?hl=id",
                    "Chat" to "zt://chat",
                ).forEach { (label, url) ->
                    androidx.compose.material3.TextButton(onClick = { if (url == "zt://chat") ZtChat.open.value = true else webView?.loadUrl(url) }) {
                        Text(label, color = Color.White)
                    }
                }
            }
        }
        ZtMiniPlayer(webView)
        customView?.let { video ->
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { video },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Runs on every page: hides ad slots and Premium nags, and skips video ads the moment they show
 * (jump to the end of the ad, press "Lewati"). Installed once per page; the timer survives YouTube's in-page navigation.
 */
private const val CLEANUP_SCRIPT = """
(function () {
  if (window.__zannTube) return;
  window.__zannTube = true;
  var css = [
    'ytm-promoted-sparkles-web-renderer', 'ytm-promoted-video-renderer', 'ytm-companion-slot',
    'ytm-ad-slot-renderer', 'ad-slot-renderer', 'ytm-in-feed-ad-layout-renderer',
    'ytm-statement-banner-renderer', 'ytm-brand-video-singleton-renderer', 'ytm-banner-promo-renderer',
    'ytm-mealbar-promo-renderer', 'ytm-upsell-dialog-renderer', '#player-ads', '.ytp-ad-overlay-container',
    '.ytp-ad-module', '.video-ads'
  ].join(',') + '{display:none !important;}';
  var style = document.createElement('style');
  style.textContent = css;
  (document.head || document.documentElement).appendChild(style);
  setInterval(function () {
    try {
      var player = document.querySelector('#movie_player.ad-showing, .html5-video-player.ad-showing');
      if (!player) return;
      var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-container button');
      if (skip) skip.click();
      var v = document.querySelector('video');
      if (v && isFinite(v.duration) && v.duration > 0) v.currentTime = v.duration;
    } catch (e) {}
  }, 400);
})();
"""
