package com.arango.base.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.arango.base.ui.CategorySpend

/**
 * Palette and dynamic distinct color resolution engine for Analytics.
 * Ensures that every category displayed in Analytics (flow bar, legend, ranked breakdown)
 * receives a guaranteed unique color from the curated 24-color palette or procedural fallback.
 */
object AnalyticsColorPalette {

    /**
     * Curated 24 high-contrast, harmonious swatches optimized for financial visualization
     * on both light and dark surfaces.
     */
    val curatedPalette: List<Color> = listOf(
        Color(0xFF2E7D32), // 0: Emerald Green
        Color(0xFFD35400), // 1: Terracotta Orange
        Color(0xFF2980B9), // 2: Slate Blue
        Color(0xFFD48806), // 3: Amber Gold
        Color(0xFF8E44AD), // 4: Amethyst Purple
        Color(0xFF16A085), // 5: Turquoise Teal
        Color(0xFFC0392B), // 6: Berry Crimson
        Color(0xFF00838F), // 7: Deep Cyan
        Color(0xFF6D4C41), // 8: Warm Bronze
        Color(0xFF546E7A), // 9: Steel Blue-Grey
        Color(0xFF3F51B5), // 10: Indigo Blue
        Color(0xFFE64A19), // 11: Deep Orange / Coral
        Color(0xFF689F38), // 12: Lime / Olive
        Color(0xFF6A1B9A), // 13: Royal Violet
        Color(0xFF00897B), // 14: Teal Mint
        Color(0xFFEF6C00), // 15: Bright Tangerine
        Color(0xFFAD1457), // 16: Magenta Rose
        Color(0xFF0288D1), // 17: Sky Blue
        Color(0xFFF57F17), // 18: Golden Ochre
        Color(0xFF4E342E), // 19: Deep Umber
        Color(0xFF512DA8), // 20: Lavender Indigo
        Color(0xFF33691E), // 21: Olive Moss
        Color(0xFF1565C0), // 22: Cobalt Blue
        Color(0xFFB71C1C)  // 23: Crimson Red
    )

    /**
     * Generates a procedural color using golden-ratio hue distribution
     * to provide infinite distinct hues when category count exceeds the curated palette.
     */
    fun getProceduralColor(index: Int): Color {
        val hue = ((index * 137.507764f) % 360f + 360f) % 360f
        val hsv = floatArrayOf(hue, 0.72f, 0.82f)
        val argb = android.graphics.Color.HSVToColor(hsv)
        return Color(argb)
    }

    /**
     * Returns a color from the curated palette, falling back to procedural generation
     * if the index exceeds the size of the curated palette.
     */
    fun getColor(index: Int): Color {
        return if (index < curatedPalette.size) {
            curatedPalette[index]
        } else {
            getProceduralColor(index)
        }
    }

    /**
     * Resolves distinct colors for a list of [CategorySpend] items in Analytics.
     * Ensures that no two items in the breakdown share the same color.
     *
     * Behavior:
     * - Preserves the category's original color if not yet claimed by another category.
     * - If two categories collide (e.g. have the same color), the colliding category is
     *   dynamically reassigned the next available distinctive color from the palette.
     */
    fun resolveDistinctCategoryColors(breakdown: List<CategorySpend>): List<CategorySpend> {
        if (breakdown.isEmpty()) return emptyList()

        val claimedColors = mutableSetOf<Int>()
        var palettePointer = 0

        fun getNextUnclaimedColor(): Color {
            while (true) {
                val candidate = getColor(palettePointer++)
                val argb = candidate.toArgb()
                if (claimedColors.add(argb)) {
                    return candidate
                }
            }
        }

        return breakdown.map { item ->
            val origColor = item.category.color
            val origArgb = origColor.toArgb()

            val resolvedColor = if (claimedColors.add(origArgb)) {
                origColor
            } else {
                getNextUnclaimedColor()
            }

            if (resolvedColor == origColor) {
                item
            } else {
                item.copy(category = item.category.copy(color = resolvedColor))
            }
        }
    }
}
