package com.zann.music.ui.screens.zanntube

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.zann.music.data.youtube.StreamResolver
import com.zann.music.data.zanntube.TubeStreams
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Landscape + hidden system bars while [enabled]; everything is put back when it turns off or leaves. */
@Composable
internal fun FullscreenEffect(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val activity = context.findActivity()
        val window = activity?.window
        if (enabled && activity != null && window != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (enabled && activity != null && window != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@OptIn(UnstableApi::class)
internal fun newTubePlayer(context: Context): ExoPlayer = ExoPlayer.Builder(context.applicationContext)
    .setLoadControl(
        // A bigger buffer than the default: 1080p and above need the headroom on mobile data.
        DefaultLoadControl.Builder().setBufferDurationsMs(20_000, 90_000, 1_500, 3_000).build(),
    )
    .build()

/** Video rung of at most [maxHeight] merged with the best audio; a muxed stream when nothing else exists. */
@OptIn(UnstableApi::class)
internal fun tubeSource(streams: TubeStreams, maxHeight: Int): MediaSource? {
    fun progressive(url: String): MediaSource {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(StreamResolver.headersFor(url))
        return ProgressiveMediaSource.Factory(http).createMediaSource(MediaItem.fromUri(url))
    }
    val video = streams.pick(maxHeight)
    val audio = streams.audioUrl
    return when {
        video != null && audio != null -> MergingMediaSource(progressive(video.url), progressive(audio))
        streams.muxedUrl != null -> progressive(streams.muxedUrl)
        else -> null
    }
}

internal fun clock(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * The watch-page player: YouTube-style overlay (tap to show controls), quality menu from 144p up to what the
 * video offers (1080p, 1440p, 4K), fullscreen button, and one automatic retry when a stream URL has expired.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun TubePlayer(
    videoId: String,
    streams: TubeStreams?,
    height: Int,
    onHeight: (Int) -> Unit,
    fullscreen: Boolean,
    onFullscreen: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exo = remember(videoId) { newTubePlayer(context) }
    var ready by remember(videoId) { mutableStateOf(false) }
    var failed by remember(videoId) { mutableStateOf(false) }
    var isPlaying by remember(videoId) { mutableStateOf(true) }
    var position by remember(videoId) { mutableStateOf(0L) }
    var duration by remember(videoId) { mutableStateOf(0L) }
    var scrubbing by remember(videoId) { mutableStateOf(false) }
    var controls by remember(videoId) { mutableStateOf(true) }
    var tick by remember(videoId) { mutableStateOf(0) }
    var menu by remember { mutableStateOf(false) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) ready = true
                duration = exo.duration.coerceAtLeast(0L)
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) {
                // Stream URLs expire; fetch fresh ones once before giving up.
                if (!failed) { failed = true; onRetry() } else ready = false
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener); exo.release() }
    }

    // (Re)load the source whenever the streams or the chosen quality change, keeping the place.
    LaunchedEffect(streams, height) {
        val s = streams ?: return@LaunchedEffect
        val source = tubeSource(s, height) ?: return@LaunchedEffect
        val resume = exo.currentPosition
        exo.setMediaSource(source, if (resume > 0) resume else 0L)
        exo.prepare()
        exo.playWhenReady = true
    }
    LaunchedEffect(exo) {
        while (isActive) {
            if (!scrubbing) position = exo.currentPosition
            duration = exo.duration.coerceAtLeast(0L)
            delay(400L)
        }
    }
    // Controls hide a few seconds after the last touch while playing.
    LaunchedEffect(controls, tick, isPlaying) {
        if (controls && isPlaying && !menu) { delay(3_000L); controls = false }
    }

    Box(modifier.background(Color.Black).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { controls = !controls; tick++ }) {
        AndroidView(
            factory = { ctx -> TextureView(ctx).also { exo.setVideoTextureView(it) } },
            onRelease = { exo.clearVideoSurface() },
            modifier = Modifier.fillMaxSize(),
        )
        if (!ready) CircularProgressIndicator(Modifier.align(Alignment.Center).size(34.dp), color = Color.White)
        if (controls) {
            Box(Modifier.fillMaxSize().background(Color(0x66000000)))
            IconButton(onClick = { if (fullscreen) onFullscreen(false) else onBack() }, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    val rungs = streams?.renditions.orEmpty().sortedByDescending { it.height }
                    rungs.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.label + if (r.height >= 2160) "  4K" else if (r.height >= 1440) "  2K" else "") },
                            onClick = { menu = false; onHeight(r.height) },
                        )
                    }
                    if (rungs.isEmpty()) DropdownMenuItem(text = { Text("Auto") }, onClick = { menu = false })
                }
            }
            IconButton(onClick = { if (isPlaying) exo.pause() else exo.play() }, modifier = Modifier.align(Alignment.Center).size(64.dp)) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${clock(position)} / ${clock(duration)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                    Box(Modifier.weight(1f))
                    val shown = streams?.pick(height)?.label.orEmpty()
                    Text(shown, color = Color.White, fontSize = 12.sp)
                    IconButton(onClick = { onFullscreen(!fullscreen) }) {
                        Icon(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, null, tint = Color.White)
                    }
                }
                Slider(
                    value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                    onValueChange = { scrubbing = true; position = (it * duration).toLong(); tick++ },
                    onValueChangeFinished = { exo.seekTo(position); scrubbing = false },
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF0033), activeTrackColor = Color(0xFFFF0033), inactiveTrackColor = Color(0x66FFFFFF)),
                )
            }
        }
    }
}
