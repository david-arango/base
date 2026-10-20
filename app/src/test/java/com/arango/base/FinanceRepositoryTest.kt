package com.arango.base

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.CategoryEntity
import com.arango.base.data.local.FinanceDao
import com.arango.base.data.local.IncomeEngineEntity
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.CategoryItem
import com.arango.base.data.model.DefaultCategories
import com.arango.base.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    fun checkAndSeedInitialData_keepsLedgerEmptyOnFreshInstallAndSeedsCategories() = runBlocking {
        repository.checkAndSeedInitialData()

        val transactions = repository.allTransactions.first()
        assertTrue("Transactions should be empty on initial install", transactions.isEmpty())

        val categories = repository.allCategories.first()
        assertTrue("Default categories should be seeded", categories.isNotEmpty())
        assertEquals(7, categories.size)

        val incomeEngine = repository.incomeEngineConfig.first()
        assertTrue(incomeEngine == null || !incomeEngine.isActive)

        // Running seed again should be idempotent
        repository.checkAndSeedInitialData()
        val countAfter = repository.allTransactions.first().size
        assertEquals("Seeding again should keep transactions empty", 0, countAfter)
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
    fun importCsvData_readsExportFormat_createsAndBackdatesTransactions() = runBlocking {
        repository.resetAllData()
        val dayMillis = 24L * 60 * 60 * 1000
        val backdate1 = System.currentTimeMillis() - (10 * dayMillis)
        val backdate2 = System.currentTimeMillis() - (20 * dayMillis)

        // Add 2 transactions with distinct historical dates
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Historical Grocery Trip",
                category = "Groceries",
                amount = 75.50,
                isExpense = true,
                timestamp = backdate1,
                note = "Weekly groceries",
                statusText = "Settled"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Historical Paycheck",
                category = "Recurring Income",
                amount = 2500.00,
                isExpense = false,
                timestamp = backdate2,
                note = "Paycheck",
                statusText = "Deposited"
            )
        )

        // Generate exact export
        val exportedCsv = repository.generateCsvData()

        // Clear all data
        repository.resetAllData()
        val txsBefore = repository.allTransactions.first()
        assertTrue(txsBefore.isEmpty())

        // Import the exact CSV export
        val importedCount = repository.importCsvData(exportedCsv)
        assertEquals(2, importedCount)

        val txsAfter = repository.allTransactions.first()
        assertEquals(2, txsAfter.size)

        val groceryTx = txsAfter.first { it.title == "Historical Grocery Trip" }
        assertEquals("Groceries", groceryTx.category)
        assertEquals(75.50, groceryTx.amount, 0.001)
        assertTrue(groceryTx.isExpense)
        assertEquals("Weekly groceries", groceryTx.note)
        assertEquals("Settled", groceryTx.statusText)
        // Verify timestamp matches backdate1 within 1000ms (seconds precision of export format)
        assertEquals(backdate1 / 1000, groceryTx.timestamp / 1000)

        val paycheckTx = txsAfter.first { it.title == "Historical Paycheck" }
        assertEquals("Recurring Income", paycheckTx.category)
        assertEquals(2500.00, paycheckTx.amount, 0.001)
        assertFalse(paycheckTx.isExpense)
        assertEquals("Paycheck", paycheckTx.note)
        assertEquals("Deposited", paycheckTx.statusText)
        assertEquals(backdate2 / 1000, paycheckTx.timestamp / 1000)
    }

    @Test
    fun importCsvData_handlesQuotedStringsAndCommas() = runBlocking {
        repository.resetAllData()

        val csv = "ID,Date,Title,Category,Type,Amount,Note,Status\n" +
            "1,\"2026-05-15 14:30:00\",\"Special Coffee, Dark & Rich\",\"Dining Out\",\"EXPENSE\",6.75,\"Extra shot, oat milk\",\"Settled\"\n" +
            "2,\"2026-05-16 18:00:00\",\"Novel \"\"A Great Story\"\"\",\"Shopping\",\"EXPENSE\",19.99,\"Bookstore purchase\",\"Settled\""

        val count = repository.importCsvData(csv)
        assertEquals(2, count)

        val txs = repository.allTransactions.first()
        val coffeeTx = txs.first { it.amount == 6.75 }
        assertEquals("Special Coffee, Dark & Rich", coffeeTx.title)
        assertEquals("Extra shot, oat milk", coffeeTx.note)

        val bookTx = txs.first { it.amount == 19.99 }
        assertEquals("Novel \"A Great Story\"", bookTx.title)
    }

    @Test
    fun importCsvData_autoCreatesNewCategories() = runBlocking {
        repository.resetAllData()

        val csv = """
            ID,Date,Title,Category,Type,Amount,Note,Status
            1,"2026-06-01 10:00:00","Tesla Supercharger","EV Charging","EXPENSE",24.50,"Highway stop","Settled"
        """.trimIndent()

        val count = repository.importCsvData(csv)
        assertEquals(1, count)

        val categories = repository.allCategories.first()
        assertTrue(categories.any { it.name == "EV Charging" })
    }

    @Test
    fun importCsvData_emptyOrInvalidCsv_returnsZero() = runBlocking {
        repository.resetAllData()
        assertEquals(0, repository.importCsvData(""))
        assertEquals(0, repository.importCsvData("   "))
        assertEquals(0, repository.importCsvData("ID,Date,Title,Category,Type,Amount,Note,Status\n"))
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
        repository.insertTransaction(
            TransactionEntity(
                title = "Coffee",
                category = "Dining Out",
                amount = 4.50,
                isExpense = true,
                timestamp = System.currentTimeMillis()
            )
        )
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
    fun importCsv_demoData_importsSuccessfully() = runBlocking {
        val demoCsv = """
            ID,Date,Title,Category,Type,Amount,Note,Status
            1,"2026-10-14 21:30:00","Trader Joe's organic basket","Groceries","EXPENSE",64.20,"Groceries","Settled"
            2,"2026-10-14 17:30:00","Morning pour-over coffee","Dining Out","EXPENSE",4.75,"Dining Out","Settled"
            3,"2026-10-11 13:30:00","Bi-Weekly Paycheck","Recurring Income","INCOME",2100.00,"Paycheck","Deposited"
        """.trimIndent()

        val count = repository.importCsvData(demoCsv)
        assertEquals(3, count)
        val txs = repository.allTransactions.first()
        assertEquals(3, txs.size)
        assertTrue(txs.any { it.title == "Trader Joe's organic basket" && it.amount == 64.20 && it.isExpense })
        assertTrue(txs.any { it.title == "Morning pour-over coffee" && it.amount == 4.75 && it.isExpense })
        assertTrue(txs.any { it.title == "Bi-Weekly Paycheck" && it.amount == 2100.00 && !it.isExpense })
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

    @Test
    fun category_spendingTargetCrudAndUpdateCategoryTarget() = runBlocking {
        repository.seedDefaultCategoriesIfEmpty()
        val categories = repository.allCategories.first()
        val groceries = categories.first { it.name == "Groceries" }
        assertNull(groceries.spendingTarget)

        // Update target for existing seeded category
        repository.updateCategorySpendingTarget(
            dbId = groceries.id,
            name = groceries.name,
            iconKey = groceries.iconKey,
            colorHex = groceries.colorHex,
            spendingTarget = 220.0
        )
        val afterSet = repository.allCategories.first().first { it.name == "Groceries" }
        assertEquals(220.0, afterSet.spendingTarget!!, 0.001)

        // Clear target
        repository.updateCategorySpendingTarget(
            dbId = groceries.id,
            name = groceries.name,
            iconKey = groceries.iconKey,
            colorHex = groceries.colorHex,
            spendingTarget = null
        )
        val afterClear = repository.allCategories.first().first { it.name == "Groceries" }
        assertNull(afterClear.spendingTarget)

        // Update target on an unseeded category ("Other") inserts it with target
        repository.updateCategorySpendingTarget(
            dbId = 0L,
            name = "Other",
            iconKey = "payments",
            colorHex = 0xFF546E7AL,
            spendingTarget = 95.0
        )
        val otherCat = repository.allCategories.first().firstOrNull { it.name == "Other" }
        assertNotNull(otherCat)
        assertEquals(95.0, otherCat!!.spendingTarget!!, 0.001)
    }

    @Test
    fun category_insertAppendsToEndWithMaxDisplayOrderPlusOne() = runBlocking {
        repository.seedDefaultCategoriesIfEmpty()
        val initialCats = repository.allCategories.first()
        assertEquals(7, initialCats.size)
        assertEquals(6, initialCats.maxOf { it.displayOrder })

        // Insert new category
        val newCatId1 = repository.insertCategory(
            CategoryEntity(
                name = "Investments",
                iconKey = "trending_up",
                colorHex = 0xFF1B5E20L
            )
        )
        val afterFirst = repository.allCategories.first()
        val inserted1 = afterFirst.first { it.id == newCatId1 }
        assertEquals(7, inserted1.displayOrder)

        // Insert second category
        val newCatId2 = repository.insertCategory(
            CategoryEntity(
                name = "Gifts",
                iconKey = "card_giftcard",
                colorHex = 0xFFD81B60L
            )
        )
        val afterSecond = repository.allCategories.first()
        val inserted2 = afterSecond.first { it.id == newCatId2 }
        assertEquals(8, inserted2.displayOrder)
    }

    @Test
    fun category_updateCategoryOrdersPersistsCustomOrder() = runBlocking {
        repository.seedDefaultCategoriesIfEmpty()
        val originalList = repository.allCategories.first().map { DefaultCategories.fromEntity(it) }
        val reversed = originalList.reversed()

        repository.updateCategoryOrders(reversed)

        val updatedList = repository.allCategories.first().map { DefaultCategories.fromEntity(it) }
        assertEquals(reversed.map { it.name }, updatedList.map { it.name })
        updatedList.forEachIndexed { index, item ->
            assertEquals(index, item.displayOrder)
        }
    }

    @Test
    fun onboardingPreference_persistsAndEmitsCorrectly() = runBlocking {
        assertFalse(repository.hasCompletedOnboarding.first())

        repository.setCompletedOnboarding(true)
        assertTrue(repository.hasCompletedOnboarding.first())

        repository.setCompletedOnboarding(false)
        assertFalse(repository.hasCompletedOnboarding.first())
    }
}
