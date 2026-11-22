package com.zann.music.data.island

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class IslandPreset { IPHONE, GLASS, COLORFUL, MINIMAL }
enum class IslandContentMode { COVER_AND_EQ, COVER_ONLY, EQ_ONLY }
enum class IslandEqStyle { BARS, WAVE, DOTS }
enum class IslandTapAction { EXPAND, OPEN_APP, PLAY_PAUSE, NONE }
enum class IslandButtonStyle { FILLED, OUTLINE }
enum class IslandShape { ROUNDED_SQUARE, CIRCLE, SQUARE }
enum class IslandAccent(val argb: Long) {
    AUTO(0xFFB57BFF), WHITE(0xFFFFFFFF), PURPLE(0xFFB57BFF), BLUE(0xFF5AA9FF),
    GREEN(0xFF4CD98A), ORANGE(0xFFFF9F43), PINK(0xFFFF6FB1)
}

/** Semua opsi Dynamic Island. Nilai bawaan = tampilan preset "iPhone". */
data class IslandConfig(
    val enabled: Boolean = false,
    val preset: IslandPreset = IslandPreset.IPHONE,
    // Tampilan
    val cornerPercent: Int = 100,
    val opacityPercent: Int = 100,
    val glow: Boolean = true,
    val glowIntensityPercent: Int = 60,
    val borderWidthDp: Int = 0,            // 0 = otomatis
    val coverShape: IslandShape = IslandShape.ROUNDED_SQUARE,
    val accent: IslandAccent = IslandAccent.AUTO,
    // Kapsul ringkas
    val capsuleWidthDp: Int = 190,
    val capsuleHeightDp: Int = 38,
    val content: IslandContentMode = IslandContentMode.COVER_AND_EQ,
    val showCapsuleLyrics: Boolean = true,   // lirik berjalan di tengah kapsul
    val fitLyricsToCamera: Boolean = false,
    val cameraGapDp: Int = 36,
    val eqStyle: IslandEqStyle = IslandEqStyle.BARS,
    // Kartu diperluas
    val cardWidthDp: Int = 360,
    val cardSameCorner: Boolean = true,
    val coverSizePercent: Int = 100,
    val textSizePercent: Int = 100,
    val showArtist: Boolean = true,
    val showProgress: Boolean = true,
    val showLyrics: Boolean = true,
    val showButtons: Boolean = true,
    val buttonStyle: IslandButtonStyle = IslandButtonStyle.FILLED,
    // Perilaku
    val autoCollapseSec: Int = 12,         // 0 = tidak pernah menciut sendiri
    val tapAction: IslandTapAction = IslandTapAction.EXPAND,
    val vibrate: Boolean = true,
    // Posisi dan ukuran
    val offsetXDp: Int = 0,                // geser halus ke samping
    val topGapDp: Int = 6,                 // jarak dari bilah status
    val scalePercent: Int = 100,
) {
    companion object {
        fun forPreset(p: IslandPreset, keep: IslandConfig): IslandConfig = when (p) {
            IslandPreset.IPHONE -> IslandConfig(enabled = keep.enabled, preset = p)
            IslandPreset.GLASS -> keep.copy(preset = p, opacityPercent = 70, glow = true, glowIntensityPercent = 40, borderWidthDp = 1)
            IslandPreset.COLORFUL -> keep.copy(preset = p, opacityPercent = 100, glow = true, glowIntensityPercent = 90, borderWidthDp = 0)
            IslandPreset.MINIMAL -> keep.copy(
                preset = p, glow = false, opacityPercent = 100, content = IslandContentMode.COVER_ONLY,
                capsuleWidthDp = 120, showLyrics = false, showCapsuleLyrics = false,
            )
        }
    }
}

/**
 * Penyimpanan lokal (SharedPreferences "zann_island") + StateFlow supaya layar pengaturan
 * dan overlay langsung ikut berubah.
 */
object IslandSettings {
    private const val PREFS = "zann_island"
    private var prefs: SharedPreferences? = null
    private val _config = MutableStateFlow(IslandConfig())
    val config: StateFlow<IslandConfig> = _config.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _config.value = read(p)
    }

    @Synchronized
    fun update(block: (IslandConfig) -> IslandConfig) {
        val next = block(_config.value)
        _config.value = next
        prefs?.let { write(it, next) }
    }

    fun resetAll() = update { IslandConfig(enabled = it.enabled) }

    private inline fun <reified E : Enum<E>> SharedPreferences.enum(key: String, def: E): E =
        runCatching { enumValueOf<E>(getString(key, def.name) ?: def.name) }.getOrDefault(def)

    private fun read(p: SharedPreferences): IslandConfig {
        val d = IslandConfig()
        return IslandConfig(
            enabled = p.getBoolean("enabled", d.enabled),
            preset = p.enum("preset", d.preset),
            cornerPercent = p.getInt("corner", d.cornerPercent),
            opacityPercent = p.getInt("opacity", d.opacityPercent),
            glow = p.getBoolean("glow", d.glow),
            glowIntensityPercent = p.getInt("glowIntensity", d.glowIntensityPercent),
            borderWidthDp = p.getInt("borderWidth", d.borderWidthDp),
            coverShape = p.enum("coverShape", d.coverShape),
            accent = p.enum("accent", d.accent),
            capsuleWidthDp = p.getInt("capW", d.capsuleWidthDp),
            capsuleHeightDp = p.getInt("capH", d.capsuleHeightDp),
            content = p.enum("content", d.content),
            showCapsuleLyrics = p.getBoolean("capLyrics", d.showCapsuleLyrics),
            fitLyricsToCamera = p.getBoolean("fitLyrics", d.fitLyricsToCamera),
            cameraGapDp = p.getInt("cameraGap", d.cameraGapDp),
            eqStyle = p.enum("eqStyle", d.eqStyle),
            cardWidthDp = p.getInt("cardW", d.cardWidthDp),
            cardSameCorner = p.getBoolean("cardSameCorner", d.cardSameCorner),
            coverSizePercent = p.getInt("coverSize", d.coverSizePercent),
            textSizePercent = p.getInt("textSize", d.textSizePercent),
            showArtist = p.getBoolean("showArtist", d.showArtist),
            showProgress = p.getBoolean("showProgress", d.showProgress),
            showLyrics = p.getBoolean("showLyrics", d.showLyrics),
            showButtons = p.getBoolean("showButtons", d.showButtons),
            buttonStyle = p.enum("buttonStyle", d.buttonStyle),
            autoCollapseSec = p.getInt("autoCollapse", d.autoCollapseSec).let { v -> if (p.getInt("ver", 1) < 2 && v == 4) d.autoCollapseSec else v },
            tapAction = p.enum("tapAction", d.tapAction),
            vibrate = p.getBoolean("vibrate", d.vibrate),
            offsetXDp = p.getInt("offsetX", d.offsetXDp),
            topGapDp = p.getInt("topGap", d.topGapDp),
            scalePercent = p.getInt("scale", d.scalePercent),
        )
    }

    private fun write(p: SharedPreferences, c: IslandConfig) {
        p.edit()
            .putBoolean("enabled", c.enabled).putString("preset", c.preset.name)
            .putInt("corner", c.cornerPercent).putInt("opacity", c.opacityPercent)
            .putBoolean("glow", c.glow).putInt("glowIntensity", c.glowIntensityPercent)
            .putInt("borderWidth", c.borderWidthDp).putString("coverShape", c.coverShape.name).putString("accent", c.accent.name)
            .putInt("capW", c.capsuleWidthDp).putInt("capH", c.capsuleHeightDp)
            .putString("content", c.content.name).putBoolean("capLyrics", c.showCapsuleLyrics).putBoolean("fitLyrics", c.fitLyricsToCamera)
            .putInt("cameraGap", c.cameraGapDp).putString("eqStyle", c.eqStyle.name)
            .putInt("cardW", c.cardWidthDp).putBoolean("cardSameCorner", c.cardSameCorner)
            .putInt("coverSize", c.coverSizePercent).putInt("textSize", c.textSizePercent)
            .putBoolean("showArtist", c.showArtist).putBoolean("showProgress", c.showProgress)
            .putBoolean("showLyrics", c.showLyrics).putBoolean("showButtons", c.showButtons)
            .putString("buttonStyle", c.buttonStyle.name)
            .putInt("autoCollapse", c.autoCollapseSec).putString("tapAction", c.tapAction.name)
            .putBoolean("vibrate", c.vibrate)
            .putInt("ver", 2)
            .putInt("offsetX", c.offsetXDp).putInt("topGap", c.topGapDp).putInt("scale", c.scalePercent)
            .apply()
    }
}
