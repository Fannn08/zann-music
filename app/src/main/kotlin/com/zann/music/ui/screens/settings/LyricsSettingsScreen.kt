package com.zann.music.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.zann.music.R
import com.zann.music.data.settings.LyricsProviderPreference
import com.zann.music.data.settings.LyricsTranslateLang
import com.zann.music.data.settings.SettingsRepository

private data class LyricsProviderChoice(
    val provider: LyricsProviderPreference,
    val title: Int,
)

/** Localized label shared by the settings root summary and provider picker. */
fun lyricsProviderTitleRes(provider: LyricsProviderPreference): Int = when (provider) {
    LyricsProviderPreference.AUTO -> R.string.lyrics_provider_auto
    LyricsProviderPreference.LRC_RED -> R.string.lyrics_provider_lrc_red
    LyricsProviderPreference.BETTER_LYRICS -> R.string.lyrics_provider_better
    LyricsProviderPreference.BETTER_LYRICS_PORTATO -> R.string.lyrics_provider_portato
    LyricsProviderPreference.LYRICS_PLUS -> R.string.lyrics_provider_plus
    LyricsProviderPreference.UNISON -> R.string.lyrics_provider_unison
    LyricsProviderPreference.KUGOU -> R.string.lyrics_provider_kugou
    LyricsProviderPreference.LRCLIB -> R.string.lyrics_provider_lrclib
}

@Composable
fun LyricsSettingsScreen(onBack: () -> Unit) {
    val settings by SettingsRepository.settings.collectAsState()
    val choices = listOf(
        LyricsProviderChoice(LyricsProviderPreference.AUTO, R.string.lyrics_provider_auto),
        LyricsProviderChoice(LyricsProviderPreference.LRC_RED, R.string.lyrics_provider_lrc_red),
        LyricsProviderChoice(LyricsProviderPreference.BETTER_LYRICS, R.string.lyrics_provider_better),
        LyricsProviderChoice(LyricsProviderPreference.BETTER_LYRICS_PORTATO, R.string.lyrics_provider_portato),
        LyricsProviderChoice(LyricsProviderPreference.LYRICS_PLUS, R.string.lyrics_provider_plus),
        LyricsProviderChoice(LyricsProviderPreference.UNISON, R.string.lyrics_provider_unison),
        LyricsProviderChoice(LyricsProviderPreference.KUGOU, R.string.lyrics_provider_kugou),
        LyricsProviderChoice(LyricsProviderPreference.LRCLIB, R.string.lyrics_provider_lrclib),
    )

    SettingsPage(title = stringResource(R.string.lyrics), onBack = onBack) {
        item(key = "lyrics_provider") {
            SettingsGroup(
                header = stringResource(R.string.lyrics_provider_heading),
                footer = stringResource(R.string.lyrics_provider_footer),
            ) {
                choices.forEachIndexed { index, choice ->
                    SettingsChoiceRow(
                        title = stringResource(choice.title),
                        selected = settings.lyricsProvider == choice.provider,
                        divider = index < choices.lastIndex,
                        onClick = {
                            if (settings.lyricsProvider != choice.provider) {
                                SettingsRepository.update { it.copy(lyricsProvider = choice.provider) }
                            }
                        },
                    )
                }
            }
        }
        item(key = "lyrics_translate") {
            SettingsGroup(
                header = stringResource(R.string.lyrics_translate_heading),
                footer = stringResource(R.string.lyrics_translate_footer),
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.lyrics_translate_toggle),
                    subtitle = stringResource(R.string.lyrics_translate_toggle_desc),
                    checked = settings.lyricsTranslate,
                    onCheckedChange = { on -> SettingsRepository.update { it.copy(lyricsTranslate = on) } },
                    icon = Icons.Default.Translate,
                    iconTint = com.zann.music.ui.theme.SystemColors.Blue,
                    divider = true,
                )
                val langs = LyricsTranslateLang.entries
                langs.forEachIndexed { index, lang ->
                    SettingsChoiceRow(
                        title = lang.nativeName,
                        selected = settings.lyricsTranslateLang == lang,
                        divider = index < langs.lastIndex,
                        onClick = {
                            if (settings.lyricsTranslateLang != lang) {
                                SettingsRepository.update { it.copy(lyricsTranslateLang = lang) }
                            }
                        },
                    )
                }
            }
        }
        item(key = "player_lyrics") {
            SettingsGroup(header = stringResource(R.string.lyrics_player_section)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.lyrics_player_strip),
                    subtitle = stringResource(R.string.lyrics_player_strip_desc),
                    checked = settings.showPlayerLyricsStrip,
                    onCheckedChange = { visible ->
                        SettingsRepository.update { it.copy(showPlayerLyricsStrip = visible) }
                    },
                    icon = Icons.Default.Lyrics,
                    iconTint = com.zann.music.ui.theme.SystemColors.Purple,
                    divider = false,
                )
            }
        }
    }
}
