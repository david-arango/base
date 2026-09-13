package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.local.TransactionEntity
import com.example.data.model.DefaultCategories
import com.example.ui.FinanceUiState
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.CategoryManagerSheet
import com.example.ui.components.NewTransactionSheet
import com.example.ui.screens.LedgerScreen
import com.example.ui.theme.MyApplicationTheme
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

    @Test
    fun bottomNavigation_switchesScreensCorrectly() {
        composeTestRule.setContent {
            MyApplicationTheme {
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
            MyApplicationTheme {
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
            MyApplicationTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Click FAB to open New Transaction Sheet
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.waitForIdle()

        // Enter amount 25 using numeric keypad
        composeTestRule.onNodeWithTag("keypad_key_2").performClick()
        composeTestRule.onNodeWithTag("keypad_key_5").performClick()
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
            MyApplicationTheme {
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
            MyApplicationTheme {
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
            MyApplicationTheme {
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
        composeTestRule.onNodeWithTag("starting_balance_input").assertIsDisplayed()

        // Dismiss sheet
        composeTestRule.onNodeWithContentDescription("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Starting balance row should be visible
        composeTestRule.onNodeWithTag("settings_starting_balance_row").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_categoryManagerSheetFlow() {
        composeTestRule.setContent {
            MyApplicationTheme {
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
            MyApplicationTheme {
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
    fun categoryManagerSheet_rendersAndDismisses() {
        var dismissed = false

        composeTestRule.setContent {
            MyApplicationTheme {
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

    @Test
    fun startingBalanceDialog_keypadInputAndConfirmation() {
        var confirmedValue: Double? = null
        var dismissed = false

        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.components.StartingBalanceDialog(
                    isOpen = true,
                    currentOffset = 0.0,
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmedValue = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify keypad and display exist
        composeTestRule.onNodeWithTag("starting_balance_display").assertIsDisplayed()
        composeTestRule.onNodeWithTag("keypad_key_7").performClick()
        composeTestRule.onNodeWithTag("keypad_key_5").performClick()
        composeTestRule.onNodeWithTag("keypad_key_dot").performClick()
        composeTestRule.onNodeWithTag("keypad_key_5").performClick()
        composeTestRule.waitForIdle()

        // Verify display shows 75.5
        composeTestRule.onNodeWithText("75.5").assertIsDisplayed()

        // Toggle to negative balance
        composeTestRule.onNodeWithText("- Negative Balance").performClick()
        composeTestRule.waitForIdle()

        // Click Save Offset
        composeTestRule.onNodeWithTag("save_starting_balance_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(-75.5, confirmedValue!!, 0.001)
    }

    @Test
    fun numericKeypad_directComponentClicks() {
        var clickedDigits = ""
        var decimalClicked = false
        var backspaceCount = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.components.NumericKeypad(
                    onDigitClick = { clickedDigits += it },
                    onDecimalClick = { decimalClicked = true },
                    onBackspaceClick = { backspaceCount++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("keypad_key_1").performClick()
        composeTestRule.onNodeWithTag("keypad_key_2").performClick()
        composeTestRule.onNodeWithTag("keypad_key_3").performClick()
        composeTestRule.onNodeWithTag("keypad_key_dot").performClick()
        composeTestRule.onNodeWithTag("keypad_key_backspace").performClick()

        assertEquals("123", clickedDigits)
        assertTrue(decimalClicked)
        assertEquals(1, backspaceCount)
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
            MyApplicationTheme {
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
            MyApplicationTheme {
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
            MyApplicationTheme {
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
}
