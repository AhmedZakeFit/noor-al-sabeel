package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.notifications.ReminderRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the "'10 minutes left' notification never updates"
 * report: the reminder must carry a live countdown to the prayer time (not a
 * frozen minute count), must never be posted once its target has passed, must
 * be replaced by the prayer notification at prayer time, and stale
 * notifications for prayers with no scheduled alarm must be cleaned up.
 */
class ReminderRulesTest {

    @Test
    fun `spec builds a countdown to the prayer time`() {
        val now = 1_000_000L
        val prayerAt = now + 10 * 60_000L
        val spec = ReminderRules.spec(Prayer.DHUHR, prayerAt, now)
        assertNotNull(spec)
        assertEquals(ReminderRules.reminderNotificationId(Prayer.DHUHR), spec!!.id)
        assertEquals(prayerAt, spec.countdownTargetMillis)
        assertTrue(spec.usesChronometer)
        assertTrue(spec.countsDown)
    }

    @Test
    fun `a reminder whose prayer time passed is stale and never posted`() {
        val now = 1_000_000L
        val prayerAt = now - 1 // one millisecond ago
        assertTrue(ReminderRules.isStale(prayerAt, now))
        assertNull(ReminderRules.spec(Prayer.ASR, prayerAt, now))
        // Exactly at the prayer time the main alarm fires instead.
        assertTrue(ReminderRules.isStale(now, now))
    }

    @Test
    fun `reminder and prayer notifications use distinct stable ids`() {
        for (prayer in Prayer.entries) {
            assertNotEquals(
                ReminderRules.prayerNotificationId(prayer),
                ReminderRules.reminderNotificationId(prayer),
            )
        }
        // Distinct across prayers too.
        assertEquals(
            Prayer.entries.size,
            Prayer.entries.map { ReminderRules.reminderNotificationId(it) }.toSet().size,
        )
    }

    @Test
    fun `prayers without scheduled alarms get their notifications cancelled`() {
        // All enabled (SUNRISE is never scheduled/notifiable — it is part of
        // the full set and its cancellation is a harmless no-op) -> nothing to
        // cancel beyond it.
        val all = Prayer.entries.toSet()
        assertTrue(ReminderRules.prayersToCancel(all).isEmpty())
        // ASR alerts switched off -> only ASR notifications are cleaned.
        val withoutAsr = all - Prayer.ASR
        assertEquals(setOf(Prayer.ASR), ReminderRules.prayersToCancel(withoutAsr))
        // Master switch off -> everything.
        assertEquals(all, ReminderRules.prayersToCancel(emptySet()))
    }
}
