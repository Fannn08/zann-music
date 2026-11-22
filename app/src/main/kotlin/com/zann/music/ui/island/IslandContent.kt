package com.zann.music.ui.island

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zann.music.data.island.IslandButtonStyle
import com.zann.music.data.island.IslandConfig
import com.zann.music.data.island.IslandContentMode
import com.zann.music.data.island.IslandEqStyle
import com.zann.music.data.lyrics.Lyrics
import com.zann.music.data.lyrics.LyricsRepository
import com.zann.music.data.island.IslandSettings
import com.zann.music.data.island.IslandShape
import com.zann.music.data.island.IslandTapAction
import com.zann.music.playback.PlayerManager
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

internal fun IslandConfig.accentColor(): Color = Color(accent.argb)

/**
 * Isi overlay. Dipasang oleh [IslandOverlayService]; posisi layar (x/y) diatur lewat
 * WindowManager di service, jadi di sini hanya kapsul <-> kartu diperluas.
 */
@Composable
fun IslandOverlay(onOpenApp: () -> Unit) {
    val cfg by IslandSettings.config.collectAsState()
    val appForeground by IslandOverlayService.appForeground.collectAsState()
    val playing by PlayerManager.isPlaying.collectAsState()
    val item by PlayerManager.currentItem.collectAsState()
    val title by PlayerManager.currentTitle.collectAsState()
    val artist by PlayerManager.currentArtist.collectAsState()
    val thumb by PlayerManager.currentThumbnail.collectAsState()
    val position by PlayerManager.positionMs.collectAsState()
    val duration by PlayerManager.durationMs.collectAsState()

    val realDensity = LocalContext.current.resources.displayMetrics.density
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    var hiddenForId by remember { mutableStateOf<String?>(null) }
    var lastSeenId by remember { mutableStateOf<String?>(null) }
    var interactTick by remember { mutableIntStateOf(0) }

    val trackId = item?.id
    // Lagu berganti: munculkan lagi dan tampilkan kartu sebentar.
    LaunchedEffect(trackId) {
        if (trackId != null) {
            hiddenForId = null
            if (lastSeenId != null && lastSeenId != trackId) expanded = true
            lastSeenId = trackId
        }
    }
    LaunchedEffect(expanded, interactTick, cfg.autoCollapseSec) {
        if (expanded && cfg.autoCollapseSec > 0) {
            delay(cfg.autoCollapseSec * 1000L)
            expanded = false
        }
    }
    LaunchedEffect(appForeground) { if (appForeground) expanded = false }

    val visible = cfg.enabled && !appForeground && item != null && hiddenForId != trackId

    // Lirik sinkron dari LyricsRepository (sama dengan yang dipakai pemutar), hanya diambil bila island tampil.
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    val wantLyrics = visible && (cfg.showLyrics || cfg.fitLyricsToCamera)
    LaunchedEffect(trackId, wantLyrics) {
        lyrics = null
        val cur = item
        if (wantLyrics && cur != null) {
            lyrics = runCatching {
                LyricsRepository.lyricsFor(cur.id, cur.title, cur.artist, (duration / 1000).coerceAtLeast(0))
            }.getOrNull()
        }
    }
    val lyric by remember { derivedStateOf { lyrics?.synced?.lastOrNull { it.timeMs <= position && it.text.isNotBlank() }?.text } }
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density * cfg.scalePercent / 100f, base.fontScale)) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) { -it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(200)),
        ) {
            val accent = cfg.accentColor()
            Box(Modifier.animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))) {
                if (expanded) {
                    ExpandedCard(
                        cfg = cfg, title = title.orEmpty(), artist = artist.orEmpty(), thumb = thumb,
                        playing = playing, position = position, duration = duration, lyric = lyric, accent = accent,
                        onOpenApp = onOpenApp,
                        onCollapse = { expanded = false },
                        onInteract = { interactTick++ },
                    )
                } else {
                    Capsule(
                        cfg = cfg, thumb = thumb, playing = playing, lyric = lyric, accent = accent,
                        onTap = {
                            if (cfg.vibrate) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (cfg.tapAction) {
                                IslandTapAction.EXPAND -> expanded = true
                                IslandTapAction.OPEN_APP -> onOpenApp()
                                IslandTapAction.PLAY_PAUSE -> PlayerManager.togglePlayPause()
                                IslandTapAction.NONE -> Unit
                            }
                        },
                        onSwipeUp = { hiddenForId = trackId },
                        onLongPress = { if (cfg.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onMoveByPx = { dx, dy ->
                            IslandSettings.update {
                                it.copy(
                                    offsetXDp = (it.offsetXDp + dx / realDensity).toInt().coerceIn(-300, 300),
                                    topGapDp = (it.topGapDp + dy / realDensity).toInt().coerceIn(-60, 300),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

// ───────────────────────── Kapsul ringkas ─────────────────────────

@Composable
private fun Capsule(
    cfg: IslandConfig,
    thumb: String?,
    playing: Boolean,
    lyric: String?,
    accent: Color,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onLongPress: () -> Unit,
    onMoveByPx: (Float, Float) -> Unit,
) {
    val radius = cfg.capsuleHeightDp / 2f * cfg.cornerPercent / 100f
    val shape = RoundedCornerShape(radius.dp)
    val coverSize = (cfg.capsuleHeightDp - 10).coerceAtLeast(12).dp
    Row(
        modifier = Modifier
            .width(cfg.capsuleWidthDp.dp)
            .height(cfg.capsuleHeightDp.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = cfg.opacityPercent / 100f))
            .let { m -> if (cfg.borderWidthDp > 0) m.border(cfg.borderWidthDp.dp, accent.copy(alpha = 0.5f), shape) else m }
            .islandGestures(onTap = onTap, onSwipeUp = onSwipeUp, onLongPress = onLongPress, onMoveByPx = onMoveByPx)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cfg.content != IslandContentMode.EQ_ONLY) Cover(thumb, coverSize, cfg.coverShape)
        if (cfg.fitLyricsToCamera && !lyric.isNullOrBlank()) {
            val (left, right) = splitLyric(lyric)
            Text(
                left, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            Spacer(Modifier.width(cfg.cameraGapDp.dp))
            Text(
                right, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (cfg.content != IslandContentMode.COVER_ONLY) {
            Equalizer(cfg.eqStyle, accent, playing, Modifier.padding(end = 6.dp).size(width = 20.dp, height = 14.dp))
        }
    }
}

private fun splitLyric(text: String): Pair<String, String> {
    val words = text.trim().split(' ')
    if (words.size < 2) return text to ""
    val mid = words.size / 2
    return words.take(mid).joinToString(" ") to words.drop(mid).joinToString(" ")
}

// ───────────────────────── Kartu diperluas ─────────────────────────

@Composable
private fun ExpandedCard(
    cfg: IslandConfig,
    title: String,
    artist: String,
    thumb: String?,
    playing: Boolean,
    position: Long,
    duration: Long,
    lyric: String?,
    accent: Color,
    onOpenApp: () -> Unit,
    onCollapse: () -> Unit,
    onInteract: () -> Unit,
) {
    val shape = RoundedCornerShape((if (cfg.cardSameCorner) 32f * cfg.cornerPercent / 100f else 28f).dp)
    val g = cfg.glowIntensityPercent / 100f
    val textScale = cfg.textSizePercent / 100f
    val coverSize = (56 * cfg.coverSizePercent / 100f).dp
    val borderW = if (cfg.borderWidthDp > 0) cfg.borderWidthDp.dp else if (cfg.glow) 2.dp else 0.dp
    Column(
        modifier = Modifier
            .width(cfg.cardWidthDp.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = cfg.opacityPercent / 100f))
            .let { m ->
                if (cfg.glow) m.background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f * g), Color.Transparent, accent.copy(alpha = 0.22f * g)))) else m
            }
            .let { m ->
                if (borderW > 0.dp) {
                    val a = if (cfg.glow) 0.6f * g else 0.35f
                    m.border(borderW, Brush.verticalGradient(listOf(accent.copy(alpha = a), accent.copy(alpha = a * 0.25f), accent.copy(alpha = a))), shape)
                } else m
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -40f) onCollapse() },
                ) { _, dy -> total += dy }
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenApp() }) {
                Cover(thumb, coverSize, cfg.coverShape)
            }
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenApp() },
            ) {
                Text(title, color = Color.White, fontSize = (17 * textScale).sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (cfg.showArtist && artist.isNotBlank()) {
                    Text(artist, color = Color.White.copy(alpha = 0.6f), fontSize = (14 * textScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (cfg.showLyrics && !lyric.isNullOrBlank()) {
                    Text(lyric, color = Color.White, fontSize = (13 * textScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Equalizer(cfg.eqStyle, accent, playing, Modifier.padding(top = 4.dp).size(width = 22.dp, height = 16.dp))
        }
        if (cfg.showProgress) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fmt(position), color = Color.White.copy(alpha = 0.6f), fontSize = (11 * textScale).sp)
                ProgressBar(
                    fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                    color = accent,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    onSeek = { f -> if (duration > 0) { PlayerManager.seekTo((duration * f).toLong()); onInteract() } },
                )
                Text("-" + fmt((duration - position).coerceAtLeast(0)), color = Color.White.copy(alpha = 0.6f), fontSize = (11 * textScale).sp)
            }
        }
        if (cfg.showButtons) {
            val filled = cfg.buttonStyle == IslandButtonStyle.FILLED
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CtrlButton(if (filled) Icons.Filled.SkipPrevious else Icons.Outlined.SkipPrevious, 30.dp) { PlayerManager.previous(); onInteract() }
                CtrlButton(
                    if (playing) (if (filled) Icons.Filled.Pause else Icons.Outlined.Pause) else (if (filled) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow),
                    38.dp,
                ) { PlayerManager.togglePlayPause(); onInteract() }
                CtrlButton(if (filled) Icons.Filled.SkipNext else Icons.Outlined.SkipNext, 30.dp) { PlayerManager.next(); onInteract() }
                CtrlButton(Icons.Filled.Lyrics, 24.dp, tint = Color.White.copy(alpha = if (cfg.showLyrics) 1f else 0.45f)) {
                    IslandSettings.update { it.copy(showLyrics = !it.showLyrics) }; onInteract()
                }
            }
        }
    }
}

@Composable
private fun CtrlButton(icon: ImageVector, size: Dp, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        Modifier.size(size + 14.dp).clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size)) }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color, modifier: Modifier, onSeek: (Float) -> Unit) {
    Box(
        modifier.height(20.dp).pointerInput(Unit) {
            detectTapGestures { o -> onSeek((o.x / size.width).coerceIn(0f, 1f)) }
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
        Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(CircleShape).background(Color.White))
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ───────────────────────── Komponen kecil ─────────────────────────

@Composable
private fun Cover(url: String?, size: Dp, kind: IslandShape) {
    val shape = when (kind) {
        IslandShape.CIRCLE -> CircleShape
        IslandShape.SQUARE -> RoundedCornerShape(3.dp)
        IslandShape.ROUNDED_SQUARE -> RoundedCornerShape(size * 0.28f)
    }
    Box(
        Modifier.size(size).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFE86BFF), Color(0xFF6B7BFF)))),
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Equalizer(style: IslandEqStyle, color: Color, playing: Boolean, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = List(5) { i ->
        t.animateFloat(
            initialValue = 0.25f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(380 + i * 90, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar$i",
        )
    }
    val phase = t.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)), label = "wave",
    )
    val idle = floatArrayOf(0.35f, 0.55f, 0.35f, 0.75f, 1f)
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        when (style) {
            IslandEqStyle.BARS -> {
                val bw = w / 9f
                for (i in 0 until 5) {
                    val f = if (playing) bars[i].value else idle[i] * 0.6f
                    val bh = (h * f).coerceAtLeast(bw)
                    drawLine(color, Offset(bw * (1 + i * 2) + bw / 2, (h + bh) / 2), Offset(bw * (1 + i * 2) + bw / 2, (h - bh) / 2), strokeWidth = bw, cap = StrokeCap.Round)
                }
            }
            IslandEqStyle.WAVE -> {
                var prev = Offset(0f, h / 2)
                val amp = if (playing) h * 0.38f else h * 0.08f
                val steps = 24
                for (s in 1..steps) {
                    val x = w * s / steps
                    val y = h / 2 + amp * sin(phase.value + s * 0.5f)
                    val cur = Offset(x, y)
                    drawLine(color, prev, cur, strokeWidth = h * 0.14f, cap = StrokeCap.Round)
                    prev = cur
                }
            }
            IslandEqStyle.DOTS -> {
                val r = h * 0.12f
                for (i in 0 until 5) {
                    val f = if (playing) bars[i].value else 0.5f
                    drawCircle(color.copy(alpha = 0.35f + 0.65f * f), r * (0.7f + 0.8f * f), Offset(w * (i + 0.5f) / 5f, h / 2))
                }
            }
        }
    }
}

/**
 * Gestur kapsul lewat MotionEvent mentah, supaya geseran tetap benar walau window ikut bergerak:
 * ketuk = onTap, usap ke atas = onSwipeUp, tekan lama lalu seret = pindahkan.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.islandGestures(
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onLongPress: () -> Unit,
    onMoveByPx: (Float, Float) -> Unit,
): Modifier = composed {
    val handler = remember { Handler(Looper.getMainLooper()) }
    val slop = LocalDensity.current.density * 10f
    val st = remember { GestureState() }
    val currentLongPress by rememberUpdatedState(onLongPress)
    val longPress = remember { Runnable { st.dragging = true; currentLongPress() } }
    pointerInteropFilter { ev ->
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                st.downX = ev.rawX; st.downY = ev.rawY; st.lastX = ev.rawX; st.lastY = ev.rawY
                st.moved = false; st.dragging = false
                handler.postDelayed(longPress, 450L)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!st.dragging && (abs(ev.rawX - st.downX) > slop || abs(ev.rawY - st.downY) > slop)) {
                    st.moved = true
                    handler.removeCallbacks(longPress)
                }
                if (st.dragging) onMoveByPx(ev.rawX - st.lastX, ev.rawY - st.lastY)
                st.lastX = ev.rawX; st.lastY = ev.rawY
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                val up = st.downY - ev.rawY
                when {
                    st.dragging -> Unit
                    st.moved && up > slop * 2 && abs(ev.rawX - st.downX) < up -> onSwipeUp()
                    !st.moved -> onTap()
                }
                st.dragging = false
            }
            MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(longPress); st.dragging = false }
        }
        true
    }
}

private class GestureState {
    var downX = 0f; var downY = 0f; var lastX = 0f; var lastY = 0f
    var moved = false; var dragging = false
}

// ───────────────────────── Pratinjau di layar Pengaturan ─────────────────────────

@Composable
fun IslandPreview(cfg: IslandConfig, expanded: Boolean) {
    val title = androidx.compose.ui.res.stringResource(com.zann.music.R.string.island_sample_title)
    val artist = androidx.compose.ui.res.stringResource(com.zann.music.R.string.island_sample_artist)
    val lyric = androidx.compose.ui.res.stringResource(com.zann.music.R.string.island_sample_lyric)
    val base = LocalDensity.current
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val fit = cfg.copy(cardWidthDp = minOf(cfg.cardWidthDp, maxWidth.value.toInt() - 8))
        CompositionLocalProvider(LocalDensity provides Density(base.density * cfg.scalePercent / 100f, base.fontScale)) {
            Box(Modifier.animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))) {
                if (expanded) {
                    ExpandedCard(
                        cfg = fit, title = title, artist = artist, thumb = null, playing = true,
                        position = 42_000L, duration = 180_000L, lyric = lyric, accent = cfg.accentColor(),
                        onOpenApp = {}, onCollapse = {}, onInteract = {},
                    )
                } else {
                    Capsule(
                        cfg = cfg, thumb = null, playing = true, lyric = lyric, accent = cfg.accentColor(),
                        onTap = {}, onSwipeUp = {}, onLongPress = {}, onMoveByPx = { _, _ -> },
                    )
                }
            }
        }
    }
}
