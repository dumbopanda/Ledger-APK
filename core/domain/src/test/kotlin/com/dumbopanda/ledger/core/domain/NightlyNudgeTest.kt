package com.dumbopanda.ledger.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class NightlyNudgeTest {
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = LocalDate.of(2026, 9, 25)) =
        date.atTime(hour, minute).atZone(ZoneOffset.UTC)

    @Test
    fun `no entries -- never shown, even at midnight with no backup ever`() {
        assertFalse(shouldShowNightlyNudge(at(23), hasEntries = false, lastBackupAt = null, snoozedUntil = null))
    }

    @Test
    fun `daytime -- suppressed outside the 22-00 to 10-00 window`() {
        assertFalse(shouldShowNightlyNudge(at(14), hasEntries = true, lastBackupAt = null, snoozedUntil = null))
        assertFalse(shouldShowNightlyNudge(at(10), hasEntries = true, lastBackupAt = null, snoozedUntil = null))
        assertFalse(shouldShowNightlyNudge(at(21, 59), hasEntries = true, lastBackupAt = null, snoozedUntil = null))
    }

    @Test
    fun `night window with entries and no backup ever -- shown`() {
        assertTrue(shouldShowNightlyNudge(at(23), hasEntries = true, lastBackupAt = null, snoozedUntil = null))
        assertTrue(shouldShowNightlyNudge(at(22, 0), hasEntries = true, lastBackupAt = null, snoozedUntil = null))
        assertTrue(shouldShowNightlyNudge(at(9, 59), hasEntries = true, lastBackupAt = null, snoozedUntil = null))
        assertTrue(shouldShowNightlyNudge(at(0), hasEntries = true, lastBackupAt = null, snoozedUntil = null))
    }

    @Test
    fun `a backup after the most recent 22-00 cutoff suppresses the nudge`() {
        val backedUp = at(22, 30).toInstant() // backed up just after tonight's cutoff
        assertFalse(shouldShowNightlyNudge(at(23), hasEntries = true, lastBackupAt = backedUp, snoozedUntil = null))
    }

    @Test
    fun `a backup BEFORE the most recent cutoff does not suppress it`() {
        val backedUpYesterdayAfternoon = at(15, date = LocalDate.of(2026, 9, 24)).toInstant()
        assertTrue(shouldShowNightlyNudge(at(23), hasEntries = true, lastBackupAt = backedUpYesterdayAfternoon, snoozedUntil = null))
    }

    @Test
    fun `at 8am, the relevant cutoff is LAST night's 22-00, not tonight's (which hasn't happened)`() {
        // Backed up at 23:00 last night -- after last night's cutoff, so suppressed at 8am today.
        val backedUpLastNight = at(23, date = LocalDate.of(2026, 9, 24)).toInstant()
        assertFalse(shouldShowNightlyNudge(at(8), hasEntries = true, lastBackupAt = backedUpLastNight, snoozedUntil = null))
    }

    @Test
    fun `snoozed until a future time suppresses the nudge`() {
        val snoozedUntil = at(23, 30).toInstant()
        assertFalse(shouldShowNightlyNudge(at(23), hasEntries = true, lastBackupAt = null, snoozedUntil = snoozedUntil))
    }

    @Test
    fun `a snooze that has already expired no longer suppresses it`() {
        val snoozedUntil = at(22, 30).toInstant()
        assertTrue(shouldShowNightlyNudge(at(23), hasEntries = true, lastBackupAt = null, snoozedUntil = snoozedUntil))
    }
}
