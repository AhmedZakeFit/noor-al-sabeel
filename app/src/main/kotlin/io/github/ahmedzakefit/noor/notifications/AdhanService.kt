package io.github.ahmedzakefit.noor.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.ahmedzakefit.noor.core.model.Prayer

/**
 * Foreground service (mediaPlayback type) that recites the full adhan at prayer
 * time. Started by [PrayerAlarmReceiver] — an exact AlarmManager alarm, which
 * Android exempts from background foreground-service start restrictions — or by
 * the "test adhan" button in settings.
 *
 * The audio plays on the alarm stream (USAGE_ALARM), so it is independent of
 * the notification volume and is audible in "alarms only" Do-Not-Disturb. A
 * partial wake lock is held through MediaPlayer.setWakeMode so playback
 * survives Doze with the screen off. The user can stop the adhan from the
 * notification (visible on the lock screen) at any time.
 *
 * If the system refuses the foreground-service start (vendor-specific killer
 * or restricted alarms), [start] returns false and the caller falls back to a
 * regular notification, so a prayer time is never silently missed.
 */
class AdhanService : Service() {

    companion object {
        const val ACTION_START = "io.github.ahmedzakefit.noor.adhan.START"
        const val ACTION_STOP = "io.github.ahmedzakefit.noor.adhan.STOP"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_PREVIEW = "preview"
        const val EXTRA_TIME_LABEL = "time_label"
        const val EXTRA_LOCATION_LABEL = "location_label"
        const val EXTRA_VOICE = "voice"

        /** Offset so adhan notifications never collide with regular ones (0..5 / 100+). */
        private const val NOTIF_ID_OFFSET = 500
        private const val WATCHDOG_MS = 6 * 60_000L
        private const val TAG = "AdhanService"

        fun notificationId(prayer: Prayer): Int = NOTIF_ID_OFFSET + prayer.ordinal

        /**
         * Attempts to start the adhan for [prayer]. Returns false when the
         * system currently forbids starting a foreground service — the caller
         * should then show a regular notification instead.
         */
        fun start(
            context: Context,
            prayer: Prayer,
            preview: Boolean,
            timeLabel: String? = null,
            locationLabel: String? = null,
            voice: io.github.ahmedzakefit.noor.data.AdhanVoice = io.github.ahmedzakefit.noor.data.AdhanVoice.MAKKAH,
        ): Boolean = try {
            val intent = Intent(context, AdhanService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PRAYER, prayer.name)
                putExtra(EXTRA_PREVIEW, preview)
                putExtra(EXTRA_VOICE, voice.name)
                if (timeLabel != null) putExtra(EXTRA_TIME_LABEL, timeLabel)
                if (locationLabel != null) putExtra(EXTRA_LOCATION_LABEL, locationLabel)
            }
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (_: Exception) {
            // ForegroundServiceStartNotAllowedException (API 31+) or an app in a
            // background state that cannot start services — fall back silently.
            false
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var prayer: Prayer = Prayer.DHUHR
    private var voice: io.github.ahmedzakefit.noor.data.AdhanVoice = io.github.ahmedzakefit.noor.data.AdhanVoice.MAKKAH
    private var preview = false
    private var timeLabel: String? = null
    private var locationLabel: String? = null
    private var pausedByFocus = false
    private var finalized = false
    private val watchdog = Runnable { finalize(stopService = true, postFinal = true) }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // START_NOT_STICKY never redelivers, but never risk a spurious adhan.
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_STOP -> {
                finalize(stopService = true, postFinal = false)
                return START_NOT_STICKY
            }
            else -> {
                intent.getStringExtra(EXTRA_PRAYER)?.let { name ->
                    runCatching { Prayer.valueOf(name) }.getOrNull()?.let { prayer = it }
                }
                preview = intent.getBooleanExtra(EXTRA_PREVIEW, preview)
                timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL) ?: timeLabel
                locationLabel = intent.getStringExtra(EXTRA_LOCATION_LABEL) ?: locationLabel
                intent.getStringExtra(EXTRA_VOICE)?.let {
                    runCatching { io.github.ahmedzakefit.noor.data.AdhanVoice.valueOf(it) }.getOrNull()?.let { voice = it }
                }
                startAdhan()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAdhan() {
        AdhanPlayback.setPlaying(true, voice = voice, forFajr = (prayer == Prayer.FAJR))
        // 1) Enter the foreground immediately (5-second window) with a silent
        //    notification; the MediaPlayer below is the actual sound.
        val resId = AdhanAudio.rawResourceFor(this, voice, prayer)
        ServiceCompat.startForeground(
            this,
            notificationId(prayer),
            Notifier.adhanNotification(this, prayer, preview, timeLabel, locationLabel, playing = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )

        // 2) Build the player. Fails (e.g. resource missing) -> graceful finish.
        releasePlayer()
        val mp = try {
            createPlayer(resId)
        } catch (t: Throwable) {
            Log.w(TAG, "adhan player init failed", t)
            finalize(stopService = true, postFinal = true)
            return
        }
        player = mp

        mp.setOnPreparedListener { p ->
            if (finalized) return@setOnPreparedListener
            requestAdhanFocus()
            p.start()
            // Watchdog = file duration + slack, so a vendor ROM that swallows
            // the completion callback still ends the service on time.
            handler.removeCallbacks(watchdog)
            val durationMs = runCatching { p.duration.toLong() }.getOrDefault(0L)
            handler.postDelayed(watchdog, durationMs.coerceAtLeast(60_000L) + 60_000L)
        }
        mp.setOnCompletionListener {
            finalize(stopService = true, postFinal = true)
        }
        mp.setOnErrorListener { _, what, extra ->
            Log.w(TAG, "adhan player error what=$what extra=$extra")
            finalize(stopService = true, postFinal = true)
            true
        }

        // 3) Interim watchdog until the real duration is known at prepare time.
        handler.postDelayed(watchdog, WATCHDOG_MS)
        mp.prepareAsync()
    }

    private fun createPlayer(resId: Int): MediaPlayer {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val mp = MediaPlayer()
        mp.setAudioAttributes(attrs)
        // Holds a PARTIAL_WAKE_LOCK on the app's behalf for the whole playback,
        // so the adhan continues while the device dozes with the screen off.
        mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
        mp.isLooping = false
        resources.openRawResourceFd(resId)?.use { afd ->
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        } ?: throw IllegalStateException("raw resource $resId unavailable")
        return mp
    }

    /** Requests focus on the alarm stream; pauses during calls, resumes after. */
    private fun requestAdhanFocus() {
        val am = audioManager ?: return
        if (focusListener != null) return
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS -> finalize(stopService = true, postFinal = true)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    pausedByFocus = true
                    runCatching { player?.takeIf { it.isPlaying }?.pause() }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (pausedByFocus && !finalized) {
                        pausedByFocus = false
                        runCatching { player?.start() }
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Unit // adhan keeps playing
                else -> Unit
            }
        }
        focusListener = listener
        am.requestAudioFocus(listener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        focusListener?.let { am.abandonAudioFocus(it) }
        focusListener = null
    }

    /**
     * Single exit path. Stops playback, drops the foreground notification and —
     * unless the user pressed stop — leaves a short auto-cancel reminder that
     * the prayer time has entered.
     */
    private fun finalize(stopService: Boolean, postFinal: Boolean) {
        if (finalized) return
        finalized = true
        AdhanPlayback.setPlaying(false)
        handler.removeCallbacks(watchdog)
        abandonFocus()
        releasePlayer()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (postFinal && !preview) {
            Notifier.showAdhanFinishedNotification(this, prayer, timeLabel, locationLabel)
        }
        if (stopService) stopSelf()
    }

    private fun releasePlayer() {
        player?.let { p ->
            runCatching {
                if (p.isPlaying) p.stop()
            }
            runCatching { p.release() }
        }
        player = null
    }

    override fun onDestroy() {
        AdhanPlayback.setPlaying(false)
        handler.removeCallbacksAndMessages(null)
        abandonFocus()
        releasePlayer()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startId: Int) {
        // Legacy entry for Android < 5 starts; forward to onStartCommand.
        onStartCommand(intent, 0, startId)
    }
}
