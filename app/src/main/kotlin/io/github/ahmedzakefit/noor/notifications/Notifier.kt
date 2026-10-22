package io.github.ahmedzakefit.noor.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import io.github.ahmedzakefit.noor.MainActivity
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.ui.common.Formatters
import java.util.Locale

/**
 * Builds and posts localized notifications. Receivers are not AppCompatActivity,
 * so per-app locales on Android < 13 need an explicit localized context.
 */
object Notifier {

    private fun Context.localized(): Context {
        val current = resources.configuration.locales[0]
        val appLocale: Locale? = try {
            val locales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) null else locales[0]
        } catch (_: Throwable) {
            null
        }
        return if (appLocale != null && appLocale != current) {
            val config = Configuration(resources.configuration)
            config.setLocale(appLocale)
            createConfigurationContext(config)
        } else {
            this
        }
    }

    private fun prayerName(ctx: Context, prayer: Prayer): String = with(ctx.localized()) {
        when (prayer) {
            Prayer.FAJR -> getString(R.string.prayer_fajr)
            Prayer.SUNRISE -> getString(R.string.prayer_sunrise)
            Prayer.DHUHR -> getString(R.string.prayer_dhuhr)
            Prayer.ASR -> getString(R.string.prayer_asr)
            Prayer.MAGHRIB -> getString(R.string.prayer_maghrib)
            Prayer.ISHA -> getString(R.string.prayer_isha)
        }
    }

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun showPrayerNotification(
        context: Context,
        prayer: Prayer,
        timeLabel: String,
        locationLabel: String,
        arabicDigits: Boolean,
    ) {
        val ctx = context.localized()
        val title = ctx.getString(R.string.notif_prayer_title, prayerName(ctx, prayer))
        val body = ctx.getString(R.string.notif_prayer_body, timeLabel, locationLabel)
        val notification = baseBuilder(ctx, NotificationChannels.CHANNEL_PRAYER)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()
        post(context, prayer.ordinal, notification)
    }

    fun showReminderNotification(
        context: Context,
        prayer: Prayer,
        minutesLeft: Int,
        timeLabel: String,
        locationLabel: String,
    ) {
        val ctx = context.localized()
        val title = ctx.getString(R.string.notif_reminder_title, prayerName(ctx, prayer))
        val body = ctx.getString(R.string.notif_reminder_body, minutesLeft, timeLabel, locationLabel)
        val notification = baseBuilder(ctx, NotificationChannels.CHANNEL_REMINDER)
            .setContentTitle(title)
            .setContentText(body)
            .build()
        post(context, 100 + prayer.ordinal, notification)
    }

    fun showTestNotification(context: Context) {
        val ctx = context.localized()
        val notification = baseBuilder(ctx, NotificationChannels.CHANNEL_REMINDER)
            .setContentTitle(ctx.getString(R.string.notif_test_title))
            .setContentText(ctx.getString(R.string.notif_test_body))
            .build()
        post(context, 999, notification)
    }

    private fun baseBuilder(ctx: Context, channel: String): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(ctx))
            .setOnlyAlertOnce(false)
        if (Build.VERSION.SDK_INT < 26) {
            // Channel-less devices: per-notification behaviour.
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
        }
        return builder
    }

    private fun post(context: Context, id: Int, notification: Notification) {
        if (!NotificationChannels.areNotificationsEnabled(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked while the app ran; nothing to do.
        }
    }

    fun cancelPrayerNotifications(context: Context) {
        NotificationManagerCompat.from(context).apply {
            for (prayer in Prayer.entries) {
                cancel(prayer.ordinal)
                cancel(100 + prayer.ordinal)
            }
        }
    }

    /** Formats "HH:mm" using the localized context; kept here so receivers share one path. */
    fun timeLabel(context: Context, hour: Int, minute: Int, use24h: Boolean): String {
        val ctx = context.localized()
        val locale = ctx.resources.configuration.locales[0]
        val amPm = if (use24h) null else Formatters.amPmLabel(hour, locale)
        val arabic = locale.language == "ar"
        return Formatters.formatClock(hour, minute, use24h, amPm, arabic)
    }
}
