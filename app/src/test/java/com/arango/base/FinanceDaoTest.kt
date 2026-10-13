package com.arango.base

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.AppPreferenceEntity
import com.arango.base.data.local.CategoryEntity
import com.arango.base.data.local.FinanceDao
import com.arango.base.data.local.IncomeEngineEntity
import com.arango.base.data.local.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class FinanceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FinanceDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.financeDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun getAllTransactions_initiallyEmpty() = runBlocking {
        val transactions = dao.getAllTransactions().first()
        assertTrue(transactions.isEmpty())
    }

    @Test
    fun insertAndRetrieveTransaction() = runBlocking {
        val tx = TransactionEntity(
            id = 0,
            title = "Organic Coffee",
            category = "Dining Out",
            amount = 5.50,
            isExpense = true,
            timestamp = 1000L,
            note = "Espresso",
            statusText = "Settled"
        )
        val generatedId = dao.insertTransaction(tx)
        assertTrue(generatedId > 0)

        val list = dao.getAllTransactions().first()
        assertEquals(1, list.size)
        assertEquals("Organic Coffee", list[0].title)
        assertEquals(5.50, list[0].amount, 0.001)
        assertTrue(list[0].isExpense)
        assertEquals("Settled", list[0].statusText)
    }

    @Test
    fun batchInsertAndOrdering() = runBlocking {
        val tx1 = TransactionEntity(
            id = 1,
            title = "Older Item",
            category = "Groceries",
            amount = 20.0,
            isExpense = true,
            timestamp = 1000L
        )
        val tx2 = TransactionEntity(
            id = 2,
            title = "Newer Item",
            category = "Groceries",
            amount = 40.0,
            isExpense = true,
            timestamp = 2000L
        )
        dao.insertTransactions(listOf(tx1, tx2))

        val list = dao.getAllTransactions().first()
        assertEquals(2, list.size)
        // Ordered by timestamp DESC
        assertEquals("Newer Item", list[0].title)
        assertEquals("Older Item", list[1].title)
    }

    @Test
    fun deleteTransactionById() = runBlocking {
        val tx = TransactionEntity(
            id = 42,
            title = "Book",
            category = "Shopping",
            amount = 15.0,
            isExpense = true,
            timestamp = 1500L
        )
        dao.insertTransaction(tx)
        assertEquals(1, dao.getAllTransactions().first().size)

        dao.deleteTransactionById(42)
        assertEquals(0, dao.getAllTransactions().first().size)
    }

    @Test
    fun clearAllTransactions() = runBlocking {
        dao.insertTransactions(
            listOf(
                TransactionEntity(id = 1, title = "A", category = "Groceries", amount = 10.0, isExpense = true, timestamp = 100L),
                TransactionEntity(id = 2, title = "B", category = "Groceries", amount = 20.0, isExpense = true, timestamp = 200L)
            )
        )
        assertEquals(2, dao.getAllTransactions().first().size)

        dao.clearAllTransactions()
        assertEquals(0, dao.getAllTransactions().first().size)
    }

    @Test
    fun incomeEngineConfiguration_initiallyNull_thenSaveAndUpdate() = runBlocking {
        val initialConfig = dao.getIncomeEngineConfig().first()
        assertNull(initialConfig)

        val config1 = IncomeEngineEntity(
            id = 1,
            expectedIncome = 2500.0,
            cadence = "Bi-Weekly",
            nextPaydayTimestamp = 1700000000L,
            isActive = true
        )
        dao.setIncomeEngineConfig(config1)

        val retrieved1 = dao.getIncomeEngineConfig().first()
        assertNotNull(retrieved1)
        assertEquals(2500.0, retrieved1!!.expectedIncome, 0.001)
        assertEquals("Bi-Weekly", retrieved1.cadence)
        assertEquals(1700000000L, retrieved1.nextPaydayTimestamp)
        assertTrue(retrieved1.isActive)

        // Update config with onConflict REPLACE
        val config2 = IncomeEngineEntity(
            id = 1,
            expectedIncome = 3000.0,
            cadence = "Monthly",
            nextPaydayTimestamp = 1750000000L,
            isActive = false
        )
        dao.setIncomeEngineConfig(config2)

        val retrieved2 = dao.getIncomeEngineConfig().first()
        assertNotNull(retrieved2)
        assertEquals(3000.0, retrieved2!!.expectedIncome, 0.001)
        assertEquals("Monthly", retrieved2.cadence)
        assertEquals(1750000000L, retrieved2.nextPaydayTimestamp)
        assertEquals(false, retrieved2.isActive)
    }

    @Test
    fun appPreferences_absentReturnsNull_thenSaveAndOverwrite() = runBlocking {
        // Querying absent preference
        assertNull(dao.getPreference("non_existent_key"))
        assertNull(dao.getPreferenceFlow("non_existent_key").first())

        // Set preference
        dao.setPreference(AppPreferenceEntity("starting_balance_offset", "500.0"))
        assertEquals("500.0", dao.getPreference("starting_balance_offset"))
        assertEquals("500.0", dao.getPreferenceFlow("starting_balance_offset").first())

        // Overwrite preference
        dao.setPreference(AppPreferenceEntity("starting_balance_offset", "750.25"))
        assertEquals("750.25", dao.getPreference("starting_balance_offset"))
        assertEquals("750.25", dao.getPreferenceFlow("starting_balance_offset").first())
    }

    @Test
    fun category_crudAndTransactionReassignment() = runBlocking {
        // Initially empty
        val initialCats = dao.getAllCategories().first()
        assertTrue(initialCats.isEmpty())

        // Insert categories
        val catId1 = dao.insertCategory(CategoryEntity(name = "Fitness", iconKey = "fitness_center", colorHex = 0xFF123456L))
        val catId2 = dao.insertCategory(CategoryEntity(name = "Hobbies", iconKey = "movie", colorHex = 0xFF654321L))
        assertTrue(catId1 > 0)
        assertTrue(catId2 > 0)

        val catsAfterInsert = dao.getAllCategories().first()
        assertEquals(2, catsAfterInsert.size)
        assertEquals("Fitness", catsAfterInsert[0].name)
        assertEquals("Hobbies", catsAfterInsert[1].name)

        // Insert a transaction using "Fitness"
        val txId = dao.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Gym Membership",
                category = "Fitness",
                amount = 45.0,
                isExpense = true,
                timestamp = 1000L
            )
        )

        // Update category name from "Fitness" to "Wellness"
        dao.updateCategory(CategoryEntity(id = catId1, name = "Wellness", iconKey = "fitness_center", colorHex = 0xFF123456L))
        dao.updateCategoryInTransactions("Fitness", "Wellness")

        val updatedCats = dao.getAllCategoriesList()
        assertEquals("Wellness", updatedCats[0].name)
        val updatedTx = dao.getAllTransactionsList().first { it.id == txId }
        assertEquals("Wellness", updatedTx.category)

        // Delete category and reassign transactions to "Other"
        dao.deleteCategoryById(catId1)
        dao.reassignCategoryInTransactions("Wellness")

        val remainingCats = dao.getAllCategoriesList()
        assertEquals(1, remainingCats.size)
        assertEquals("Hobbies", remainingCats[0].name)

        val reassignedTx = dao.getAllTransactionsList().first { it.id == txId }
        assertEquals("Other", reassignedTx.category)
    }
}
