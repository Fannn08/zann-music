package com.zann.music.ui.screens.stats

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A flame that is alive: the tip and tongues sway on their own phases, the core breathes, sparks rise and
 * fade, and a halo pulses. Flip [lit] to false and it dies down with a wisp of smoke; flip it back and it
 * catches again. [lit] = false at first composition draws the cold, grey flame without any animation.
 */
@Composable
fun LivingFlame(lit: Boolean, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val burn by animateFloatAsState(if (lit) 1f else 0f, tween(if (lit) 700 else 1100), label = "burn")
    val transition = rememberInfiniteTransition(label = "livingFlame")
    // One full cycle of every wobble; each part uses a different multiple so they never line up.
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            if (burn > 0.01f) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Ember.copy(alpha = 0.38f * pulse * burn), Color.Transparent),
                        center = Offset(this.size.width / 2, this.size.height * 0.62f),
                        radius = this.size.width * 0.62f,
                    ),
                )
            }
            drawLivingFlame(burn, phase, pulse)
            if (burn > 0.05f) drawSparks(phase, burn)
            if (burn in 0.02f..0.98f) drawSmoke(phase, 1f - burn)
        }
    }
}

private fun wob(phase: Float, mult: Int, shift: Float): Float =
    sin((phase * mult + shift) * 2f * PI.toFloat())

private fun DrawScope.drawLivingFlame(burn: Float, phase: Float, pulse: Float) {
    val k = size.minDimension / 100f
    // A dying flame shrinks toward its base and stops swaying.
    val grow = 0.55f + 0.45f * burn
    val sway = burn
    val baseY = 96f
    fun y(v: Float) = (baseY - (baseY - v) * grow) * k

    val tipX = 50f + wob(phase, 1, 0f) * 5f * sway
    val tipLift = wob(phase, 2, 0.3f) * 3f * sway
    val leftTongue = 32f + wob(phase, 3, 0.1f) * 3.5f * sway
    val leftTongueY = 28f + wob(phase, 2, 0.6f) * 3f * sway
    val sideBulge = wob(phase, 2, 0.8f) * 2.2f * sway

    val outer = Path().apply {
        moveTo(tipX * k, y(4f + tipLift))
        cubicTo((60f + sideBulge) * k, y(22f), (88f + sideBulge) * k, y(38f), 84f * k, y(66f))
        cubicTo(82f * k, y(86f), 66f * k, y(96f), 50f * k, y(96f))
        cubicTo(34f * k, y(96f), 18f * k, y(86f), 16f * k, y(66f))
        cubicTo((14f - sideBulge) * k, y(50f), (28f - sideBulge) * k, y(42f), leftTongue * k, y(leftTongueY))
        cubicTo(38f * k, y(36f), 42f * k, y(40f), 44f * k, y(46f))
        cubicTo(48f * k, y(34f), (46f + wob(phase, 2, 0.5f) * 2f * sway) * k, y(16f), tipX * k, y(4f + tipLift))
        close()
    }
    val innerSway = wob(phase, 3, 0.4f) * 3f * sway
    val inner = Path().apply {
        moveTo((50f + innerSway) * k, y(48f))
        cubicTo(58f * k, y(60f), 70f * k, y(66f), 68f * k, y(78f))
        cubicTo(66f * k, y(88f), 58f * k, y(92f), 50f * k, y(92f))
        cubicTo(42f * k, y(92f), 34f * k, y(88f), 32f * k, y(78f))
        cubicTo(30f * k, y(68f), 44f * k, y(62f), (50f + innerSway) * k, y(48f))
        close()
    }
    val coldTop = Color(0xFF6B6F76)
    val coldBottom = Color(0xFF8A8E95)
    fun mix(a: Color, b: Color) = Color(
        a.red + (b.red - a.red) * burn, a.green + (b.green - a.green) * burn,
        a.blue + (b.blue - a.blue) * burn, 1f,
    )
    drawPath(
        outer,
        Brush.verticalGradient(
            listOf(mix(coldTop, EmberDeep), mix(coldBottom, Ember)),
            startY = 0f, endY = size.height,
        ),
    )
    val coreTop = mix(Color(0xFFB4B8BE), EmberGold)
    val coreBottom = mix(coldBottom, Ember)
    drawPath(
        inner,
        Brush.verticalGradient(listOf(coreTop, coreBottom), startY = 40f * k, endY = size.height),
        alpha = (0.5f + 0.5f * pulse) * (0.4f + 0.6f * burn),
    )
}

/** Six embers drifting up and fading, each on its own offset of the shared phase. */
private fun DrawScope.drawSparks(phase: Float, burn: Float) {
    val k = size.minDimension / 100f
    for (i in 0 until 6) {
        val t = (phase * 2f + i / 6f) % 1f
        val x = (50f + (i - 2.5f) * 7f + sin((t * 3f + i) * PI.toFloat()) * 6f) * k
        val y = (62f - t * 62f) * k
        val a = (1f - t) * 0.9f * burn
        drawCircle(EmberGold.copy(alpha = a), radius = (1.8f + (1f - t) * 1.6f) * k * 0.6f, center = Offset(x, y))
    }
}

/** Grey wisps that rise while the flame is going out; [strength] is how far it has died (0 to 1). */
private fun DrawScope.drawSmoke(phase: Float, strength: Float) {
    val k = size.minDimension / 100f
    for (i in 0 until 3) {
        val t = (phase * 1.5f + i / 3f) % 1f
        val x = (46f + i * 6f + sin((t * 2f + i) * PI.toFloat()) * 7f) * k
        val y = (58f - t * 54f) * k
        drawCircle(
            Color(0xFF9A9EA5).copy(alpha = (1f - t) * 0.45f * strength),
            radius = (3f + t * 6f) * k * 0.6f,
            center = Offset(x, y),
            style = Stroke(width = 1.6f * k * 0.6f),
        )
    }
}
