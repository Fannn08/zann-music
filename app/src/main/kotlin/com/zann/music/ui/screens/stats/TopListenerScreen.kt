package com.zann.music.ui.screens.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zann.music.R
import com.zann.music.data.stats.ListeningStats
import com.zann.music.data.stats.SongStat
import com.zann.music.data.stats.StatsSnapshot
import com.zann.music.data.stats.dailySeries
import com.zann.music.data.stats.songsPlayed
import com.zann.music.data.stats.topArtists
import com.zann.music.data.stats.topSongs
import com.zann.music.data.stats.totalMs
import com.zann.music.playback.PlayerManager
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Gold = Color(0xFFFFC94D)
private val Silver = Color(0xFFC9CED6)
private val Bronze = Color(0xFFD68A59)

private enum class Period(val days: Int?) { WEEK(7), MONTH(30), ALL(null) }

@Composable
fun TopListenerScreen(onBack: () -> Unit) {
    val snapshot by ListeningStats.snapshot.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var periodIndex by rememberSaveable { mutableIntStateOf(1) }
    val period = Period.entries[periodIndex]
    val songs = remember(snapshot, period) { snapshot.topSongs(period.days, 50) }

    StatsPage(
        title = stringResource(R.string.top_title),
        onBack = onBack,
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TabsRow(tab) { tab = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(stringResource(R.string.period_7), periodIndex == 0) { periodIndex = 0 }
                    Pill(stringResource(R.string.period_30), periodIndex == 1) { periodIndex = 1 }
                    Pill(stringResource(R.string.period_all), periodIndex == 2) { periodIndex = 2 }
                }
            }
        },
    ) {
        if (snapshot.totalMs(period.days) <= 0L) {
            item(key = "empty") { EmptyState() }
        } else if (tab == 0) {
            item(key = "time_hero") { TimeHero(snapshot, period) }
            item(key = "artists_label") { SectionLabel(stringResource(R.string.top_artists).uppercase()) }
            item(key = "artists") { ArtistsCard(snapshot, period) }
        } else {
            if (songs.isEmpty()) {
                item(key = "empty_songs") { EmptyState() }
            } else {
                item(key = "featured") {
                    FeaturedSong(songs.first()) { PlayerManager.setQueue(songs.map { it.toSimpleItem() }, 0) }
                }
                itemsIndexed(songs.drop(1), key = { _, s -> "song_" + s.id }) { i, song ->
                    SongRow(rank = i + 2, song = song) { PlayerManager.setQueue(songs.map { it.toSimpleItem() }, i + 1) }
                }
            }
        }
    }
}

@Composable
private fun TabsRow(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(stringResource(R.string.top_tab_time), stringResource(R.string.top_tab_songs))
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        labels.forEachIndexed { i, label ->
            Column(Modifier.clickableNoRipple { onSelect(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (i == selected) MaterialTheme.colorScheme.onSurface else mutedColor(),
                )
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .width(if (i == selected) 34.dp else 0.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun TimeHero(snapshot: StatsSnapshot, period: Period) {
    val total = snapshot.totalMs(period.days)
    val played = snapshot.songsPlayed(period.days)
    val daysCount = period.days ?: snapshot.days.keys.minOrNull()
        ?.let { (ChronoUnit.DAYS.between(it, LocalDate.now()) + 1).toInt() } ?: 1
    val avg = total / daysCount.coerceAtLeast(1)
    val series = remember(snapshot, period) { snapshot.dailySeries(if (period == Period.WEEK) 7 else 30) }

    StatsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.top_total_time).uppercase(), fontSize = 12.sp, letterSpacing = 0.8.sp, color = mutedColor(), fontWeight = FontWeight.SemiBold)
            Text(
                formatDuration(total),
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.top_songs_played, played), fontSize = 13.sp, color = mutedColor())
                Text(stringResource(R.string.top_daily_avg, formatDuration(avg)), fontSize = 13.sp, color = mutedColor())
            }
            Box(Modifier.padding(top = 20.dp))
            BarChart(series.map { it.second }, labels = if (period == Period.WEEK) series.map { it.first.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) } else null)
        }
    }
}

@Composable
private fun BarChart(values: List<Long>, labels: List<String>?) {
    val grow = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { grow.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val lastIndex = values.lastIndex

    Column {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = values.size.coerceAtLeast(1)
            val gap = if (n > 10) 3.dp.toPx() else 10.dp.toPx()
            val barW = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val x = i * (barW + gap)
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2))
                val h = (size.height * (v.toFloat() / max) * grow.value).coerceAtLeast(if (v > 0) barW else 0f)
                if (h > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            if (i == lastIndex) listOf(Ember, EmberDeep) else listOf(accent, accent.copy(alpha = 0.55f)),
                            startY = size.height - h, endY = size.height,
                        ),
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2),
                    )
                }
            }
        }
        if (labels != null) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEachIndexed { i, l ->
                    Text(
                        l, fontSize = 11.sp,
                        fontWeight = if (i == lastIndex) FontWeight.Bold else FontWeight.Medium,
                        color = if (i == lastIndex) MaterialTheme.colorScheme.onSurface else mutedColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsCard(snapshot: StatsSnapshot, period: Period) {
    val artists = remember(snapshot, period) { snapshot.topArtists(period.days, 5) }
    val top = (artists.firstOrNull()?.ms ?: 1L).coerceAtLeast(1L)
    StatsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            artists.forEachIndexed { i, a ->
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RankBadge(i + 1)
                        Text(
                            a.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 10.dp).weight(1f),
                        )
                        Text(formatDuration(a.ms), fontSize = 13.sp, color = mutedColor())
                    }
                    Box(
                        Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                    ) {
                        Box(
                            Modifier.fillMaxWidth((a.ms.toFloat() / top).coerceIn(0.03f, 1f)).height(6.dp).clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, Ember))),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val color = when (rank) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    Box(Modifier.size(26.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(
            rank.toString(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
            color = if (rank <= 3) Color(0xFF1B1B1F) else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FeaturedSong(song: SongStat, onPlay: () -> Unit) {
    StatsCard(Modifier.fillMaxWidth().clickableNoRipple(onPlay)) {
        Box {
            AsyncImage(
                model = song.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.82f))),
                ),
            )
            Column(Modifier.padding(20.dp).padding(top = 56.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(1)
                    Text(
                        stringResource(R.string.top_most_played).uppercase(), fontSize = 12.sp, letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold, color = Gold, modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    song.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp),
                )
                Text(song.artist, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.top_plays_time, song.plays, formatDuration(song.ms)),
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SongRow(rank: Int, song: SongStat, onPlay: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor())
            .clickableNoRipple(onPlay)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) { RankBadge(rank) }
        AsyncImage(
            model = song.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.padding(start = 6.dp).size(52.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            Text(song.artist, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = mutedColor())
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp, end = 6.dp)) {
            Text(stringResource(R.string.top_plays, song.plays), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(formatDuration(song.ms), fontSize = 12.sp, color = mutedColor())
        }
    }
}

@Composable
private fun EmptyState() {
    StatsCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            Text(
                stringResource(R.string.top_empty), fontSize = 15.sp, color = mutedColor(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
internal fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000L
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> stringResource(R.string.stats_h_min, h.toInt(), m.toInt())
        h > 0 -> stringResource(R.string.stats_h, h.toInt())
        else -> stringResource(R.string.stats_min, m.toInt())
    }
}
