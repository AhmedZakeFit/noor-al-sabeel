package io.github.ahmedzakefit.noor.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.ahmedzakefit.noor.core.calc.PrayerCalculator
import io.github.ahmedzakefit.noor.core.model.GeoPosition
import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.notifications.Notifier
import io.github.ahmedzakefit.noor.notifications.PrayerAlarmReceiver
import io.github.ahmedzakefit.noor.notifications.ReminderRules
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Schedules exact alarms for upcoming prayers (and pre-prayer reminders) for the
 * next three days, and reschedules itself after every fired alarm, boot, time or
 * timezone change. Falls back to inexact windowed alarms when exact alarms are
 * not permitted.
 *
 * Also keeps posted notifications honest: a prayer whose alarms are all gone
 * (alerts turned off per-prayer, master switch off, location cleared) gets its
 * stale prayer/reminder/adhan notifications cancelled — a leftover countdown
 * would otherwise keep ticking towards a time nobody will be alerted for.
 */
class AlarmScheduler(
    private val context: Context,
    private val repository: SettingsRepository,
    private val calculator: PrayerCalculator,
) {

    suspend fun reschedule() {
        val settings = repository.settings.first()
        cancelAll()
        if (!settings.notificationsMaster) {
            Notifier.cancelPrayerNotifications(context)
            return
        }
        val location = settings.location ?: run {
            Notifier.cancelPrayerNotifications(context)
            return
        }

        val zone = ZoneId.of(location.timeZoneId)
        val now = Instant.now()
        val today = LocalDate.now(zone)

        val scheduledPrayers = mutableSetOf<Prayer>()
        for (dayOffset in 0..2) {
            val date = today.plusDays(dayOffset.toLong())
            val params = settings.calculationParametersFor(date)
            val times = calculator.compute(
                date,
                GeoPosition(location.latitude, location.longitude, 0.0, location.timeZoneId),
                params,
                zone,
            )
            for ((prayer, time) in times.entries) {
                if (!prayer.notifiable) continue
                if (!settings.notificationsPerPrayer.contains(prayer)) continue
                val at = time.toInstant()
                if (at <= now) continue
                scheduledPrayers += prayer
                schedule(at.toEpochMilli(), prayer, dayOffset, isReminder = false)
                val reminder = settings.reminderMinutes
                if (reminder > 0) {
                    val reminderAt = time.minusMinutes(reminder.toLong()).toInstant()
                    if (reminderAt > now) {
                        schedule(reminderAt.toEpochMilli(), prayer, dayOffset, isReminder = true)
                    }
                }
            }
        }

        // Notifications for prayers with no scheduled alarm anymore are stale.
        for (prayer in ReminderRules.prayersToCancel(scheduledPrayers)) {
            Notifier.cancelPrayerFamily(context, prayer)
        }
    }

    fun cancelAll() {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (day in 0..2) {
            for (prayer in Prayer.entries) {
                for (reminder in intArrayOf(0, 1)) {
                    val rc = requestCode(day, prayer, reminder == 1)
                    val pi = PendingIntent.getBroadcast(
                        context,
                        rc,
                        intent(prayer, reminder == 1),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
                    ) ?: continue
                    am.cancel(pi)
                    pi.cancel()
                }
            }
        }
    }

    fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun schedule(atMillis: Long, prayer: Prayer, day: Int, isReminder: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(day, prayer, isReminder),
            intent(prayer, isReminder),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (canScheduleExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            // Bounded window keeps delays small without the exact-alarm permission.
            am.setWindow(AlarmManager.RTC_WAKEUP, atMillis, 10 * 60_000L, pi)
        }
    }

    private fun requestCode(day: Int, prayer: Prayer, isReminder: Boolean): Int =
        day * 100 + prayer.ordinal * 10 + (if (isReminder) 1 else 0)

    private fun intent(prayer: Prayer, isReminder: Boolean): Intent =
        Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.name)
            putExtra(PrayerAlarmReceiver.EXTRA_IS_REMINDER, isReminder)
        }
}
