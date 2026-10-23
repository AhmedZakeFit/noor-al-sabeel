package io.github.ahmedzakefit.noor.notifications

import io.github.ahmedzakefit.noor.core.model.Prayer

/**
 * Pure, JVM-testable rules for the pre-prayer reminder notification.
 *
 * v1.0.3 posted the reminder once with a frozen "N minutes left" text that
 * never ticked and never handed over to the prayer notification. The rules
 * below capture the fixed behaviour:
 *  - the notification carries a live system-rendered countdown
 *    (chronometer) that counts down to the prayer time with zero extra
 *    alarms or wakeups — Android's SystemUI animates it;
 *  - the countdown target is the prayer time itself, so the text stays true
 *    no matter when the reminder was posted;
 *  - a reminder whose prayer time already passed is never (re)posted — it
 *    would count up from zero and lie;
 *  - when the prayer alarm fires, the reminder is cancelled and replaced by
 *    the prayer/adhan notification (same moment, same event);
 *  - prayers with no scheduled alarm anymore must have their notifications
 *    cleaned up (alerts disabled per-prayer or master switch off).
 */
object ReminderRules {

    const val REMINDER_ID_OFFSET = 100

    fun reminderNotificationId(prayer: Prayer): Int = REMINDER_ID_OFFSET + prayer.ordinal

    fun prayerNotificationId(prayer: Prayer): Int = prayer.ordinal

    /** Never post a countdown that would start at (or below) zero. */
    fun isStale(prayerAtMillis: Long, nowMillis: Long): Boolean = nowMillis >= prayerAtMillis

    /**
     * Prayers whose posted prayer/reminder notifications must be cancelled
     * because no alarm is scheduled for them anymore.
     */
    fun prayersToCancel(scheduledPrayers: Set<Prayer>): Set<Prayer> =
        Prayer.entries.toSet() - scheduledPrayers

    /**
     * Description of the reminder notification the builder renders — kept as
     * data so the countdown decision itself is unit-testable without Android.
     */
    data class Spec(
        val id: Int,
        val prayer: Prayer,
        /** Chronometer target: the prayer time (countdown end). */
        val countdownTargetMillis: Long,
        val usesChronometer: Boolean = true,
        val countsDown: Boolean = true,
    )

    fun spec(prayer: Prayer, prayerAtMillis: Long, nowMillis: Long): Spec? =
        if (isStale(prayerAtMillis, nowMillis)) null
        else Spec(
            id = reminderNotificationId(prayer),
            prayer = prayer,
            countdownTargetMillis = prayerAtMillis,
        )
}
