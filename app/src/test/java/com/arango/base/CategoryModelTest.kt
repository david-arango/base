package com.arango.base

import androidx.compose.ui.graphics.Color
import com.arango.base.data.local.CategoryEntity
import com.arango.base.data.model.CategoryItem
import com.arango.base.data.model.CategoryVisuals
import com.arango.base.data.model.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryModelTest {

    @Test
    fun defaultCategoriesList_hasExpectedCategories() {
        val list = DefaultCategories.list
        assertTrue(list.size >= 7)
        assertTrue(list.any { it.name == "Groceries" })
        assertTrue(list.any { it.name == "Dining Out" })
        assertTrue(list.any { it.name == "Bills & Utilities" })
        assertTrue(list.any { it.name == "Transport" })
        assertTrue(list.any { it.name == "Shopping" })
        assertTrue(list.any { it.name == "Health" })
        assertTrue(list.any { it.name == "Entertainment" })
    }

    @Test
    fun getCategoryByName_exactAndCaseInsensitiveMatches() {
        assertEquals("groceries", DefaultCategories.getCategoryByName("Groceries").id)
        assertEquals("groceries", DefaultCategories.getCategoryByName("groceries").id)
        assertEquals("dining", DefaultCategories.getCategoryByName("Dining Out").id)
        assertEquals("dining", DefaultCategories.getCategoryByName("dining out").id)
        assertEquals("bills", DefaultCategories.getCategoryByName("Bills & Utilities").id)
        assertEquals("transport", DefaultCategories.getCategoryByName("Transport").id)
        assertEquals("shopping", DefaultCategories.getCategoryByName("Shopping").id)
        assertEquals("health", DefaultCategories.getCategoryByName("Health").id)
        assertEquals("entertainment", DefaultCategories.getCategoryByName("Entertainment").id)
    }

    @Test
    fun getCategoryByName_fallbackContainsMatches() {
        // Groceries keyword
        assertEquals("Groceries", DefaultCategories.getCategoryByName("Organic Groceries").name)

        // Dining or Coffee keyword
        assertEquals("Dining Out", DefaultCategories.getCategoryByName("Fine Dining").name)
        assertEquals("Dining Out", DefaultCategories.getCategoryByName("Morning Coffee").name)

        // Bill or Utility keyword
        assertEquals("Bills & Utilities", DefaultCategories.getCategoryByName("Electric Bill").name)
        assertEquals("Bills & Utilities", DefaultCategories.getCategoryByName("City Utility").name)

        // Transport keyword
        assertEquals("Transport", DefaultCategories.getCategoryByName("Public Transport Pass").name)

        // Shopping keyword
        assertEquals("Shopping", DefaultCategories.getCategoryByName("Online Shopping").name)

        // Health keyword
        assertEquals("Health", DefaultCategories.getCategoryByName("Health & Wellness").name)

        // Income or Paycheck keyword
        val incomeCat = DefaultCategories.getCategoryByName("Salary Income")
        assertEquals("income", incomeCat.id)
        assertEquals("Recurring Income", incomeCat.name)

        val paycheckCat = DefaultCategories.getCategoryByName("Bi-Weekly Paycheck")
        assertEquals("income", paycheckCat.id)
        assertEquals("Recurring Income", paycheckCat.name)
    }

    @Test
    fun getCategoryByName_unmatchedFallback() {
        val unknownCat = DefaultCategories.getCategoryByName("Custom Unmatched Hobby")
        assertEquals("other", unknownCat.id)
        assertEquals("Custom Unmatched Hobby", unknownCat.name)
        assertNotNull(unknownCat.icon)
    }

    @Test
    fun categoryVisuals_iconAndColorOptions() {
        assertTrue(CategoryVisuals.iconOptions.isNotEmpty())
        assertTrue(CategoryVisuals.colorOptions.isNotEmpty())

        val icon = CategoryVisuals.getIcon("restaurant")
        assertNotNull(icon)
        val key = CategoryVisuals.getIconKey(icon)
        assertEquals("restaurant", key)

        val fallbackIcon = CategoryVisuals.getIcon("unknown_key_xyz")
        assertNotNull(fallbackIcon)
    }

    @Test
    fun defaultCategories_entityMapping() {
        val entity = CategoryEntity(
            id = 42L,
            name = "Travel & Leisure",
            iconKey = "flight",
            colorHex = 0xFF5588AAL
        )

        val item = DefaultCategories.fromEntity(entity)
        assertEquals(42L, item.dbId)
        assertEquals("Travel & Leisure", item.name)
        assertEquals("flight", item.iconKey)
        assertEquals(Color(0xFF5588AAL), item.color)

        val convertedEntity = DefaultCategories.toEntity(item)
        assertEquals(42L, convertedEntity.id)
        assertEquals("Travel & Leisure", convertedEntity.name)
        assertEquals("flight", convertedEntity.iconKey)
        assertEquals(0xFF5588AAL, convertedEntity.colorHex)
        assertNull(convertedEntity.spendingTarget)
    }

    @Test
    fun defaultCategories_haveNullSpendingTargetByDefault() {
        DefaultCategories.list.forEach { cat ->
            assertNull("Default category ${cat.name} should have null target", cat.spendingTarget)
        }
    }

    @Test
    fun defaultCategories_entityMappingWithSpendingTarget() {
        val entity = CategoryEntity(
            id = 77L,
            name = "Groceries",
            iconKey = "local_grocery_store",
            colorHex = 0xFF2E7D32L,
            spendingTarget = 250.0
        )

        val item = DefaultCategories.fromEntity(entity)
        assertEquals(77L, item.dbId)
        assertEquals(250.0, item.spendingTarget!!, 0.001)

        val backToEntity = DefaultCategories.toEntity(item)
        assertEquals(250.0, backToEntity.spendingTarget!!, 0.001)

        // Zero or negative target normalizes to null
        val zeroTargetEntity = entity.copy(spendingTarget = 0.0)
        assertNull(DefaultCategories.fromEntity(zeroTargetEntity).spendingTarget)
    }

    @Test
    fun defaultCategories_displayOrderMapping() {
        DefaultCategories.list.forEachIndexed { index, cat ->
            assertEquals("Default category ${cat.name} should have displayOrder = $index", index, cat.displayOrder)
        }

        val entity = CategoryEntity(
            id = 55L,
            name = "Gaming",
            iconKey = "sports_esports",
            colorHex = 0xFF8E44ADL,
            displayOrder = 9
        )
        val item = DefaultCategories.fromEntity(entity)
        assertEquals(9, item.displayOrder)

        val backToEntity = DefaultCategories.toEntity(item)
        assertEquals(9, backToEntity.displayOrder)
    }

    @Test
    fun getCategoryByName_preservesCustomCategorySpendingTarget() {
        val customGroceries = DefaultCategories.list.first { it.name == "Groceries" }
            .copy(dbId = 10L, spendingTarget = 175.0)
        val resolved = DefaultCategories.getCategoryByName("Groceries", listOf(customGroceries))
        assertEquals(175.0, resolved.spendingTarget!!, 0.001)
    }
}
