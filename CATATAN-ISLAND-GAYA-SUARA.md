# Dynamic Island + Gaya suara (ZANN MUSIC)

## 1. Gaya suara (sheet seperti screenshot)
Di Now Playing, ketuk tombol kecepatan (angka "1x") -> terbuka lembar:
- **Ekualiser**: "Buka Ekualiser Android bawaan" (intent sistem; kalau HP tidak punya, muncul toast).
- **Gaya suara**: dialog 7 pilihan, langsung dipakai ke lagu yang sedang diputar.
- **Lanjutan**: dialog kecepatan dan nada lama (PlaybackSpeedDialog).

Preset (tempo/nada dikalikan dengan pengaturan Lanjutan, jadi bisa dikombinasi):
| Gaya | Tempo | Nada | Efek |
|---|---|---|---|
| Normal | 1.0 | 1.0 | - |
| Sped up | 1.25 | 1.25 | - |
| Slowed + reverb | 0.85 | 0.85 | reverb lembut |
| Nightcore | 1.4 | 1.4 | - |
| Lo-fi | 0.95 | 0.95 | low-pass hangat + saturasi + gema tipis |
| 8D | 1.0 | 1.0 | panning berputar (8 dtk/putaran) + beda waktu antar telinga |
| Konser | 1.0 | 1.0 | reverb aula besar + pre-delay |

File:
- playback/audio/SoundStyle.kt            enum + parameter
- playback/audio/StyleAudioProcessor.kt   reverb (Freeverb stereo), low-pass, 8D
- ui/player/SoundSheet.kt                 sheet + dialog
Diubah: VyoraRenderersFactory (prosesor masuk rantai), PlaybackService (terapkan gaya + tempo/nada efektif),
AppSettings (`soundStyle`, kunci "sound_style"), VyoraFullPlayer (tombol kecepatan membuka SoundSheet,
label kecepatan memakai tempo efektif).
Catatan: efek hanya untuk PCM 16-bit stereo (sama seperti Spatial audio). Kalau output diset FLOAT 32-bit dan
lagunya lossless high-res, efek reverb/8D/lo-fi dilewati; tempo dan nada tetap bekerja.

## 2. Dynamic Island
Pengaturan > Dynamic Island. Overlay lewat layanan aksesibilitas (tanpa izin "tampil di atas aplikasi lain").
File baru: data/island/IslandConfig.kt, ui/island/IslandContent.kt, ui/island/IslandOverlayService.kt,
ui/screens/settings/IslandSettingsScreen.kt, res/xml/island_accessibility_config.xml, res/values*/island_strings.xml.
Diubah: AndroidManifest.xml (service), VyoraAppRoot.kt (route settings/island), SettingsScreen.kt (baris),
PlayerManager.kt (IslandSettings.init).
Lirik memakai LyricsRepository milik app (baris sinkron sesuai posisi lagu).
Android 13+: jika layanan terkunci, buka Info aplikasi > titik tiga > Izinkan pengaturan terbatas.

## Belum diuji
Saya tidak punya Android SDK di sini, jadi belum di-compile dan belum dicoba di HP. Kalau ada error build,
kirim lognya. Hal yang paling mungkin perlu disesuaikan: import ekstensi view-tree (lifecycle/savedstate) di
IslandOverlayService, dan suara reverb/8D perlu didengar langsung untuk diatur takarannya
(konstanta ada di bagian `when (current)` di StyleAudioProcessor.queueInput).
Belum ada: layar "Atur di layar" (seret di tiruan HP); warna aksen "Otomatis" masih ungu tetap.
