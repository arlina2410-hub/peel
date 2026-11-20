package com.ar.peel.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NoteGeometryTest {
    @Test
    fun `keeps the note reachable`() {
        assertEquals(0.08f, NoteGeometry.clampX(-4f))
        assertEquals(0.92f, NoteGeometry.clampX(4f))
        assertEquals(0.05f, NoteGeometry.clampY(0f))
        assertEquals(0.95f, NoteGeometry.clampY(1f))
    }

    @Test
    fun `keeps width and rotation inside the allowed range`() {
        assertEquals(0.30f, NoteGeometry.clampWidth(0.1f))
        assertEquals(0.95f, NoteGeometry.clampWidth(2f))
        assertEquals(-15f, NoteGeometry.clampRotation(-40f))
        assertEquals(15f, NoteGeometry.clampRotation(40f))
    }

    @Test
    fun `never makes a note taller than the screen`() {
        val landscapePhone = NoteGeometry.noteWidthPx(2000, 900, 0.95f)
        assertTrue(landscapePhone * 1.6f <= 900.1f)
    }

    @Test
    fun `random tilt stays subtle`() {
        repeat(100) {
            val tilt = NoteGeometry.randomTilt(Random(it))
            assertTrue(tilt >= -2.5f && tilt <= 2.5f)
        }
    }
}
