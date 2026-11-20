package com.ar.peel.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class TimeFormatTest {
    private val utc = ZoneId.of("UTC")
    private val fridayEvening = ZonedDateTime.of(2026, 11, 20, 18, 42, 0, 0, utc)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `uses 12 hour clock when the device asks for it`() {
        val text = TimeFormat.stamp(fridayEvening, Locale.US, is24Hour = false, zone = utc)
        assertEquals("Fri, 20 Nov 2026 · 6:42 PM", text.replace('\u202F', ' '))
    }

    @Test
    fun `uses 24 hour clock when the device asks for it`() {
        val text = TimeFormat.stamp(fridayEvening, Locale.US, is24Hour = true, zone = utc)
        assertEquals("Fri, 20 Nov 2026 · 18:42", text)
    }

    @Test
    fun `keeps the exact minute the note was created`() {
        val text = TimeFormat.stamp(fridayEvening, Locale.US, is24Hour = false, zone = utc)
        assertTrue(text.contains("6:42"))
    }
}
