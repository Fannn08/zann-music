package com.zann.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.zann.music.data.account.SyncEngine
import com.zann.music.data.download.Downloads
import com.zann.music.data.download.OfflineArtInterceptor
import com.zann.music.data.library.LibraryManager
import com.zann.music.data.library.SearchHistory
import com.zann.music.data.settings.SettingsRepository
import com.zann.music.data.update.UpdateManager

class VyoraApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        com.zann.music.core.CrashLog.install(this)
        LibraryManager.init(this)
        SearchHistory.init(this)
        SyncEngine.init(this)
        SettingsRepository.init(this)
        UpdateManager.init(this)
        Downloads.init(this)
        com.zann.music.core.SecureBox.init(this)
        com.zann.music.data.discord.DiscordAccount.init(this)
        com.zann.music.data.spotify.SpotifyAccount.init(this)
        com.zann.music.data.motion.MotionArtwork.init(this)
        com.zann.music.core.NetworkState.init(this)
        com.zann.music.data.server.MusicServer.init(this)
        com.zann.music.data.lyrics.LyricsRepository.init(this)
        com.zann.music.data.scrobble.Scrobbler.init(this)
        com.zann.music.data.local.LosslessOutput.init(this)
        com.zann.music.data.youtube.LoudnessStore.init(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.15)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(DISK_CACHE_MB * 1024L * 1024L)
                .build()
        }
        .components { add(OfflineArtInterceptor(this@VyoraApp)) }
        .crossfade(true)
        .build()

    companion object {
        const val DISK_CACHE_MB = 80L
    }
}
