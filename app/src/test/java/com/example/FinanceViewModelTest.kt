package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AnalyticsPeriod
import com.example.ui.AppScreen
import com.example.ui.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        viewModel = FinanceViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun navigationChangesScreen() = runTest(testDispatcher) {
        assertEquals(AppScreen.LEDGER, viewModel.currentScreen.value)

        viewModel.navigateTo(AppScreen.ANALYTICS)
        assertEquals(AppScreen.ANALYTICS, viewModel.currentScreen.value)

        viewModel.navigateTo(AppScreen.SETTINGS)
        assertEquals(AppScreen.SETTINGS, viewModel.currentScreen.value)

        viewModel.navigateTo(AppScreen.LEDGER)
        assertEquals(AppScreen.LEDGER, viewModel.currentScreen.value)
    }

    @Test
    fun analyticsPeriodChanges() = runTest(testDispatcher) {
        assertEquals(AnalyticsPeriod.CURRENT_PAYCHECK, viewModel.analyticsPeriod.value)

        viewModel.setAnalyticsPeriod(AnalyticsPeriod.LAST_30_DAYS)
        assertEquals(AnalyticsPeriod.LAST_30_DAYS, viewModel.analyticsPeriod.value)
    }

    @Test
    fun modalSheetsOpenAndClose() = runTest(testDispatcher) {
        assertFalse(viewModel.isNewTransactionSheetOpen.value)
        viewModel.openNewTransactionSheet()
        assertTrue(viewModel.isNewTransactionSheetOpen.value)
        viewModel.closeNewTransactionSheet()
        assertFalse(viewModel.isNewTransactionSheetOpen.value)

        assertFalse(viewModel.isIncomeEngineOpen.value)
        viewModel.openIncomeEngine()
        assertTrue(viewModel.isIncomeEngineOpen.value)
        viewModel.closeIncomeEngine()
        assertFalse(viewModel.isIncomeEngineOpen.value)

        assertFalse(viewModel.isCategoryManagerOpen.value)
        viewModel.openCategoryManager()
        assertTrue(viewModel.isCategoryManagerOpen.value)
        viewModel.closeCategoryManager()
        assertFalse(viewModel.isCategoryManagerOpen.value)

        assertFalse(viewModel.isStartingBalanceDialogOpen.value)
        viewModel.openStartingBalanceDialog()
        assertTrue(viewModel.isStartingBalanceDialogOpen.value)
        viewModel.closeStartingBalanceDialog()
        assertFalse(viewModel.isStartingBalanceDialogOpen.value)
    }

    @Test
    fun toastNotificationHandling() = runTest(testDispatcher) {
        assertNull(viewModel.toastMessage.value)

        viewModel.showToast("Test alert message")
        assertEquals("Test alert message", viewModel.toastMessage.value)

        viewModel.clearToast()
        assertNull(viewModel.toastMessage.value)
    }

    private fun waitForCondition(timeoutMs: Long = 3000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition() && System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(25)
            testDispatcher.scheduler.advanceUntilIdle()
        }
        assertTrue("Condition not met within timeout", condition())
    }

    @Test
    fun addTransactionUpdatesStateAndShowsToast() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.addTransaction(
            title = "Fresh Apples",
            category = "Groceries",
            amount = 12.50,
            isExpense = true,
            note = "Crisp apples"
        )
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("12.50"))
        assertFalse(viewModel.isNewTransactionSheetOpen.value)
    }

    @Test
    fun updateStartingBalanceOffset() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateStartingBalanceOffset(150.0)
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("150.00"))
        assertFalse(viewModel.isStartingBalanceDialogOpen.value)
    }

    @Test
    fun updateIncomeEngine() = runTest(testDispatcher) {
        advanceUntilIdle()

        val nextDate = System.currentTimeMillis() + 86400000L * 7
        viewModel.updateIncomeEngine(
            expectedIncome = 3200.0,
            cadence = "Monthly",
            nextPaydayDate = nextDate
        )
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("Income Engine updated"))
        assertFalse(viewModel.isIncomeEngineOpen.value)
    }
}
