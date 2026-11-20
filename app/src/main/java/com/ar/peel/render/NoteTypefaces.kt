package com.ar.peel.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.ar.peel.R

/** Excon is used only on the note itself. System fallback still covers missing scripts. */
class NoteTypefaces(context: Context) {
    val regular: Typeface =
        ResourcesCompat.getFont(context, R.font.excon_regular) ?: Typeface.DEFAULT
    val medium: Typeface =
        ResourcesCompat.getFont(context, R.font.excon_medium) ?: Typeface.DEFAULT_BOLD
    val bold: Typeface =
        ResourcesCompat.getFont(context, R.font.excon_bold) ?: Typeface.DEFAULT_BOLD
}
