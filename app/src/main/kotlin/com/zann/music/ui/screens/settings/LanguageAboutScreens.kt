package com.zann.music.ui.screens.settings

import androidx.compose.material.icons.automirrored.filled.Send
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.zann.music.data.update.UpdateManager
import com.zann.music.data.update.UpdateState
import com.zann.music.ui.components.updateErrorText
import com.zann.music.ui.theme.SystemColors
import com.zann.music.ui.theme.VyoraGroupedCell
import com.zann.music.R
import com.zann.music.core.LocaleHelper.findActivity
import com.zann.music.data.settings.AppLanguage
import com.zann.music.data.settings.SettingsRepository
import com.zann.music.ui.theme.VyoraText
import com.zann.music.ui.theme.VyoraTextMuted

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val settings by SettingsRepository.settings.collectAsState()
    val context = LocalContext.current
    SettingsPage(title = stringResource(R.string.settings_language), onBack = onBack) {
        item(key = "languages") {
            SettingsGroup(footer = stringResource(R.string.language_footer)) {
                AppLanguage.entries.forEachIndexed { i, lang ->
                    SettingsChoiceRow(
                        title = languageLabel(lang),
                        subtitle = if (lang == AppLanguage.SYSTEM) null else lang.tag.uppercase(),
                        selected = settings.language == lang,
                        divider = i < AppLanguage.entries.lastIndex,
                        onClick = {
                            if (settings.language != lang) {
                                SettingsRepository.update { it.copy(language = lang) }
                                context.findActivity()?.recreate()
                            }
                        },
                    )
                }
            }
        }
    }
}

const val TELEGRAM_URL = "https://t.me/zannhdr"
const val TIKTOK_URL = "https://www.tiktok.com/@hydrococoz"
private const val DEVELOPER_NAME = "ZANN"

private data class Credit(val name: String, val license: String, val url: String)

private val Credits = listOf(
    Credit("NewPipe Extractor", "GPL-3.0", "https://github.com/TeamNewPipe/NewPipeExtractor"),
    Credit("Convx (glass modifier, nav bar, mini player)", "GPL-3.0", "https://github.com/cosmictaserdev-creator/Convx"),
    Credit("Kyant0 / backdrop", "Apache-2.0", "https://github.com/Kyant0/backdrop"),
    Credit("Kyant0 / Capsule", "Apache-2.0", "https://github.com/Kyant0/Capsule"),
    Credit("compose-floating-tab-bar", "Apache-2.0", "https://github.com/elyesmansour/compose-floating-tab-bar"),
    Credit("AndroidX Media3", "Apache-2.0", "https://github.com/androidx/media"),
    Credit("Coil", "Apache-2.0", "https://github.com/coil-kt/coil"),
    Credit("BitChord (lossless output detection)", "GPL-3.0", "https://github.com/kushagrasinghx/BitChord"),
    Credit("YumaPlayer (word-synced lyrics approach)", "GPL-3.0", "https://github.com/MuwMx/YumaPlayer"),
    Credit("BetterLyrics (word-synced lyrics)", "", "https://lyrics-api.boidu.dev"),
    Credit("LRCLIB (lyrics)", "", "https://lrclib.net"),
)

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenLicenses: () -> Unit = {}) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            "${info.versionName} ($code)"
        }.getOrDefault("")
    }
    val icon = remember {
        runCatching { context.packageManager.getApplicationIcon(context.packageName).toBitmap(192, 192).asImageBitmap() }.getOrNull()
    }
    fun open(url: String) = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    val updateState by UpdateManager.state.collectAsState()
    val currentVersion = UpdateManager.currentVersion?.toString() ?: com.zann.music.BuildConfig.VERSION_NAME
    val updateStatus = when (val s = updateState) {
        UpdateState.Idle -> stringResource(R.string.update_current_version, currentVersion)
        UpdateState.Checking -> stringResource(R.string.update_checking)
        UpdateState.UpToDate -> stringResource(R.string.update_up_to_date, currentVersion)
        is UpdateState.Available -> stringResource(R.string.update_new_version, s.release.version.toString())
        is UpdateState.Downloading ->
            if (s.progress < 0f) stringResource(R.string.update_downloading)
            else stringResource(R.string.update_downloading_percent, (s.progress * 100).toInt())
        is UpdateState.ReadyToInstall -> stringResource(R.string.update_ready, s.release.version.toString())
        is UpdateState.Failed -> updateErrorText(s.reason)
    }

    SettingsPage(title = stringResource(R.string.settings_about), onBack = onBack) {
        item(key = "hero") {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(VyoraGroupedCell)
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Image(icon, null, Modifier.size(76.dp).clip(RoundedCornerShape(19.dp)))
                    Spacer(Modifier.size(20.dp))
                }
                Column {
                    Text(
                        stringResource(R.string.app_name), color = VyoraText,
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${com.zann.music.BuildConfig.VERSION_NAME} \u00B7 universal", color = VyoraTextMuted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        item(key = "update") {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.update_check_now),
                    subtitle = updateStatus,
                    icon = Icons.Default.CloudDownload, iconTint = SystemColors.Green,
                    divider = false,
                    onClick = {
                        when (updateState) {
                            is UpdateState.Available, is UpdateState.Downloading, is UpdateState.ReadyToInstall -> UpdateManager.showPrompt()
                            UpdateState.Checking -> Unit
                            else -> UpdateManager.check(context, manual = true)
                        }
                    },
                )
            }
        }
        item(key = "developer") {
            Box(
                Modifier.fillMaxWidth().padding(top = 22.dp, start = 16.dp, end = 16.dp)
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black),
            ) {
                Image(
                    painterResource(R.drawable.about_photo), null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color(0xE6000000),
                        )
                    )
                )
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            DEVELOPER_NAME, color = Color.White,
                            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.about_developer), color = Color(0xCCFFFFFF),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Image(
                        painterResource(R.drawable.about_avatar), null,
                        Modifier.size(104.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        item(key = "social") {
            SettingsGroup(header = stringResource(R.string.about_social)) {
                SettingsNavRow(
                    title = "Telegram",
                    icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                    iconTint = Color(0xFF229ED9),
                    value = "@zannhdr",
                    onClick = { open(TELEGRAM_URL) },
                )
                SettingsNavRow(
                    title = "TikTok",
                    icon = Icons.Default.MusicNote,
                    iconTint = Color(0xFFFF2D55),
                    value = "@hydrococoz",
                    onClick = { open(TIKTOK_URL) },
                    divider = false,
                )
            }
        }
        item(key = "info") {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.about_version),
                    trailing = { Text(version, color = VyoraTextMuted, style = MaterialTheme.typography.bodyLarge) },
                )
                SettingsNavRow(
                    title = stringResource(R.string.about_license),
                    value = "GPL-3.0",
                    onClick = onOpenLicenses,
                )
                SettingsNavRow(
                    title = stringResource(R.string.about_source_code),
                    value = "GitHub",
                    onClick = { open("https://github.com/Fannn08/zann-music") },
                    divider = false,
                )
            }
        }
        item(key = "credits") {
            SettingsGroup(
                header = stringResource(R.string.about_credits),
                footer = stringResource(R.string.about_disclaimer),
            ) {
                Credits.forEachIndexed { i, c ->
                    SettingsNavRow(
                        title = c.name,
                        value = c.license.ifBlank { null },
                        onClick = { open(c.url) },
                        divider = i < Credits.lastIndex,
                    )
                }
            }
        }
    }
}
