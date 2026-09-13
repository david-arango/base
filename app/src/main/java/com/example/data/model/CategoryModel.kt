package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.CategoryEntity
import com.example.ui.theme.CalmNegativeDeep
import com.example.ui.theme.CalmOutline
import com.example.ui.theme.CalmPositiveDeep
import com.example.ui.theme.CalmSage
import com.example.ui.theme.CalmSecondary
import com.example.ui.theme.CalmSurfaceTint
import com.example.ui.theme.CalmTerracotta

data class CategoryItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val dbId: Long = 0L,
    val iconKey: String = "category"
)

object CategoryVisuals {
    val iconOptions: List<Pair<String, ImageVector>> = listOf(
        "local_grocery_store" to Icons.Filled.LocalGroceryStore,
        "restaurant" to Icons.Filled.Restaurant,
        "receipt" to Icons.Filled.Receipt,
        "directions_subway" to Icons.Filled.DirectionsSubway,
        "shopping_bag" to Icons.Filled.ShoppingBag,
        "spa" to Icons.Filled.Spa,
        "movie" to Icons.Filled.ConfirmationNumber,
        "local_cafe" to Icons.Filled.LocalCafe,
        "bolt" to Icons.Filled.Bolt,
        "work" to Icons.Filled.Work,
        "flight" to Icons.Filled.Flight,
        "home" to Icons.Filled.Home,
        "pets" to Icons.Filled.Pets,
        "school" to Icons.Filled.School,
        "fitness_center" to Icons.Filled.FitnessCenter,
        "directions_car" to Icons.Filled.DirectionsCar,
        "medical" to Icons.Filled.LocalHospital,
        "payments" to Icons.Filled.Payments,
        "category" to Icons.Filled.Category
    )

    val colorOptions: List<Color> = listOf(
        CalmSecondary,
        CalmTerracotta,
        CalmPositiveDeep,
        CalmNegativeDeep,
        CalmSurfaceTint,
        CalmSage,
        Color(0xFF6B4E3F),
        Color(0xFF1B555B),
        Color(0xFF384656),
        Color(0xFF7A4F1D)
    )

    fun getIcon(key: String): ImageVector {
        return iconOptions.firstOrNull { it.first == key }?.second ?: Icons.Filled.Category
    }

    fun getIconKey(icon: ImageVector): String {
        return iconOptions.firstOrNull { it.second == icon }?.first ?: "category"
    }
}

object DefaultCategories {
    val list = listOf(
        CategoryItem("groceries", "Groceries", Icons.Filled.LocalGroceryStore, CalmSecondary, 1L, "local_grocery_store"),
        CategoryItem("dining", "Dining Out", Icons.Filled.Restaurant, CalmNegativeDeep, 2L, "restaurant"),
        CategoryItem("bills", "Bills & Utilities", Icons.Filled.Receipt, CalmSurfaceTint, 3L, "receipt"),
        CategoryItem("transport", "Transport", Icons.Filled.DirectionsSubway, CalmTerracotta, 4L, "directions_subway"),
        CategoryItem("shopping", "Shopping", Icons.Filled.ShoppingBag, CalmOutline, 5L, "shopping_bag"),
        CategoryItem("health", "Health", Icons.Filled.Spa, CalmPositiveDeep, 6L, "spa"),
        CategoryItem("entertainment", "Entertainment", Icons.Filled.ConfirmationNumber, Color(0xFF6B4E3F), 7L, "movie")
    )

    fun fromEntity(entity: CategoryEntity): CategoryItem {
        val defaultMatch = list.firstOrNull { it.name.equals(entity.name, ignoreCase = true) }
        val id = defaultMatch?.id ?: entity.name.lowercase(java.util.Locale.US).replace("&", "").trim().replace(Regex("\\s+"), "_")
        return CategoryItem(
            id = id,
            name = entity.name,
            icon = CategoryVisuals.getIcon(entity.iconKey),
            color = Color(entity.colorHex),
            dbId = entity.id,
            iconKey = entity.iconKey
        )
    }

    fun toEntity(item: CategoryItem): CategoryEntity {
        val hex = try {
            item.color.toArgb().toLong() and 0xFFFFFFFFL
        } catch (_: Exception) {
            item.color.value.toLong()
        }
        return CategoryEntity(
            id = item.dbId,
            name = item.name,
            iconKey = item.iconKey,
            colorHex = hex
        )
    }

    fun getCategoryByName(name: String, customCategories: List<CategoryItem> = emptyList()): CategoryItem {
        if (customCategories.isNotEmpty()) {
            val customMatch = customCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (customMatch != null) return customMatch
        }
        return list.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: when {
                name.contains("Groceries", ignoreCase = true) -> list[0]
                name.contains("Dining", ignoreCase = true) || name.contains("Coffee", ignoreCase = true) -> list[1]
                name.contains("Bill", ignoreCase = true) || name.contains("Utility", ignoreCase = true) -> list[2]
                name.contains("Transport", ignoreCase = true) -> list[3]
                name.contains("Shopping", ignoreCase = true) -> list[4]
                name.contains("Health", ignoreCase = true) -> list[5]
                name.contains("Income", ignoreCase = true) || name.contains("Paycheck", ignoreCase = true) ->
                    CategoryItem("income", "Recurring Income", Icons.Filled.Work, CalmPositiveDeep)
                else -> CategoryItem("other", name, Icons.Filled.Payments, CalmSecondary)
            }
    }
}
