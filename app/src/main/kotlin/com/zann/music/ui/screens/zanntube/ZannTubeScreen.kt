package com.zann.music.ui.screens.zanntube

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.zann.music.data.zanntube.TubePage
import com.zann.music.data.zanntube.TubeStore
import com.zann.music.data.zanntube.TubeStreams
import com.zann.music.data.zanntube.TubeVideo
import com.zann.music.data.zanntube.ZannTubeRepository
import com.zann.music.playback.PlayerManager
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page

private val TubeRed = Color(0xFFFF0033)

/** A paged list of videos: refresh from the start, then load more as the user nears the end. */
@Stable
internal class Feed(private val source: suspend (Page?) -> Result<TubePage>, private val keep: (TubeVideo) -> Boolean = { true }) {
    var items by mutableStateOf<List<TubeVideo>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf(false); private set
    private var next: Page? = null
    val hasMore: Boolean get() = next != null

    suspend fun refresh() {
        loading = true; error = false
        source(null).fold(
            onSuccess = { items = it.items.filter(keep); next = it.next },
            onFailure = { items = emptyList(); next = null; error = true },
        )
        loading = false
    }

    suspend fun more() {
        val page = next ?: return
        if (loading) return
        loading = true
        source(page).onSuccess { items = (items + it.items.filter(keep)).distinctBy { v -> v.id }; next = it.next }
            .onFailure { next = null }
        loading = false
    }
}

private val CHIPS = listOf(
    "Semua" to null, "Musik" to "musik indonesia terbaru", "Game" to "gameplay game", "Live" to "live streaming",
    "Berita" to "berita terkini", "Olahraga" to "olahraga highlight", "Film" to "trailer film", "Komedi" to "komedi lucu",
    "Podcast" to "podcast indonesia", "Belajar" to "tutorial belajar", "Masak" to "resep masakan", "Teknologi" to "review teknologi",
)

@Composable
fun ZannTubeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { TubeStore.init(context) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var playing by remember { mutableStateOf<TubeVideo?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }

    val video = playing
    if (video != null) {
        BackHandler { playing = null }
        WatchPage(video, onClose = { playing = null }, onPlay = { playing = it })
        return
    }
    BackHandler(enabled = searching) { searching = false }
    BackHandler(enabled = !searching && tab != 0) { tab = 0 }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                searching -> SearchPage(onClose = { searching = false }, onPlay = { playing = it })
                tab == 0 -> HomePage(onBack, onSearch = { searching = true }, onPlay = { playing = it })
                tab == 1 -> ShortsPage()
                tab == 2 -> SubscriptionsPage(onPlay = { playing = it })
                else -> YouPage(onPlay = { playing = it })
            }
        }
        if (!searching) NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
            val tabs = listOf("Beranda" to Icons.Default.Home, "Shorts" to Icons.Default.Slideshow, "Langganan" to Icons.Default.Subscriptions, "Anda" to Icons.Default.AccountCircle)
            tabs.forEachIndexed { i, (label, icon) ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 11.sp) })
            }
        }
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 30.dp, height = 21.dp).clip(RoundedCornerShape(6.dp)).background(TubeRed), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text("ZannTube", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun FeedList(feed: Feed, onPlay: (TubeVideo) -> Unit, header: (@Composable () -> Unit)? = null, empty: String = "Tidak ada video") {
    val state = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0; feed.items.isNotEmpty() && last >= feed.items.lastIndex - 3 } }
    LaunchedEffect(nearEnd) { if (nearEnd && feed.hasMore && !feed.loading) feed.more() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (header != null) item(key = "header") { header() }
            items(feed.items, key = { it.id }) { VideoCard(it) { onPlay(it) } }
            if (feed.loading && feed.items.isNotEmpty()) item(key = "more") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = TubeRed) }
            }
        }
        if (feed.loading && feed.items.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.Center), color = TubeRed)
        if (!feed.loading && feed.items.isEmpty()) Text(if (feed.error) "Gagal memuat. Periksa koneksi." else empty, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun HomePage(onBack: () -> Unit, onSearch: () -> Unit, onPlay: (TubeVideo) -> Unit) {
    var chip by rememberSaveable { mutableStateOf(0) }
    val query = CHIPS[chip].second
    val feed = remember(chip) {
        Feed({ page -> if (query == null) ZannTubeRepository.trending(page) else ZannTubeRepository.search(query, page) }, keep = { it.durationSec > 60L || it.isLive })
    }
    LaunchedEffect(feed) { feed.refresh() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Logo(Modifier.weight(1f))
            IconButton(onClick = onSearch) { Icon(Icons.Default.Search, null) }
        }
        LazyRow(Modifier.padding(bottom = 10.dp), contentPadding = contentPaddingValues(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CHIPS.size) { i ->
                val on = i == chip
                Text(
                    CHIPS[i].first, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (on) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .clickable { chip = i }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        FeedList(feed, onPlay)
    }
}

@Composable
private fun contentPaddingValues() = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)

@Composable
private fun SearchPage(onClose: () -> Unit, onPlay: (TubeVideo) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    val feed = remember(submitted) { Feed({ page -> ZannTubeRepository.search(submitted, page) }) }
    LaunchedEffect(feed) { if (submitted.isNotBlank()) feed.refresh() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            TextField(
                value = text, onValueChange = { text = it }, singleLine = true, placeholder = { Text("Telusuri ZannTube") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitted = text.trim() }),
            )
            IconButton(onClick = { submitted = text.trim() }) { Icon(Icons.Default.Search, null) }
        }
        if (submitted.isBlank()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cari video apa saja", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        else FeedList(feed, onPlay)
    }
}

@Composable
private fun SubscriptionsPage(onPlay: (TubeVideo) -> Unit) {
    val subs by TubeStore.subscriptions.collectAsState()
    val feed = remember(subs) {
        Feed({ page ->
            if (page != null || subs.isEmpty()) Result.success(TubePage(emptyList(), null))
            else runCatching {
                // Newest-looking videos of every followed channel, interleaved so no channel crowds the rest.
                val lists = subs.map { name -> ZannTubeRepository.search(name).getOrNull()?.items.orEmpty().filter { it.channel.equals(name, ignoreCase = true) } }
                val merged = mutableListOf<TubeVideo>()
                var i = 0
                while (lists.any { i < it.size }) { lists.forEach { l -> l.getOrNull(i)?.let(merged::add) }; i++ }
                TubePage(merged, null)
            }
        })
    }
    LaunchedEffect(feed) { feed.refresh() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Logo(Modifier.weight(1f)) }
        if (subs.isNotEmpty()) LazyRow(Modifier.padding(bottom = 12.dp), contentPadding = contentPaddingValues(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(subs, key = { it }) { name ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width72()) {
                    Avatar(null, name, 56)
                    Text(name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        FeedList(feed, onPlay, empty = "Belum ada channel yang diikuti.\nBuka video lalu tekan Berlangganan.")
    }
}

private fun Modifier.width72() = this.then(Modifier.size(width = 72.dp, height = 90.dp))

@Composable
private fun YouPage(onPlay: (TubeVideo) -> Unit) {
    val history by TubeStore.history.collectAsState()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Riwayat", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (history.isNotEmpty()) Text("Hapus", color = TubeRed, modifier = Modifier.clickable { TubeStore.clearHistory() }.padding(8.dp))
        }
        if (history.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada riwayat tontonan", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) { items(history, key = { it.id }) { VideoCard(it) { onPlay(it) } } }
    }
}

@Composable
private fun Avatar(url: String?, name: String, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        if (url != null) AsyncImage(model = url, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun VideoCard(video: TubeVideo, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box {
            AsyncImage(model = video.thumbnail, contentDescription = video.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF222222)))
            Text(
                if (video.isLive) "LIVE" else clock(video.durationSec * 1000L), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(if (video.isLive) TubeRed else Color(0xCC000000)).padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Avatar(video.avatar, video.channel, 38)
            Column(Modifier.padding(start = 12.dp)) {
                Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                val meta = listOfNotNull(video.channel.takeIf { it.isNotBlank() }, viewsText(video.views), video.uploaded.takeIf { it.isNotBlank() }).joinToString(" · ")
                Text(meta, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

internal fun viewsText(count: Long): String? = when {
    count < 0 -> null
    count >= 1_000_000_000 -> "%.1f mi x ditonton".format(count / 1e9)
    count >= 1_000_000 -> "%.1f jt x ditonton".format(count / 1e6)
    count >= 1_000 -> "%.1f rb x ditonton".format(count / 1e3)
    else -> "$count x ditonton"
}

@Composable
private fun WatchPage(video: TubeVideo, onClose: () -> Unit, onPlay: (TubeVideo) -> Unit) {
    var streams by remember(video.id) { mutableStateOf<TubeStreams?>(null) }
    var failed by remember(video.id) { mutableStateOf(false) }
    var attempt by remember(video.id) { mutableStateOf(0) }
    var height by rememberSaveable(video.id) { mutableStateOf(1080) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val subs by TubeStore.subscriptions.collectAsState()

    LaunchedEffect(video.id) { PlayerManager.pause(); TubeStore.addHistory(video) }
    LaunchedEffect(video.id, attempt) {
        failed = false
        ZannTubeRepository.streams(video.id).fold(onSuccess = { streams = it }, onFailure = { failed = true })
    }
    FullscreenEffect(fullscreen)
    BackHandler(enabled = fullscreen) { fullscreen = false }

    val player: @Composable (Modifier) -> Unit = { m ->
        TubePlayer(video.id, streams, height, { height = it }, fullscreen, { fullscreen = it }, onClose, { attempt++ }, m)
    }
    if (fullscreen) {
        player(Modifier.fillMaxSize())
        return
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        player(Modifier.statusBarsPadding().fillMaxWidth().aspectRatio(16f / 9f))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(key = "info") {
                Column(Modifier.padding(12.dp)) {
                    Text(streams?.title?.ifBlank { null } ?: video.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    val meta = listOfNotNull(viewsText(streams?.views ?: video.views), (streams?.uploaded ?: video.uploaded).takeIf { it.isNotBlank() }).joinToString(" · ")
                    Text(meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                    val channel = streams?.channel?.ifBlank { null } ?: video.channel
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(video.avatar, channel, 40)
                        Text(channel, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                        val on = channel in subs
                        Text(
                            if (on) "Diikuti" else "Berlangganan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.background,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (on) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface)
                                .clickable { TubeStore.toggleSubscription(channel) }.padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                    if (failed) Text("Gagal memuat video. Ketuk untuk coba lagi.", color = TubeRed, modifier = Modifier.padding(top = 12.dp).clickable { attempt++ })
                }
            }
            items(streams?.related.orEmpty(), key = { it.id }) { VideoCard(it) { onPlay(it) } }
        }
    }
}

/** Vertical, swipe-up Shorts: one short plays at a time, looping, with its audio. */
@Composable
private fun ShortsPage() {
    val queries = remember { listOf("shorts", "short lucu", "shorts viral indonesia", "shorts musik") }
    val feed = remember {
        Feed({ page -> ZannTubeRepository.search(queries.random(), page) }, keep = { it.durationSec in 1L..60L })
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(feed) {
        feed.refresh()
        var guard = 0
        while (feed.items.size < 6 && feed.hasMore && guard++ < 3) feed.more()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (feed.items.isEmpty()) {
            if (feed.loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = TubeRed)
            else Text("Shorts tidak tersedia. Tarik ulang nanti.", color = Color.White, modifier = Modifier.align(Alignment.Center).clickable { scope.launch { feed.refresh() } }.padding(16.dp))
        } else {
            val pager = rememberPagerState { feed.items.size }
            LaunchedEffect(pager.currentPage) {
                if (pager.currentPage >= feed.items.size - 3 && !feed.loading) {
                    feed.more()
                    if (feed.items.size - pager.currentPage < 4 && feed.hasMore) feed.more()
                }
            }
            VerticalPager(pager, Modifier.fillMaxSize(), key = { feed.items[it].id }) { page ->
                ShortItem(feed.items[page], active = pager.currentPage == page)
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ShortItem(video: TubeVideo, active: Boolean) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(model = video.thumbnail, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        if (active) {
            val exo = remember(video.id) { newTubePlayer(context).apply { repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE } }
            var paused by remember(video.id) { mutableStateOf(false) }
            androidx.compose.runtime.DisposableEffect(exo) { onDispose { exo.release() } }
            LaunchedEffect(video.id) {
                PlayerManager.pause()
                ZannTubeRepository.streams(video.id).onSuccess { s ->
                    tubeSource(s, 720)?.let { exo.setMediaSource(it); exo.prepare(); exo.playWhenReady = true }
                }
            }
            AndroidView(
                factory = { ctx -> android.view.TextureView(ctx).also { exo.setVideoTextureView(it) } },
                onRelease = { exo.clearVideoSurface() },
                modifier = Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    paused = !paused; exo.playWhenReady = !paused
                },
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(video.channel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(video.title, color = Color.White, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
