package com.dumbopanda.ledger.core.domain

import java.time.Instant
import java.time.ZonedDateTime

/**
 * §7.14's nightly nudge: "banner between 22:00 and 10:00 if there are entries and no backup
 * since the last 22:00 cutoff. Snoozeable. Outside that window it is suppressed." [now] is
 * already zone-applied (the device's own zone) -- the 22:00/10:00 cutoffs are wall-clock local
 * time, not UTC.
 */
fun shouldShowNightlyNudge(
    now: ZonedDateTime,
    hasEntries: Boolean,
    lastBackupAt: Instant?,
    snoozedUntil: Instant?,
): Boolean {
    if (!hasEntries) return false
    if (now.hour !in NIGHT_START..23 && now.hour !in 0 until NIGHT_END) return false
    if (snoozedUntil != null && !now.toInstant().isAfter(snoozedUntil)) return false
    val cutoff = lastNightCutoff(now).toInstant()
    val backedUpSinceCutoff = lastBackupAt != null && !lastBackupAt.isBefore(cutoff)
    return !backedUpSinceCutoff
}

// The most recent 22:00 that has already passed -- at 08:00 that's last night's 22:00 (a day
// earlier), not today's (which hasn't happened yet).
private fun lastNightCutoff(now: ZonedDateTime): ZonedDateTime {
    val todayCutoff = now.toLocalDate().atTime(NIGHT_START, 0).atZone(now.zone)
    return if (now.isBefore(todayCutoff)) todayCutoff.minusDays(1) else todayCutoff
}

private const val NIGHT_START = 22
private const val NIGHT_END = 10
