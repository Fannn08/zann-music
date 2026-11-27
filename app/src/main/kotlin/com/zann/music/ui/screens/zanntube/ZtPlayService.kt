package com.zann.music.ui.screens.zanntube

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.zann.music.core.Http
import okhttp3.Request

/** Menjaga proses tetap hidup selama video diputar di latar belakang dan menampilkan kontrol media. */
class ZtPlayService : Service() {
    companion object {
        @Volatile var inst: ZtPlayService? = null
        const val CH = "zanntube_play"
        const val NID = 4721
        const val A_TOGGLE = "zt.toggle"
        const val A_BACK = "zt.back"
        const val A_FWD = "zt.fwd"
    }

    private val main = Handler(Looper.getMainLooper())
    private var session: MediaSession? = null
    @Volatile private var art: Bitmap? = null
    @Volatile private var artId = ""
    private var foreground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        inst = this
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "ZannTube", NotificationManager.IMPORTANCE_LOW))
        val s = MediaSession(this, "ZannTube")
        s.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { ZtBg.play() }
            override fun onPause() { ZtBg.pause() }
            override fun onSeekTo(pos: Long) { ZtBg.seekTo(pos) }
            override fun onSkipToNext() { ZtBg.seekBy(10) }
            override fun onSkipToPrevious() { ZtBg.seekBy(-10) }
            override fun onFastForward() { ZtBg.seekBy(10) }
            override fun onRewind() { ZtBg.seekBy(-10) }
        })
        s.isActive = true
        session = s
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            A_TOGGLE -> if (ZtBg.playing) ZtBg.pause() else ZtBg.play()
            A_BACK -> ZtBg.seekBy(-10)
            A_FWD -> ZtBg.seekBy(10)
        }
        render()
        return START_NOT_STICKY
    }

    private fun pi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code, Intent(this, ZtPlayService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun act(res: Int, label: String, action: String, code: Int): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(this, res), label, pi(action, code)).build()

    private fun loadArt(id: String) {
        if (id == artId) return
        artId = id
        art = null
        Thread {
            try {
                val req = Request.Builder().url("https://i.ytimg.com/vi/" + id + "/hqdefault.jpg").build()
                Http.base.newCall(req).execute().use { r ->
                    val ins = if (r.isSuccessful) r.body?.byteStream() else null
                    val bmp = if (ins != null) BitmapFactory.decodeStream(ins) else null
                    if (bmp != null && artId == id) {
                        art = bmp
                        main.post { render() }
                    }
                }
            } catch (e: Exception) {
                // tanpa cover, notifikasi tetap jalan
            }
        }.start()
    }

    fun render() {
        val sess = session ?: return
        if (ZtBg.id.isEmpty()) {
            shutdown()
            return
        }
        loadArt(ZtBg.id)
        val playing = ZtBg.playing
        val cover = art

        val md = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, ZtBg.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, ZtBg.channel)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, ZtBg.durSec * 1000L)
        if (cover != null) md.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover)
        sess.setMetadata(md.build())

        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_FAST_FORWARD or PlaybackState.ACTION_REWIND
        val st = PlaybackState.Builder()
            .setActions(actions)
            .setState(
                if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                ZtBg.curPos() * 1000L,
                if (playing) ZtBg.rate else 0f,
            )
            .build()
        sess.setPlaybackState(st)

        val b = Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (ZtBg.title.isBlank()) "ZannTube" else ZtBg.title)
            .setContentText(ZtBg.channel)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(act(android.R.drawable.ic_media_rew, "Mundur 10 dtk", A_BACK, 1))
            .addAction(
                if (playing) act(android.R.drawable.ic_media_pause, "Jeda", A_TOGGLE, 2)
                else act(android.R.drawable.ic_media_play, "Putar", A_TOGGLE, 2),
            )
            .addAction(act(android.R.drawable.ic_media_ff, "Maju 10 dtk", A_FWD, 3))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(sess.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
        if (cover != null) b.setLargeIcon(Icon.createWithBitmap(cover))
        val open = packageManager.getLaunchIntentForPackage(packageName)
        if (open != null) {
            b.setContentIntent(
                PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
        val n = b.build()
        try {
            if (!foreground) {
                startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                foreground = true
            } else {
                getSystemService(NotificationManager::class.java).notify(NID, n)
            }
        } catch (e: Exception) {
            // izin notifikasi ditolak atau sistem menolak layanan latar depan
        }
    }

    fun shutdown() {
        if (inst === this) inst = null
        if (!foreground) {
            try {
                val n = Notification.Builder(this, CH).setSmallIcon(android.R.drawable.ic_media_play).build()
                startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                foreground = true
            } catch (e: Exception) { }
        }
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (e: Exception) { }
        foreground = false
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        ZtBg.hardStop()
        shutdown()
    }

    override fun onDestroy() {
        if (inst === this) inst = null
        session?.release()
        session = null
        super.onDestroy()
    }
}
