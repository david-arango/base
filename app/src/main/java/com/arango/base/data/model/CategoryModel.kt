package com.arango.base.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.arango.base.data.local.CategoryEntity
import com.arango.base.ui.theme.CalmNegativeDeep
import com.arango.base.ui.theme.CalmOutline
import com.arango.base.ui.theme.CalmPositiveDeep
import com.arango.base.ui.theme.CalmSage
import com.arango.base.ui.theme.CalmSecondary
import com.arango.base.ui.theme.CalmSurfaceTint
import com.arango.base.ui.theme.CalmTerracotta

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
        // Food, Drink & Dining
        "local_grocery_store" to Icons.Filled.LocalGroceryStore,
        "restaurant" to Icons.Filled.Restaurant,
        "take_out" to Icons.Filled.TakeoutDining,
        "local_cafe" to Icons.Filled.LocalCafe,
        "bar" to Icons.Filled.LocalBar,
        "nightlife" to Icons.Filled.Nightlife,

        // Entertainment, Hobbies & Media
        "movies" to Icons.Filled.Movie,
        "subscriptions" to Icons.Filled.Subscriptions,
        "concert" to Icons.Filled.Stadium,
        "music" to Icons.Filled.MusicNote,
        "gaming" to Icons.Filled.SportsEsports,
        "trading_cards" to Icons.Filled.Style,
        "puzzles" to Icons.Filled.Extension,
        "books" to Icons.AutoMirrored.Filled.MenuBook,
        "sports" to Icons.Filled.SportsSoccer,
        "hiking" to Icons.Filled.Hiking,

        // Shopping, Apparel & Goods
        "shopping_bag" to Icons.Filled.ShoppingBag,
        "clothes" to Icons.Filled.Checkroom,
        "jewelry" to Icons.Filled.Diamond,
        "electronics" to Icons.Filled.Devices,
        "furniture" to Icons.Filled.Weekend,
        "business_supplies" to Icons.Filled.BusinessCenter,

        // Transportation, Auto & Travel
        "car" to Icons.Filled.DirectionsCar,
        "gas_station" to Icons.Filled.LocalGasStation,
        "taxis" to Icons.Filled.LocalTaxi,
        "transportation" to Icons.Filled.DirectionsSubway,
        "travel" to Icons.Filled.Flight,
        "vacation" to Icons.Filled.BeachAccess,

        // Home, Services & Living
        "home" to Icons.Filled.Home,
        "renovations" to Icons.Filled.HomeRepairService,
        "repairs" to Icons.Filled.Build,
        "cleaning_supplies" to Icons.Filled.CleaningServices,
        "laundry" to Icons.Filled.LocalLaundryService,
        "plants" to Icons.Filled.Yard,
        "pets" to Icons.Filled.Pets,

        // Health, Personal Care & Medical
        "doctor" to Icons.Filled.MedicalServices,
        "pharmacy" to Icons.Filled.LocalPharmacy,
        "massages_physio" to Icons.Filled.Healing,
        "yoga" to Icons.Filled.SelfImprovement,
        "fitness_center" to Icons.Filled.FitnessCenter,
        "spa" to Icons.Filled.Spa,
        "makeup" to Icons.Filled.Face,

        // Family, Occasions & Gifts
        "child_care" to Icons.Filled.ChildCare,
        "gifts" to Icons.Filled.CardGiftcard,
        "birthdays" to Icons.Filled.Cake,

        // Bills, Finance & Legal
        "receipt" to Icons.Filled.Receipt,
        "credit_cards" to Icons.Filled.CreditCard,
        "phone_charges" to Icons.Filled.PhoneAndroid,
        "legal_fees" to Icons.Filled.Gavel,
        "bolt" to Icons.Filled.Bolt,
        "payments" to Icons.Filled.Payments,
        "work" to Icons.Filled.Work,
        "school" to Icons.Filled.School,
        "category" to Icons.Filled.Category
    )

    // Legacy and alternate key mappings to ensure full backward compatibility
    private val keyAliases: Map<String, String> = mapOf(
        "movie" to "movies",
        "directions_car" to "car",
        "directions_subway" to "transportation",
        "medical" to "doctor",
        "flight" to "travel"
    )

    private val iconLabels: Map<String, String> = mapOf(
        "local_grocery_store" to "Groceries",
        "restaurant" to "Restaurant",
        "take_out" to "Take-Out",
        "local_cafe" to "Cafe / Coffee",
        "bar" to "Bar",
        "nightlife" to "Nightlife",
        "movies" to "Movies",
        "subscriptions" to "Subscriptions",
        "concert" to "Concert",
        "music" to "Music",
        "gaming" to "Gaming",
        "trading_cards" to "Trading Cards",
        "puzzles" to "Puzzles",
        "books" to "Books",
        "sports" to "Sports",
        "hiking" to "Hiking",
        "shopping_bag" to "Shopping",
        "clothes" to "Clothes",
        "jewelry" to "Jewelry",
        "electronics" to "Electronics",
        "furniture" to "Furniture",
        "business_supplies" to "Business Supplies",
        "car" to "Car",
        "gas_station" to "Gas Station",
        "taxis" to "Taxis",
        "transportation" to "Transportation",
        "travel" to "Travel",
        "vacation" to "Vacation",
        "home" to "Home",
        "renovations" to "Renovations",
        "repairs" to "Repairs",
        "cleaning_supplies" to "Cleaning Supplies",
        "laundry" to "Laundry",
        "plants" to "Plants",
        "pets" to "Pets",
        "doctor" to "Doctor",
        "pharmacy" to "Pharmacy",
        "massages_physio" to "Massages / Physio",
        "yoga" to "Yoga",
        "fitness_center" to "Fitness / Gym",
        "spa" to "Spa",
        "makeup" to "Makeup",
        "child_care" to "Child Care",
        "gifts" to "Gifts",
        "birthdays" to "Birthdays",
        "receipt" to "Bills & Receipts",
        "credit_cards" to "Credit Cards",
        "phone_charges" to "Phone Charges",
        "legal_fees" to "Legal Fees",
        "bolt" to "Utilities",
        "payments" to "Payments",
        "work" to "Work",
        "school" to "Education",
        "category" to "Other"
    )

    fun getIconLabel(key: String): String {
        val resolved = keyAliases[key] ?: key
        return iconLabels[resolved] ?: resolved.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    val colorOptions: List<Color> = listOf(
        Color(0xFF2E7D32), // Emerald Green (Groceries / Food)
        Color(0xFFD35400), // Terracotta Orange (Dining / Cafe / Food)
        Color(0xFF2980B9), // Slate Blue (Bills / Utilities / Housing)
        Color(0xFFD48806), // Amber Gold (Transport / Auto / Travel)
        Color(0xFF8E44AD), // Amethyst Purple (Shopping / Retail / Goods)
        Color(0xFF16A085), // Turquoise Teal (Health / Spa / Wellness)
        Color(0xFFC0392B), // Berry Crimson (Entertainment / Hobbies / Media)
        Color(0xFF00838F), // Deep Cyan (Travel / Electronics / Tech)
        Color(0xFF6D4C41), // Warm Bronze / Brown (Home / Furniture)
        Color(0xFF546E7A)  // Steel Blue-Grey (Bills / Other)
    )

    fun findMatchingColor(name: String, iconKey: String = ""): Color {
        val lower = name.lowercase(java.util.Locale.US).trim()
        val key = if (iconKey.isNotBlank()) iconKey else (findMatchingIconKey(name) ?: "")
        return when {
            lower.contains("grocer") || key == "local_grocery_store" -> colorOptions[0]
            lower.contains("dining") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("coffee") || key == "restaurant" || key == "take_out" -> colorOptions[1]
            lower.contains("bill") || lower.contains("util") || lower.contains("phone") || lower.contains("power") || key == "receipt" || key == "bolt" -> colorOptions[2]
            lower.contains("transport") || lower.contains("transit") || lower.contains("subway") || lower.contains("car") || lower.contains("gas") || key == "car" || key == "directions_subway" -> colorOptions[3]
            lower.contains("shop") || lower.contains("cloth") || lower.contains("book") || key == "shopping_bag" -> colorOptions[4]
            lower.contains("health") || lower.contains("medical") || lower.contains("doctor") || lower.contains("gym") || lower.contains("spa") || key == "spa" || key == "doctor" -> colorOptions[5]
            lower.contains("entertain") || lower.contains("movie") || lower.contains("music") || lower.contains("game") || key == "movies" || key == "subscriptions" -> colorOptions[6]
            lower.contains("travel") || lower.contains("vacation") || lower.contains("flight") || key == "travel" -> colorOptions[7]
            lower.contains("home") || lower.contains("furniture") || lower.contains("pet") || key == "home" -> colorOptions[8]
            else -> {
                val index = (lower.hashCode() and 0x7FFFFFFF) % colorOptions.size
                colorOptions[index]
            }
        }
    }

    fun findMatchingColorHex(name: String, iconKey: String = ""): Long {
        val c = findMatchingColor(name, iconKey)
        return try {
            c.toArgb().toLong() and 0xFFFFFFFFL
        } catch (_: Exception) {
            c.value.toLong()
        }
    }

    fun getIcon(key: String): ImageVector {
        val resolved = keyAliases[key] ?: key
        return iconOptions.firstOrNull { it.first == resolved }?.second
            ?: when (key) {
                "movie" -> Icons.Filled.Movie
                "directions_car" -> Icons.Filled.DirectionsCar
                "directions_subway" -> Icons.Filled.DirectionsSubway
                "medical" -> Icons.Filled.MedicalServices
                "flight" -> Icons.Filled.Flight
                else -> Icons.Filled.Category
            }
    }

    fun getIconKey(icon: ImageVector): String {
        return iconOptions.firstOrNull { it.second == icon }?.first ?: "category"
    }

    fun findMatchingIconKey(name: String): String? {
        val lower = name.lowercase(java.util.Locale.US).trim()
        return when {
            lower.contains("bar") || lower.contains("pub") || lower.contains("cocktail") || lower.contains("liquor") || lower.contains("beer") || lower.contains("wine") -> "bar"
            lower.contains("nightlife") || lower.contains("club") || lower.contains("party") || lower.contains("lounge") -> "nightlife"
            lower.contains("movie") || lower.contains("cinema") || lower.contains("theatre") || lower.contains("theater") || lower.contains("film") -> "movies"
            lower.contains("subscription") || lower.contains("netflix") || lower.contains("streaming") || lower.contains("hulu") || lower.contains("disney") -> "subscriptions"
            lower.contains("sport") || lower.contains("soccer") || lower.contains("football") || lower.contains("basketball") || lower.contains("tennis") -> "sports"
            lower.contains("concert") || lower.contains("festival") || lower.contains("gig") -> "concert"
            lower.contains("music") || lower.contains("spotify") || lower.contains("album") || lower.contains("song") -> "music"
            lower.contains("pharmacy") || lower.contains("medicine") || lower.contains("drug") || lower.contains("prescription") || lower.contains("pill") -> "pharmacy"
            lower.contains("gas") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("chevron") || lower.contains("shell") -> "gas_station"
            lower.contains("yoga") || lower.contains("pilates") || lower.contains("meditation") || lower.contains("mindfulness") -> "yoga"
            lower.contains("car") || lower.contains("auto") || lower.contains("vehicle") || lower.contains("parking") || lower.contains("toll") -> "car"
            lower.contains("jewelry") || lower.contains("jewel") || lower.contains("ring") || lower.contains("necklace") || lower.contains("diamond") -> "jewelry"
            lower.contains("pet") || lower.contains("dog") || lower.contains("cat") || lower.contains("vet") || lower.contains("puppy") -> "pets"
            lower.contains("game") || lower.contains("gaming") || lower.contains("playstation") || lower.contains("xbox") || lower.contains("nintendo") || lower.contains("steam") -> "gaming"
            lower.contains("cloth") || lower.contains("apparel") || lower.contains("fashion") || lower.contains("dress") || lower.contains("shoes") -> "clothes"
            lower.contains("business") || lower.contains("office supply") || lower.contains("stationery") || lower.contains("supplies") -> "business_supplies"
            lower.contains("furniture") || lower.contains("couch") || lower.contains("sofa") || lower.contains("chair") || lower.contains("table") || lower.contains("desk") -> "furniture"
            lower.contains("clean") || lower.contains("detergent") || lower.contains("soap") || lower.contains("janitor") -> "cleaning_supplies"
            lower.contains("electronic") || lower.contains("gadget") || lower.contains("computer") || lower.contains("laptop") || lower.contains("ipad") || lower.contains("tech") -> "electronics"
            lower.contains("vacation") || lower.contains("resort") || lower.contains("beach") || lower.contains("holiday") || lower.contains("getaway") -> "vacation"
            lower.contains("taxi") || lower.contains("uber") || lower.contains("lyft") || lower.contains("cab") -> "taxis"
            lower.contains("flight") || lower.contains("travel") || lower.contains("airline") || lower.contains("airport") || lower.contains("hotel") -> "travel"
            lower.contains("transit") || lower.contains("subway") || lower.contains("train") || lower.contains("metro") || lower.contains("bus") -> "transportation"
            lower.contains("takeout") || lower.contains("take out") || lower.contains("take-out") || lower.contains("delivery") || lower.contains("doordash") || lower.contains("uber eats") -> "take_out"
            lower.contains("plant") || lower.contains("garden") || lower.contains("flower") || lower.contains("botanical") || lower.contains("succulent") -> "plants"
            lower.contains("renovation") || lower.contains("remodel") || lower.contains("contractor") || lower.contains("carpentry") -> "renovations"
            lower.contains("book") || lower.contains("novel") || lower.contains("reading") || lower.contains("library") || lower.contains("kindle") -> "books"
            lower.contains("birthday") || lower.contains("bday") -> "birthdays"
            lower.contains("gift") || lower.contains("present") || lower.contains("donation") -> "gifts"
            lower.contains("repair") || lower.contains("handyman") || lower.contains("fix") || lower.contains("mechanic") -> "repairs"
            lower.contains("laundry") || lower.contains("dry clean") || lower.contains("washer") -> "laundry"
            lower.contains("child") || lower.contains("daycare") || lower.contains("baby") || lower.contains("babysit") || lower.contains("kid") || lower.contains("nursery") -> "child_care"
            lower.contains("credit card") || lower.contains("visa") || lower.contains("mastercard") || lower.contains("amex") -> "credit_cards"
            lower.contains("legal") || lower.contains("lawyer") || lower.contains("attorney") || lower.contains("court") || lower.contains("fee") -> "legal_fees"
            lower.contains("phone") || lower.contains("cellular") || lower.contains("mobile") || lower.contains("verizon") || lower.contains("at&t") || lower.contains("t-mobile") -> "phone_charges"
            lower.contains("hike") || lower.contains("hiking") || lower.contains("trail") || lower.contains("outdoor") || lower.contains("camping") -> "hiking"
            lower.contains("puzzle") || lower.contains("board game") || lower.contains("jigsaw") -> "puzzles"
            lower.contains("makeup") || lower.contains("cosmetic") || lower.contains("skincare") || lower.contains("beauty") || lower.contains("lipstick") -> "makeup"
            lower.contains("doctor") || lower.contains("clinic") || lower.contains("physician") || lower.contains("dental") || lower.contains("dentist") -> "doctor"
            lower.contains("massage") || lower.contains("physio") || lower.contains("therapy") || lower.contains("chiropractic") || lower.contains("physical therapy") -> "massages_physio"
            lower.contains("trading card") || lower.contains("card collect") || lower.contains("pokemon") || lower.contains("mtg") || lower.contains("yugioh") || lower.contains("sports card") -> "trading_cards"
            lower.contains("grocer") -> "local_grocery_store"
            lower.contains("cafe") || lower.contains("coffee") -> "local_cafe"
            lower.contains("dining") || lower.contains("restaurant") -> "restaurant"
            lower.contains("shop") -> "shopping_bag"
            lower.contains("gym") || lower.contains("fitness") || lower.contains("workout") -> "fitness_center"
            lower.contains("spa") -> "spa"
            lower.contains("bill") || lower.contains("utility") -> "receipt"
            else -> null
        }
    }
}

object DefaultCategories {
    val list = listOf(
        CategoryItem("groceries", "Groceries", Icons.Filled.LocalGroceryStore, Color(0xFF2E7D32), 1L, "local_grocery_store"),
        CategoryItem("dining", "Dining Out", Icons.Filled.Restaurant, Color(0xFFD35400), 2L, "restaurant"),
        CategoryItem("bills", "Bills & Utilities", Icons.Filled.Receipt, Color(0xFF2980B9), 3L, "receipt"),
        CategoryItem("transport", "Transport", Icons.Filled.DirectionsSubway, Color(0xFFD48806), 4L, "directions_subway"),
        CategoryItem("shopping", "Shopping", Icons.Filled.ShoppingBag, Color(0xFF8E44AD), 5L, "shopping_bag"),
        CategoryItem("health", "Health", Icons.Filled.Spa, Color(0xFF16A085), 6L, "spa"),
        CategoryItem("entertainment", "Entertainment", Icons.Filled.ConfirmationNumber, Color(0xFFC0392B), 7L, "movie")
    )

    fun fromEntity(entity: CategoryEntity): CategoryItem {
        val defaultMatch = list.firstOrNull { it.name.equals(entity.name, ignoreCase = true) }
        val id = defaultMatch?.id ?: entity.name.lowercase(java.util.Locale.US).replace("&", "").trim().replace(Regex("\\s+"), "_")
        val resolvedColor = if (defaultMatch != null && (entity.colorHex == 0xFF5B604BL || entity.colorHex == 0L)) {
            defaultMatch.color
        } else if (entity.colorHex == 0xFF5B604BL || entity.colorHex == 0L) {
            CategoryVisuals.findMatchingColor(entity.name, entity.iconKey)
        } else {
            Color(entity.colorHex)
        }
        return CategoryItem(
            id = id,
            name = entity.name,
            icon = CategoryVisuals.getIcon(entity.iconKey),
            color = resolvedColor,
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
        val defaultMatch = list.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (defaultMatch != null) return defaultMatch

        return when {
            name.contains("Groceries", ignoreCase = true) -> list[0]
            name.contains("Dining", ignoreCase = true) || name.contains("Coffee", ignoreCase = true) -> list[1]
            name.contains("Bill", ignoreCase = true) || name.contains("Utility", ignoreCase = true) -> list[2]
            name.contains("Transport", ignoreCase = true) -> list[3]
            name.contains("Shopping", ignoreCase = true) -> list[4]
            name.contains("Health", ignoreCase = true) -> list[5]
            name.contains("Entertainment", ignoreCase = true) -> list[6]
            name.contains("Income", ignoreCase = true) || name.contains("Paycheck", ignoreCase = true) ->
                CategoryItem("income", "Recurring Income", Icons.Filled.Work, Color(0xFF1E824C))
            else -> {
                val assignedColor = CategoryVisuals.findMatchingColor(name)
                CategoryItem("other", name, Icons.Filled.Payments, assignedColor)
            }
        }
    }
}
