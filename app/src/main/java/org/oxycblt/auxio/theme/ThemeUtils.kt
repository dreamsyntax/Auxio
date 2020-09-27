package org.oxycblt.auxio.theme

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import org.oxycblt.auxio.R

// Pairs of the base accent and its theme
private val ACCENTS = listOf(
    Pair(R.color.red, R.style.Theme_Red),
    Pair(R.color.pink, R.style.Theme_Pink),
    Pair(R.color.purple, R.style.Theme_Purple),
    Pair(R.color.deep_purple, R.style.Theme_DeepPurple),
    Pair(R.color.indigo, R.style.Theme_Indigo),
    Pair(R.color.blue, R.style.Theme_Blue),
    Pair(R.color.light_blue, R.style.Theme_Blue),
    Pair(R.color.cyan, R.style.Theme_Cyan),
    Pair(R.color.teal, R.style.Theme_Teal),
    Pair(R.color.green, R.style.Theme_Green),
    Pair(R.color.light_green, R.style.Theme_LightGreen),
    Pair(R.color.lime, R.style.Theme_Lime),
    Pair(R.color.yellow, R.style.Theme_Yellow),
    Pair(R.color.amber, R.style.Theme_Amber),
    Pair(R.color.orange, R.style.Theme_Orange),
    Pair(R.color.deep_orange, R.style.Theme_DeepOrange),
    Pair(R.color.brown, R.style.Theme_Brown),
    Pair(R.color.grey, R.style.Theme_Gray),
    Pair(R.color.blue_grey, R.style.Theme_BlueGrey)
)

val accent = ACCENTS[5]

// Get the transparent variant of a color int
@ColorInt
fun getTransparentAccent(context: Context, color: Int, alpha: Int): Int {
    return ColorUtils.setAlphaComponent(
        ContextCompat.getColor(context, color),
        alpha
    )
}

// Get the inactive transparency of an accent
@ColorInt
fun getInactiveAlpha(color: Int): Int {
    return if (color == R.color.yellow) 100 else 150
}

// Convert an integer to a color
@ColorInt
fun Int.toColor(context: Context): Int {
    return try {
        ContextCompat.getColor(context, this)
    } catch (e: Exception) {
        // Default to the emergency color [Black] if the loading fails.
        ContextCompat.getColor(context, android.R.color.black)
    }
}

// Apply a custom vertical divider
fun RecyclerView.applyDivider() {
    val div = DividerItemDecoration(
        context,
        DividerItemDecoration.VERTICAL
    )

    div.setDrawable(
        ColorDrawable(
            R.color.divider_color.toColor(context)
        )
    )

    addItemDecoration(div)
}