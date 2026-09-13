package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.repository.FinanceRepository
import com.example.ui.FinanceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceCalculationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            application,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = FinanceRepository(application, database.financeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testFromZero_mathAlwaysAddsUpSequentially() = runBlocking {
        // Ensure starting from clean 0
        repository.resetAllData()
        val initialTxs = repository.allTransactions.first()
        assertTrue(initialTxs.isEmpty())
        val initialOffset = repository.startingBalanceOffset.first()
        assertEquals(0.0, initialOffset, 0.001)

        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        val cutoff = now - (14L * dayMillis)

        // Helper function matching ViewModel's calculation
        fun computeTotals(txs: List<TransactionEntity>, offset: Double): Triple<Double, Double, Double> {
            val periodTxs = txs.filter { it.timestamp >= cutoff }
            val periodIn = periodTxs.filter { !it.isExpense }.sumOf { it.amount }
            val periodOut = periodTxs.filter { it.isExpense }.sumOf { it.amount }
            val totalIncome = txs.filter { !it.isExpense }.sumOf { it.amount }
            val totalExpenses = txs.filter { it.isExpense }.sumOf { it.amount }
            val balance = offset + totalIncome - totalExpenses
            return Triple(periodIn, periodOut, balance)
        }

        // 0. Verify zero state
        var (periodIn, periodOut, balance) = computeTotals(initialTxs, initialOffset)
        assertEquals(0.0, periodIn, 0.001)
        assertEquals(0.0, periodOut, 0.001)
        assertEquals(0.0, balance, 0.001)
        assertEquals(balance, initialOffset + periodIn - periodOut, 0.001)

        // 1. Add Income $2,000.00
        repository.insertTransaction(
            TransactionEntity(
                title = "Bi-Weekly Paycheck",
                category = "Income",
                amount = 2000.00,
                isExpense = false,
                timestamp = now,
                note = "Direct deposit"
            )
        )
        var txs = repository.allTransactions.first()
        assertEquals(1, txs.size)
        var totals = computeTotals(txs, 0.0)
        assertEquals(2000.00, totals.first, 0.001) // periodIn
        assertEquals(0.00, totals.second, 0.001)   // periodOut
        assertEquals(2000.00, totals.third, 0.001) // balance
        assertEquals(totals.third, totals.first - totals.second, 0.001) // balance == in - out

        // 2. Add Expense $65.50 (Groceries)
        repository.insertTransaction(
            TransactionEntity(
                title = "Trader Joe's",
                category = "Groceries",
                amount = 65.50,
                isExpense = true,
                timestamp = now + 1000,
                note = "Organic produce"
            )
        )
        txs = repository.allTransactions.first()
        assertEquals(2, txs.size)
        totals = computeTotals(txs, 0.0)
        assertEquals(2000.00, totals.first, 0.001) // periodIn
        assertEquals(65.50, totals.second, 0.001)   // periodOut
        assertEquals(1934.50, totals.third, 0.001) // balance: 2000 - 65.50
        assertEquals(totals.third, totals.first - totals.second, 0.001)

        // 3. Add Expense $120.00 (Utilities)
        repository.insertTransaction(
            TransactionEntity(
                title = "Electric utility",
                category = "Utilities",
                amount = 120.00,
                isExpense = true,
                timestamp = now + 2000,
                note = "Power"
            )
        )
        txs = repository.allTransactions.first()
        assertEquals(3, txs.size)
        totals = computeTotals(txs, 0.0)
        assertEquals(2000.00, totals.first, 0.001) // periodIn
        assertEquals(185.50, totals.second, 0.001)  // periodOut: 65.50 + 120.00
        assertEquals(1814.50, totals.third, 0.001) // balance: 2000 - 185.50
        assertEquals(totals.third, totals.first - totals.second, 0.001)

        // 4. Add additional Income $350.00 (Side gig)
        repository.insertTransaction(
            TransactionEntity(
                title = "Design Gig",
                category = "Income",
                amount = 350.00,
                isExpense = false,
                timestamp = now + 3000,
                note = "Freelance"
            )
        )
        txs = repository.allTransactions.first()
        assertEquals(4, txs.size)
        totals = computeTotals(txs, 0.0)
        assertEquals(2350.00, totals.first, 0.001) // periodIn: 2000 + 350
        assertEquals(185.50, totals.second, 0.001)  // periodOut
        assertEquals(2164.50, totals.third, 0.001) // balance: 2350 - 185.50
        assertEquals(totals.third, totals.first - totals.second, 0.001)

        // 5. Delete an Expense
        val groceryTx = txs.first { it.title == "Trader Joe's" }
        repository.deleteTransaction(groceryTx.id)
        txs = repository.allTransactions.first()
        assertEquals(3, txs.size)
        totals = computeTotals(txs, 0.0)
        assertEquals(2350.00, totals.first, 0.001) // periodIn unchanged
        assertEquals(120.00, totals.second, 0.001)  // periodOut reduced by 65.50
        assertEquals(2230.00, totals.third, 0.001) // balance increases: 2350 - 120
        assertEquals(totals.third, totals.first - totals.second, 0.001)

        // 6. Set Starting Balance Offset to $500.00
        repository.setStartingBalanceOffset(500.00)
        val offset = repository.startingBalanceOffset.first()
        assertEquals(500.00, offset, 0.001)
        totals = computeTotals(txs, offset)
        assertEquals(2350.00, totals.first, 0.001)
        assertEquals(120.00, totals.second, 0.001)
        assertEquals(2730.00, totals.third, 0.001) // 500 + 2350 - 120
        assertEquals(totals.third, offset + totals.first - totals.second, 0.001)

        // 7. Reset all data back to 0
        repository.resetAllData()
        txs = repository.allTransactions.first()
        assertTrue(txs.isEmpty())
        val resetOffset = repository.startingBalanceOffset.first()
        assertEquals(0.0, resetOffset, 0.001)
        totals = computeTotals(txs, resetOffset)
        assertEquals(0.0, totals.first, 0.001)
        assertEquals(0.0, totals.second, 0.001)
        assertEquals(0.0, totals.third, 0.001)
        assertEquals(totals.third, totals.first - totals.second, 0.001)
    }

    @Test
    fun testViewModelUiState_initialZeroState() = runBlocking {
        repository.resetAllData()
        val viewModel = FinanceViewModel(application, repository)
        val state = viewModel.uiState.value

        assertEquals(0.0, state.rollingBalance, 0.001)
        assertEquals(0.0, state.periodIn, 0.001)
        assertEquals(0.0, state.periodOut, 0.001)
        assertEquals(state.rollingBalance, state.startingBalanceOffset + state.periodIn - state.periodOut, 0.001)
    }
}
