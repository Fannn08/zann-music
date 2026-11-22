package com.zann.music.ui.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.zann.music.R
import com.zann.music.data.settings.SettingsRepository
import com.zann.music.playback.PlayerManager
import com.zann.music.playback.audio.SoundStyle
import com.zann.music.ui.theme.VyoraGroupedBackground
import com.zann.music.ui.theme.VyoraGroupedCell
import com.zann.music.ui.theme.VyoraSeparator
import com.zann.music.ui.theme.VyoraText
import com.zann.music.ui.theme.VyoraTextMuted

/**
 * Lembar "Ekualiser" di pemutar: ekualiser bawaan Android, Gaya suara (sped up, slowed + reverb, ...)
 * dan Lanjutan (tempo dan nada). Dibuka dari tombol kecepatan di Now Playing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings by SettingsRepository.settings.collectAsState()
    val title by PlayerManager.currentTitle.collectAsState()
    val artist by PlayerManager.currentArtist.collectAsState()
    val thumb by PlayerManager.currentThumbnail.collectAsState()
    var styleDialog by remember { mutableStateOf(false) }
    var advancedDialog by remember { mutableStateOf(false) }
    val noSystemEq = stringResource(R.string.sound_system_eq_missing)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VyoraGroupedBackground,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF6B7BFF), Color(0xFFE86BFF)))),
                ) {
                    if (!thumb.isNullOrBlank()) {
                        AsyncImage(model = thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title.orEmpty(), color = VyoraText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist.orEmpty(), color = VyoraTextMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.size(18.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(VyoraGroupedCell)) {
                SheetRow(
                    title = stringResource(R.string.sound_equalizer),
                    subtitle = stringResource(R.string.sound_system_eq_desc),
                    icon = { Icon(Icons.Default.GraphicEq, null, tint = VyoraTextMuted, modifier = Modifier.size(30.dp)) },
                ) {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, noSystemEq, Toast.LENGTH_SHORT).show()
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 22.dp), color = VyoraSeparator.copy(alpha = 0.6f))
                SheetRow(
                    title = stringResource(R.string.sound_style),
                    subtitle = stringResource(R.string.sound_style_summary),
                    icon = { Icon(Icons.Default.Speed, null, tint = VyoraTextMuted, modifier = Modifier.size(30.dp)) },
                ) { styleDialog = true }
                HorizontalDivider(Modifier.padding(horizontal = 22.dp), color = VyoraSeparator.copy(alpha = 0.6f))
                SheetRow(
                    title = stringResource(R.string.sound_advanced),
                    subtitle = stringResource(R.string.sound_advanced_summary),
                    icon = { Icon(Icons.Default.Tune, null, tint = VyoraTextMuted, modifier = Modifier.size(30.dp)) },
                ) { advancedDialog = true }
            }
        }
    }
    if (styleDialog) SoundStyleDialog(selected = settings.soundStyle, onDismiss = { styleDialog = false })
    if (advancedDialog) PlaybackSpeedDialog(onDismiss = { advancedDialog = false })
}

@Composable
private fun SheetRow(title: String, subtitle: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 26.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = VyoraText, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(2.dp))
            Text(subtitle, color = VyoraTextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        icon()
    }
}

/** Dialog "Gaya suara": satu pilihan, langsung dipakai ke lagu yang sedang diputar. */
@Composable
fun SoundStyleDialog(selected: SoundStyle, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(36.dp), color = VyoraGroupedCell) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.sound_style),
                    color = VyoraText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp),
                )
                Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                    SoundStyle.entries.forEach { style ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                .clickable { SettingsRepository.update { it.copy(soundStyle = style) } }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = style == selected,
                                onClick = { SettingsRepository.update { it.copy(soundStyle = style) } },
                                colors = RadioButtonDefaults.colors(selectedColor = VyoraText, unselectedColor = VyoraTextMuted),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(styleTitle(style)), color = VyoraText, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(styleDesc(style)), color = VyoraTextMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.size(10.dp))
                HorizontalDivider(color = VyoraSeparator.copy(alpha = 0.6f))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(stringResource(R.string.sound_ok), color = VyoraText, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun styleTitle(s: SoundStyle): Int = when (s) {
    SoundStyle.NORMAL -> R.string.sound_style_normal
    SoundStyle.SPED_UP -> R.string.sound_style_sped_up
    SoundStyle.SLOWED_REVERB -> R.string.sound_style_slowed_reverb
    SoundStyle.NIGHTCORE -> R.string.sound_style_nightcore
    SoundStyle.LOFI -> R.string.sound_style_lofi
    SoundStyle.EIGHT_D -> R.string.sound_style_8d
    SoundStyle.CONCERT -> R.string.sound_style_concert
}

private fun styleDesc(s: SoundStyle): Int = when (s) {
    SoundStyle.NORMAL -> R.string.sound_style_normal_desc
    SoundStyle.SPED_UP -> R.string.sound_style_sped_up_desc
    SoundStyle.SLOWED_REVERB -> R.string.sound_style_slowed_reverb_desc
    SoundStyle.NIGHTCORE -> R.string.sound_style_nightcore_desc
    SoundStyle.LOFI -> R.string.sound_style_lofi_desc
    SoundStyle.EIGHT_D -> R.string.sound_style_8d_desc
    SoundStyle.CONCERT -> R.string.sound_style_concert_desc
}
