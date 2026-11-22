@file:OptIn(coil.annotation.ExperimentalCoilApi::class)

package com.zann.music.ui.screens.settings

import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.zann.music.R
import com.zann.music.data.library.LibraryManager
import com.zann.music.data.library.SearchHistory
import com.zann.music.data.lyrics.LyricsRepository
import com.zann.music.data.settings.AccentChoice
import com.zann.music.data.settings.AppLanguage
import com.zann.music.data.settings.AudioQuality
import com.zann.music.data.settings.CacheSize
import com.zann.music.data.settings.GlassLook
import com.zann.music.data.settings.SettingsRepository
import com.zann.music.data.settings.ThemeMode
import com.zann.music.data.settings.VideoArtwork
import com.zann.music.data.update.UpdateManager
import com.zann.music.data.update.UpdateState
import com.zann.music.ui.components.updateErrorText
import com.zann.music.data.youtube.StreamResolver
import com.zann.music.playback.source.MediaCache
import com.zann.music.ui.theme.SystemColors
import com.zann.music.ui.theme.VyoraDestructive
import com.zann.music.ui.theme.VyoraGroupedCell
import com.zann.music.ui.theme.VyoraText
import com.zann.music.ui.theme.VyoraTextMuted
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings root, laid out like Apple Music's settings: one short list of sections, details on their own pages
 * (Appearance, Audio Quality, Song Transitions, Downloads, Storage, Backup, Software Update).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val settings by SettingsRepository.settings.collectAsState()
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    val clearedText = stringResource(R.string.cleared)
    val downloads by com.zann.music.data.download.Downloads.entries.collectAsState()
    val server by com.zann.music.data.server.MusicServer.config.collectAsState()
    val spotify by com.zann.music.data.spotify.SpotifyAccount.profile.collectAsState()
    val update by UpdateManager.state.collectAsState()
    val islandSettings by com.zann.music.data.island.IslandSettings.config.collectAsState()

    SettingsPage(title = stringResource(R.string.settings), onBack = onBack) {
        item(key = "account") {
            SettingsGroup { AccountSummaryRow(onClick = { onOpen("account") }) }
        }
        item(key = "stats") {
            SettingsGroup(header = stringResource(R.string.settings_stats_group)) {
                SettingsNavRow(
                    title = stringResource(R.string.streak_title),
                    icon = Icons.Default.LocalFireDepartment, iconTint = SystemColors.Orange,
                    onClick = { onOpen("streak") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.top_title),
                    icon = Icons.Default.Leaderboard, iconTint = SystemColors.Purple,
                    onClick = { onOpen("top_listener") },
                    divider = false,
                )
            }
        }
        item(key = "general") {
            SettingsGroup {
                SettingsNavRow(
                    title = stringResource(R.string.settings_appearance),
                    icon = Icons.Default.Palette, iconTint = SystemColors.Indigo,
                    value = themeLabel(settings.themeMode),
                    onClick = { onOpen("appearance") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.island_title),
                    icon = Icons.Default.PhoneAndroid, iconTint = SystemColors.Purple,
                    value = if (islandSettings.enabled) stringResource(R.string.island_on) else stringResource(R.string.vyora_off),
                    onClick = { onOpen("island") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_liquid_glass),
                    icon = Icons.Default.WaterDrop, iconTint = SystemColors.Teal,
                    value = if (!settings.glass.enabled) stringResource(R.string.vyora_off) else glassLookLabel(settings.glass.style),
                    onClick = { onOpen("glass") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_language),
                    icon = Icons.Default.Language, iconTint = SystemColors.Blue,
                    value = languageLabel(settings.language),
                    onClick = { onOpen("language") },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_haptics),
                    icon = Icons.Default.Vibration, iconTint = SystemColors.Gray,
                    checked = settings.haptics,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(haptics = v) } },
                    divider = false,
                )
            }
        }
        item(key = "audio") {
            SettingsGroup(header = stringResource(R.string.settings_audio)) {
                SettingsNavRow(
                    title = stringResource(R.string.audio_quality),
                    icon = Icons.Default.HighQuality, iconTint = SystemColors.Red,
                    value = qualityLabel(settings.audioQuality),
                    onClick = { onOpen("audio_quality") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_output),
                    icon = Icons.Default.SettingsInputComponent, iconTint = SystemColors.Teal,
                    value = outputModeLabel(settings.outputPcmMode),
                    onClick = { onOpen("output") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_equalizer),
                    icon = Icons.Default.Equalizer, iconTint = SystemColors.Pink,
                    value = stringResource(if (settings.equalizer.enabled) R.string.vyora_on else R.string.vyora_off),
                    onClick = { onOpen("equalizer") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_transitions),
                    icon = Icons.Default.Tune, iconTint = SystemColors.Orange,
                    value = if (settings.crossfade) stringResource(R.string.seconds_short, settings.crossfadeSeconds.roundToInt())
                    else stringResource(R.string.vyora_off),
                    onClick = { onOpen("transitions") },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.sound_check),
                    icon = Icons.AutoMirrored.Filled.VolumeUp, iconTint = SystemColors.Purple,
                    checked = settings.loudnessNormalization,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(loudnessNormalization = v) } },
                    divider = false,
                )
            }
        }
        item(key = "playback") {
            SettingsGroup(
                header = stringResource(R.string.settings_playback),
                footer = stringResource(R.string.autoplay_footer),
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.autoplay),
                    icon = Icons.Default.AllInclusive, iconTint = SystemColors.Purple,
                    checked = settings.autoplay,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(autoplay = v) } },
                )
                if (settings.autoplay) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.dont_repeat_suggestions),
                        icon = Icons.Default.Repeat, iconTint = SystemColors.Indigo,
                        checked = settings.dontRepeatSuggestions,
                        onCheckedChange = { v -> SettingsRepository.update { it.copy(dontRepeatSuggestions = v) } },
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.prefer_songs),
                        icon = Icons.Default.MusicNote, iconTint = SystemColors.Red,
                        checked = settings.preferSongsOverVideos,
                        onCheckedChange = { v -> SettingsRepository.update { it.copy(preferSongsOverVideos = v) } },
                    )
                }
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_background_playback),
                    subtitle = stringResource(R.string.settings_background_playback_desc),
                    icon = Icons.Default.PlayCircle, iconTint = SystemColors.Green,
                    checked = settings.backgroundPlayback,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(backgroundPlayback = v) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.pause_on_disconnect),
                    icon = Icons.Default.Headphones, iconTint = SystemColors.Blue,
                    checked = settings.pauseOnDisconnect,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(pauseOnDisconnect = v) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.skip_unavailable),
                    icon = Icons.Default.SkipNext, iconTint = SystemColors.Indigo,
                    checked = settings.skipUnavailable,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(skipUnavailable = v) } },
                    divider = false,
                )
            }
        }
        item(key = "lyrics") {
            SettingsGroup {
                SettingsNavRow(
                    title = stringResource(R.string.lyrics),
                    subtitle = stringResource(R.string.lyrics_settings_summary),
                    icon = Icons.Default.Lyrics, iconTint = SystemColors.Purple,
                    value = stringResource(lyricsProviderTitleRes(settings.lyricsProvider)),
                    onClick = { onOpen("lyrics") },
                    divider = false,
                )
            }
        }
        item(key = "library") {
            SettingsGroup(header = stringResource(R.string.library)) {
                SettingsNavRow(
                    title = stringResource(R.string.downloads),
                    icon = Icons.Default.Download, iconTint = SystemColors.Blue,
                    value = downloads.size.takeIf { it > 0 }?.toString(),
                    onClick = { onOpen("downloads") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_storage),
                    icon = Icons.Default.Storage, iconTint = SystemColors.Gray,
                    value = cacheSizeLabel(settings.mediaCache),
                    onClick = { onOpen("storage") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.server_title),
                    icon = Icons.Default.Dns, iconTint = SystemColors.Indigo,
                    value = stringResource(if (server != null) R.string.on else R.string.off),
                    onClick = { onOpen("server") },
                    divider = false,
                )
            }
        }
        item(key = "integrations") {
            SettingsGroup(header = stringResource(R.string.settings_integrations)) {
                SettingsNavRow(
                    title = stringResource(R.string.spotify_title),
                    icon = Icons.Default.MusicNote, iconTint = Color(0xFF1DB954),
                    value = spotify?.name ?: stringResource(R.string.off),
                    onClick = { onOpen("spotify") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.discord_title),
                    icon = Icons.Default.Gamepad, iconTint = Color(0xFF5865F2),
                    value = stringResource(if (settings.discordPresence) R.string.on else R.string.off),
                    onClick = { onOpen("discord") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.scrobble_title),
                    icon = Icons.Default.GraphicEq, iconTint = Color(0xFFD51007),
                    value = stringResource(if (settings.scrobbleLastFm || settings.scrobbleListenBrainz) R.string.on else R.string.off),
                    onClick = { onOpen("scrobble") },
                    divider = false,
                )
            }
        }
        item(key = "about") {
            SettingsGroup {
                SettingsNavRow(
                    title = stringResource(R.string.backup_header),
                    icon = Icons.Default.Backup, iconTint = SystemColors.Green,
                    onClick = { onOpen("backup") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_updates),
                    icon = Icons.Default.SystemUpdate, iconTint = SystemColors.Gray,
                    value = when (val u = update) {
                        is UpdateState.Available -> u.release.version.toString()
                        is UpdateState.ReadyToInstall -> u.release.version.toString()
                        else -> com.zann.music.BuildConfig.VERSION_NAME
                    },
                    onClick = { onOpen("updates") },
                )
                SettingsNavRow(
                    title = stringResource(R.string.settings_about),
                    icon = Icons.Default.Info, iconTint = SystemColors.Gray,
                    onClick = { onOpen("about") },
                    divider = false,
                )
            }
        }
        item(key = "reset") {
            SettingsGroup {
                SettingsActionRow(
                    title = stringResource(R.string.reset_settings),
                    destructive = true,
                    onClick = { confirmReset = true },
                    divider = false,
                )
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            message = stringResource(R.string.confirm_reset),
            onDismiss = { confirmReset = false },
            onConfirm = {
                confirmReset = false
                SettingsRepository.resetAll()
                Toast.makeText(context, clearedText, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

/** Appearance: theme, accent, backgrounds and animated artwork. */
@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val settings by SettingsRepository.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var wallpaperReady by remember { mutableStateOf(com.zann.music.ui.design.HomeWallpaper.exists(context)) }
    val wallpaperFailed = stringResource(R.string.settings_home_bg_failed)
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) scope.launch {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
            val ok = withContext(Dispatchers.IO) {
                if (isVideo) com.zann.music.ui.design.HomeWallpaper.importVideo(context, uri)
                else com.zann.music.ui.design.HomeWallpaper.import(context, uri)
            }
            if (ok) {
                wallpaperReady = true
                SettingsRepository.update { it.copy(customBackground = true) }
            } else {
                Toast.makeText(context, wallpaperFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    SettingsPage(title = stringResource(R.string.settings_appearance), onBack = onBack) {
        item(key = "theme") {
            SettingsGroup(header = stringResource(R.string.settings_theme)) {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SettingsChoiceRow(
                        title = themeLabel(mode),
                        selected = settings.themeMode == mode,
                        onClick = { SettingsRepository.update { it.copy(themeMode = mode) } },
                        divider = i < ThemeMode.entries.lastIndex,
                    )
                }
            }
        }
        item(key = "accent") {
            SettingsGroup(header = stringResource(R.string.settings_accent)) {
                SettingsControlRow(divider = false) {
                    AccentSwatches(
                        selected = settings.accent,
                        onSelect = { accent -> SettingsRepository.update { it.copy(accent = accent) } },
                    )
                }
            }
        }
        item(key = "home_background") {
            SettingsGroup(
                header = stringResource(R.string.settings_home_bg_heading),
                footer = stringResource(R.string.settings_home_bg_footer),
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_home_bg),
                    subtitle = stringResource(R.string.settings_home_bg_desc),
                    checked = settings.customBackground && wallpaperReady,
                    onCheckedChange = { on ->
                        if (on && !wallpaperReady) {
                            picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        } else {
                            SettingsRepository.update { it.copy(customBackground = on) }
                        }
                    },
                    divider = true,
                )
                SettingsActionRow(
                    title = stringResource(
                        if (wallpaperReady) R.string.settings_home_bg_change else R.string.settings_home_bg_choose,
                    ),
                    onClick = {
                        picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    divider = wallpaperReady,
                )
                if (wallpaperReady) {
                    SettingsSliderRow(
                        title = stringResource(R.string.settings_home_bg_dim),
                        value = settings.customBackgroundDim,
                        onValueChange = { v -> SettingsRepository.update { it.copy(customBackgroundDim = v) } },
                        valueRange = 0f..0.9f,
                        valueLabel = "${(settings.customBackgroundDim * 100).toInt()}%",
                        enabled = settings.customBackground,
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.settings_home_bg_remove),
                        onClick = {
                            com.zann.music.ui.design.HomeWallpaper.remove(context)
                            wallpaperReady = false
                            SettingsRepository.update { it.copy(customBackground = false) }
                        },
                        destructive = true,
                        divider = false,
                    )
                }
            }
        }
        item(key = "backgrounds") {
            SettingsGroup {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_ambient),
                    subtitle = stringResource(R.string.settings_ambient_desc),
                    checked = settings.ambientBackground,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(ambientBackground = v) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_player_bg),
                    checked = settings.animatedPlayerBackground,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(animatedPlayerBackground = v) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.full_bleed_artwork),
                    subtitle = stringResource(R.string.full_bleed_artwork_desc),
                    checked = settings.fullBleedArtwork,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(fullBleedArtwork = v) } },
                    divider = false,
                )
            }
        }
        item(key = "video") {
            SettingsGroup(header = stringResource(R.string.video_artwork), footer = stringResource(R.string.video_artwork_footer)) {
                VideoArtwork.entries.forEachIndexed { i, mode ->
                    SettingsChoiceRow(
                        title = videoArtworkLabel(mode),
                        selected = settings.videoArtwork == mode,
                        onClick = { SettingsRepository.update { it.copy(videoArtwork = mode) } },
                        divider = i < VideoArtwork.entries.lastIndex,
                    )
                }
            }
        }
        item(key = "motion") {
            SettingsGroup(header = stringResource(R.string.motion_artwork), footer = stringResource(R.string.motion_artwork_footer)) {
                VideoArtwork.entries.forEachIndexed { i, mode ->
                    SettingsChoiceRow(
                        title = videoArtworkLabel(mode),
                        selected = settings.motionArtwork == mode,
                        onClick = { SettingsRepository.update { it.copy(motionArtwork = mode) } },
                        divider = i < VideoArtwork.entries.lastIndex,
                    )
                }
            }
        }
    }
}

/** Audio Quality, like Apple Music's: one checklist for Wi-Fi and, optionally, one for mobile data. */
/** Song Transitions: crossfade on/off and its length. */
@Composable
fun TransitionsScreen(onBack: () -> Unit) {
    val settings by SettingsRepository.settings.collectAsState()
    SettingsPage(title = stringResource(R.string.settings_transitions), onBack = onBack) {
        item(key = "crossfade") {
            SettingsGroup(footer = stringResource(R.string.crossfade_footer)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.crossfade),
                    checked = settings.crossfade,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(crossfade = v) } },
                    divider = settings.crossfade,
                )
                if (settings.crossfade) {
                    SettingsSliderRow(
                        title = stringResource(R.string.crossfade_duration),
                        value = settings.crossfadeSeconds,
                        onValueChange = { v -> SettingsRepository.update { it.copy(crossfadeSeconds = v.roundToInt().toFloat()) } },
                        valueRange = 1f..12f,
                        steps = 10,
                        valueLabel = stringResource(R.string.seconds_short, settings.crossfadeSeconds.roundToInt()),
                        divider = false,
                    )
                }
            }
        }
    }
}

/** Downloads: quality, Wi-Fi only, exporting to the Music folder and removing everything. */
@Composable
fun DownloadsSettingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit = {}) {
    val settings by SettingsRepository.settings.collectAsState()
    val metered by com.zann.music.core.NetworkState.metered.collectAsState()
    val context = LocalContext.current
    val downloads by com.zann.music.data.download.Downloads.entries.collectAsState()
    var offlineBytes by remember { mutableStateOf(0L) }
    var confirm by remember { mutableStateOf(false) }
    val clearedText = stringResource(R.string.cleared)
    LaunchedEffect(downloads.size) {
        offlineBytes = withContext(Dispatchers.IO) { com.zann.music.data.download.Downloads.usedBytes(context) }
    }
    SettingsPage(title = stringResource(R.string.downloads), onBack = onBack) {
        item(key = "quality") {
            SettingsGroup(footer = stringResource(R.string.download_quality_footer)) {
                SettingsNavRow(
                    title = stringResource(R.string.download_quality),
                    value = downloadQualityLabel(settings.downloadQuality),
                    onClick = { onOpen("quality/download") },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.download_wifi_only),
                    checked = settings.downloadWifiOnly,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(downloadWifiOnly = v) } },
                    badge = stringResource(R.string.waiting_for_wifi).takeIf { settings.downloadWifiOnly && metered },
                    divider = false,
                )
            }
        }
        item(key = "export") {
            SettingsGroup(footer = stringResource(R.string.export_downloads_footer)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.export_downloads),
                    subtitle = com.zann.music.data.download.DownloadExport.FOLDER.takeIf { settings.exportDownloads },
                    checked = settings.exportDownloads,
                    onCheckedChange = { v -> SettingsRepository.update { it.copy(exportDownloads = v) } },
                    divider = false,
                )
            }
        }
        item(key = "remove") {
            SettingsGroup(footer = stringResource(R.string.downloads_usage, downloads.size, Formatter.formatShortFileSize(context, offlineBytes))) {
                SettingsActionRow(
                    title = stringResource(R.string.downloads_remove_all),
                    destructive = true,
                    onClick = { if (downloads.isNotEmpty()) confirm = true },
                    divider = false,
                )
            }
        }
    }
    if (confirm) {
        ConfirmDialog(
            message = stringResource(R.string.confirm_remove_downloads),
            onDismiss = { confirm = false },
            onConfirm = {
                confirm = false
                com.zann.music.data.download.Downloads.removeAll(context)
                Toast.makeText(context, clearedText, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

/** Storage: song cache size, clearing caches and listening history. */
@Composable
fun StorageSettingsScreen(onBack: () -> Unit) {
    val settings by SettingsRepository.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableStateOf<Long?>(null) }
    var confirmHistory by remember { mutableStateOf(false) }
    val clearedText = stringResource(R.string.cleared)
    fun toast() = Toast.makeText(context, clearedText, Toast.LENGTH_SHORT).show()
    LaunchedEffect(Unit) { cacheBytes = withContext(Dispatchers.IO) { cacheUsage(context) } }
    SettingsPage(title = stringResource(R.string.settings_storage), onBack = onBack) {
        item(key = "cache") {
            SettingsGroup(footer = stringResource(R.string.clear_cache_usage, Formatter.formatShortFileSize(context, cacheBytes ?: 0L))) {
                SettingsSliderRow(
                    title = stringResource(R.string.media_cache_size),
                    value = settings.mediaCache.ordinal.toFloat(),
                    onValueChange = { v ->
                        val size = CacheSize.entries[v.roundToInt().coerceIn(0, CacheSize.entries.lastIndex)]
                        SettingsRepository.update { it.copy(mediaCache = size) }
                    },
                    valueRange = 0f..CacheSize.entries.lastIndex.toFloat(),
                    steps = CacheSize.entries.size - 2,
                    valueLabel = cacheSizeLabel(settings.mediaCache),
                )
                SettingsActionRow(
                    title = stringResource(R.string.clear_cache),
                    divider = false,
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                context.imageLoader.diskCache?.clear()
                                MediaCache.clear(context)
                                StreamResolver.clear()
                                LyricsRepository.clearCache()
                            }
                            context.imageLoader.memoryCache?.clear()
                            cacheBytes = withContext(Dispatchers.IO) { cacheUsage(context) }
                            toast()
                        }
                    },
                )
            }
        }
        item(key = "history") {
            SettingsGroup {
                SettingsActionRow(
                    title = stringResource(R.string.clear_search_history),
                    onClick = { SearchHistory.clear(); toast() },
                )
                SettingsActionRow(
                    title = stringResource(R.string.clear_listening_history),
                    destructive = true,
                    onClick = { confirmHistory = true },
                    divider = false,
                )
            }
        }
    }
    if (confirmHistory) {
        ConfirmDialog(
            message = stringResource(R.string.confirm_clear_history),
            onDismiss = { confirmHistory = false },
            onConfirm = {
                confirmHistory = false
                LibraryManager.clearRecent()
                toast()
            },
        )
    }
}

@Composable
fun BackupSettingsScreen(onBack: () -> Unit) {
    SettingsPage(title = stringResource(R.string.backup_header), onBack = onBack) {
        item(key = "backup") { BackupGroup() }
    }
}

@Composable
fun UpdatesSettingsScreen(onBack: () -> Unit) {
    val settings by SettingsRepository.settings.collectAsState()
    SettingsPage(title = stringResource(R.string.settings_updates), onBack = onBack) {
        item(key = "updates") { UpdatesGroup(autoCheck = settings.autoUpdateCheck) }
    }
}


@Composable
private fun UpdatesGroup(autoCheck: Boolean) {
    val context = LocalContext.current
    val state by UpdateManager.state.collectAsState()
    val version = UpdateManager.currentVersion?.toString() ?: com.zann.music.BuildConfig.VERSION_NAME
    val status = when (val s = state) {
        UpdateState.Idle -> stringResource(R.string.update_current_version, version)
        UpdateState.Checking -> stringResource(R.string.update_checking)
        UpdateState.UpToDate -> stringResource(R.string.update_up_to_date, version)
        is UpdateState.Available -> stringResource(R.string.update_new_version, s.release.version.toString())
        is UpdateState.Downloading ->
            if (s.progress < 0f) stringResource(R.string.update_downloading)
            else stringResource(R.string.update_downloading_percent, (s.progress * 100).toInt())
        is UpdateState.ReadyToInstall -> stringResource(R.string.update_ready, s.release.version.toString())
        is UpdateState.Failed -> updateErrorText(s.reason)
    }
    SettingsGroup(footer = stringResource(R.string.update_footer)) {
        SettingsRow(
            title = stringResource(R.string.update_check_now),
            subtitle = status,
            icon = Icons.Default.SystemUpdate, iconTint = SystemColors.Green,
            onClick = {
                when (state) {
                    is UpdateState.Available, is UpdateState.Downloading, is UpdateState.ReadyToInstall -> UpdateManager.showPrompt()
                    UpdateState.Checking -> Unit
                    else -> UpdateManager.check(context, manual = true)
                }
            },
        )
        SettingsSwitchRow(
            title = stringResource(R.string.update_auto_check),
            icon = Icons.Default.Update, iconTint = SystemColors.Blue,
            checked = autoCheck,
            onCheckedChange = { v -> UpdateManager.setAutoCheck(context, v) },
            divider = false,
        )
    }
}

private fun cacheUsage(context: Context): Long =
    runCatching { MediaCache.usedBytes(context) }.getOrDefault(0L) + (context.imageLoader.diskCache?.size ?: 0L)

private fun cacheSizeLabel(size: CacheSize): String =
    if (size.megabytes >= 1024) "${size.megabytes / 1024} GB" else "${size.megabytes} MB"

@Composable
private fun videoArtworkLabel(mode: VideoArtwork) = stringResource(
    when (mode) {
        VideoArtwork.ALWAYS -> R.string.video_artwork_always
        VideoArtwork.WIFI_ONLY -> R.string.video_artwork_wifi
        VideoArtwork.OFF -> R.string.vyora_off
    }
)

@Composable
fun ConfirmDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VyoraGroupedCell,
        titleContentColor = VyoraText,
        textContentColor = VyoraTextMuted,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        title = { Text(stringResource(R.string.confirm_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_continue), color = VyoraDestructive) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = VyoraText) }
        },
    )
}

@Composable
private fun AccentSwatches(selected: AccentChoice, onSelect: (AccentChoice) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AccentChoice.entries.forEach { choice ->
            val color = Color(choice.argb)
            val isSelected = choice == selected
            val ring by animateDpAsState(if (isSelected) 2.5.dp else 0.dp, label = "accentRing")
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(ring, VyoraText.copy(alpha = 0.85f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, role = Role.RadioButton,
                    ) { onSelect(choice) },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(if (isSelected) 26.dp else 34.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                    if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun themeLabel(mode: ThemeMode) = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
)

@Composable
fun qualityLabel(q: AudioQuality) = stringResource(
    when (q) {
        AudioQuality.LOSSLESS -> R.string.quality_lossless
        AudioQuality.HIGH -> R.string.quality_high
        AudioQuality.NORMAL -> R.string.quality_normal
        AudioQuality.LOW -> R.string.quality_low
    }
)

@Composable
fun glassLookLabel(look: GlassLook) = stringResource(
    when (look) {
        GlassLook.LIQUID -> R.string.glass_style_liquid
        GlassLook.FROSTED -> R.string.glass_style_frosted
        GlassLook.TINTED -> R.string.glass_style_tinted
    }
)

@Composable
fun languageLabel(language: AppLanguage) =
    if (language == AppLanguage.SYSTEM) stringResource(R.string.language_system) else language.nativeName
