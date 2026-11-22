package com.zann.music.ui.island

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.zann.music.data.island.IslandAccent
import com.zann.music.data.island.IslandButtonStyle
import com.zann.music.data.island.IslandConfig
import com.zann.music.data.island.IslandContentMode
import com.zann.music.data.island.IslandEqStyle
import com.zann.music.data.island.IslandSettings
import com.zann.music.data.island.IslandShape
import com.zann.music.data.island.IslandTapAction
import com.zann.music.data.lyrics.LyricLine
import com.zann.music.data.lyrics.Lyrics
import com.zann.music.data.lyrics.LyricsRepository
import com.zann.music.playback.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal fun IslandConfig.accentColor(): Color = Color(accent.argb)

/** Baris lirik ditampilkan sedikit lebih awal supaya tidak terasa telat. */
private const val LYRIC_LOOKAHEAD_MS = 200L
private const val NOTE = "♪"

private data class LyricPair(val cur: String, val next: String)

private enum class CapLyric { NONE, CENTER, FIT }

private fun lyricPairAt(lines: List<LyricLine>?, pos: Long): LyricPair? {
    if (lines.isNullOrEmpty()) return null
    var cur = -1
    for (i in lines.indices) {
        if (lines[i].timeMs - LYRIC_LOOKAHEAD_MS > pos) break
        if (lines[i].text.isNotBlank()) cur = i
    }
    var next = ""
    for (j in (cur + 1) until lines.size) {
        if (lines[j].text.isNotBlank()) { next = lines[j].text.trim(); break }
    }
    return LyricPair(if (cur >= 0) lines[cur].text.trim() else "", next)
}

private data class IslandDims(val capDp: Float, val cardDp: Float)

/** Lebar kapsul dan kartu (dalam dp setelah skala), dibatasi lebar layar. */
private fun islandDims(cfg: IslandConfig, mode: CapLyric, maxW: Float): IslandDims {
    val cover = (cfg.capsuleHeightDp - 10).coerceAtLeast(12)
    val cap = when (mode) {
        // Dua sisi kamera masing-masing dapat ~80 dp teks, jadi tidak terpotong jadi "Ab…".
        CapLyric.FIT -> maxOf(cfg.capsuleWidthDp, 14 + cover + 26 + 16 + cfg.cameraGapDp + 160)
        CapLyric.CENTER -> maxOf(cfg.capsuleWidthDp, 240)
        CapLyric.NONE -> cfg.capsuleWidthDp
    }.toFloat().coerceAtMost(maxW)
    val card = maxOf(cfg.cardWidthDp.toFloat().coerceAtMost(maxW), cap)
    return IslandDims(cap, card)
}

/**
 * Isi overlay. Dipasang oleh [IslandOverlayService]; posisi layar (x/y) diatur lewat
 * WindowManager di service. Kapsul <-> kartu bergerak lewat satu permukaan yang digambar
 * (bukan mengubah ukuran window tiap frame), jadi animasinya mulus.
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
    // Posisi berubah terus; jangan dibaca di sini supaya seluruh overlay tidak ikut recompose tiap tick.
    val positionState = PlayerManager.positionMs.collectAsState()
    val durationState = PlayerManager.durationMs.collectAsState()

    val ctx = LocalContext.current
    val realDensity = ctx.resources.displayMetrics.density
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
    val wantLyrics = visible && (cfg.showLyrics || cfg.fitLyricsToCamera || cfg.showCapsuleLyrics)
    LaunchedEffect(trackId, wantLyrics) {
        lyrics = null
        val cur = item
        if (wantLyrics && cur != null) {
            lyrics = runCatching {
                LyricsRepository.lyricsFor(cur.id, cur.title, cur.artist, (durationState.value / 1000).coerceAtLeast(0))
            }.getOrNull()
        }
    }
    val hasLyrics = lyrics?.isSynced == true
    val lyricPair by remember { derivedStateOf { lyricPairAt(lyrics?.synced, positionState.value) } }

    val capMode = when {
        !hasLyrics -> CapLyric.NONE
        cfg.fitLyricsToCamera -> CapLyric.FIT
        cfg.showCapsuleLyrics -> CapLyric.CENTER
        else -> CapLyric.NONE
    }
    val scale = cfg.scalePercent / 100f
    val screenDp = ctx.resources.displayMetrics.widthPixels / realDensity
    val dims = islandDims(cfg, capMode, (screenDp / scale - 12f).coerceAtLeast(160f))
    val capW by animateDpAsState(dims.capDp.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "capW")

    val coverAccent = rememberCoverAccent(thumb, cfg.accent == IslandAccent.AUTO && visible, cfg.accentColor())
    val accent = if (cfg.accent == IslandAccent.AUTO) coverAccent else cfg.accentColor()

    val base = LocalDensity.current
    val scaledDensity = remember(base, cfg.scalePercent) { Density(base.density * cfg.scalePercent / 100f, base.fontScale) }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) { -it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(200)),
        ) {
            IslandSurface(
                cfg = cfg,
                expanded = expanded,
                accent = accent,
                capWidth = capW,
                cardWidth = dims.cardDp.dp,
                capsuleModifier = Modifier.islandGestures(
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
                ),
                capsuleContent = { CapsuleContent(cfg, thumb, playing, capMode, lyricPair, accent) },
                cardContent = {
                    CardContent(
                        cfg = cfg, title = title.orEmpty(), artist = artist.orEmpty(), thumb = thumb,
                        playing = playing, position = positionState, duration = durationState,
                        pair = lyricPair, hasLyrics = hasLyrics, accent = accent,
                        onOpenApp = onOpenApp,
                        onCollapse = { expanded = false },
                        onInteract = { interactTick++ },
                    )
                },
            )
        }
    }
}

// ───────────────────────── Permukaan yang berubah bentuk ─────────────────────────

/**
 * Satu permukaan hitam yang menggambar dirinya sendiri sebagai kapsul atau kartu.
 *
 * Kenapa tidak lagi memakai animateContentSize: ukuran window WRAP_CONTENT ikut berubah di setiap
 * frame dan tiap perubahan itu memaksa sistem menata ulang window (berat, jadi patah-patah).
 * Di sini ukuran layout hanya berganti satu kali (kapsul -> ukuran kartu penuh); perubahan bentuk
 * yang terlihat murni digambar (clip + alpha) sehingga tidak ada relayout window selama animasi.
 */
@Composable
private fun IslandSurface(
    cfg: IslandConfig,
    expanded: Boolean,
    accent: Color,
    capWidth: Dp,
    cardWidth: Dp,
    capsuleModifier: Modifier,
    capsuleContent: @Composable () -> Unit,
    cardContent: @Composable () -> Unit,
) {
    val progress = remember { Animatable(if (expanded) 1f else 0f) }
    var cardShown by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded) {
            cardShown = true
            progress.animateTo(1f, spring(dampingRatio = 0.84f, stiffness = 340f))
        } else {
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 460f))
            cardShown = false
        }
    }

    val capH = cfg.capsuleHeightDp.dp
    val capR = capH / 2f * (cfg.cornerPercent / 100f)
    val cardR = (if (cfg.cardSameCorner) 32f * cfg.cornerPercent / 100f else 28f).dp
    val glow = cfg.glowIntensityPercent / 100f
    val bg = Color.Black.copy(alpha = cfg.opacityPercent / 100f)

    val surface = Modifier.drawWithContent {
        val p = progress.value.coerceIn(0f, 1f)
        val capWpx = capWidth.toPx()
        val capHpx = capH.toPx()
        val w = capWpx + (size.width - capWpx) * p
        val h = capHpx + (size.height - capHpx) * p
        val left = (size.width - w) / 2f
        val radius = (capR.toPx() + (cardR.toPx() - capR.toPx()) * p).coerceAtMost(minOf(w, h) / 2f)
        val path = Path().apply {
            addRoundRect(RoundRect(left, 0f, left + w, h, CornerRadius(radius)))
        }
        clipPath(path) {
            drawRect(bg)
            if (cfg.glow && glow > 0f) {
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.36f * glow * (0.3f + 0.7f * p)), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.8f,
                    ),
                )
            }
            this@drawWithContent.drawContent()
        }
        val border: Brush = when {
            cfg.borderWidthDp > 0 -> SolidColor(accent.copy(alpha = 0.55f))
            cfg.glow -> Brush.verticalGradient(listOf(accent.copy(alpha = 0.2f + 0.4f * glow * p), Color.White.copy(alpha = 0.06f)))
            else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f)))
        }
        drawPath(path, border, style = Stroke(width = (if (cfg.borderWidthDp > 0) cfg.borderWidthDp.dp else 1.dp).toPx()))
    }

    Layout(
        modifier = surface,
        content = {
            Box(
                Modifier
                    .layoutId("cap")
                    .graphicsLayer { alpha = (1f - progress.value * 3.2f).coerceIn(0f, 1f) }
                    .then(capsuleModifier),
            ) { capsuleContent() }
            if (cardShown) {
                Box(
                    Modifier
                        .layoutId("card")
                        .graphicsLayer {
                            val p = progress.value.coerceIn(0f, 1f)
                            alpha = ((p - 0.2f) / 0.6f).coerceIn(0f, 1f)
                            val s = 0.94f + 0.06f * p
                            scaleX = s; scaleY = s
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        },
                ) { cardContent() }
            }
        },
    ) { measurables, _ ->
        val capWpx = capWidth.roundToPx()
        val capHpx = capH.roundToPx()
        val cap = measurables.first { it.layoutId == "cap" }.measure(Constraints.fixed(capWpx, capHpx))
        val cardM = measurables.firstOrNull { it.layoutId == "card" }
        if (cardM == null) {
            layout(capWpx, capHpx) { cap.place(0, 0) }
        } else {
            val cardWpx = maxOf(cardWidth.roundToPx(), capWpx)
            val card = cardM.measure(Constraints(minWidth = cardWpx, maxWidth = cardWpx, minHeight = 0, maxHeight = Constraints.Infinity))
            val w = cardWpx
            val h = maxOf(card.height, capHpx)
            layout(w, h) {
                cap.place((w - capWpx) / 2, 0)
                card.place(0, 0)
            }
        }
    }
}

// ───────────────────────── Isi kapsul ─────────────────────────

@Composable
private fun CapsuleContent(
    cfg: IslandConfig,
    thumb: String?,
    playing: Boolean,
    mode: CapLyric,
    pair: LyricPair?,
    accent: Color,
) {
    val coverSize = (cfg.capsuleHeightDp - 10).coerceAtLeast(12).dp
    Row(
        Modifier.fillMaxSize().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cfg.content != IslandContentMode.EQ_ONLY) Cover(thumb, coverSize, cfg.coverShape)
        val cur = pair?.cur.orEmpty()
        when (mode) {
            CapLyric.FIT -> FitLyric(cur, cfg.cameraGapDp.dp)
            CapLyric.CENTER -> CenterLyric(cur)
            CapLyric.NONE -> Spacer(Modifier.weight(1f))
        }
        if (cfg.content != IslandContentMode.COVER_ONLY) {
            Equalizer(cfg.eqStyle, accent, playing, Modifier.padding(end = 6.dp).size(width = 20.dp, height = 14.dp))
        }
    }
}

private fun lyricTransition(): ContentTransform =
    (slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(260, delayMillis = 60))) togetherWith
        (slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(160)))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.CenterLyric(text: String) {
    AnimatedContent(
        targetState = text,
        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        transitionSpec = { lyricTransition() },
        contentAlignment = Alignment.Center,
        label = "capLyric",
    ) { t ->
        Text(
            t, color = Color.White.copy(alpha = 0.94f), fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.FitLyric(text: String, gap: Dp) {
    AnimatedContent(
        targetState = text,
        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        transitionSpec = { lyricTransition() },
        contentAlignment = Alignment.Center,
        label = "capLyricFit",
    ) { t ->
        val (left, right) = splitLyric(t)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                left, color = Color.White.copy(alpha = 0.94f), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(horizontal = 3.dp).basicMarquee(),
            )
            Spacer(Modifier.width(gap))
            Text(
                right, color = Color.White.copy(alpha = 0.94f), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f).padding(horizontal = 3.dp).basicMarquee(),
            )
        }
    }
}

private fun splitLyric(text: String): Pair<String, String> {
    val words = text.trim().split(' ').filter { it.isNotEmpty() }
    if (words.size < 2) return text to ""
    val mid = (words.size + 1) / 2
    return words.take(mid).joinToString(" ") to words.drop(mid).joinToString(" ")
}

// ───────────────────────── Isi kartu diperluas ─────────────────────────

@Composable
private fun CardContent(
    cfg: IslandConfig,
    title: String,
    artist: String,
    thumb: String?,
    playing: Boolean,
    position: State<Long>,
    duration: State<Long>,
    pair: LyricPair?,
    hasLyrics: Boolean,
    accent: Color,
    onOpenApp: () -> Unit,
    onCollapse: () -> Unit,
    onInteract: () -> Unit,
) {
    val textScale = cfg.textSizePercent / 100f
    val coverSize = (58 * cfg.coverSizePercent / 100f).dp
    val collapse by rememberUpdatedState(onCollapse)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -40f) collapse() },
                ) { _, dy -> total += dy }
            }
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickableNoRipple(onOpenApp)) { Cover(thumb, coverSize, cfg.coverShape) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp).clickableNoRipple(onOpenApp)) {
                Text(
                    title, color = Color.White, fontSize = (16 * textScale).sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (cfg.showArtist && artist.isNotBlank()) {
                    Text(
                        artist, color = Color.White.copy(alpha = 0.62f), fontSize = (13 * textScale).sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Equalizer(cfg.eqStyle, accent, playing, Modifier.size(width = 24.dp, height = 18.dp))
        }
        if (cfg.showLyrics && hasLyrics) LyricBlock(pair, textScale, accent)
        if (cfg.showProgress) {
            ProgressRow(
                position = position, duration = duration, accent = accent, textScale = textScale,
                onSeek = { f ->
                    val d = duration.value
                    if (d > 0) { PlayerManager.seekTo((d * f).toLong()); onInteract() }
                },
            )
        }
        if (cfg.showButtons) {
            val filled = cfg.buttonStyle == IslandButtonStyle.FILLED
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CtrlButton(if (filled) Icons.Filled.SkipPrevious else Icons.Outlined.SkipPrevious, 28.dp) { PlayerManager.previous(); onInteract() }
                PlayButton(playing, filled) { PlayerManager.togglePlayPause(); onInteract() }
                CtrlButton(if (filled) Icons.Filled.SkipNext else Icons.Outlined.SkipNext, 28.dp) { PlayerManager.next(); onInteract() }
                CtrlButton(Icons.Filled.Lyrics, 22.dp, tint = Color.White.copy(alpha = if (cfg.showLyrics) 1f else 0.45f)) {
                    IslandSettings.update { it.copy(showLyrics = !it.showLyrics) }; onInteract()
                }
            }
        }
    }
}

/** Baris lirik sekarang (tebal, sampai 2 baris) dan baris berikutnya (redup), pindah dengan geser halus. */
@Composable
private fun LyricBlock(pair: LyricPair?, textScale: Float, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height((68 * textScale).dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight(0.62f).clip(CircleShape).background(accent))
        Spacer(Modifier.width(11.dp))
        AnimatedContent(
            targetState = pair ?: LyricPair("", ""),
            modifier = Modifier.weight(1f),
            transitionSpec = { lyricTransition() },
            contentAlignment = Alignment.CenterStart,
            label = "cardLyric",
        ) { p ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                Text(
                    p.cur.ifBlank { NOTE }, color = Color.White, fontSize = (15 * textScale).sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (p.next.isNotBlank()) {
                    Text(
                        p.next, color = Color.White.copy(alpha = 0.42f), fontSize = (12 * textScale).sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(
    position: State<Long>,
    duration: State<Long>,
    accent: Color,
    textScale: Float,
    onSeek: (Float) -> Unit,
) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val pos = position.value
    val dur = duration.value
    val d = drag
    val fraction = d ?: if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val shown = if (d != null) (dur * d).toLong() else pos
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fmt(shown), color = Color.White.copy(alpha = 0.6f), fontSize = (11 * textScale).sp)
        ProgressBar(
            fraction = fraction, color = accent, active = d != null,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            onDrag = { drag = it }, onSeek = onSeek,
        )
        Text("-" + fmt((dur - shown).coerceAtLeast(0)), color = Color.White.copy(alpha = 0.6f), fontSize = (11 * textScale).sp)
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
    active: Boolean,
    modifier: Modifier,
    onDrag: (Float?) -> Unit,
    onSeek: (Float) -> Unit,
) {
    val seek by rememberUpdatedState(onSeek)
    val thickness by animateDpAsState(if (active) 8.dp else 5.dp, label = "barH")
    Canvas(
        modifier
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { o -> seek((o.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = { o -> last = (o.x / size.width).coerceIn(0f, 1f); onDrag(last) },
                    onDragEnd = { seek(last); onDrag(null) },
                    onDragCancel = { onDrag(null) },
                ) { change, _ ->
                    last = (change.position.x / size.width).coerceIn(0f, 1f)
                    onDrag(last)
                }
            },
    ) {
        val th = thickness.toPx()
        val top = (size.height - th) / 2f
        drawRoundRect(Color.White.copy(alpha = 0.2f), Offset(0f, top), Size(size.width, th), CornerRadius(th / 2f))
        val fw = size.width * fraction
        if (fw > 0f) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(color, Color.White), startX = 0f, endX = size.width),
                Offset(0f, top), Size(fw.coerceAtLeast(th), th), CornerRadius(th / 2f),
            )
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ───────────────────────── Tombol ─────────────────────────

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

@Composable
private fun CtrlButton(icon: ImageVector, size: Dp, tint: Color = Color.White, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.82f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "btn")
    Box(
        Modifier.size(size + 18.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size)) }
}

@Composable
private fun PlayButton(playing: Boolean, filled: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "play")
    Box(
        Modifier.size(50.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape)
            .let { m -> if (filled) m.background(Color.White) else m.border(1.5.dp, Color.White, CircleShape) }
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = playing, animationSpec = tween(140), label = "playIcon") { p ->
            Icon(
                if (p) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null,
                tint = if (filled) Color.Black else Color.White, modifier = Modifier.size(30.dp),
            )
        }
    }
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

private val IdleBars = floatArrayOf(0.35f, 0.55f, 0.35f, 0.75f, 1f)

/** Animasi hanya berjalan saat musik diputar; saat jeda digambar diam supaya tidak membebani frame. */
@Composable
private fun Equalizer(style: IslandEqStyle, color: Color, playing: Boolean, modifier: Modifier) {
    if (playing) {
        LiveEqualizer(style, color, modifier)
    } else {
        Canvas(modifier) {
            drawEq(style, color, active = false, phase = 0f) { i -> if (style == IslandEqStyle.BARS) IdleBars[i] * 0.6f else 0.5f }
        }
    }
}

@Composable
private fun LiveEqualizer(style: IslandEqStyle, color: Color, modifier: Modifier) {
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
    Canvas(modifier) {
        drawEq(style, color, active = true, phase = phase.value) { i -> bars[i].value }
    }
}

private fun DrawScope.drawEq(style: IslandEqStyle, color: Color, active: Boolean, phase: Float, level: (Int) -> Float) {
    val h = size.height
    val w = size.width
    when (style) {
        IslandEqStyle.BARS -> {
            val bw = w / 9f
            for (i in 0 until 5) {
                val bh = (h * level(i)).coerceAtLeast(bw)
                val x = bw * (1 + i * 2) + bw / 2
                drawLine(color, Offset(x, (h + bh) / 2), Offset(x, (h - bh) / 2), strokeWidth = bw, cap = StrokeCap.Round)
            }
        }
        IslandEqStyle.WAVE -> {
            var prev = Offset(0f, h / 2)
            val amp = if (active) h * 0.38f else h * 0.08f
            val steps = 24
            for (s in 1..steps) {
                val cur = Offset(w * s / steps, h / 2 + amp * sin(phase + s * 0.5f))
                drawLine(color, prev, cur, strokeWidth = h * 0.14f, cap = StrokeCap.Round)
                prev = cur
            }
        }
        IslandEqStyle.DOTS -> {
            val r = h * 0.12f
            for (i in 0 until 5) {
                val f = level(i)
                drawCircle(color.copy(alpha = 0.35f + 0.65f * f), r * (0.7f + 0.8f * f), Offset(w * (i + 0.5f) / 5f, h / 2))
            }
        }
    }
}

// ───────────────────────── Warna aksen dari cover ─────────────────────────

/** Warna "Otomatis": diambil dari cover lagu (cerah dan jenuh), pindah dengan fade halus. */
@Composable
private fun rememberCoverAccent(url: String?, enabled: Boolean, fallback: Color): Color {
    val ctx = LocalContext.current.applicationContext
    var found by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(url, enabled) {
        if (!enabled || url.isNullOrBlank()) {
            found = null
            return@LaunchedEffect
        }
        val c = withContext(Dispatchers.Default) { runCatching { extractAccent(ctx, url) }.getOrNull() }
        if (c != null) found = c
    }
    val target = if (enabled) (found ?: fallback) else fallback
    val animated by animateColorAsState(target, tween(450), label = "accent")
    return animated
}

private suspend fun extractAccent(ctx: Context, url: String): Color? {
    val req = ImageRequest.Builder(ctx).data(url).size(64).allowHardware(false).build()
    val result = ctx.imageLoader.execute(req) as? SuccessResult ?: return null
    val src = (result.drawable as? BitmapDrawable)?.bitmap ?: return null
    val n = 24
    val bmp = Bitmap.createScaledBitmap(src, n, n, true)
    val px = IntArray(n * n)
    bmp.getPixels(px, 0, n, 0, 0, n, n)
    val hsv = FloatArray(3)
    var sx = 0.0
    var sy = 0.0
    var satSum = 0.0
    var wSum = 0.0
    for (p in px) {
        android.graphics.Color.colorToHSV(p, hsv)
        if (hsv[2] < 0.18f) continue
        val w = (hsv[1] * hsv[2]).toDouble().let { it * it }
        val a = Math.toRadians(hsv[0].toDouble())
        sx += cos(a) * w
        sy += sin(a) * w
        satSum += hsv[1] * w
        wSum += w
    }
    if (wSum < 1.0) return Color(0xFFEDEDF5) // cover hampir abu-abu: pakai putih lembut
    val hue = ((Math.toDegrees(atan2(sy, sx)) + 360.0) % 360.0).toFloat()
    val sat = (satSum / wSum).toFloat().coerceIn(0.55f, 0.9f)
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f)))
}

// ───────────────────────── Gestur ─────────────────────────

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
    val currentTap by rememberUpdatedState(onTap)
    val currentSwipe by rememberUpdatedState(onSwipeUp)
    val currentMove by rememberUpdatedState(onMoveByPx)
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
                if (st.dragging) currentMove(ev.rawX - st.lastX, ev.rawY - st.lastY)
                st.lastX = ev.rawX; st.lastY = ev.rawY
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                val up = st.downY - ev.rawY
                when {
                    st.dragging -> Unit
                    st.moved && up > slop * 2 && abs(ev.rawX - st.downX) < up -> currentSwipe()
                    !st.moved -> currentTap()
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
    val position = remember { mutableStateOf(42_000L) }
    val duration = remember { mutableStateOf(180_000L) }
    val pair = remember(lyric) { LyricPair(lyric, lyric) }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val scale = cfg.scalePercent / 100f
        val mode = when {
            cfg.fitLyricsToCamera -> CapLyric.FIT
            cfg.showCapsuleLyrics -> CapLyric.CENTER
            else -> CapLyric.NONE
        }
        val dims = islandDims(cfg, mode, (maxWidth.value / scale - 8f).coerceAtLeast(160f))
        val accent = cfg.accentColor()
        val scaled = remember(base, cfg.scalePercent) { Density(base.density * cfg.scalePercent / 100f, base.fontScale) }
        CompositionLocalProvider(LocalDensity provides scaled) {
            IslandSurface(
                cfg = cfg, expanded = expanded, accent = accent,
                capWidth = dims.capDp.dp, cardWidth = dims.cardDp.dp,
                capsuleModifier = Modifier,
                capsuleContent = { CapsuleContent(cfg, null, true, mode, pair, accent) },
                cardContent = {
                    CardContent(
                        cfg = cfg, title = title, artist = artist, thumb = null, playing = true,
                        position = position, duration = duration, pair = pair, hasLyrics = true, accent = accent,
                        onOpenApp = {}, onCollapse = {}, onInteract = {},
                    )
                },
            )
        }
    }
}
