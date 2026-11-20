package com.ar.peel.util

/** Checked list items are stored as "0,2,5" in the Note row. */
object CheckedItems {
    fun parse(raw: String?): Set<Int> =
        raw.orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 0 }
            .toSet()

    fun serialize(indexes: Set<Int>): String = indexes.sorted().joinToString(",")
}
