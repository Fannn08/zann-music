package com.zann.music.data.zanntube

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** ZannTube's local data: subscribed channels and watch history. Nothing leaves the phone. */
object TubeStore {
    private const val PREFS = "zanntube"
    private const val MAX_HISTORY = 200
    private var prefs: android.content.SharedPreferences? = null

    private val _subs = MutableStateFlow<List<String>>(emptyList())
    val subscriptions: StateFlow<List<String>> = _subs.asStateFlow()
    private val _history = MutableStateFlow<List<TubeVideo>>(emptyList())
    val history: StateFlow<List<TubeVideo>> = _history.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _subs.value = runCatching {
            val a = JSONArray(p.getString("subs", "[]"))
            List(a.length()) { a.getString(it) }
        }.getOrDefault(emptyList())
        _history.value = runCatching {
            val a = JSONArray(p.getString("history", "[]"))
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                TubeVideo(
                    id = o.getString("id"), title = o.optString("t"), channel = o.optString("c"),
                    thumbnail = o.optString("th").ifBlank { null }, durationSec = o.optLong("d"),
                    views = o.optLong("v", -1L), uploaded = "", isLive = false,
                    avatar = o.optString("av").ifBlank { null },
                )
            }
        }.getOrDefault(emptyList())
    }

    fun isSubscribed(channel: String) = channel.isNotBlank() && channel in _subs.value

    fun toggleSubscription(channel: String) {
        if (channel.isBlank()) return
        _subs.value = if (channel in _subs.value) _subs.value - channel else _subs.value + channel
        prefs?.edit()?.putString("subs", JSONArray(_subs.value).toString())?.apply()
    }

    fun addHistory(video: TubeVideo) {
        _history.value = (listOf(video) + _history.value.filter { it.id != video.id }).take(MAX_HISTORY)
        saveHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory()
    }

    private fun saveHistory() {
        val a = JSONArray()
        _history.value.forEach {
            a.put(
                JSONObject().put("id", it.id).put("t", it.title).put("c", it.channel)
                    .put("th", it.thumbnail ?: "").put("d", it.durationSec).put("v", it.views).put("av", it.avatar ?: ""),
            )
        }
        prefs?.edit()?.putString("history", a.toString())?.apply()
    }
}
