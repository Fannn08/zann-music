package com.zann.music.data.zanntube

import com.zann.music.data.youtube.SimpleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** One entry of a ZannTube feed. */
data class TubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnail: String?,
    val durationSec: Long,
    val views: Long,
    val uploaded: String,
    val isLive: Boolean,
    val avatar: String? = null,
)

data class TubePage(val items: List<TubeVideo>, val next: Page?)

/** One playable video rung. Video-only; it is merged with [TubeStreams.audioUrl] at play time. */
data class TubeRendition(val height: Int, val fps: Int, val url: String, val vp9: Boolean) {
    val label: String get() = if (fps > 30) "${height}p$fps" else "${height}p"
}

class TubeStreams(
    val title: String,
    val channel: String,
    val views: Long,
    val likes: Long,
    val uploaded: String,
    val description: String,
    val renditions: List<TubeRendition>,
    val audioUrl: String?,
    val muxedUrl: String?,
    val related: List<TubeVideo>,
) {
    /** The best rung that is not taller than [maxHeight]; the lowest one when everything is taller. */
    fun pick(maxHeight: Int): TubeRendition? =
        renditions.filter { it.height <= maxHeight }.maxByOrNull { it.height } ?: renditions.minByOrNull { it.height }
}

/**
 * ZannTube's data: regular YouTube videos read through NewPipe's extractor, which talks to YouTube's
 * public endpoints directly. No ads are ever requested, so there is nothing to block.
 */
object ZannTubeRepository {
    suspend fun trending(page: Page? = null): Result<TubePage> = withContext(Dispatchers.IO) {
        runCatching {
            val service = ServiceList.YouTube
            val extractor = service.kioskList.getDefaultKioskExtractor()
            if (page == null) {
                val info = KioskInfo.getInfo(service, extractor.url)
                TubePage(info.relatedItems.toVideos(), info.nextPage)
            } else {
                val more = KioskInfo.getMoreItems(service, extractor.url, page)
                TubePage(more.items.toVideos(), more.nextPage)
            }
        }
    }

    suspend fun search(query: String, page: Page? = null): Result<TubePage> = withContext(Dispatchers.IO) {
        runCatching {
            val service = ServiceList.YouTube
            val handler = service.searchQHFactory.fromQuery(query, listOf("videos"), "")
            if (page == null) {
                val info = SearchInfo.getInfo(service, handler)
                TubePage(info.relatedItems.toVideos(), info.nextPage)
            } else {
                val more = SearchInfo.getMoreItems(service, handler, page)
                TubePage(more.items.toVideos(), more.nextPage)
            }
        }
    }

    /** Every rung YouTube offers (up to 4K), the best audio, and the related videos of the watch page. */
    suspend fun streams(id: String): Result<TubeStreams> = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = ServiceList.YouTube.getStreamExtractor(SimpleItem.watchUrl(id))
            val info = StreamInfo.getInfo(extractor)
            val rungs = info.videoOnlyStreams
                .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && !it.content.isNullOrBlank() && it.height > 0 }
                .groupBy { it.height }
                .map { (_, group) ->
                    // H.264 plays on every phone; where it stops (above 1080p) VP9 is next, AV1 last.
                    val best = group.sortedWith(
                        compareBy({ codecRank(it.codec.orEmpty()) }, { it.fps }, { it.bitrate }),
                    ).last()
                    TubeRendition(best.height, best.fps, best.content.orEmpty(), best.codec.orEmpty().startsWith("vp9", true) || best.codec.orEmpty().startsWith("vp09", true))
                }
                .sortedBy { it.height }
            val audio = info.audioStreams
                .filter {
                    it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && !it.content.isNullOrBlank() &&
                        (it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL)
                }
                .maxByOrNull { it.averageBitrate }
            val muxed = info.videoStreams
                .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && !it.content.isNullOrBlank() }
                .maxByOrNull { it.height }
            TubeStreams(
                title = info.name.orEmpty(),
                channel = info.uploaderName.orEmpty(),
                views = info.viewCount,
                likes = info.likeCount,
                uploaded = info.textualUploadDate.orEmpty(),
                description = info.description?.content.orEmpty(),
                renditions = rungs,
                audioUrl = audio?.content,
                muxedUrl = muxed?.content,
                related = info.relatedItems.toVideos(),
            )
        }
    }

    private fun codecRank(codec: String): Int = when {
        codec.startsWith("avc", true) -> 3
        codec.startsWith("vp9", true) || codec.startsWith("vp09", true) -> 2
        codec.startsWith("av01", true) -> 1
        else -> 0
    }

    private fun List<InfoItem>.toVideos(): List<TubeVideo> =
        filterIsInstance<StreamInfoItem>().mapNotNull { item ->
            val id = Regex("[?&]v=([A-Za-z0-9_-]{11})").find(item.url)?.groupValues?.get(1)
                ?: Regex("/shorts/([A-Za-z0-9_-]{11})").find(item.url)?.groupValues?.get(1)
                ?: item.url.substringAfterLast('/').takeIf { it.length == 11 }
                ?: return@mapNotNull null
            TubeVideo(
                id = id,
                title = item.name,
                channel = item.uploaderName.orEmpty(),
                thumbnail = item.thumbnails.maxByOrNull { it.height }?.url ?: item.thumbnails.firstOrNull()?.url,
                durationSec = item.duration.coerceAtLeast(0L),
                views = item.viewCount,
                uploaded = item.textualUploadDate.orEmpty(),
                isLive = item.duration < 0L,
                avatar = item.uploaderAvatars.firstOrNull()?.url,
            )
        }.distinctBy { it.id }
}
