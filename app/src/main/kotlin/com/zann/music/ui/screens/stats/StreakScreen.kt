package com.zann.music.ui.screens.stats

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zann.music.R
import com.zann.music.data.stats.ListeningStats
import com.zann.music.data.stats.StatsSnapshot
import com.zann.music.data.stats.StreakCalculator
import com.zann.music.data.stats.activeDates
import com.zann.music.data.stats.msOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StreakScreen(onBack: () -> Unit) {
    val snapshot by ListeningStats.snapshot.collectAsState()
    val today = remember { LocalDate.now() }
    val info = remember(snapshot) { StreakCalculator.compute(snapshot.activeDates(), today) }
    val todayMs = snapshot.msOn(today)

    StatsPage(
        title = stringResource(R.string.streak_title),
        subtitle = stringResource(R.string.streak_subtitle),
        onBack = onBack,
    ) {
        item(key = "hero") { StreakHero(info.current, info.todayActive, todayMs) }
        item(key = "week") { WeekCard(snapshot, today) }
        item(key = "numbers") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberTile(Modifier.weight(1f), info.longest.toString(), stringResource(R.string.streak_longest))
                NumberTile(Modifier.weight(1f), info.activeDays.toString(), stringResource(R.string.streak_active_days))
                NumberTile(Modifier.weight(1f), (todayMs / 60_000L).toString(), stringResource(R.string.streak_today_min))
            }
        }
        item(key = "heat_label") { SectionLabel(stringResource(R.string.streak_activity).uppercase()) }
        item(key = "heat") { Heatmap(snapshot, today) }
    }
}

@Composable
private fun StreakHero(current: Int, todayActive: Boolean, todayMs: Long) {
    val lit = current > 0
    val count by animateIntAsState(current, tween(900, easing = FastOutSlowInEasing), label = "streakCount")

    val transition = rememberInfiniteTransition(label = "flame")
    val sway by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val glow by transition.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    StatsCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                if (lit) {
                    Canvas(Modifier.size(150.dp)) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Ember.copy(alpha = 0.35f * glow), Color.Transparent),
                                center = Offset(size.width / 2, size.height * 0.6f),
                                radius = size.width * 0.62f,
                            ),
                        )
                    }
                }
                Canvas(Modifier.size(104.dp).scale(if (lit) sway else 1f)) { drawFlame(lit, glow) }
            }
            Text(
                count.toString(),
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (lit) Ember else mutedColor(),
            )
            Text(
                pluralStringResource(R.plurals.streak_day_label, current),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.streak_current),
                fontSize = 12.sp,
                color = mutedColor(),
                modifier = Modifier.padding(top = 2.dp),
            )

            Box(Modifier.padding(top = 20.dp))
            val goal = ListeningStats.STREAK_GOAL_MS
            val progress = (todayMs.toFloat() / goal).coerceIn(0f, 1f)
            val animated by androidx.compose.animation.core.animateFloatAsState(progress, tween(700), label = "goal")
            Box(
                Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(animated).height(10.dp).clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(EmberDeep, Ember, EmberGold))),
                )
            }
            val leftMin = (((goal - todayMs).coerceAtLeast(0L)) + 59_999L) / 60_000L
            Text(
                text = when {
                    todayActive -> stringResource(R.string.streak_goal_done)
                    current > 0 -> stringResource(R.string.streak_goal_left, leftMin.toInt())
                    else -> stringResource(R.string.streak_empty_hint, (goal / 60_000L).toInt())
                },
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = if (todayActive) Ember else mutedColor(),
                fontWeight = if (todayActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun WeekCard(snapshot: StatsSnapshot, today: LocalDate) {
    val active = remember(snapshot) { snapshot.activeDates() }
    val monday = today.with(DayOfWeek.MONDAY)
    StatsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.streak_this_week), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0..6) {
                    val date = monday.plusDays(i.toLong())
                    DayDot(date, done = date in active, isToday = date == today, future = date.isAfter(today))
                }
            }
        }
    }
}

@Composable
private fun DayDot(date: LocalDate, done: Boolean, isToday: Boolean, future: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.NARROW, LocalConfiguration.current.locales[0]),
            fontSize = 12.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) accent else mutedColor(),
        )
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> Brush.verticalGradient(listOf(EmberDeep, Ember))
                        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    },
                )
                .then(
                    if (!done) Modifier.border(
                        width = if (isToday) 2.dp else 1.dp,
                        color = if (isToday) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = if (future) 0.06f else 0.14f),
                        shape = CircleShape,
                    ) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun NumberTile(modifier: Modifier, value: String, label: String) {
    StatsCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 11.sp, color = mutedColor(), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Last 5 weeks, Monday first. Brighter square = more listening that day. */
@Composable
private fun Heatmap(snapshot: StatsSnapshot, today: LocalDate) {
    val weeks = 5
    val start = today.with(DayOfWeek.MONDAY).minusWeeks(weeks - 1L)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    StatsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (w in 0 until weeks) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 0..6) {
                        val date = start.plusDays((w * 7 + d).toLong())
                        val minutes = snapshot.msOn(date) / 60_000L
                        val level = when {
                            date.isAfter(today) -> -1
                            minutes <= 0L && snapshot.msOn(date) < ListeningStats.STREAK_GOAL_MS -> 0
                            minutes < 15 -> 1
                            minutes < 30 -> 2
                            minutes < 60 -> 3
                            else -> 4
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (level) {
                                        -1 -> Color.Transparent
                                        0 -> trackColor
                                        1 -> Ember.copy(alpha = 0.35f)
                                        2 -> Ember.copy(alpha = 0.55f)
                                        3 -> Ember.copy(alpha = 0.78f)
                                        else -> Ember
                                    },
                                )
                                .then(if (date == today) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), RoundedCornerShape(8.dp)) else Modifier),
                        )
                    }
                }
            }
        }
    }
}
