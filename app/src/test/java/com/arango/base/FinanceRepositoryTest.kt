package com.arango.base

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.CategoryEntity
import com.arango.base.data.local.FinanceDao
import com.arango.base.data.local.IncomeEngineEntity
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
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
    fun insertAndDeleteTransaction() = runBlocking {
        val tx = TransactionEntity(
            id = 0,
            title = "Matcha Latte",
            category = "Dining Out",
            amount = 6.75,
            isExpense = true,
            timestamp = System.currentTimeMillis(),
            note = "Oat milk",
            statusText = "Settled"
        )
        val insertedId = repository.insertTransaction(tx)
        assertTrue(insertedId > 0)

        val txsAfterInsert = repository.allTransactions.first()
        assertEquals(1, txsAfterInsert.size)
        assertEquals("Matcha Latte", txsAfterInsert[0].title)

        repository.deleteTransaction(insertedId)
        val txsAfterDelete = repository.allTransactions.first()
        assertTrue(txsAfterDelete.isEmpty())
    }

    @Test
    fun updateIncomeEngine_persistsConfiguration() = runBlocking {
        val config = IncomeEngineEntity(
            id = 1,
            expectedIncome = 3500.0,
            cadence = "Semi-Monthly",
            nextPaydayTimestamp = 1800000000L,
            isActive = true
        )
        repository.updateIncomeEngine(config)

        val stored = repository.incomeEngineConfig.first()
        assertNotNull(stored)
        assertEquals(3500.0, stored!!.expectedIncome, 0.001)
        assertEquals("Semi-Monthly", stored.cadence)
        assertEquals(1800000000L, stored.nextPaydayTimestamp)
        assertTrue(stored.isActive)
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
        repository.insertTransaction(
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
    fun offsetTransactions_multipleOffsetsLoggedAndPreserved() = runBlocking {
        repository.clearAllOffsetTransactions()

        val id1 = repository.addOffsetTransaction(250.0, "Initial adjustment")
        val id2 = repository.addOffsetTransaction(-50.0, "Cash withdrawal")
        val id3 = repository.addOffsetTransaction(15.25, "Found cash")

        val allOffsets = repository.allOffsetTransactions.first()
        assertEquals(3, allOffsets.size)

        val total = repository.totalOffset.first()
        assertEquals(215.25, total, 0.001)

        // Offsets must NOT show up in the regular ledger transactions
        val transactions = repository.allTransactions.first()
        val hasOffsetInLedger = transactions.any { it.note == "Initial adjustment" || it.note == "Cash withdrawal" }
        assertFalse("Offset transactions must not appear in ledger", hasOffsetInLedger)

        // Delete id2
        repository.deleteOffsetTransaction(id2)
        val remainingOffsets = repository.allOffsetTransactions.first()
        assertEquals(2, remainingOffsets.size)
        assertEquals(265.25, repository.totalOffset.first(), 0.001)
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

    @Test
    fun getDatabaseSizeBytes_returnsNonNegativeValue() {
        val size = repository.getDatabaseSizeBytes()
        assertTrue("Database size should be non-negative", size >= 0)
    }

    @Test
    fun category_repositorySeedAndCrud() = runBlocking {
        // Seed default categories
        repository.seedDefaultCategoriesIfEmpty()
        val seeded = repository.allCategories.first()
        assertEquals(7, seeded.size)

        // Insert new custom category
        val newCat = CategoryEntity(name = "Investments", iconKey = "work", colorHex = 0xFF445566L)
        val insertedId = repository.insertCategory(newCat)
        assertTrue(insertedId > 0)

        val withNew = repository.allCategories.first()
        assertEquals(8, withNew.size)

        // Update custom category
        repository.updateCategory(
            CategoryEntity(id = insertedId, name = "Crypto & Stocks", iconKey = "bolt", colorHex = 0xFF112233L),
            oldName = "Investments"
        )
        val withUpdated = repository.allCategories.first()
        assertTrue(withUpdated.any { it.name == "Crypto & Stocks" })

        // Delete custom category
        repository.deleteCategory(insertedId, "Crypto & Stocks")
        val afterDelete = repository.allCategories.first()
        assertEquals(7, afterDelete.size)
        assertTrue(afterDelete.none { it.name == "Crypto & Stocks" })
    }

    @Test
    fun checkAndTriggerRecurringIncome_whenPaydayDue_insertsAutoTransactionAndAdvancesNextPayday() = runBlocking {
        val now = System.currentTimeMillis()
        val yesterday = now - (24L * 60 * 60 * 1000)

        // Config active income with payday yesterday (due)
        repository.updateIncomeEngine(
            IncomeEngineEntity(
                id = 1,
                expectedIncome = 2400.0,
                cadence = "Bi-Weekly",
                nextPaydayTimestamp = yesterday,
                isActive = true
            )
        )

        val triggered = repository.checkAndTriggerRecurringIncome(now)
        assertEquals(1, triggered.size)
        val tx = triggered.first()
        assertEquals("Bi-Weekly Paycheck", tx.title)
        assertEquals(2400.0, tx.amount, 0.001)
        assertEquals("Recurring Income", tx.category)
        assertEquals("Paycheck", tx.note)
        assertTrue(tx.isAuto)
        assertEquals(false, tx.isExpense)

        // All transactions flow in repository should contain the triggered transaction
        val allTxs = repository.allTransactions.first()
        assertTrue(allTxs.any { it.id == tx.id })

        // Next payday should have rolled forward by 14 days
        val updatedConfig = repository.incomeEngineConfig.first()
        assertNotNull(updatedConfig)
        assertTrue(updatedConfig!!.nextPaydayTimestamp > now)
    }

    @Test
    fun checkAndTriggerRecurringIncome_whenPaydayInFuture_doesNotTrigger() = runBlocking {
        val now = System.currentTimeMillis()
        val fiveDaysFuture = now + (5L * 24 * 60 * 60 * 1000)

        repository.updateIncomeEngine(
            IncomeEngineEntity(
                id = 1,
                expectedIncome = 1500.0,
                cadence = "Weekly",
                nextPaydayTimestamp = fiveDaysFuture,
                isActive = true
            )
        )

        val triggered = repository.checkAndTriggerRecurringIncome(now)
        assertTrue(triggered.isEmpty())

        val config = repository.incomeEngineConfig.first()
        assertEquals(fiveDaysFuture, config!!.nextPaydayTimestamp)
    }

    @Test
    fun checkAndTriggerRecurringIncome_preventsDuplicateDepositsOnSameDay() = runBlocking {
        val now = System.currentTimeMillis()

        repository.updateIncomeEngine(
            IncomeEngineEntity(
                id = 1,
                expectedIncome = 2000.0,
                cadence = "Bi-Weekly",
                nextPaydayTimestamp = now,
                isActive = true
            )
        )

        val firstTrigger = repository.checkAndTriggerRecurringIncome(now)
        assertEquals(1, firstTrigger.size)

        // Second call should not trigger duplicate transaction
        val secondTrigger = repository.checkAndTriggerRecurringIncome(now)
        assertTrue(secondTrigger.isEmpty())
    }

    @Test
    fun triggerIncomeNow_insertsAutoTransactionAndAdvancesPayday() = runBlocking {
        val now = System.currentTimeMillis()
        val originalPayday = now + (7L * 24 * 60 * 60 * 1000)

        repository.updateIncomeEngine(
            IncomeEngineEntity(
                id = 1,
                expectedIncome = 3100.0,
                cadence = "Monthly",
                nextPaydayTimestamp = originalPayday,
                isActive = true
            )
        )

        val triggered = repository.triggerIncomeNow()
        assertNotNull(triggered)
        assertEquals(3100.0, triggered!!.amount, 0.001)
        assertEquals("Monthly Paycheck", triggered.title)
        assertEquals("Paycheck", triggered.note)
        assertTrue(triggered.isAuto)

        val allTxs = repository.allTransactions.first()
        assertTrue(allTxs.any { it.id == triggered.id })

        val config = repository.incomeEngineConfig.first()
        assertTrue(config!!.nextPaydayTimestamp > originalPayday)
    }
}
