package com.ar.peel.util

import com.ar.peel.data.DraftItem
import com.ar.peel.data.NoteDraft
import com.ar.peel.data.NoteMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckedItemsTest {
    @Test
    fun `parses only valid non-negative indexes`() {
        assertEquals(setOf(0, 2, 5), CheckedItems.parse("0, 2,5"))
        assertEquals(setOf(3), CheckedItems.parse("3,3,3"))
        assertEquals(emptySet<Int>(), CheckedItems.parse(null))
        assertEquals(emptySet<Int>(), CheckedItems.parse(""))
        assertEquals(emptySet<Int>(), CheckedItems.parse("x,-1,2.5"))
    }

    @Test
    fun `serializes in ascending order`() {
        assertEquals("1,2,4", CheckedItems.serialize(setOf(4, 1, 2)))
        assertEquals("", CheckedItems.serialize(emptySet()))
    }

    @Test
    fun `blank list items do not shift saved checkmarks`() {
        val draft = NoteDraft(
            mode = NoteMode.LIST,
            items = listOf(
                DraftItem("Milk", checked = true),
                DraftItem("", checked = true),
                DraftItem("Eggs", checked = true),
            ),
        )
        assertEquals("Milk\nEggs", draft.savedBody())
        assertEquals("0,1", draft.savedChecked())
    }
}
