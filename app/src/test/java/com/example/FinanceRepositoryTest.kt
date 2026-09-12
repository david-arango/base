package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FinanceDao
import com.example.data.local.TransactionEntity
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FinanceDao
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.financeDao()
        repository = FinanceRepository(context, dao)
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun checkAndSeedInitialData_seedsTransactionsAndIncomeEngine() = runBlocking {
        repository.checkAndSeedInitialData()

        val transactions = repository.allTransactions.first()
        assertTrue("Transactions should not be empty after seeding", transactions.isNotEmpty())

        val incomeEngine = repository.incomeEngineConfig.first()
        assertNotNull(incomeEngine)
        assertEquals(2100.0, incomeEngine!!.expectedIncome, 0.001)
        assertEquals("Bi-Weekly", incomeEngine.cadence)

        // Running seed again should be idempotent
        val countBefore = transactions.size
        repository.checkAndSeedInitialData()
        val countAfter = repository.allTransactions.first().size
        assertEquals("Seeding again should not create duplicates", countBefore, countAfter)
    }

    @Test
    fun generateCsvData_containsHeaderAndTransactions() = runBlocking {
        dao.insertTransaction(
            TransactionEntity(
                id = 1,
                title = "Coffee, Dark Roast",
                category = "Dining Out",
                amount = 4.25,
                isExpense = true,
                timestamp = 1000L,
                note = "Double shot",
                statusText = "Settled"
            )
        )

        val csv = repository.generateCsvData()
        assertTrue(csv.contains("ID,Date,Title,Category,Type,Amount,Note,Status"))
        assertTrue(csv.contains("Coffee, Dark Roast"))
        assertTrue(csv.contains("EXPENSE"))
        assertTrue(csv.contains("4.25"))
        assertTrue(csv.contains("Settled"))
    }

    @Test
    fun startingBalanceOffset_updatesPreference() = runBlocking {
        repository.setStartingBalanceOffset(350.75)
        val offset = repository.startingBalanceOffset.first()
        assertEquals(350.75, offset, 0.001)
    }

    @Test
    fun resetAllData_clearsTransactionsAndOffset() = runBlocking {
        repository.checkAndSeedInitialData()
        repository.setStartingBalanceOffset(999.0)
        assertEquals(999.0, repository.startingBalanceOffset.first(), 0.001)

        repository.resetAllData()

        // Offset should be reset to 0.0
        assertEquals(0.0, repository.startingBalanceOffset.first(), 0.001)
        // Transactions should be completely cleared
        val txs = repository.allTransactions.first()
        assertTrue(txs.isEmpty())
    }

    @Test
    fun restoreDemoData_reseedsTransactions() = runBlocking {
        repository.resetAllData()
        assertTrue(repository.allTransactions.first().isEmpty())

        repository.restoreDemoData()
        val txs = repository.allTransactions.first()
        assertTrue(txs.isNotEmpty())
    }
}
