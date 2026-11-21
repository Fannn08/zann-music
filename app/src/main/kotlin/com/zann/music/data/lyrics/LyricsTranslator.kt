package com.zann.music.data.lyrics

import com.zann.music.core.Http
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONArray

/**
 * Translates lyric lines into another language with Google Translate's keyless web endpoint.
 * Lines are sent in a few batches (joined by newlines); if a batch comes back with a different number of
 * lines, that batch is retried line by line so every translation still lines up with its original.
 */
object LyricsTranslator {
    private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
    private const val BATCH_CHARS = 1500
    private const val CACHE_MAX = 40

    private val client by lazy { Http.with(8, 15) }
    private val cache = object : LinkedHashMap<String, List<String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?) = size > CACHE_MAX
    }

    /**
     * Returns a list the same size as [lines]; blank or untranslatable lines come back as "".
     * Returns null when nothing could be translated (offline, blocked, ...).
     */
    suspend fun translate(trackId: String, lines: List<String>, targetTag: String): List<String>? {
        val key = "$trackId|$targetTag|${lines.hashCode()}"
        synchronized(cache) { cache[key] }?.let { return it }
        val result = withContext(Dispatchers.IO) { run(lines, targetTag) } ?: return null
        synchronized(cache) { cache[key] = result }
        return result
    }

    private fun run(lines: List<String>, target: String): List<String>? {
        val out = MutableList(lines.size) { "" }
        val pending = lines.indices.filter { lines[it].isNotBlank() }
        if (pending.isEmpty()) return out
        var translatedAny = false
        var start = 0
        while (start < pending.size) {
            var end = start
            var chars = 0
            while (end < pending.size && (end == start || chars + lines[pending[end]].length < BATCH_CHARS)) {
                chars += lines[pending[end]].length + 1
                end++
            }
            val batch = pending.subList(start, end)
            val joined = batch.joinToString("\n") { lines[it].replace('\n', ' ').trim() }
            val batched = request(joined, target)?.split("\n")
            if (batched != null && batched.size == batch.size) {
                batch.forEachIndexed { i, index -> out[index] = batched[i].trim() }
                translatedAny = true
            } else {
                for (index in batch) {
                    val one = request(lines[index].replace('\n', ' ').trim(), target)
                    if (one != null) {
                        out[index] = one.replace('\n', ' ').trim()
                        translatedAny = true
                    }
                }
            }
            start = end
        }
        return if (translatedAny) out else null
    }

    /** One call; null on any failure. */
    private fun request(text: String, target: String): String? {
        if (text.isBlank()) return ""
        return try {
            val body = FormBody.Builder()
                .add("client", "gtx")
                .add("sl", "auto")
                .add("tl", target)
                .add("dt", "t")
                .add("q", text)
                .build()
            val request = Request.Builder().url(ENDPOINT)
                .header("User-Agent", Http.USER_AGENT)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                parse(response.body?.string().orEmpty())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    /** `[[["translated","original",...],...],...]` → the translated segments, concatenated. */
    internal fun parse(raw: String): String? = runCatching {
        val segments = JSONArray(raw).getJSONArray(0)
        buildString {
            for (i in 0 until segments.length()) {
                val part = segments.optJSONArray(i) ?: continue
                if (!part.isNull(0)) append(part.optString(0))
            }
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
