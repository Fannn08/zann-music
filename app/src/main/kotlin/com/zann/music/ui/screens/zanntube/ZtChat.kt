package com.zann.music.ui.screens.zanntube

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

object ZtChat {
    val open = mutableStateOf(false)
}

const val ZT_HIDE_BAR_JS = "(function(){var id='zt-hide';if(document.getElementById(id))return;var s=document.createElement('style');s.id=id;s.textContent='ytm-pivot-bar-renderer,.pivot-bar-renderer,ytm-pivot-bar-item-renderer{display:none !important;}';document.head.appendChild(s);})();"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZtChatDialog(webView: WebView?) {
    if (!ZtChat.open.value) return
    val vid = Uri.parse(webView?.url ?: "").getQueryParameter("v")
    Dialog(onDismissRequest = { ZtChat.open.value = false }) {
        Box(Modifier.fillMaxWidth().height(460.dp).background(Color.Black)) {
            if (vid == null) {
                Text(
                    "Buka dulu video live, lalu ketuk Chat.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = WebViewClient()
                            loadUrl("https://www.youtube.com/live_chat?is_popout=1&v=$vid")
                        }
                    },
                )
            }
        }
    }
}
