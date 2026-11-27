package com.zann.music.ui.screens.zanntube

import androidx.compose.foundation.layout.Arrangement
import android.content.Context
import android.content.ContextWrapper
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val ZT_HOME = "https://m.youtube.com/?hl=id"
private const val ZT_SUBS = "https://m.youtube.com/feed/subscriptions?hl=id"
private const val ZT_YOU = "https://m.youtube.com/feed/you?hl=id"

@Composable
fun ZtBottomBar(
    webView: WebView?,
    onMusic: (() -> Unit)?,
    navPad: Boolean,
    modifier: Modifier = Modifier,
) {
    var url by remember { mutableStateOf("") }
    LaunchedEffect(webView) {
        while (true) {
            url = webView?.url ?: ""
            delay(400)
        }
    }
    val ctx = LocalContext.current
    val sel = when {
        url.contains("/feed/subscriptions") -> 1
        url.contains("/feed/you") || url.contains("/feed/library") || url.contains("/feed/history") -> 3
        else -> 0
    }
    val goMusic: () -> Unit = {
        if (onMusic != null) {
            onMusic()
        } else {
            var c: Context = ctx
            while (c is ContextWrapper && c !is ComponentActivity) c = c.baseContext
            (c as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
        }
    }
    val shape = RoundedCornerShape(38.dp)
    Box(
        modifier
            .then(if (navPad) Modifier.navigationBarsPadding() else Modifier)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xF22A272F), Color(0xF2141218))))
                .border(1.dp, Color(0x40FFFFFF), shape)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZtTab("Beranda", sel == 0, {
                if (sel == 0 && !url.contains("/watch")) webView?.reload() else webView?.loadUrl(ZT_HOME)
            }) { zHome(it) }
            ZtTab("Subscription", sel == 1, { webView?.loadUrl(ZT_SUBS) }) { zSubs(it) }
            ZtTab("Musik", false, goMusic) { zNote(it) }
            ZtTab("Kamu", sel == 3, { webView?.loadUrl(ZT_YOU) }) { zYou(it) }
        }
    }
}

@Composable
private fun RowScope.ZtTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: DrawScope.(Color) -> Unit,
) {
    val tint = if (selected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0)
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(30.dp))
            .background(if (selected) Color(0x665B5566) else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(26.dp)) { icon(tint) }
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun DrawScope.zHome(c: Color) {
    val w = size.width
    val h = size.height
    val p = Path().apply {
        moveTo(w * .5f, h * .08f); lineTo(w * .96f, h * .5f); lineTo(w * .82f, h * .5f)
        lineTo(w * .82f, h * .92f); lineTo(w * .6f, h * .92f); lineTo(w * .6f, h * .64f)
        lineTo(w * .4f, h * .64f); lineTo(w * .4f, h * .92f); lineTo(w * .18f, h * .92f)
        lineTo(w * .18f, h * .5f); lineTo(w * .04f, h * .5f); close()
    }
    drawPath(p, c)
}

private fun DrawScope.zSubs(c: Color) {
    val w = size.width
    val h = size.height
    drawLine(c, Offset(w * .22f, h * .1f), Offset(w * .78f, h * .1f), strokeWidth = w * .08f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * .13f, h * .25f), Offset(w * .87f, h * .25f), strokeWidth = w * .08f, cap = StrokeCap.Round)
    drawRoundRect(
        c, topLeft = Offset(w * .08f, h * .4f), size = Size(w * .84f, h * .52f),
        cornerRadius = CornerRadius(w * .08f), style = Stroke(width = w * .08f),
    )
    val t = Path().apply {
        moveTo(w * .42f, h * .52f); lineTo(w * .42f, h * .8f); lineTo(w * .67f, h * .66f); close()
    }
    drawPath(t, c)
}

private fun DrawScope.zNote(c: Color) {
    val w = size.width
    val h = size.height
    drawCircle(c, radius = w * .18f, center = Offset(w * .36f, h * .78f))
    drawLine(c, Offset(w * .52f, h * .78f), Offset(w * .52f, h * .1f), strokeWidth = w * .1f)
    val f = Path().apply {
        moveTo(w * .52f, h * .1f); lineTo(w * .9f, h * .1f); lineTo(w * .9f, h * .32f); lineTo(w * .52f, h * .32f); close()
    }
    drawPath(f, c)
}

private fun DrawScope.zYou(c: Color) {
    val w = size.width
    val h = size.height
    drawCircle(c, radius = w * .44f, center = Offset(w * .5f, h * .5f), style = Stroke(width = w * .08f))
    drawCircle(c, radius = w * .14f, center = Offset(w * .5f, h * .4f))
    drawArc(
        c, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(w * .26f, h * .52f), size = Size(w * .48f, h * .36f),
        style = Stroke(width = w * .07f, cap = StrokeCap.Round),
    )
}
