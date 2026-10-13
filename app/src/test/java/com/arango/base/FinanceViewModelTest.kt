package com.arango.base

import android.app.Application
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.IncomeEngineEntity
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.repository.FinanceRepository
import com.arango.base.ui.AnalyticsPeriod
import com.arango.base.ui.AppScreen
import com.arango.base.ui.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(application, db.financeDao())
        viewModel = FinanceViewModel(application, repository, SharingStarted.Eagerly)
        waitForCondition { repository.allTransactions.first().isNotEmpty() }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun waitForCondition(timeoutMs: Long = 3000, condition: suspend () -> Boolean) = kotlinx.coroutines.runBlocking {
        val start = System.currentTimeMillis()
        var met = false
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) {
                met = true
                break
            }
            Thread.sleep(20)
            testDispatcher.scheduler.advanceTimeBy(50)
            testDispatcher.scheduler.runCurrent()
        }
        assertTrue("Condition not met within timeout", met)
    }

    @Test
    fun navigateTo_changesScreens() = runTest(testDispatcher) {
        assertEquals(AppScreen.LEDGER, viewModel.currentScreen.value)

        viewModel.navigateTo(AppScreen.ANALYTICS)
        assertEquals(AppScreen.ANALYTICS, viewModel.currentScreen.value)

        viewModel.navigateTo(AppScreen.SETTINGS)
        assertEquals(AppScreen.SETTINGS, viewModel.currentScreen.value)

        viewModel.navigateTo(AppScreen.LEDGER)
        assertEquals(AppScreen.LEDGER, viewModel.currentScreen.value)
    }

    @Test
    fun setAnalyticsPeriod_updatesPeriodAndUiState() = runTest(testDispatcher) {
        assertEquals(AnalyticsPeriod.CURRENT_PAYCHECK, viewModel.analyticsPeriod.value)

        viewModel.setAnalyticsPeriod(AnalyticsPeriod.LAST_30_DAYS)
        assertEquals(AnalyticsPeriod.LAST_30_DAYS, viewModel.analyticsPeriod.value)

        viewModel.setAnalyticsPeriod(AnalyticsPeriod.CURRENT_PAYCHECK)
        assertEquals(AnalyticsPeriod.CURRENT_PAYCHECK, viewModel.analyticsPeriod.value)
    }

    @Test
    fun modalSheets_openAndCloseFlags() = runTest(testDispatcher) {
        // New Transaction Sheet
        assertFalse(viewModel.isNewTransactionSheetOpen.value)
        viewModel.openNewTransactionSheet()
        assertTrue(viewModel.isNewTransactionSheetOpen.value)
        viewModel.closeNewTransactionSheet()
        assertFalse(viewModel.isNewTransactionSheetOpen.value)

        // Income Engine Sheet
        assertFalse(viewModel.isIncomeEngineOpen.value)
        viewModel.openIncomeEngine()
        assertTrue(viewModel.isIncomeEngineOpen.value)
        viewModel.closeIncomeEngine()
        assertFalse(viewModel.isIncomeEngineOpen.value)

        // Category Manager Sheet
        assertFalse(viewModel.isCategoryManagerOpen.value)
        viewModel.openCategoryManager()
        assertTrue(viewModel.isCategoryManagerOpen.value)
        viewModel.closeCategoryManager()
        assertFalse(viewModel.isCategoryManagerOpen.value)

        // Starting Balance Dialog
        assertFalse(viewModel.isStartingBalanceDialogOpen.value)
        viewModel.openStartingBalanceDialog()
        assertTrue(viewModel.isStartingBalanceDialogOpen.value)
        viewModel.closeStartingBalanceDialog()
        assertFalse(viewModel.isStartingBalanceDialogOpen.value)
    }

    @Test
    fun showToastAndClearToast() = runTest(testDispatcher) {
        assertNull(viewModel.toastMessage.value)

        viewModel.showToast("Test notification")
        assertEquals("Test notification", viewModel.toastMessage.value)

        viewModel.clearToast()
        assertNull(viewModel.toastMessage.value)
    }

    @Test
    fun addTransaction_withCustomTitleExpense() = runTest(testDispatcher) {
        viewModel.openNewTransactionSheet()
        assertTrue(viewModel.isNewTransactionSheetOpen.value)

        viewModel.addTransaction(
            title = "Organic Berries",
            category = "Groceries",
            amount = 8.50,
            isExpense = true,
            note = "Fresh blueberries"
        )
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("-$8.50"))
        assertFalse(viewModel.isNewTransactionSheetOpen.value)

        val txs = repository.allTransactions.first()
        val created = txs.firstOrNull { it.title == "Organic Berries" }
        assertNotNull(created)
        assertEquals("Groceries", created!!.category)
        assertEquals(8.50, created.amount, 0.001)
        assertTrue(created.isExpense)
        assertEquals("Settled", created.statusText)
    }

    @Test
    fun addTransaction_withBlankTitleFallsBackToCategory() = runTest(testDispatcher) {
        viewModel.addTransaction(
            title = "   ",
            category = "Dining Out",
            amount = 14.20,
            isExpense = true,
            note = "Lunch"
        )
        waitForCondition { viewModel.toastMessage.value != null }

        val txs = repository.allTransactions.first()
        val created = txs.firstOrNull { it.amount == 14.20 }
        assertNotNull(created)
        assertEquals("Dining Out", created!!.title)
    }

    @Test
    fun addTransaction_incomeTransaction() = runTest(testDispatcher) {
        viewModel.addTransaction(
            title = "Freelance Consulting",
            category = "Income",
            amount = 600.00,
            isExpense = false,
            note = "Client payment"
        )
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("+$600.00"))

        val txs = repository.allTransactions.first()
        val created = txs.firstOrNull { it.title == "Freelance Consulting" }
        assertNotNull(created)
        assertFalse(created!!.isExpense)
        assertEquals("Deposited", created.statusText)
    }

    @Test
    fun deleteTransaction_removesFromRepositoryAndShowsToast() = runTest(testDispatcher) {
        val id = repository.insertTransaction(
            TransactionEntity(
                title = "To Delete",
                category = "Shopping",
                amount = 25.00,
                isExpense = true,
                timestamp = System.currentTimeMillis()
            )
        )
        waitForCondition { repository.allTransactions.first().any { it.id == id } }

        viewModel.deleteTransaction(id)
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("Transaction removed"))

        waitForCondition { repository.allTransactions.first().none { it.id == id } }
        val txsAfter = repository.allTransactions.first()
        assertFalse(txsAfter.any { it.id == id })
    }

    @Test
    fun updateIncomeEngine_persistsDataAndShowsToast() = runTest(testDispatcher) {
        viewModel.openIncomeEngine()
        assertTrue(viewModel.isIncomeEngineOpen.value)

        val nextDate = System.currentTimeMillis() + 86400000L * 5
        viewModel.updateIncomeEngine(
            expectedIncome = 2800.0,
            cadence = "Monthly",
            nextPaydayDate = nextDate
        )
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("Income Engine updated"))
        assertFalse(viewModel.isIncomeEngineOpen.value)

        val config = repository.incomeEngineConfig.first()
        assertNotNull(config)
        assertEquals(2800.0, config!!.expectedIncome, 0.001)
        assertEquals("Monthly", config.cadence)
        assertEquals(nextDate, config.nextPaydayTimestamp)
    }

    @Test
    fun updateStartingBalanceOffset_persistsAndClosesDialog() = runTest(testDispatcher) {
        viewModel.openStartingBalanceDialog()
        assertTrue(viewModel.isStartingBalanceDialogOpen.value)

        viewModel.updateStartingBalanceOffset(420.50)
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("420.50"))
        assertFalse(viewModel.isStartingBalanceDialogOpen.value)

        val offset = repository.startingBalanceOffset.first()
        assertEquals(420.50, offset, 0.001)
    }

    @Test
    fun addMultipleOffsetTransactions_logsAllOffsetsAndUpdatesBalance() = runTest(testDispatcher) {
        waitForCondition { viewModel.uiState.value.transactions.isNotEmpty() }
        repository.clearAllOffsetTransactions()
        waitForCondition { viewModel.uiState.value.offsetTransactions.isEmpty() }

        val initialTransactionsCount = viewModel.uiState.value.transactions.size

        // Add first offset
        viewModel.addOffsetTransaction(100.0, "Bonus check")
        waitForCondition { viewModel.uiState.value.offsetTransactions.size == 1 }

        assertEquals(1, viewModel.uiState.value.offsetTransactions.size)
        assertEquals(100.0, viewModel.uiState.value.startingBalanceOffset, 0.001)

        // Add second offset (negative adjustment)
        viewModel.addOffsetTransaction(-35.0, "Cash withdrawal")
        waitForCondition { viewModel.uiState.value.offsetTransactions.size == 2 }

        assertEquals(2, viewModel.uiState.value.offsetTransactions.size)
        assertEquals(65.0, viewModel.uiState.value.startingBalanceOffset, 0.001)

        // Add third offset
        viewModel.addOffsetTransaction(50.0, "Refund")
        waitForCondition { viewModel.uiState.value.offsetTransactions.size == 3 }

        assertEquals(3, viewModel.uiState.value.offsetTransactions.size)
        assertEquals(115.0, viewModel.uiState.value.startingBalanceOffset, 0.001)

        // Offsets should NOT be in the transactions ledger
        assertEquals(initialTransactionsCount, viewModel.uiState.value.transactions.size)
        assertFalse(viewModel.uiState.value.transactions.any { it.note == "Bonus check" || it.note == "Cash withdrawal" })

        // Delete one offset transaction
        val offsetToDelete = viewModel.uiState.value.offsetTransactions.first { it.amount == -35.0 }
        viewModel.deleteOffsetTransaction(offsetToDelete.id)
        waitForCondition { viewModel.uiState.value.offsetTransactions.size == 2 }

        assertEquals(2, viewModel.uiState.value.offsetTransactions.size)
        assertEquals(150.0, viewModel.uiState.value.startingBalanceOffset, 0.001)
    }

    @Test
    fun resetAllData_clearsStateAndShowsToast() = runTest(testDispatcher) {
        repository.insertTransaction(
            TransactionEntity(
                title = "Sample Item",
                category = "Groceries",
                amount = 50.0,
                isExpense = true,
                timestamp = System.currentTimeMillis()
            )
        )
        repository.setStartingBalanceOffset(300.0)

        viewModel.resetAllData()
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("Database cleared"))
        val txs = repository.allTransactions.first()
        assertTrue(txs.isEmpty())
        val offset = repository.startingBalanceOffset.first()
        assertEquals(0.0, offset, 0.001)
    }

    @Test
    fun restoreDemoData_repopulatesTransactionsAndShowsToast() = runTest(testDispatcher) {
        repository.resetAllData()
        waitForCondition { repository.allTransactions.first().isEmpty() }

        viewModel.restoreDemoData()
        waitForCondition { viewModel.toastMessage.value != null }

        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value!!.contains("Demo sample data restored"))
        val txs = repository.allTransactions.first()
        assertTrue(txs.isNotEmpty())
    }

    @Test
    fun exportCsv_triggersAndroidShareSheet() = runTest(testDispatcher) {
        repository.insertTransaction(
            TransactionEntity(
                title = "Coffee",
                category = "Dining Out",
                amount = 4.50,
                isExpense = true,
                timestamp = 1000L
            )
        )
        waitForCondition { repository.allTransactions.first().any { it.title == "Coffee" } }
        val shadowApp = shadowOf(application)

        viewModel.exportCsv()
        waitForCondition { shadowApp.peekNextStartedActivity() != null }

        val nextIntent = shadowApp.nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(Intent.ACTION_CHOOSER, nextIntent.action)
        @Suppress("DEPRECATION")
        val extraIntent = nextIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(extraIntent)
        assertEquals(Intent.ACTION_SEND, extraIntent!!.action)
        assertEquals("text/csv", extraIntent.type)
        val csvContent = extraIntent.getStringExtra(Intent.EXTRA_TEXT)
        assertNotNull(csvContent)
        assertTrue(csvContent!!.contains("Coffee"))
    }

    @Test
    fun uiState_rankedBreakdownAndPeriodCalculations() = runTest(testDispatcher) {
        repository.resetAllData()
        waitForCondition { repository.allTransactions.first().isEmpty() }
        val now = System.currentTimeMillis()

        // Insert groceries expense and dining out expense
        repository.insertTransaction(
            TransactionEntity(
                title = "Grocery item",
                category = "Groceries",
                amount = 100.0,
                isExpense = true,
                timestamp = now - 1000
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Dinner",
                category = "Dining Out",
                amount = 50.0,
                isExpense = true,
                timestamp = now - 2000
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Salary",
                category = "Recurring Income",
                amount = 2000.0,
                isExpense = false,
                timestamp = now - 3000
            )
        )

        waitForCondition { viewModel.uiState.value.transactions.size == 3 }

        val state = viewModel.uiState.value
        assertEquals(2000.0, state.periodIn, 0.001)
        assertEquals(150.0, state.periodOut, 0.001)
        assertEquals(1850.0, state.rollingBalance, 0.001)

        // Ranked breakdown should rank Groceries first, then Dining Out
        assertEquals(2, state.rankedBreakdown.size)
        assertEquals("Groceries", state.rankedBreakdown[0].category.name)
        assertEquals(100.0, state.rankedBreakdown[0].totalAmount, 0.001)
        assertEquals("Dining Out", state.rankedBreakdown[1].category.name)
        assertEquals(50.0, state.rankedBreakdown[1].totalAmount, 0.001)

        // Test pace note in 30-day view
        viewModel.setAnalyticsPeriod(AnalyticsPeriod.LAST_30_DAYS)
        waitForCondition { viewModel.uiState.value.currentPeriod == AnalyticsPeriod.LAST_30_DAYS }

        val state30 = viewModel.uiState.value
        assertTrue(state30.burnPaceNote.contains("30-day average expenditure"))
    }

    @Test
    fun category_viewModelCrudOperations() = runTest(testDispatcher) {
        // Create Category
        viewModel.createCategory(
            name = "Education",
            iconKey = "school",
            colorHex = 0xFF336699L
        )
        waitForCondition { viewModel.uiState.value.categories.any { it.name == "Education" } }
        assertTrue(viewModel.uiState.value.categories.any { it.name == "Education" })
        assertEquals("Category \"Education\" created", viewModel.toastMessage.value)

        val eduCategory = viewModel.uiState.value.categories.first { it.name == "Education" }

        // Update Category
        viewModel.updateCategory(
            id = eduCategory.dbId,
            name = "Courses & Books",
            iconKey = "school",
            colorHex = 0xFF336699L,
            oldName = "Education"
        )
        waitForCondition { viewModel.uiState.value.categories.any { it.name == "Courses & Books" } }
        assertTrue(viewModel.uiState.value.categories.any { it.name == "Courses & Books" })
        assertEquals("Category updated", viewModel.toastMessage.value)

        val updatedCat = viewModel.uiState.value.categories.first { it.name == "Courses & Books" }

        // Delete Category
        viewModel.deleteCategory(
            id = updatedCat.dbId,
            name = updatedCat.name
        )
        waitForCondition { viewModel.uiState.value.categories.none { it.name == "Courses & Books" } }
        assertTrue(viewModel.uiState.value.categories.none { it.name == "Courses & Books" })
        assertEquals("Category \"Courses & Books\" deleted", viewModel.toastMessage.value)
    }

    @Test
    fun updateIncomeEngine_whenPaydayIsTodayOrPast_automaticallyTriggersAndUpdatesLedger() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val initialTxCount = repository.allTransactions.first().size

        // Setting next payday to today should immediately trigger deposit
        viewModel.updateIncomeEngine(
            expectedIncome = 2250.0,
            cadence = "Bi-Weekly",
            nextPaydayDate = now
        )

        waitForCondition { repository.allTransactions.first().size > initialTxCount }
        val allTxs = repository.allTransactions.first()
        val autoIncomeTx = allTxs.find { it.isAuto && it.amount == 2250.0 }
        assertNotNull(autoIncomeTx)
        assertEquals("Bi-Weekly Paycheck", autoIncomeTx!!.title)
        assertEquals("Paycheck", autoIncomeTx.note)
        assertEquals(false, autoIncomeTx.isExpense)

        // Ledger UI state should contain this transaction
        waitForCondition { viewModel.uiState.value.transactions.any { it.id == autoIncomeTx.id } }
        assertTrue(viewModel.uiState.value.transactions.any { it.id == autoIncomeTx.id })
    }

    @Test
    fun triggerIncomeNow_creditsLedgerImmediately() = runTest(testDispatcher) {
        // First ensure income engine has an expected amount
        repository.updateIncomeEngine(
            IncomeEngineEntity(
                id = 1,
                expectedIncome = 1900.0,
                cadence = "Weekly",
                nextPaydayTimestamp = System.currentTimeMillis() + 86400000L * 3,
                isActive = true
            )
        )

        val initialTxCount = repository.allTransactions.first().size
        viewModel.triggerIncomeNow()

        waitForCondition { repository.allTransactions.first().size > initialTxCount }
        val allTxs = repository.allTransactions.first()
        val triggered = allTxs.find { it.isAuto && it.amount == 1900.0 }
        assertNotNull(triggered)
        assertEquals("Weekly Paycheck", triggered!!.title)

        waitForCondition { viewModel.toastMessage.value != null }
        assertTrue(viewModel.toastMessage.value!!.contains("1,900.00"))
    }
}
