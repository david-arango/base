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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CalmNegativeDeep
import com.example.ui.theme.CalmOutline
import com.example.ui.theme.CalmPositiveDeep
import com.example.ui.theme.CalmSecondary
import com.example.ui.theme.CalmSurfaceTint
import com.example.ui.theme.CalmTerracotta

data class CategoryItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object DefaultCategories {
    val list = listOf(
        CategoryItem("groceries", "Groceries", Icons.Filled.LocalGroceryStore, CalmSecondary),
        CategoryItem("dining", "Dining Out", Icons.Filled.Restaurant, CalmNegativeDeep),
        CategoryItem("bills", "Bills & Utilities", Icons.Filled.Receipt, CalmSurfaceTint),
        CategoryItem("transport", "Transport", Icons.Filled.DirectionsSubway, CalmTerracotta),
        CategoryItem("shopping", "Shopping", Icons.Filled.ShoppingBag, CalmOutline),
        CategoryItem("health", "Health", Icons.Filled.Spa, CalmPositiveDeep),
        CategoryItem("entertainment", "Entertainment", Icons.Filled.ConfirmationNumber, Color(0xFF8B6F5F))
    )

    fun getCategoryByName(name: String): CategoryItem {
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
