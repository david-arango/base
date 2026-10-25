package com.arango.base

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.CategoryEntity
import com.arango.base.data.local.FinanceDao
import com.arango.base.data.local.OffsetTransactionEntity
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.DefaultCategories
import com.arango.base.ui.theme.BaseTheme
import com.arango.base.widget.BalanceQuickAddWidgetProvider
import com.arango.base.widget.CategoryShortcutsWidgetProvider
import com.arango.base.widget.QuickAddOverlayScreen
import com.arango.base.widget.WidgetIconRenderer
import com.arango.base.widget.WidgetUpdateHelper
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class QuickAddWidgetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var dao: FinanceDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.financeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun widgetSnapshot_calculatesRollingBalanceAndRespectsDisplayOrder() = runTest {
        dao.insertOffsetTransaction(OffsetTransactionEntity(amount = 500.0, note = "Initial"))
        dao.insertTransaction(
            TransactionEntity(
                title = "Paycheck",
                category = "Recurring Income",
                amount = 1000.0,
                isExpense = false,
                timestamp = 1000L
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                title = "Groceries",
                category = "Groceries",
                amount = 259.50,
                isExpense = true,
                timestamp = 2000L
            )
        )

        // Custom category order (Transport first, Dining Out second, Groceries third)
        dao.insertCategories(
            listOf(
                CategoryEntity(id = 1, name = "Groceries", iconKey = "local_grocery_store", colorHex = 0xFF2E7D32L, displayOrder = 2),
                CategoryEntity(id = 2, name = "Dining Out", iconKey = "restaurant", colorHex = 0xFFD35400L, displayOrder = 1),
                CategoryEntity(id = 3, name = "Transport", iconKey = "directions_subway", colorHex = 0xFFD48806L, displayOrder = 0)
            )
        )

        val snapshot = WidgetUpdateHelper.loadSnapshot(dao)
        assertEquals(1240.50, snapshot.rollingBalance, 0.001)
        assertFalse(snapshot.isBalanceMasked)
        assertEquals("$1,240.50", snapshot.formattedBalance)
        assertEquals(
            listOf("Transport", "Dining Out", "Groceries"),
            snapshot.topCategories.map { it.name }
        )
    }

    @Test
    fun widgetSnapshot_togglesBalanceMaskCorrectly() = runTest {
        dao.insertOffsetTransaction(OffsetTransactionEntity(amount = 845.25))

        val initial = WidgetUpdateHelper.loadSnapshot(dao)
        assertFalse(initial.isBalanceMasked)
        assertEquals("$845.25", initial.formattedBalance)

        // Toggle mask ON
        val isNowMasked = WidgetUpdateHelper.toggleBalanceMask(dao)
        assertTrue(isNowMasked)
        val maskedSnapshot = WidgetUpdateHelper.loadSnapshot(dao)
        assertTrue(maskedSnapshot.isBalanceMasked)
        assertEquals(WidgetUpdateHelper.MASKED_BALANCE_TEXT, maskedSnapshot.formattedBalance)

        // Toggle mask OFF
        val isNowUnmasked = WidgetUpdateHelper.toggleBalanceMask(dao)
        assertFalse(isNowUnmasked)
        val unmaskedSnapshot = WidgetUpdateHelper.loadSnapshot(dao)
        assertFalse(unmaskedSnapshot.isBalanceMasked)
        assertEquals("$845.25", unmaskedSnapshot.formattedBalance)
    }

    @Test
    fun balanceQuickAddWidget_rendersBalanceEyeToggleAndResponsiveShortcuts() = runTest {
        dao.insertOffsetTransaction(OffsetTransactionEntity(amount = 420.75))
        val snapshot = WidgetUpdateHelper.loadSnapshot(dao)

        // 1. Compact 2x1 mode
        val compactOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 55)
        }
        val compactRemoteViews = BalanceQuickAddWidgetProvider.buildRemoteViews(
            context = context,
            appWidgetId = 1,
            options = compactOptions,
            snapshot = snapshot
        )
        val parent = FrameLayout(context)
        val compactApplied = compactRemoteViews.apply(context, parent)

        val balanceText = compactApplied.findViewById<TextView>(R.id.widget_balance_amount)
        val eyeToggle = compactApplied.findViewById<View>(R.id.widget_eye_toggle_btn)
        val addBtn = compactApplied.findViewById<View>(R.id.widget_add_btn)
        val expandedSection = compactApplied.findViewById<View>(R.id.widget_expanded_section)

        assertEquals("$420.75", balanceText.text.toString())
        assertEquals(View.VISIBLE, eyeToggle.visibility)
        assertEquals(View.VISIBLE, addBtn.visibility)
        assertEquals(View.GONE, expandedSection.visibility)

        // 2. Expanded 4x2 mode with masked balance
        WidgetUpdateHelper.toggleBalanceMask(dao)
        val maskedSnapshot = WidgetUpdateHelper.loadSnapshot(dao)
        val expandedOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 130)
        }
        val expandedRemoteViews = BalanceQuickAddWidgetProvider.buildRemoteViews(
            context = context,
            appWidgetId = 1,
            options = expandedOptions,
            snapshot = maskedSnapshot
        )
        val expandedApplied = expandedRemoteViews.apply(context, parent)
        val maskedBalanceText = expandedApplied.findViewById<TextView>(R.id.widget_balance_amount)
        val expandedSectionNow = expandedApplied.findViewById<View>(R.id.widget_expanded_section)
        val cat1Label = expandedApplied.findViewById<TextView>(R.id.widget_cat_label_1)

        assertEquals(WidgetUpdateHelper.MASKED_BALANCE_TEXT, maskedBalanceText.text.toString())
        assertEquals(View.VISIBLE, expandedSectionNow.visibility)
        assertEquals("Groceries", cat1Label.text.toString())
    }

    @Test
    fun categoryShortcutsWidget_rendersTopCategoryIconsAndOptionalBalanceWithEyeToggle() = runTest {
        dao.insertOffsetTransaction(OffsetTransactionEntity(amount = 910.00))
        dao.insertCategories(
            listOf(
                CategoryEntity(id = 1, name = "Coffee", iconKey = "local_cafe", colorHex = 0xFFD35400L, displayOrder = 0),
                CategoryEntity(id = 2, name = "Transit", iconKey = "directions_subway", colorHex = 0xFFD48806L, displayOrder = 1),
                CategoryEntity(id = 3, name = "Groceries", iconKey = "local_grocery_store", colorHex = 0xFF2E7D32L, displayOrder = 2),
                CategoryEntity(id = 4, name = "Health", iconKey = "spa", colorHex = 0xFF16A085L, displayOrder = 3)
            )
        )
        val snapshot = WidgetUpdateHelper.loadSnapshot(dao)

        // 1. Standard 4x1 row (no balance row shown)
        val standardOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
        }
        val parent = FrameLayout(context)
        val standardViews = CategoryShortcutsWidgetProvider.buildRemoteViews(
            context = context,
            appWidgetId = 2,
            options = standardOptions,
            snapshot = snapshot
        ).apply(context, parent)

        assertEquals(View.GONE, standardViews.findViewById<View>(R.id.widget_shortcuts_balance_row).visibility)
        assertEquals(View.VISIBLE, standardViews.findViewById<View>(R.id.widget_add_btn).visibility)
        assertEquals("Coffee", standardViews.findViewById<TextView>(R.id.widget_cat_label_1).text.toString())
        assertEquals("Transit", standardViews.findViewById<TextView>(R.id.widget_cat_label_2).text.toString())
        assertEquals("Groceries", standardViews.findViewById<TextView>(R.id.widget_cat_label_3).text.toString())
        assertEquals("Health", standardViews.findViewById<TextView>(R.id.widget_cat_label_4).text.toString())

        // 2. Vertically expanded 4x2 mode (balance row shown -> must include eye toggle)
        val tallOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120)
        }
        val tallViews = CategoryShortcutsWidgetProvider.buildRemoteViews(
            context = context,
            appWidgetId = 2,
            options = tallOptions,
            snapshot = snapshot
        ).apply(context, parent)

        assertEquals(View.VISIBLE, tallViews.findViewById<View>(R.id.widget_shortcuts_balance_row).visibility)
        assertEquals("$910.00", tallViews.findViewById<TextView>(R.id.widget_balance_amount).text.toString())
        assertEquals(View.VISIBLE, tallViews.findViewById<View>(R.id.widget_eye_toggle_btn).visibility)
    }

    @Test
    fun widgetIconRenderer_rendersAllDefaultCategoryIconsToBitmaps() {
        for (cat in DefaultCategories.list) {
            val bitmap = WidgetIconRenderer.renderCategoryBadgeBitmap(
                icon = cat.icon,
                badgeColor = cat.color,
                sizePx = 64
            )
            assertNotNull(bitmap)
            assertEquals(64, bitmap.width)
            assertEquals(64, bitmap.height)
        }
    }

    @Test
    fun quickAddOverlayScreen_preselectsCategoryAndSavesOnKeyboardDone() {
        var savedTitle = ""
        var savedCategory = ""
        var savedAmount = 0.0
        var savedIsExpense = true
        var savedNote = ""

        composeTestRule.setContent {
            BaseTheme {
                QuickAddOverlayScreen(
                    categories = DefaultCategories.list,
                    initialCategoryName = "Transport",
                    initialIsExpense = true,
                    onDismiss = {},
                    onSave = { title, category, amount, isExpense, note ->
                        savedTitle = title
                        savedCategory = category
                        savedAmount = amount
                        savedIsExpense = isExpense
                        savedNote = note
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Save button is disabled when amount is empty
        composeTestRule.onNodeWithTag("quick_add_save_btn").assertIsNotEnabled()

        // Enter amount and trigger keyboard Done
        composeTestRule.onNodeWithTag("quick_add_amount_input").performTextInput("18.50")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quick_add_save_btn").assertIsEnabled()
        composeTestRule.onNodeWithText("Save Transaction (-$18.50)").assertIsDisplayed()

        // Trigger IME Done directly from the amount field
        composeTestRule.onNodeWithTag("quick_add_amount_input").performImeAction()
        composeTestRule.waitForIdle()

        assertEquals("Transport", savedTitle)
        assertEquals("Transport", savedCategory)
        assertEquals(18.50, savedAmount, 0.001)
        assertTrue(savedIsExpense)
        assertEquals("", savedNote)
    }

    @Test
    fun quickAddOverlayScreen_allowsIncomeToggleCategoryChangeAndNote() {
        var savedTitle = ""
        var savedCategory = ""
        var savedAmount = 0.0
        var savedIsExpense = true
        var savedNote = ""

        composeTestRule.setContent {
            BaseTheme {
                QuickAddOverlayScreen(
                    categories = DefaultCategories.list,
                    initialCategoryName = "Groceries",
                    initialIsExpense = true,
                    onDismiss = {},
                    onSave = { title, category, amount, isExpense, note ->
                        savedTitle = title
                        savedCategory = category
                        savedAmount = amount
                        savedIsExpense = isExpense
                        savedNote = note
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Switch to Income
        composeTestRule.onNodeWithTag("quick_add_toggle_income").performClick()
        // Select Dining Out chip
        composeTestRule.onNodeWithTag("quick_add_cat_chip_dining").performClick()
        // Enter amount and note
        composeTestRule.onNodeWithTag("quick_add_amount_input").performTextInput("45.00")
        composeTestRule.onNodeWithTag("quick_add_note_input").performTextInput("Split dinner refund")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save Transaction (+$45.00)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_add_save_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Split dinner refund", savedTitle)
        assertEquals("Dining Out", savedCategory)
        assertEquals(45.00, savedAmount, 0.001)
        assertFalse(savedIsExpense)
        assertEquals("Split dinner refund", savedNote)
    }
}
