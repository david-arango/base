package com.arango.base

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.arango.base.data.local.OffsetTransactionEntity
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.DefaultCategories
import com.arango.base.ui.FinanceUiState
import com.arango.base.ui.components.CategoryEditorDialog
import com.arango.base.ui.components.CategoryManagerSheet
import com.arango.base.ui.components.NewTransactionSheet
import com.arango.base.ui.components.OffsetBudgetSheet
import com.arango.base.ui.screens.LedgerScreen
import com.arango.base.ui.theme.BaseTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FinanceAppComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @org.junit.Before
    fun setup() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        kotlinx.coroutines.runBlocking {
            com.arango.base.data.local.AppDatabase.getInstance(context).financeDao().setPreference(
                com.arango.base.data.local.AppPreferenceEntity("has_completed_onboarding", "true")
            )
        }
    }

    @Test
    fun bottomNavigation_switchesScreensCorrectly() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Verify Ledger is the initial screen
        composeTestRule.onNodeWithTag("nav_tab_ledger").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()

        // Navigate to Analytics screen
        composeTestRule.onNodeWithTag("nav_tab_analytics").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Analytics & Flow").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_current_paycheck").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_last_30_days").assertIsDisplayed()

        // Toggle period on Analytics
        composeTestRule.onNodeWithTag("tab_last_30_days").performClick()
        composeTestRule.waitForIdle()

        // Navigate to Settings screen
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_income_engine_row").assertIsDisplayed()

        // Return to Ledger
        composeTestRule.onNodeWithTag("nav_tab_ledger").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
    }

    @Test
    fun ledgerScreen_opensNewTransactionSheetAndInteracts() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Click FAB to open New Transaction Sheet
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.waitForIdle()

        // Check that New Transaction Sheet opened
        composeTestRule.onNodeWithTag("toggle_expense_tab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_income_tab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertIsDisplayed()

        // Switch to Income tab
        composeTestRule.onNodeWithTag("toggle_income_tab").performClick()
        composeTestRule.waitForIdle()

        // Close the sheet
        composeTestRule.onNodeWithTag("close_transaction_sheet_btn").performClick()
        composeTestRule.waitForIdle()

        // FAB is visible again on Ledger
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
    }

    @Test
    fun ledgerScreen_addNewTransactionAndSave() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Click FAB to open New Transaction Sheet
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.waitForIdle()

        // Enter amount 25 using native text input
        composeTestRule.onNodeWithTag("display_amount_text").performTextInput("25")
        composeTestRule.waitForIdle()

        // Select Dining Out category chip if present
        composeTestRule.onNodeWithTag("cat_chip_dining").performClick()
        composeTestRule.waitForIdle()

        // Click Save button
        composeTestRule.onNodeWithTag("save_transaction_button").performClick()
        composeTestRule.waitForIdle()

        // The sheet should dismiss and FAB should be visible again
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_resetDialogFlow() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Scroll down in Settings LazyColumn to reveal the reset button
        composeTestRule.onNodeWithTag("settings_lazy_column").performScrollToIndex(5)
        composeTestRule.waitForIdle()

        // Click Reset All Local Data
        composeTestRule.onNodeWithTag("reset_data_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Dialog should be shown
        composeTestRule.onNodeWithText("Clear All Ledger & Balance?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreen_incomeEngineSheetFlow() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Click Income Engine row
        composeTestRule.onNodeWithTag("settings_income_engine_row").performClick()
        composeTestRule.waitForIdle()

        // Assert sheet components
        composeTestRule.onNodeWithText("THE ENGINE", substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Income engine row should be visible again
        composeTestRule.onNodeWithTag("settings_income_engine_row").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_startingBalanceDialogFlow() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Scroll to Item 2 containing starting balance
        composeTestRule.onNodeWithTag("settings_lazy_column").performScrollToIndex(2)
        composeTestRule.waitForIdle()

        // Click Starting Balance Offset row
        composeTestRule.onNodeWithTag("settings_starting_balance_row").performClick()

        // Verify dialog / sheet
        composeTestRule.onNodeWithTag("starting_balance_display").assertIsDisplayed()

        // Dismiss sheet
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Starting balance row should be visible
        composeTestRule.onNodeWithTag("settings_starting_balance_row").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_categoryManagerSheetFlow() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Scroll to Item 2 containing categories row
        composeTestRule.onNodeWithTag("settings_lazy_column").performScrollToIndex(2)
        composeTestRule.waitForIdle()

        // Click Manage Categories row
        composeTestRule.onNodeWithTag("settings_categories_row").performClick()
        composeTestRule.waitForIdle()

        // Verify "Add New Category" button is displayed in sheet
        composeTestRule.onNodeWithTag("btn_add_category").assertIsDisplayed()

        // Dismiss sheet by clicking "Done" button
        composeTestRule.onNodeWithTag("done_category_manager_btn").performClick()
        composeTestRule.waitForIdle()

        // Back on Settings
        composeTestRule.onNodeWithTag("settings_categories_row").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_exportCsvFlow() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Scroll to export CSV button
        composeTestRule.onNodeWithTag("settings_lazy_column").performScrollToIndex(3)
        composeTestRule.waitForIdle()

        // Click Export CSV
        composeTestRule.onNodeWithTag("settings_export_csv_btn").performClick()
        composeTestRule.waitForIdle()

        // Settings row is still visible
        composeTestRule.onNodeWithTag("settings_export_csv_btn").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_importCsvFlow() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Scroll to import CSV button
        composeTestRule.onNodeWithTag("settings_lazy_column").performScrollToIndex(3)
        composeTestRule.waitForIdle()

        // Click Import CSV row
        composeTestRule.onNodeWithTag("settings_import_csv_btn").performClick()
        composeTestRule.waitForIdle()

        // Assert Import CSV Sheet opens
        composeTestRule.onNodeWithTag("import_csv_choose_file_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_csv_paste_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_csv_submit_btn").assertIsDisplayed()

        // Enter CSV data into paste field
        val sampleCsv = """
            ID,Date,Title,Category,Type,Amount,Note,Status
            1,"2026-03-20 11:00:00","Test Bakery","Dining Out","EXPENSE",12.50,"Lunch","Settled"
        """.trimIndent()
        composeTestRule.onNodeWithTag("import_csv_paste_input").performTextInput(sampleCsv)
        composeTestRule.waitForIdle()

        // Click Import button
        composeTestRule.onNodeWithTag("import_csv_submit_btn").performClick()
        composeTestRule.waitForIdle()

        // Verify toast or that sheet dismissed
        composeTestRule.onNodeWithTag("settings_import_csv_btn").assertIsDisplayed()
    }

    @Test
    fun categoryManagerSheet_rendersAndDismisses() {
        var dismissed = false

        composeTestRule.setContent {
            BaseTheme {
                CategoryManagerSheet(
                    isOpen = true,
                    categories = DefaultCategories.list,
                    onDismiss = { dismissed = true },
                    onCreateCategory = { _, _, _ -> },
                    onUpdateCategory = { _, _, _, _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_add_category").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_row_groceries").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_row_dining").assertIsDisplayed()

        // Click Done button
        composeTestRule.onNodeWithTag("done_category_manager_btn").performClick()
        assertTrue(dismissed)
    }

    @Suppress("DEPRECATION")
    @Test
    fun startingBalanceDialog_inputAndConfirmation() {
        var confirmedValue: Double? = null
        var dismissed = false

        composeTestRule.setContent {
            BaseTheme {
                com.arango.base.ui.components.StartingBalanceDialog(
                    isOpen = true,
                    currentOffset = 0.0,
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmedValue = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify input exists and enter 75.5
        composeTestRule.onNodeWithTag("starting_balance_display").assertIsDisplayed()
        composeTestRule.onNodeWithTag("starting_balance_display").performTextInput("75.5")
        composeTestRule.waitForIdle()

        // Toggle to negative balance
        composeTestRule.onNodeWithText("- Negative Balance").performClick()
        composeTestRule.waitForIdle()

        // Click Save Offset
        composeTestRule.onNodeWithTag("save_starting_balance_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(-75.5, confirmedValue!!, 0.001)
    }

    @Test
    fun newTransactionSheet_textInputHandling() {
        var savedAmount: Double? = null
        var savedNote: String? = null

        composeTestRule.setContent {
            BaseTheme {
                NewTransactionSheet(
                    isOpen = true,
                    onDismiss = {},
                    onSave = { _, _, amount, _, note, _ ->
                        savedAmount = amount
                        savedNote = note
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Enter amount and note using native text fields
        composeTestRule.onNodeWithTag("display_amount_text").performTextInput("42.50")
        composeTestRule.onNodeWithTag("note_input_field").performTextInput("Coffee run")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("save_transaction_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(42.50, savedAmount!!, 0.001)
        assertEquals("Coffee run", savedNote)
    }

    @Test
    fun editTransaction_loadsExistingDataAndSaves() {
        val originalTx = TransactionEntity(
            id = 42L,
            title = "Whole Foods Market",
            category = "Groceries",
            amount = 88.50,
            isExpense = true,
            timestamp = 1700000000000L,
            note = "Weekly organic groceries",
            statusText = "Settled"
        )

        var savedTitle: String? = null
        var savedCategory: String? = null
        var savedAmount: Double? = null
        var savedIsExpense: Boolean? = null
        var savedNote: String? = null

        composeTestRule.setContent {
            BaseTheme {
                NewTransactionSheet(
                    isOpen = true,
                    transactionToEdit = originalTx,
                    onDismiss = {},
                    onSave = { title, category, amount, isExpense, note, _ ->
                        savedTitle = title
                        savedCategory = category
                        savedAmount = amount
                        savedIsExpense = isExpense
                        savedNote = note
                    },
                    onDelete = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify title indicates Edit Transaction
        composeTestRule.onNodeWithText("Edit Transaction").assertIsDisplayed()

        // Verify delete button is displayed in header
        composeTestRule.onNodeWithTag("header_delete_transaction_btn").assertIsDisplayed()

        // Verify sheet delete button is displayed
        composeTestRule.onNodeWithTag("sheet_delete_button").assertIsDisplayed()

        // Verify Save button has Update Transaction label
        composeTestRule.onNodeWithTag("save_transaction_button").assertIsDisplayed()

        // Click save button
        composeTestRule.onNodeWithTag("save_transaction_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Weekly organic groceries", savedNote)
        assertEquals(true, savedIsExpense)
        assertTrue(savedAmount != null && savedAmount!! > 0)
    }

    @Test
    fun editTransaction_deleteConfirmationFlow() {
        val originalTx = TransactionEntity(
            id = 99L,
            title = "Old Subscription",
            category = "Utilities",
            amount = 14.99,
            isExpense = true,
            timestamp = 1700000000000L,
            note = "Streaming",
            statusText = "Settled"
        )

        var deletedId: Long? = null

        composeTestRule.setContent {
            BaseTheme {
                NewTransactionSheet(
                    isOpen = true,
                    transactionToEdit = originalTx,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _ -> },
                    onDelete = { deletedId = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Click delete button on the sheet
        composeTestRule.onNodeWithTag("sheet_delete_button").performClick()
        composeTestRule.waitForIdle()

        // Verify confirmation dialog is visible
        composeTestRule.onNodeWithText("Delete Transaction?").assertIsDisplayed()

        // Cancel first
        composeTestRule.onNodeWithTag("cancel_delete_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(null, deletedId)

        // Click header delete button
        composeTestRule.onNodeWithTag("header_delete_transaction_btn").performClick()
        composeTestRule.waitForIdle()

        // Confirm delete
        composeTestRule.onNodeWithTag("confirm_delete_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(99L, deletedId)
    }

    @Test
    fun ledgerScreen_rowClickTriggersEdit_andDeleteIconTriggersConfirmation() {
        val testTx = TransactionEntity(
            id = 101L,
            title = "Dinner with Team",
            category = "Dining Out",
            amount = 45.0,
            isExpense = true,
            timestamp = System.currentTimeMillis(),
            note = "Team dinner",
            statusText = "Settled"
        )

        var editedTx: TransactionEntity? = null
        var deletedId: Long? = null

        composeTestRule.setContent {
            BaseTheme {
                LedgerScreen(
                    uiState = FinanceUiState(
                        transactions = listOf(testTx)
                    ),
                    onOpenNewTransaction = {},
                    onEditTransaction = { editedTx = it },
                    onDeleteTransaction = { deletedId = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify transaction row is displayed
        composeTestRule.onNodeWithTag("transaction_row_101").assertIsDisplayed()

        // Tap on the row to trigger edit
        composeTestRule.onNodeWithTag("transaction_row_101").performClick()
        composeTestRule.waitForIdle()
        assertEquals(101L, editedTx?.id)

        // Tap the delete icon button on the row
        composeTestRule.onNodeWithTag("btn_delete_tx_101").performClick()
        composeTestRule.waitForIdle()

        // Verify ledger delete confirmation dialog appears
        composeTestRule.onNodeWithText("Delete Transaction?").assertIsDisplayed()

        // Confirm delete
        composeTestRule.onNodeWithTag("ledger_confirm_delete_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(101L, deletedId)
    }

    @Test
    fun offsetBudgetSheet_multipleOffsetsDisplayedAndHandled() {
        val testOffsets = listOf(
            OffsetTransactionEntity(id = 1, amount = 200.0, note = "Starting Balance", timestamp = System.currentTimeMillis()),
            OffsetTransactionEntity(id = 2, amount = -50.0, note = "Cash expense", timestamp = System.currentTimeMillis() - 60000)
        )
        var addedAmount: Double? = null
        var addedNote: String? = null
        var deletedId: Long? = null

        composeTestRule.setContent {
            BaseTheme {
                OffsetBudgetSheet(
                    isOpen = true,
                    currentOffset = 150.0,
                    offsetTransactions = testOffsets,
                    onDismiss = {},
                    onAddOffset = { amount, note ->
                        addedAmount = amount
                        addedNote = note
                    },
                    onDeleteOffset = { id -> deletedId = id }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Both offset rows should be displayed in the list
        composeTestRule.onNodeWithTag("offset_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offset_row_2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Starting Balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cash expense").assertIsDisplayed()

        // Delete offset 1
        composeTestRule.onNodeWithTag("delete_offset_btn_1").performClick()
        assertEquals(1L, deletedId)

        // Add a new positive offset with note
        composeTestRule.onNodeWithTag("starting_balance_display").performTextInput("75.0")
        composeTestRule.onNodeWithTag("offset_note_input").performTextInput("Gift money")
        composeTestRule.onNodeWithTag("save_starting_balance_btn").performClick()

        assertEquals(75.0, addedAmount!!, 0.001)
        assertEquals("Gift money", addedNote)
    }

    @Test
    fun offsetBudgetSheet_negativeOffsetWithToggle() {
        var addedAmount: Double? = null
        var addedNote: String? = null

        composeTestRule.setContent {
            BaseTheme {
                OffsetBudgetSheet(
                    isOpen = true,
                    currentOffset = 0.0,
                    offsetTransactions = emptyList(),
                    onDismiss = {},
                    onAddOffset = { amount, note ->
                        addedAmount = amount
                        addedNote = note
                    },
                    onDeleteOffset = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("starting_balance_display").performTextInput("120.50")
        composeTestRule.onNodeWithText("- Negative Balance").performClick()
        composeTestRule.onNodeWithTag("save_starting_balance_btn").performClick()

        assertEquals(-120.50, addedAmount!!, 0.001)
        assertEquals("Offset adjustment", addedNote)
    }

    @Test
    fun settingsScreen_restoreDemoDataIsHidden() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Verify Settings screen is displayed
        composeTestRule.onNodeWithTag("settings_income_engine_row").assertIsDisplayed()
        // Verify Restore Demo Data option is hidden from Settings
        composeTestRule.onAllNodesWithTag("settings_restore_demo_row").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Restore Demo Data").assertCountEquals(0)

        // Verify Balance Offset row is displayed and "Offset Budget" is replaced
        composeTestRule.onNodeWithText("Balance Offset").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Offset Budget").assertCountEquals(0)

        // Open Balance Offset sheet and verify title
        composeTestRule.onNodeWithTag("settings_starting_balance_row").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("starting_balance_display").assertIsDisplayed()
    }

    @Test
    fun welcomeOverlayCard_displayedOnFreshLaunch_andDismissesOnExplore() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.arango.base.data.local.AppDatabase.getInstance(app)
        val repository = com.arango.base.data.repository.FinanceRepository(app, db.financeDao())
        kotlinx.coroutines.runBlocking {
            repository.setCompletedOnboarding(false)
        }
        val testVm = com.arango.base.ui.FinanceViewModel(app, repository, kotlinx.coroutines.flow.SharingStarted.Eagerly)

        composeTestRule.setContent {
            BaseTheme {
                BaseApp(viewModel = testVm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("welcome_explore_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("welcome_help_setup_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_explore_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcome_explore_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("welcome_explore_button").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onAllNodesWithTag("welcome_explore_button").assertCountEquals(0)
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
    }

    @Test
    fun welcomeOverlayCard_helpMeSetup_walksThroughWizardAndCompletes() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.arango.base.data.local.AppDatabase.getInstance(app)
        val repository = com.arango.base.data.repository.FinanceRepository(app, db.financeDao())
        kotlinx.coroutines.runBlocking {
            repository.setCompletedOnboarding(false)
        }
        val testVm = com.arango.base.ui.FinanceViewModel(app, repository, kotlinx.coroutines.flow.SharingStarted.Eagerly)

        composeTestRule.setContent {
            BaseTheme {
                BaseApp(viewModel = testVm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithTag("welcome_help_setup_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("welcome_help_setup_button").performClick()
        composeTestRule.waitForIdle()

        // Wizard is opened on Page 0
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithTag("onboarding_wizard_surface").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("onboarding_wizard_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_philosophy_continue_button").assertIsDisplayed()

        // Advance to Page 1 (Balance)
        composeTestRule.onNodeWithTag("onboarding_philosophy_continue_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_starting_balance_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_starting_balance_field").performTextInput("1250")
        composeTestRule.waitForIdle()

        // Advance to Page 2 (Income)
        composeTestRule.onNodeWithTag("onboarding_balance_continue_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_income_amount_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_income_amount_field").performTextInput("2400")
        composeTestRule.waitForIdle()

        // Advance to Page 3 (All Set)
        composeTestRule.onNodeWithTag("onboarding_income_continue_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_start_tracking_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_start_tracking_button").performClick()
        composeTestRule.waitForIdle()

        // Wizard dismissed, back on Ledger
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithTag("onboarding_wizard_surface").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onAllNodesWithTag("onboarding_wizard_surface").assertCountEquals(0)
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_revisitOnboarding_opensWizard() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Click Revisit Onboarding row
        composeTestRule.onNodeWithTag("settings_revisit_onboarding_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_revisit_onboarding_btn").performClick()
        composeTestRule.waitForIdle()

        // Verify Onboarding Wizard is displayed
        composeTestRule.onNodeWithTag("onboarding_wizard_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_close_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("onboarding_wizard_surface").assertCountEquals(0)
    }
}
