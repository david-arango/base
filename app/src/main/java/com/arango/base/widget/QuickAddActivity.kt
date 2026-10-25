package com.arango.base.widget

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.CategoryItem
import com.arango.base.data.model.DefaultCategories
import com.arango.base.data.repository.FinanceRepository
import com.arango.base.ui.components.NewTransactionSheet
import com.arango.base.ui.theme.BaseTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class QuickAddActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CATEGORY_NAME = "extra_category_name"
        const val EXTRA_IS_EXPENSE = "extra_is_expense"
    }

    private lateinit var repository: FinanceRepository
    private val requestedCategoryState = MutableStateFlow<String?>(null)
    private val requestedIsExpenseState = MutableStateFlow(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        val dao = AppDatabase.getInstance(applicationContext).financeDao()
        repository = FinanceRepository(applicationContext, dao)

        lifecycleScope.launch {
            repository.checkAndSeedInitialData()
        }

        parseIntentExtras(intent)

        val categoriesFlow = repository.allCategories
            .map { entities ->
                if (entities.isEmpty()) {
                    DefaultCategories.list
                } else {
                    entities
                        .sortedWith(compareBy({ it.displayOrder }, { it.id }))
                        .map { DefaultCategories.fromEntity(it) }
                }
            }
            .stateIn(lifecycleScope, SharingStarted.Eagerly, DefaultCategories.list)

        setContent {
            BaseTheme {
                val categories by categoriesFlow.collectAsStateWithLifecycle()
                val initialCategory by requestedCategoryState.collectAsStateWithLifecycle()
                val initialIsExpense by requestedIsExpenseState.collectAsStateWithLifecycle()

                QuickAddOverlayScreen(
                    categories = categories,
                    initialCategoryName = initialCategory,
                    initialIsExpense = initialIsExpense,
                    onDismiss = { finish() },
                    onSave = { title, category, amount, isExpense, note, timestamp ->
                        saveAndFinish(title, category, amount, isExpense, note, timestamp)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntentExtras(intent)
    }

    private fun parseIntentExtras(intent: Intent?) {
        requestedCategoryState.value = intent?.getStringExtra(EXTRA_CATEGORY_NAME)
        requestedIsExpenseState.value = intent?.getBooleanExtra(EXTRA_IS_EXPENSE, true) ?: true
    }

    private fun saveAndFinish(
        title: String,
        category: String,
        amount: Double,
        isExpense: Boolean,
        note: String,
        timestamp: Long
    ) {
        lifecycleScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    category = category,
                    amount = amount,
                    isExpense = isExpense,
                    timestamp = timestamp,
                    note = note,
                    isAuto = false,
                    statusText = "Settled"
                )
            )
            WidgetUpdateHelper.updateAllWidgetsSync(applicationContext)
            val sign = if (isExpense) "-" else "+"
            val formattedAmt = String.format(Locale.US, "%.2f", amount)
            Toast.makeText(
                applicationContext,
                "Added $sign$$formattedAmt to $category",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}

@Composable
fun QuickAddOverlayScreen(
    categories: List<CategoryItem>,
    initialCategoryName: String? = null,
    initialIsExpense: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, amount: Double, isExpense: Boolean, note: String, timestamp: Long) -> Unit
) {
    NewTransactionSheet(
        isOpen = true,
        transactionToEdit = null,
        categories = categories,
        initialCategory = initialCategoryName,
        initialIsExpense = initialIsExpense,
        autoFocusAmount = true,
        onDismiss = onDismiss,
        onSave = onSave
    )
}
