package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.CategoryItem
import com.example.ui.CategorySpend
import com.example.ui.theme.AnalyticsColorPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnalyticsColorTest {

    private fun createCategorySpend(id: String, name: String, color: Color, amount: Double = 100.0): CategorySpend {
        val cat = CategoryItem(
            id = id,
            name = name,
            icon = Icons.Filled.Category,
            color = color,
            iconKey = "category"
        )
        return CategorySpend(
            category = cat,
            totalAmount = amount,
            transactionCount = 1,
            percentageOfSpend = 0.5f,
            averageAmount = amount
        )
    }

    @Test
    fun resolveDistinctCategoryColors_emptyList_returnsEmpty() {
        val result = AnalyticsColorPalette.resolveDistinctCategoryColors(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun resolveDistinctCategoryColors_singleItem_preservesColor() {
        val singleColor = Color(0xFF2E7D32)
        val items = listOf(createCategorySpend("cat1", "Groceries", singleColor))
        val resolved = AnalyticsColorPalette.resolveDistinctCategoryColors(items)

        assertEquals(1, resolved.size)
        assertEquals(singleColor.toArgb(), resolved[0].category.color.toArgb())
    }

    @Test
    fun resolveDistinctCategoryColors_identicalColors_reassignsDistinctColors() {
        // Create 5 categories that all share the exact same Color
        val sharedColor = Color(0xFFD35400) // Terracotta
        val items = listOf(
            createCategorySpend("cat1", "Dining", sharedColor),
            createCategorySpend("cat2", "Takeout", sharedColor),
            createCategorySpend("cat3", "Coffee", sharedColor),
            createCategorySpend("cat4", "Snacks", sharedColor),
            createCategorySpend("cat5", "Bakery", sharedColor)
        )

        val resolved = AnalyticsColorPalette.resolveDistinctCategoryColors(items)

        assertEquals(5, resolved.size)

        // First item retains its original color
        assertEquals(sharedColor.toArgb(), resolved[0].category.color.toArgb())

        // Every item in the resolved list must have a strictly unique color
        val distinctArgbSet = resolved.map { it.category.color.toArgb() }.toSet()
        assertEquals("Each category must receive a unique color with zero duplicates", 5, distinctArgbSet.size)

        // Verify none of the subsequent 4 items share the color of the first or each other
        for (i in 1 until resolved.size) {
            assertNotEquals("Item $i must not have the shared colliding color", sharedColor.toArgb(), resolved[i].category.color.toArgb())
        }
    }

    @Test
    fun resolveDistinctCategoryColors_preservesDistinctOriginals_andResolvesOnlyCollisions() {
        val green = Color(0xFF2E7D32)
        val blue = Color(0xFF2980B9)
        val items = listOf(
            createCategorySpend("cat1", "Groceries", green),
            createCategorySpend("cat2", "Utilities", blue),
            createCategorySpend("cat3", "Duplicate Green", green) // Collides with Groceries
        )

        val resolved = AnalyticsColorPalette.resolveDistinctCategoryColors(items)

        assertEquals(3, resolved.size)
        // First two should keep their non-colliding colors
        assertEquals(green.toArgb(), resolved[0].category.color.toArgb())
        assertEquals(blue.toArgb(), resolved[1].category.color.toArgb())

        // Third was colliding, should get a distinct color that is neither green nor blue
        assertNotEquals(green.toArgb(), resolved[2].category.color.toArgb())
        assertNotEquals(blue.toArgb(), resolved[2].category.color.toArgb())

        val distinctArgbSet = resolved.map { it.category.color.toArgb() }.toSet()
        assertEquals(3, distinctArgbSet.size)
    }

    @Test
    fun resolveDistinctCategoryColors_largeCategoryCount_proceduralFallbackEnsuresUniqueness() {
        // Create 30 categories with identical starting colors to force using the full 24 curated palette + procedural fallback
        val duplicateColor = Color(0xFF000000)
        val items = (1..30).map { i ->
            createCategorySpend("cat$i", "Category $i", duplicateColor)
        }

        val resolved = AnalyticsColorPalette.resolveDistinctCategoryColors(items)

        assertEquals(30, resolved.size)
        val distinctArgbSet = resolved.map { it.category.color.toArgb() }.toSet()
        assertEquals("All 30 categories must have unique colors even beyond the 24 curated palette", 30, distinctArgbSet.size)
    }

    @Test
    fun curatedPalette_has24DistinctColors() {
        assertEquals(24, AnalyticsColorPalette.curatedPalette.size)
        val distinctArgb = AnalyticsColorPalette.curatedPalette.map { it.toArgb() }.toSet()
        assertEquals("Curated palette swatches must all be distinct", 24, distinctArgb.size)
    }
}
