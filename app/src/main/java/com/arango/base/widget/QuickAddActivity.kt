package com.arango.base.widget

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.CategoryItem
import com.arango.base.data.model.DefaultCategories
import com.arango.base.data.repository.FinanceRepository
import com.arango.base.ui.theme.BaseTheme
import com.arango.base.ui.theme.CalmPositiveDeep
import com.arango.base.ui.theme.CalmTerracotta
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
                    onSave = { title, category, amount, isExpense, note ->
                        saveAndFinish(title, category, amount, isExpense, note)
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
        note: String
    ) {
        lifecycleScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    category = category,
                    amount = amount,
                    isExpense = isExpense,
                    timestamp = System.currentTimeMillis(),
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
    onSave: (title: String, category: String, amount: Double, isExpense: Boolean, note: String) -> Unit
) {
    var isExpense by remember(initialIsExpense) { mutableStateOf(initialIsExpense) }
    var rawAmount by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val resolvedInitialCategory = remember(categories, initialCategoryName) {
        if (!initialCategoryName.isNullOrBlank()) {
            categories.firstOrNull { it.name.equals(initialCategoryName, ignoreCase = true) }?.name
                ?: initialCategoryName
        } else {
            categories.firstOrNull()?.name ?: "Groceries"
        }
    }

    var selectedCategory by remember(resolvedInitialCategory) {
        mutableStateOf(resolvedInitialCategory)
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(selectedCategory, categories) {
        val index = categories.indexOfFirst { it.name.equals(selectedCategory, ignoreCase = true) }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0
    val triggerSave = {
        if (parsedAmount > 0.0) {
            onSave(
                noteText.ifBlank { selectedCategory },
                selectedCategory,
                parsedAmount,
                isExpense,
                noteText
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("quick_add_backdrop"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks inside the sheet
                )
                .imePadding()
                .navigationBarsPadding()
                .testTag("quick_add_card"),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Drag Handle Visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }

                // Header Row: Close, Title, Expense/Income Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("quick_add_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Quick Add",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Quick Add",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("quick_add_title")
                        )
                    }

                    // Expense / Income Pill Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (isExpense) CalmTerracotta else Color.Transparent)
                                .clickable { isExpense = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("quick_add_toggle_expense"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isExpense) Color.White else CalmTerracotta)
                            )
                            Text(
                                text = "Expense",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (!isExpense) CalmPositiveDeep else Color.Transparent)
                                .clickable { isExpense = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("quick_add_toggle_income"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (!isExpense) Color.White else CalmPositiveDeep)
                            )
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Auto-focused Amount Input Field
                    OutlinedTextField(
                        value = rawAmount,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it == '.' }
                            if (clean.count { it == '.' } <= 1) {
                                val parts = clean.split(".")
                                if (parts.size <= 1 || parts[1].length <= 2) {
                                    if (clean.length <= 10) {
                                        rawAmount = clean
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                text = if (isExpense) "Expense Amount" else "Income Amount",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Text(
                                text = if (isExpense) "-$" else "+$",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isExpense) CalmTerracotta else CalmPositiveDeep,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        placeholder = {
                            Text(
                                text = "0.00",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { triggerSave() }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isExpense) CalmTerracotta else CalmPositiveDeep,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = if (isExpense) CalmTerracotta else CalmPositiveDeep
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .focusRequester(focusRequester)
                            .testTag("quick_add_amount_input")
                    )

                    // Horizontal Category Chips
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("quick_add_category_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(categories) { _, category ->
                            val isSelected = selectedCategory.equals(category.name, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clickable { selectedCategory = category.name }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("quick_add_cat_chip_${category.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else category.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Optional Quick Note Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = "Note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        TextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = {
                                Text(
                                    "Add a note (optional)...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { triggerSave() }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_note_input")
                        )

                        if (noteText.isNotEmpty()) {
                            IconButton(
                                onClick = { noteText = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = "Clear note",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Save Transaction Primary Button
                    val amountStr = String.format(Locale.US, "%.2f", parsedAmount)
                    Button(
                        onClick = { triggerSave() },
                        enabled = parsedAmount > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("quick_add_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isExpense) CalmTerracotta else CalmPositiveDeep,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (parsedAmount > 0.0) {
                                "Save Transaction (${if (isExpense) "-" else "+"}$$amountStr)"
                            } else {
                                "Save Transaction"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Quick Add Overlay",
    showBackground = true,
    backgroundColor = 0xFF888888
)
@Composable
fun QuickAddOverlayPreview() {
    BaseTheme {
        QuickAddOverlayScreen(
            categories = DefaultCategories.list,
            initialCategoryName = "Groceries",
            initialIsExpense = true,
            onDismiss = {},
            onSave = { _, _, _, _, _ -> }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "1. Balance & Quick Add Widget",
    showBackground = true,
    widthDp = 320,
    heightDp = 90
)
@Composable
fun BalanceQuickAddWidgetPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot = remember {
        WidgetDataSnapshot(
            rollingBalance = 1240.50,
            isBalanceMasked = false,
            formattedBalance = "$1,240.50",
            periodSummaryText = "• 11d left",
            topCategories = DefaultCategories.list
        )
    }
    Box(modifier = Modifier.padding(8.dp)) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                val options = Bundle().apply {
                    putInt( android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
                    putInt( android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 64)
                }
                val rv = BalanceQuickAddWidgetProvider.buildRemoteViews(ctx, 1, options, snapshot)
                rv.apply(ctx, android.widget.FrameLayout(ctx))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "2. Category Shortcuts Widget",
    showBackground = true,
    widthDp = 360,
    heightDp = 95
)
@Composable
fun CategoryShortcutsWidgetPreview() {
    val snapshot = remember {
        WidgetDataSnapshot(
            rollingBalance = 1240.50,
            isBalanceMasked = false,
            formattedBalance = "$1,240.50",
            periodSummaryText = "• 11d left",
            topCategories = DefaultCategories.list
        )
    }
    Box(modifier = Modifier.padding(8.dp)) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                val options = Bundle().apply {
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 64)
                }
                val rv = CategoryShortcutsWidgetProvider.buildRemoteViews(ctx, 2, options, snapshot)
                rv.apply(ctx, android.widget.FrameLayout(ctx))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

