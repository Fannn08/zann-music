package com.zann.music.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zann.music.R

internal val Ember = Color(0xFFFF7A18)
internal val EmberDeep = Color(0xFFFF3D2E)
internal val EmberGold = Color(0xFFFFD36B)

/** Subtle surface that works in both light and dark themes. */
@Composable
internal fun cardColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

@Composable
internal fun mutedColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)

@Composable
internal fun StatsPage(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Soft ember glow at the top for the aesthetic feel.
        Box(
            Modifier
                .fillMaxWidth()
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Ember.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(540f, 0f),
                        radius = 900f,
                    ),
                ),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stats_back))
                    }
                    Column(Modifier.padding(start = 4.dp)) {
                        Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = mutedColor())
                    }
                }
            }
            if (header != null) item(key = "extra_header") { header() }
            content()
        }
    }
}

@Composable
internal fun StatsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(cardColor())
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(24.dp)),
    ) { content() }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = mutedColor(),
        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
    )
}

@Composable
internal fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) accent else cardColor())
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun Dp.toPxF(): Float = with(androidx.compose.ui.platform.LocalDensity.current) { this@toPxF.toPx() }

internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

/** Hand-drawn flame, so no emoji font or vector asset is needed. [lit] = false renders it grey. */
internal fun DrawScope.drawFlame(lit: Boolean, flicker: Float = 1f) {
    val k = size.minDimension / 100f
    fun Path.p(block: Path.() -> Unit) = apply(block)
    val outer = Path().p {
        moveTo(50f * k, 4f * k)
        cubicTo(60f * k, 22f * k, 88f * k, 38f * k, 84f * k, 66f * k)
        cubicTo(82f * k, 86f * k, 66f * k, 96f * k, 50f * k, 96f * k)
        cubicTo(34f * k, 96f * k, 18f * k, 86f * k, 16f * k, 66f * k)
        cubicTo(14f * k, 50f * k, 28f * k, 42f * k, 32f * k, 28f * k)
        cubicTo(38f * k, 36f * k, 42f * k, 40f * k, 44f * k, 46f * k)
        cubicTo(48f * k, 34f * k, 46f * k, 16f * k, 50f * k, 4f * k)
        close()
    }
    val inner = Path().p {
        moveTo(50f * k, 48f * k)
        cubicTo(58f * k, 60f * k, 70f * k, 66f * k, 68f * k, 78f * k)
        cubicTo(66f * k, 88f * k, 58f * k, 92f * k, 50f * k, 92f * k)
        cubicTo(42f * k, 92f * k, 34f * k, 88f * k, 32f * k, 78f * k)
        cubicTo(30f * k, 68f * k, 44f * k, 62f * k, 50f * k, 48f * k)
        close()
    }
    val top = if (lit) EmberDeep else Color(0xFF6B6F76)
    val bottom = if (lit) Ember else Color(0xFF8A8E95)
    drawPath(outer, Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = size.height))
    val core = if (lit) listOf(EmberGold, Ember) else listOf(Color(0xFFB4B8BE), Color(0xFF8A8E95))
    drawPath(inner, Brush.verticalGradient(core, startY = 40f * k, endY = size.height), alpha = 0.55f + 0.45f * flicker)
}
