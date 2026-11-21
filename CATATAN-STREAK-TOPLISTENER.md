# Streak & Top Listener (ZANN MUSIC)

Tidak perlu server: semua data disimpan lokal di HP (SharedPreferences `zann_listening_stats`).

## Cara pasang
Salin isi zip ini ke project (timpa file dengan path yang sama), lalu build.

File baru:
- data/stats/ListeningStats.kt     pencatat waktu dengar per hari/lagu
- data/stats/StreakCalculator.kt   hitungan streak (ada unit test)
- ui/screens/stats/StatsComponents.kt, StreakScreen.kt, TopListenerScreen.kt
- res/values*/stats_strings.xml    teks EN + ID
- test/.../StreakCalculatorTest.kt

File yang diubah (hanya beberapa baris):
- PlayerManager.kt   4 hook ke ListeningStats
- VyoraAppRoot.kt    route settings/streak dan settings/top_listener
- SettingsScreen.kt  grup "Statistikmu" berisi Streak + Top Listener

## Aturan
- Hari dihitung aktif jika dengar >= 1 menit (ListeningStats.STREAK_GOAL_MS).
- 1 "putaran" lagu dihitung setelah didengar 30 detik.
- Streak tetap hidup sampai hari ini berakhir.

## Yang perlu dicek saat build
Source yang dikirim tidak lengkap (tanpa build.gradle, theme, SimpleItem), jadi belum bisa saya compile:
1. SongStat.toSimpleItem() di ListeningStats.kt: kalau SimpleItem butuh `type = ItemType.SONG`, tambahkan di situ.
2. Memakai java.time: butuh minSdk 26+ atau core library desugaring.
3. Ikon LocalFireDepartment dan Leaderboard butuh material-icons-extended (sudah dipakai app-mu).
4. "Tampilkan saya di papan peringkat" (leaderboard antar pengguna) butuh server, tidak termasuk.
