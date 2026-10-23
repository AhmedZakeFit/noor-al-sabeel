package io.github.ahmedzakefit.noor.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import io.github.ahmedzakefit.noor.appContainer
import io.github.ahmedzakefit.noor.core.model.GeoPosition
import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.data.calculationParametersFor
import io.github.ahmedzakefit.noor.notifications.AdhanAudio.shouldPlayAdhan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Fired by AlarmManager at prayer times (or shortly before, for reminders).
 * Posts the notification and reschedules the upcoming chain.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "io.github.ahmedzakefit.noor.ACTION_PRAYER_ALARM"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_IS_REMINDER = "is_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PRAYER_ALARM) return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER) ?: return
        val isReminder = intent.getBooleanExtra(EXTRA_IS_REMINDER, false)
        val prayer = Prayer.fromKey(prayerName.lowercase()) ?: Prayer.valueOf(prayerName)
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                val app = context.appContainer
                val settings = app.settingsRepository.settings.first()
                val location = settings.location
                if (settings.notificationsMaster && location != null) {
                    val zone = ZoneId.of(location.timeZoneId)
                    val today = LocalDate.now(zone)
                    val times = app.prayerCalculator.compute(
                        today,
                        GeoPosition(location.latitude, location.longitude, 0.0, location.timeZoneId),
                        settings.calculationParametersFor(today),
                        zone,
                    )
                    val use24h = DateFormat.is24HourFormat(context)
                    val timeLabel = Notifier.timeLabel(
                        context, times.timeOf(prayer).hour, times.timeOf(prayer).minute, use24h,
                    )
                    val locale = context.resources.configuration.locales[0]
                    val locationLabel = location.displayLabel(locale.toLanguageTag())
                    if (isReminder) {
                        Notifier.showReminderNotification(
                            context, prayer, settings.reminderMinutes, timeLabel, locationLabel,
                        )
                    } else if (shouldPlayAdhan(settings.adhanMode, isReminder = false, prayer)) {
                        // Full adhan via a mediaPlayback foreground service. If the
                        // system blocks the service start (background FGS
                        // restrictions), fall back to the regular audible alert so
                        // the prayer time is never missed silently.
                        val started = AdhanService.start(
                            context,
                            prayer,
                            preview = false,
                            timeLabel = timeLabel,
                            locationLabel = locationLabel,
                        )
                        if (!started) {
                            Notifier.showPrayerNotification(
                                context, prayer, timeLabel, locationLabel,
                                arabicDigits = settings.digits != io.github.ahmedzakefit.noor.data.DigitSetting.LATIN,
                            )
                        }
                    } else {
                        Notifier.showPrayerNotification(
                            context, prayer, timeLabel, locationLabel,
                            arabicDigits = settings.digits != io.github.ahmedzakefit.noor.data.DigitSetting.LATIN,
                        )
                    }
                }
                app.alarmScheduler.reschedule()
            } catch (_: Throwable) {
                // Never crash from a broadcast; the next reschedule point recovers.
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }
}
