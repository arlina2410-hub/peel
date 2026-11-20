package com.ar.peel.render

/** One sticky-note paper colour. ARGB ints so the shared renderer stays Compose-free. */
data class NoteColor(
    val id: String,
    val name: String,
    val argb: Int,
)

object NotePalette {
    val ink = 0xFF2A2A2A.toInt()

    val all = listOf(
        NoteColor("yellow", "Canary", 0xFFFFE066.toInt()),
        NoteColor("pink", "Bubblegum", 0xFFFFB3C7.toInt()),
        NoteColor("blue", "Sky", 0xFFA9D8FF.toInt()),
        NoteColor("mint", "Mint", 0xFFB5EAC0.toInt()),
        NoteColor("peach", "Peach", 0xFFFFC999.toInt()),
        NoteColor("lavender", "Lavender", 0xFFD4C1F5.toInt()),
        NoteColor("lime", "Lime", 0xFFE3F28A.toInt()),
        NoteColor("white", "Paper", 0xFFFFFDF5.toInt()),
    )

    fun byId(id: String): NoteColor = all.firstOrNull { it.id == id } ?: all.first()
}
