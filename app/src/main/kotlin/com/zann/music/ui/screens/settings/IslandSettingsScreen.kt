package com.zann.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zann.music.R
import com.zann.music.data.island.IslandAccent
import com.zann.music.data.island.IslandButtonStyle
import com.zann.music.data.island.IslandConfig
import com.zann.music.data.island.IslandContentMode
import com.zann.music.data.island.IslandEqStyle
import com.zann.music.data.island.IslandPreset
import com.zann.music.data.island.IslandSettings
import com.zann.music.data.island.IslandShape
import com.zann.music.data.island.IslandTapAction
import com.zann.music.ui.island.IslandOverlayService
import com.zann.music.ui.island.IslandPreview
import com.zann.music.ui.theme.SystemColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun IslandSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val cfg by IslandSettings.config.collectAsState()
    var previewExpanded by remember { mutableStateOf(false) }
    var a11yOn by remember { mutableStateOf(IslandOverlayService.isEnabledInSystem(context)) }
    LaunchedEffect(Unit) {
        IslandSettings.init(context)
        while (true) { a11yOn = IslandOverlayService.isEnabledInSystem(context); delay(800) }
    }
    val noCutout = stringResource(R.string.island_fit_camera_none)

    fun set(block: (IslandConfig) -> IslandConfig) = IslandSettings.update(block)

    fun fitToCamera() {
        val cut = view.rootWindowInsets?.displayCutout
        val rect = cut?.boundingRects?.firstOrNull { it.top <= 8 } ?: cut?.boundingRects?.firstOrNull()
        if (rect == null) { Toast.makeText(context, noCutout, Toast.LENGTH_SHORT).show(); return }
        val d = context.resources.displayMetrics.density
        val screenW = context.resources.displayMetrics.widthPixels
        val sbId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBar = if (sbId > 0) context.resources.getDimensionPixelSize(sbId) else 0
        val h = (rect.height() / d).roundToInt() + 4
        val gap = (rect.width() / d).roundToInt()
        set {
            it.copy(
                offsetXDp = ((rect.centerX() - screenW / 2f) / d).roundToInt(),
                capsuleHeightDp = h.coerceIn(24, 60),
                cameraGapDp = gap.coerceIn(12, 120),
                capsuleWidthDp = maxOf(it.capsuleWidthDp, gap + 120),
                topGapDp = (((rect.centerY() - h * d / 2f) - statusBar) / d).roundToInt(),
            )
        }
    }

    SettingsPage(title = stringResource(R.string.island_title), onBack = onBack) {
        item(key = "island_show") {
            SettingsGroup(header = stringResource(R.string.island_title)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.island_show),
                    subtitle = stringResource(R.string.island_show_desc),
                    icon = Icons.Default.PhoneAndroid, iconTint = SystemColors.Purple,
                    checked = cfg.enabled,
                    onCheckedChange = { on ->
                        set { it.copy(enabled = on) }
                        if (on && !a11yOn) IslandOverlayService.openAccessibilitySettings(context)
                    },
                )
                SettingsNavRow(
                    title = stringResource(R.string.island_a11y),
                    subtitle = if (a11yOn) stringResource(R.string.island_a11y_on) else stringResource(R.string.island_a11y_off),
                    icon = if (a11yOn) Icons.Default.Check else Icons.Default.Close,
                    iconTint = if (a11yOn) SystemColors.Green else SystemColors.Red,
                    onClick = { IslandOverlayService.openAccessibilitySettings(context) },
                    divider = false,
                )
            }
        }
        item(key = "island_preset") {
            SettingsGroup(header = stringResource(R.string.island_preset), footer = stringResource(R.string.island_preset_desc)) {
                ChipsRow(
                    options = listOf(
                        IslandPreset.IPHONE to stringResource(R.string.island_preset_iphone),
                        IslandPreset.GLASS to stringResource(R.string.island_preset_glass),
                        IslandPreset.COLORFUL to stringResource(R.string.island_preset_colorful),
                        IslandPreset.MINIMAL to stringResource(R.string.island_preset_minimal),
                    ),
                    selected = cfg.preset, divider = false,
                    onSelect = { p -> set { IslandConfig.forPreset(p, it) } },
                )
            }
        }
        item(key = "island_preview") {
            SettingsGroup(header = stringResource(R.string.island_preview), footer = stringResource(R.string.island_preview_hint)) {
                SettingsControlRow(divider = false) {
                    Box(
                        Modifier.fillMaxWidth().height(if (previewExpanded) 190.dp else 110.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF3A4254), Color(0xFF12141C))))
                            .clickable { previewExpanded = !previewExpanded }
                            .padding(top = 18.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) { IslandPreview(cfg, previewExpanded) }
                }
            }
        }
        item(key = "island_appearance") {
            SettingsGroup(header = stringResource(R.string.island_appearance)) {
                SliderRow(stringResource(R.string.island_corner), "${cfg.cornerPercent}%", cfg.cornerPercent, 0..100) { v -> set { it.copy(cornerPercent = v) } }
                SliderRow(stringResource(R.string.island_opacity), "${cfg.opacityPercent}%", cfg.opacityPercent, 20..100) { v -> set { it.copy(opacityPercent = v) } }
                ChipsRow(
                    title = stringResource(R.string.island_accent),
                    options = listOf(
                        IslandAccent.AUTO to stringResource(R.string.island_accent_auto),
                        IslandAccent.WHITE to stringResource(R.string.island_accent_white),
                        IslandAccent.PURPLE to stringResource(R.string.island_accent_purple),
                        IslandAccent.BLUE to stringResource(R.string.island_accent_blue),
                        IslandAccent.GREEN to stringResource(R.string.island_accent_green),
                        IslandAccent.ORANGE to stringResource(R.string.island_accent_orange),
                        IslandAccent.PINK to stringResource(R.string.island_accent_pink),
                    ),
                    selected = cfg.accent, onSelect = { v -> set { it.copy(accent = v) } },
                )
                SliderRow(
                    stringResource(R.string.island_border),
                    if (cfg.borderWidthDp == 0) stringResource(R.string.island_auto) else "${cfg.borderWidthDp} dp",
                    cfg.borderWidthDp, 0..6,
                ) { v -> set { it.copy(borderWidthDp = v) } }
                ChipsRow(
                    title = stringResource(R.string.island_cover_shape),
                    options = listOf(
                        IslandShape.ROUNDED_SQUARE to stringResource(R.string.island_shape_rounded),
                        IslandShape.CIRCLE to stringResource(R.string.island_shape_circle),
                        IslandShape.SQUARE to stringResource(R.string.island_shape_square),
                    ),
                    selected = cfg.coverShape, onSelect = { v -> set { it.copy(coverShape = v) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.island_glow), subtitle = stringResource(R.string.island_glow_desc),
                    checked = cfg.glow, onCheckedChange = { v -> set { it.copy(glow = v) } }, divider = cfg.glow,
                )
                if (cfg.glow) {
                    SliderRow(
                        stringResource(R.string.island_glow_intensity), "${cfg.glowIntensityPercent}%",
                        cfg.glowIntensityPercent, 0..100, divider = false,
                    ) { v -> set { it.copy(glowIntensityPercent = v) } }
                }
            }
        }
        item(key = "island_capsule") {
            SettingsGroup(header = stringResource(R.string.island_compact)) {
                SliderRow(stringResource(R.string.island_width), "${cfg.capsuleWidthDp} dp", cfg.capsuleWidthDp, 80..320) { v -> set { it.copy(capsuleWidthDp = v) } }
                SliderRow(stringResource(R.string.island_height), "${cfg.capsuleHeightDp} dp", cfg.capsuleHeightDp, 24..64) { v -> set { it.copy(capsuleHeightDp = v) } }
                ChipsRow(
                    title = stringResource(R.string.island_content),
                    options = listOf(
                        IslandContentMode.COVER_AND_EQ to stringResource(R.string.island_content_both),
                        IslandContentMode.COVER_ONLY to stringResource(R.string.island_content_cover),
                        IslandContentMode.EQ_ONLY to stringResource(R.string.island_content_eq),
                    ),
                    selected = cfg.content, onSelect = { v -> set { it.copy(content = v) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.island_fit_lyrics),
                    checked = cfg.fitLyricsToCamera, onCheckedChange = { v -> set { it.copy(fitLyricsToCamera = v) } },
                )
                SliderRow(stringResource(R.string.island_camera_gap), "${cfg.cameraGapDp} dp", cfg.cameraGapDp, 12..120) { v -> set { it.copy(cameraGapDp = v) } }
                ChipsRow(
                    title = stringResource(R.string.island_eq_style),
                    options = listOf(
                        IslandEqStyle.BARS to stringResource(R.string.island_eq_bars),
                        IslandEqStyle.WAVE to stringResource(R.string.island_eq_wave),
                        IslandEqStyle.DOTS to stringResource(R.string.island_eq_dots),
                    ),
                    selected = cfg.eqStyle, onSelect = { v -> set { it.copy(eqStyle = v) } }, divider = false,
                )
            }
        }
        item(key = "island_card") {
            SettingsGroup(header = stringResource(R.string.island_expanded_card)) {
                SliderRow(stringResource(R.string.island_card_width), "${cfg.cardWidthDp} dp", cfg.cardWidthDp, 240..420) { v -> set { it.copy(cardWidthDp = v) } }
                SettingsSwitchRow(
                    title = stringResource(R.string.island_card_same_corner),
                    checked = cfg.cardSameCorner, onCheckedChange = { v -> set { it.copy(cardSameCorner = v) } },
                )
                SliderRow(stringResource(R.string.island_cover_size), "${cfg.coverSizePercent}%", cfg.coverSizePercent, 60..140) { v -> set { it.copy(coverSizePercent = v) } }
                SliderRow(stringResource(R.string.island_text_size), "${cfg.textSizePercent}%", cfg.textSizePercent, 70..140) { v -> set { it.copy(textSizePercent = v) } }
                SettingsSwitchRow(title = stringResource(R.string.island_show_artist), checked = cfg.showArtist, onCheckedChange = { v -> set { it.copy(showArtist = v) } })
                SettingsSwitchRow(
                    title = stringResource(R.string.island_show_progress), subtitle = stringResource(R.string.island_show_progress_desc),
                    checked = cfg.showProgress, onCheckedChange = { v -> set { it.copy(showProgress = v) } },
                )
                SettingsSwitchRow(title = stringResource(R.string.island_show_lyrics), checked = cfg.showLyrics, onCheckedChange = { v -> set { it.copy(showLyrics = v) } })
                SettingsSwitchRow(title = stringResource(R.string.island_show_buttons), checked = cfg.showButtons, onCheckedChange = { v -> set { it.copy(showButtons = v) } })
                ChipsRow(
                    title = stringResource(R.string.island_button_style),
                    options = listOf(
                        IslandButtonStyle.FILLED to stringResource(R.string.island_btn_filled),
                        IslandButtonStyle.OUTLINE to stringResource(R.string.island_btn_outline),
                    ),
                    selected = cfg.buttonStyle, onSelect = { v -> set { it.copy(buttonStyle = v) } }, divider = false,
                )
            }
        }
        item(key = "island_behavior") {
            SettingsGroup(header = stringResource(R.string.island_behavior)) {
                SliderRow(
                    stringResource(R.string.island_auto_collapse),
                    stringResource(R.string.island_seconds, cfg.autoCollapseSec), cfg.autoCollapseSec, 1..15,
                ) { v -> set { it.copy(autoCollapseSec = v) } }
                ChipsRow(
                    title = stringResource(R.string.island_tap),
                    options = listOf(
                        IslandTapAction.EXPAND to stringResource(R.string.island_tap_expand),
                        IslandTapAction.OPEN_APP to stringResource(R.string.island_tap_open),
                        IslandTapAction.PLAY_PAUSE to stringResource(R.string.island_tap_playpause),
                        IslandTapAction.NONE to stringResource(R.string.island_tap_none),
                    ),
                    selected = cfg.tapAction, onSelect = { v -> set { it.copy(tapAction = v) } },
                )
                SettingsNavRow(
                    title = stringResource(R.string.island_long_press),
                    subtitle = stringResource(R.string.island_long_press_value),
                    onClick = {},
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.island_haptics),
                    checked = cfg.vibrate, onCheckedChange = { v -> set { it.copy(vibrate = v) } }, divider = false,
                )
            }
        }
        item(key = "island_position") {
            SettingsGroup(header = stringResource(R.string.island_position_size)) {
                SliderRow(
                    stringResource(R.string.island_offset_x),
                    "${cfg.offsetXDp} dp", cfg.offsetXDp, -200..200,
                ) { v -> set { it.copy(offsetXDp = v) } }
                SliderRow(
                    stringResource(R.string.island_top_gap),
                    "${cfg.topGapDp} dp", cfg.topGapDp.coerceIn(-40, 100), -40..100,
                ) { v -> set { it.copy(topGapDp = v) } }
                SliderRow(
                    stringResource(R.string.island_scale),
                    "${cfg.scalePercent}%", cfg.scalePercent, 70..140,
                ) { v -> set { it.copy(scalePercent = v) } }
                SettingsNavRow(
                    title = stringResource(R.string.island_fit_camera),
                    subtitle = stringResource(R.string.island_fit_camera_desc),
                    onClick = { fitToCamera() },
                )
                SettingsNavRow(
                    title = stringResource(R.string.island_reset_position),
                    subtitle = stringResource(R.string.island_reset_position_desc),
                    onClick = { set { it.copy(offsetXDp = 0, topGapDp = 6) } },
                    divider = false,
                )
            }
        }
        item(key = "island_gestures") {
            SettingsGroup(header = stringResource(R.string.island_gestures)) {
                SettingsControlRow {
                    Text(stringResource(R.string.island_gesture_capsule), color = MaterialTheme.colorScheme.onSurface)
                }
                SettingsControlRow(divider = false) {
                    Text(stringResource(R.string.island_gesture_card), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        item(key = "island_reset_all") {
            SettingsGroup {
                SettingsNavRow(
                    title = stringResource(R.string.island_reset_all),
                    subtitle = stringResource(R.string.island_reset_all_desc),
                    onClick = { IslandSettings.resetAll() },
                    divider = false,
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    divider: Boolean = true,
    onChange: (Int) -> Unit,
) {
    SettingsSliderRow(
        title = title,
        value = value.toFloat().coerceIn(range.first.toFloat(), range.last.toFloat()),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        valueLabel = valueLabel,
        divider = divider,
    )
}

@Composable
private fun <T> ChipsRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    title: String? = null,
    divider: Boolean = true,
) {
    SettingsControlRow(divider = divider) {
        Column(Modifier.fillMaxWidth()) {
            if (title != null) {
                Text(title, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, label) ->
                    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
                }
            }
        }
    }
}
