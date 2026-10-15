package com.arango.base

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arango.base.ui.AppScreen
import com.arango.base.ui.FinanceViewModel
import com.arango.base.ui.components.AppBottomNavigation
import com.arango.base.ui.components.AppHeader
import com.arango.base.ui.components.CategoryManagerSheet
import com.arango.base.ui.components.ImportCsvSheet
import com.arango.base.ui.components.IncomeEngineSheet
import com.arango.base.ui.components.NewTransactionSheet
import com.arango.base.ui.components.OffsetBudgetSheet
import com.arango.base.ui.screens.AnalyticsScreen
import com.arango.base.ui.screens.LedgerScreen
import com.arango.base.ui.screens.SettingsScreen
import com.arango.base.ui.theme.CalmPrimary
import com.arango.base.ui.theme.BaseTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            BaseTheme {
                BaseApp()
            }
        }
    }
}

@Composable
fun BaseApp(
    viewModel: FinanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isNewTxOpen by viewModel.isNewTransactionSheetOpen.collectAsStateWithLifecycle()
    val editingTx by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val isIncomeEngineOpen by viewModel.isIncomeEngineOpen.collectAsStateWithLifecycle()
    val isCategoryManagerOpen by viewModel.isCategoryManagerOpen.collectAsStateWithLifecycle()
    val isStartingBalanceOpen by viewModel.isStartingBalanceDialogOpen.collectAsStateWithLifecycle()
    val isImportCsvOpen by viewModel.isImportCsvSheetOpen.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCsvFromUri(it)
            viewModel.closeImportCsvSheet()
        }
    }

    // Check for any pending scheduled income when app is resumed
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkRecurringIncomeTriggers()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800)
            viewModel.clearToast()
        }
    }

    // Periodically check for scheduled recurring income while the UI is active
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            viewModel.checkRecurringIncomeTriggers()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                AppHeader(
                    currentScreen = currentScreen,
                    onAvatarClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
                )
            },
            bottomBar = {
                AppBottomNavigation(
                    currentScreen = currentScreen,
                    onScreenSelected = { viewModel.navigateTo(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = currentScreen,
                    label = "screen_crossfade"
                ) { screen ->
                    when (screen) {
                        AppScreen.LEDGER -> {
                            LedgerScreen(
                                uiState = uiState,
                                onOpenNewTransaction = { viewModel.openNewTransactionSheet() },
                                onEditTransaction = { viewModel.openEditTransaction(it) },
                                onDeleteTransaction = { viewModel.deleteTransaction(it) }
                            )
                        }

                        AppScreen.ANALYTICS -> {
                            AnalyticsScreen(
                                uiState = uiState,
                                onPeriodChange = { viewModel.setAnalyticsPeriod(it) }
                            )
                        }

                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                uiState = uiState,
                                onOpenIncomeEngine = { viewModel.openIncomeEngine() },
                                onOpenCategoryManager = { viewModel.openCategoryManager() },
                                onOpenStartingBalanceDialog = { viewModel.openStartingBalanceDialog() },
                                onExportCsv = { viewModel.exportCsv() },
                                onImportCsv = { viewModel.openImportCsvSheet() },
                                onResetData = { viewModel.resetAllData() }
                            )
                        }
                    }
                }
            }
        }

        // Floating Toast Notification
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
        ) {
            toastMessage?.let { msg ->
                Surface(
                    color = CalmPrimary,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("app_toast_message")
                    )
                }
            }
        }

        // Modal Sheets and Dialogs
        NewTransactionSheet(
            isOpen = isNewTxOpen,
            transactionToEdit = editingTx,
            categories = uiState.categories,
            onDismiss = { viewModel.closeNewTransactionSheet() },
            onSave = { title, category, amount, isExpense, note, timestamp ->
                if (editingTx != null) {
                    viewModel.updateTransaction(
                        id = editingTx!!.id,
                        title = title,
                        category = category,
                        amount = amount,
                        isExpense = isExpense,
                        note = note,
                        timestamp = timestamp
                    )
                } else {
                    viewModel.addTransaction(title, category, amount, isExpense, note, timestamp)
                }
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
            }
        )

        IncomeEngineSheet(
            isOpen = isIncomeEngineOpen,
            currentConfig = uiState.incomeEngine,
            onDismiss = { viewModel.closeIncomeEngine() },
            onSave = { expectedIncome, cadence, nextDate ->
                viewModel.updateIncomeEngine(expectedIncome, cadence, nextDate)
            },
            onTriggerNow = {
                viewModel.triggerIncomeNow()
            }
        )

        CategoryManagerSheet(
            isOpen = isCategoryManagerOpen,
            categories = uiState.categories,
            onDismiss = { viewModel.closeCategoryManager() },
            onCreateCategory = { name, iconKey, colorHex ->
                viewModel.createCategory(name, iconKey, colorHex)
            },
            onUpdateCategory = { id, name, iconKey, colorHex, oldName ->
                viewModel.updateCategory(id, name, iconKey, colorHex, oldName)
            },
            onDeleteCategory = { id, name ->
                viewModel.deleteCategory(id, name)
            }
        )

        OffsetBudgetSheet(
            isOpen = isStartingBalanceOpen,
            currentOffset = uiState.startingBalanceOffset,
            offsetTransactions = uiState.offsetTransactions,
            onDismiss = { viewModel.closeStartingBalanceDialog() },
            onAddOffset = { amount, note ->
                viewModel.addOffsetTransaction(amount, note)
            },
            onDeleteOffset = { id ->
                viewModel.deleteOffsetTransaction(id)
            }
        )

        ImportCsvSheet(
            isOpen = isImportCsvOpen,
            onDismiss = { viewModel.closeImportCsvSheet() },
            onChooseFile = {
                filePickerLauncher.launch(
                    arrayOf(
                        "text/comma-separated-values",
                        "text/csv",
                        "text/plain",
                        "application/csv",
                        "*/*"
                    )
                )
            },
            onImportText = { csvText ->
                viewModel.importCsv(csvText)
                viewModel.closeImportCsvSheet()
            }
        )
    }
}
