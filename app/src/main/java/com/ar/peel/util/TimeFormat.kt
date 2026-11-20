package com.ar.peel.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The stamp printed on every note, e.g. "Fri, 20 Nov 2026 · 6:42 PM". */
object TimeFormat {
    fun stamp(
        epochMs: Long,
        locale: Locale,
        is24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val pattern = if (is24Hour) {
            "EEE, d MMM yyyy · HH:mm"
        } else {
            "EEE, d MMM yyyy · h:mm a"
        }
        return DateTimeFormatter.ofPattern(pattern, locale)
            .format(Instant.ofEpochMilli(epochMs).atZone(zone))
    }
}
