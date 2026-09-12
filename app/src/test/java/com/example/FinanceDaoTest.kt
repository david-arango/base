package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.FinanceDao
import com.example.data.local.IncomeEngineEntity
import com.example.data.local.TransactionEntity
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
    fun insertAndRetrieveTransaction() = runBlocking {
        val tx = TransactionEntity(
            id = 1,
            title = "Organic Coffee",
            category = "Dining Out",
            amount = 5.50,
            isExpense = true,
            timestamp = 1000L,
            note = "Espresso",
            statusText = "Settled"
        )
        dao.insertTransaction(tx)

        val list = dao.getAllTransactions().first()
        assertEquals(1, list.size)
        assertEquals("Organic Coffee", list[0].title)
        assertEquals(5.50, list[0].amount, 0.001)
        assertTrue(list[0].isExpense)
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
    fun incomeEngineConfigurationSaveAndRead() = runBlocking {
        val config = IncomeEngineEntity(
            id = 1,
            expectedIncome = 2500.0,
            cadence = "Bi-Weekly",
            nextPaydayTimestamp = 1700000000L,
            isActive = true
        )
        dao.setIncomeEngineConfig(config)

        val retrieved = dao.getIncomeEngineConfig().first()
        assertNotNull(retrieved)
        assertEquals(2500.0, retrieved!!.expectedIncome, 0.001)
        assertEquals("Bi-Weekly", retrieved.cadence)
        assertEquals(1700000000L, retrieved.nextPaydayTimestamp)
        assertTrue(retrieved.isActive)
    }

    @Test
    fun appPreferencesSaveAndRead() = runBlocking {
        dao.setPreference(AppPreferenceEntity("starting_balance_offset", "500.0"))
        val value = dao.getPreference("starting_balance_offset")
        assertEquals("500.0", value)

        val flowValue = dao.getPreferenceFlow("starting_balance_offset").first()
        assertEquals("500.0", flowValue)
    }
}
