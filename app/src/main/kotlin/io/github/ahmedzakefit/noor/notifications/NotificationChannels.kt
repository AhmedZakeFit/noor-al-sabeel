package io.github.ahmedzakefit.noor.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.github.ahmedzakefit.noor.data.AppSettings

/**
 * Notification channels. Sounds/vibration are channel-level on Android 8+, so
 * settings changes re-apply them here (channels can be updated after creation).
 */
object NotificationChannels {

    const val CHANNEL_PRAYER = "prayer_times"
    const val CHANNEL_REMINDER = "pre_prayer_reminders"

    /** Silent channel for adhan alerts: the MediaPlayer stream is the sound. */
    const val CHANNEL_ADHAN = "adhan"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_PRAYER) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PRAYER,
                    context.getString(io.github.ahmedzakefit.noor.R.string.notif_channel_prayer),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(io.github.ahmedzakefit.noor.R.string.notif_channel_prayer_desc)
                },
            )
        }
        if (nm.getNotificationChannel(CHANNEL_REMINDER) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDER,
                    context.getString(io.github.ahmedzakefit.noor.R.string.notif_channel_reminder),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(io.github.ahmedzakefit.noor.R.string.notif_channel_reminder_desc)
                },
            )
        }
        if (nm.getNotificationChannel(CHANNEL_ADHAN) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ADHAN,
                    context.getString(io.github.ahmedzakefit.noor.R.string.notif_channel_adhan),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(io.github.ahmedzakefit.noor.R.string.notif_channel_adhan_desc)
                    // No channel sound: the adhan audio itself plays through
                    // MediaPlayer on the alarm stream. A short initial buzz
                    // makes the heads-up (with its Stop button) noticeable even
                    // at zero alarm volume.
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250)
                },
            )
        }
    }

    /** Applies the user's sound/vibration choices to the channels (API 26+). */
    fun applySettings(context: Context, settings: AppSettings) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        for (id in listOf(CHANNEL_PRAYER, CHANNEL_REMINDER)) {
            val channel = nm.getNotificationChannel(id) ?: continue
            if (settings.soundEnabled) {
                val uri = settings.soundUri?.let { android.net.Uri.parse(it) }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                channel.setSound(uri, attrs)
            } else {
                channel.setSound(null, null)
            }
            channel.enableVibration(settings.vibrate)
            if (settings.vibrate) {
                channel.vibrationPattern = longArrayOf(0, 400, 200, 400)
            } else {
                channel.vibrationPattern = null
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
