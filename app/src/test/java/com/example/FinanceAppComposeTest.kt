package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
        composeTestRule.onNodeWithTag("settings_lazy_column").performScrollToIndex(6)
        composeTestRule.waitForIdle()

        // Click Reset All Local Data
        composeTestRule.onNodeWithTag("reset_data_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Dialog should be shown
        composeTestRule.onNodeWithText("Clear All Ledger & Balance?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
    }
}
