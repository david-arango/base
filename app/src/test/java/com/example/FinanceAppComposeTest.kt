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
import com.example.data.model.DefaultCategories
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.CategoryManagerSheet
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
    fun settingsScreen_restoreDemoDataDialogFlow() {
        composeTestRule.setContent {
            MyApplicationTheme {
                BaseApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Scroll to restore button
        composeTestRule.onNodeWithTag("settings_lazy_column").performScrollToIndex(5)
        composeTestRule.waitForIdle()

        // Click restore demo data
        composeTestRule.onNodeWithTag("restore_demo_data_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Dialog should appear
        composeTestRule.onNodeWithText("Load Demo Sample Data?").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("The Engine").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip for now").performClick()
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
}
