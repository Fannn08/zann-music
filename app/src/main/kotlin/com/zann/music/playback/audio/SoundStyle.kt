package com.zann.music.playback.audio

/**
 * "Gaya suara": preset ala versi sped up, slowed + reverb, nightcore, lo-fi, 8D dan konser.
 * [speed] dan [pitch] dikalikan dengan pengaturan Lanjutan milik pengguna, jadi keduanya bisa dipakai bersamaan.
 */
enum class SoundStyle(
    val id: String,
    val speed: Float,
    val pitch: Float,
) {
    NORMAL("normal", 1f, 1f),
    SPED_UP("sped_up", 1.25f, 1.25f),
    SLOWED_REVERB("slowed_reverb", 0.85f, 0.85f),
    NIGHTCORE("nightcore", 1.4f, 1.4f),
    LOFI("lofi", 0.95f, 0.95f),
    EIGHT_D("eight_d", 1f, 1f),
    CONCERT("concert", 1f, 1f);

    companion object {
        fun fromId(id: String?): SoundStyle = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
