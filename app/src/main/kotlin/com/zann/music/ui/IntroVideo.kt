package com.zann.music.ui

import android.net.Uri
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zann.music.R
import kotlinx.coroutines.delay

private const val INTRO_FADE_MS = 400
private const val INTRO_START_TIMEOUT_MS = 3_000L
private const val INTRO_MAX_MS = 6_000L

/**
 * Full-screen intro video (res/raw/zann_intro.mp4) shown on a cold start, above the app.
 * Tap to skip. Every failure path (no decoder, error, slow start) just dismisses it, so the
 * intro can never keep the app from opening.
 */
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun IntroVideoOverlay(onFinished: () -> Unit) {
    val context = LocalContext.current
    var finishing by remember { mutableStateOf(false) }
    var texture by remember { mutableStateOf<TextureView?>(null) }
    var hasFrame by remember { mutableStateOf(false) }

    val player = remember {
        runCatching {
            ExoPlayer.Builder(context.applicationContext).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/${R.raw.zann_intro}")))
                playWhenReady = true
            }
        }.getOrNull()
    }

    DisposableEffect(player) {
        if (player == null) {
            finishing = true
            return@DisposableEffect onDispose { }
        }
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { hasFrame = true }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) finishing = true
            }
            override fun onPlayerError(error: PlaybackException) { finishing = true }
        }
        player.addListener(listener)
        runCatching { player.prepare() }.onFailure { finishing = true }
        onDispose {
            player.removeListener(listener)
            runCatching { player.release() }
        }
    }

    LaunchedEffect(player, texture) {
        val view = texture ?: return@LaunchedEffect
        runCatching { player?.setVideoTextureView(view) }.onFailure { finishing = true }
    }

    // Never hold the app back: give up if the first frame is slow, and cap the total time.
    LaunchedEffect(Unit) {
        delay(INTRO_START_TIMEOUT_MS)
        if (!hasFrame) finishing = true
    }
    LaunchedEffect(Unit) {
        delay(INTRO_MAX_MS)
        finishing = true
    }

    val introAlpha by animateFloatAsState(
        targetValue = if (finishing) 0f else 1f,
        animationSpec = tween(INTRO_FADE_MS),
        label = "introAlpha",
        finishedListener = { if (finishing) onFinished() },
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = introAlpha }
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                finishing = true
            }
    ) {
        AndroidView(
            factory = { ctx -> TextureView(ctx).also { texture = it } },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
