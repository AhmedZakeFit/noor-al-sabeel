package io.github.ahmedzakefit.noor.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ahmedzakefit.noor.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Re-arms the alarm schedule after boot, time/timezone/date changes, locale
 * changes and app updates — so prayer notifications survive a reboot without
 * opening the app.
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val relevant = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
        if (action !in relevant) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                // Boot / clock / timezone changes can leave countdown reminders
                // whose prayer time already passed while the device was off or
                // shifted — clear them all before re-arming; fresh ones are
                // posted by the next reminder alarms.
                for (prayer in io.github.ahmedzakefit.noor.core.model.Prayer.entries) {
                    Notifier.cancelReminder(context, prayer)
                }
                context.appContainer.alarmScheduler.reschedule()
            } catch (_: Throwable) {
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }
}
