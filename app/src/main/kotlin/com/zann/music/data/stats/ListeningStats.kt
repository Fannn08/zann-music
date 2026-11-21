package com.zann.music.data.stats

import android.content.Context
import android.os.SystemClock
import com.zann.music.data.youtube.SimpleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class SongMeta(val title: String, val artist: String, val thumbnail: String?)
data class DayStat(val ms: Long, val songs: Map<String, SongDay>)
data class SongDay(val ms: Long, val plays: Int)
data class StatsSnapshot(val days: Map<LocalDate, DayStat>, val meta: Map<String, SongMeta>)

data class SongStat(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val ms: Long,
    val plays: Int,
) {
    /**
     * Rebuilds a playable item. If your SimpleItem needs `type = ItemType.SONG` (or similar),
     * add it here: this is the only place in the stats code that constructs one.
     */
    fun toSimpleItem(): SimpleItem = SimpleItem(id = id, title = title, artist = artist, thumbnail = thumbnail, url = "")
}

data class ArtistStat(val name: String, val ms: Long, val plays: Int)

/**
 * Local listening statistics: time per day and per song, kept on device.
 * Feeds the Streak and Top Listener screens. Fed by PlayerManager (play/pause + track changes).
 */
object ListeningStats {
    /** Minimum listening time in a day for the day to count toward the streak. */
    const val STREAK_GOAL_MS = 60_000L
    /** A track counts as one "play" once it has been listened to this long. */
    private const val PLAY_THRESHOLD_MS = 30_000L
    private const val TICK_MS = 15_000L
    private const val SONG_DETAIL_DAYS = 120L
    private const val PREFS = "zann_listening_stats"
    private const val KEY = "data_v1"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var prefs: android.content.SharedPreferences? = null
    private var tickJob: Job? = null

    private val days = HashMap<LocalDate, DayStat>()
    private val meta = HashMap<String, SongMeta>()

    private val _snapshot = MutableStateFlow(StatsSnapshot(emptyMap(), emptyMap()))
    val snapshot: StateFlow<StatsSnapshot> = _snapshot.asStateFlow()

    private var item: SimpleItem? = null
    private var playingSince = 0L
    private var listenedThisTrack = 0L
    private var countedPlay = false
    private var inited = false

    @Synchronized
    fun init(context: Context) {
        if (inited) return
        inited = true
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        runCatching { load(prefs?.getString(KEY, null)) }
        publish()
    }

    @Synchronized
    fun onPlayingChanged(playing: Boolean, current: SimpleItem?) {
        if (!inited) return
        if (playing) {
            if (item?.id != current?.id) switchTrack(current)
            if (playingSince == 0L) playingSince = SystemClock.elapsedRealtime()
            startTicker()
        } else {
            commit()
            playingSince = 0L
            tickJob?.cancel()
            tickJob = null
            save()
        }
    }

    @Synchronized
    fun onTrackChanged(current: SimpleItem?) {
        if (!inited) return
        val wasPlaying = playingSince != 0L
        commit()
        switchTrack(current)
        playingSince = if (wasPlaying) SystemClock.elapsedRealtime() else 0L
    }

    private fun switchTrack(current: SimpleItem?) {
        item = current
        listenedThisTrack = 0L
        countedPlay = false
    }

    private fun startTicker() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                synchronized(this@ListeningStats) { commit(); save() }
            }
        }
    }

    /** Moves the time listened since the last commit into today's bucket. */
    private fun commit() {
        val song = item
        if (playingSince == 0L || song == null) return
        val now = SystemClock.elapsedRealtime()
        val delta = (now - playingSince).coerceIn(0L, 2 * TICK_MS)
        playingSince = now
        if (delta <= 0L) return

        listenedThisTrack += delta
        var plays = 0
        if (!countedPlay && listenedThisTrack >= PLAY_THRESHOLD_MS) {
            countedPlay = true
            plays = 1
        }
        val today = LocalDate.now()
        val day = days[today] ?: DayStat(0L, emptyMap())
        val old = day.songs[song.id] ?: SongDay(0L, 0)
        days[today] = DayStat(day.ms + delta, day.songs + (song.id to SongDay(old.ms + delta, old.plays + plays)))
        meta[song.id] = SongMeta(song.title, song.artist.orEmpty(), song.thumbnail)
        publish()
    }

    private fun publish() {
        _snapshot.value = StatsSnapshot(HashMap(days), HashMap(meta))
    }

    private fun prune() {
        val cutoff = LocalDate.now().minusDays(SONG_DETAIL_DAYS)
        for ((date, stat) in days.toMap()) {
            if (date.isBefore(cutoff) && stat.songs.isNotEmpty()) days[date] = DayStat(stat.ms, emptyMap())
        }
        val used = days.values.flatMap { it.songs.keys }.toSet()
        meta.keys.retainAll(used)
    }

    private fun save() {
        prune()
        val root = JSONObject()
        val daysJson = JSONObject()
        for ((date, stat) in days) {
            val songs = JSONObject()
            for ((id, s) in stat.songs) songs.put(id, JSONArray().put(s.ms).put(s.plays))
            daysJson.put(date.toString(), JSONObject().put("ms", stat.ms).put("s", songs))
        }
        val metaJson = JSONObject()
        for ((id, m) in meta) metaJson.put(id, JSONArray().put(m.title).put(m.artist).put(m.thumbnail ?: ""))
        root.put("days", daysJson).put("meta", metaJson)
        prefs?.edit()?.putString(KEY, root.toString())?.apply()
    }

    private fun load(raw: String?) {
        if (raw.isNullOrBlank()) return
        val root = JSONObject(raw)
        root.optJSONObject("days")?.let { dj ->
            for (key in dj.keys()) {
                val d = dj.getJSONObject(key)
                val songs = HashMap<String, SongDay>()
                d.optJSONObject("s")?.let { sj ->
                    for (id in sj.keys()) {
                        val a = sj.getJSONArray(id)
                        songs[id] = SongDay(a.getLong(0), a.getInt(1))
                    }
                }
                days[LocalDate.parse(key)] = DayStat(d.getLong("ms"), songs)
            }
        }
        root.optJSONObject("meta")?.let { mj ->
            for (id in mj.keys()) {
                val a = mj.getJSONArray(id)
                meta[id] = SongMeta(a.getString(0), a.getString(1), a.getString(2).ifBlank { null })
            }
        }
    }
}

// ---- Queries over a snapshot (pure) ----

private fun StatsSnapshot.window(lastDays: Int?, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, DayStat>> {
    val from = lastDays?.let { today.minusDays(it - 1L) }
    return days.entries.filter { from == null || !it.key.isBefore(from) }.map { it.key to it.value }
}

fun StatsSnapshot.totalMs(lastDays: Int?): Long = window(lastDays).sumOf { it.second.ms }

fun StatsSnapshot.activeDates(): Set<LocalDate> =
    days.filter { it.value.ms >= ListeningStats.STREAK_GOAL_MS }.keys

fun StatsSnapshot.msOn(date: LocalDate): Long = days[date]?.ms ?: 0L

fun StatsSnapshot.dailySeries(count: Int, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Long>> =
    (count - 1 downTo 0).map { back -> today.minusDays(back.toLong()).let { it to msOn(it) } }

fun StatsSnapshot.topSongs(lastDays: Int?, limit: Int = 50): List<SongStat> {
    val ms = HashMap<String, Long>()
    val plays = HashMap<String, Int>()
    for ((_, day) in window(lastDays)) for ((id, s) in day.songs) {
        ms[id] = (ms[id] ?: 0L) + s.ms
        plays[id] = (plays[id] ?: 0) + s.plays
    }
    return ms.entries
        .sortedWith(compareByDescending<Map.Entry<String, Long>> { plays[it.key] ?: 0 }.thenByDescending { it.value })
        .take(limit)
        .mapNotNull { (id, t) ->
            val m = meta[id] ?: return@mapNotNull null
            SongStat(id, m.title, m.artist, m.thumbnail, t, plays[id] ?: 0)
        }
}

fun StatsSnapshot.topArtists(lastDays: Int?, limit: Int = 5): List<ArtistStat> =
    topSongs(lastDays, Int.MAX_VALUE)
        .filter { it.artist.isNotBlank() }
        .groupBy { it.artist }
        .map { (name, songs) -> ArtistStat(name, songs.sumOf { it.ms }, songs.sumOf { it.plays }) }
        .sortedByDescending { it.ms }
        .take(limit)

fun StatsSnapshot.songsPlayed(lastDays: Int?): Int = topSongs(lastDays, Int.MAX_VALUE).count { it.plays > 0 }
