package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.ui.AppScreen
import com.example.ui.FinanceViewModel
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppHeader
import com.example.ui.components.CategoryManagerSheet
import com.example.ui.components.IncomeEngineSheet
import com.example.ui.components.NewTransactionSheet
import com.example.ui.components.StartingBalanceDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CalmPrimary
import com.example.ui.theme.MyApplicationTheme
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
            MyApplicationTheme {
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
    val isIncomeEngineOpen by viewModel.isIncomeEngineOpen.collectAsStateWithLifecycle()
    val isCategoryManagerOpen by viewModel.isCategoryManagerOpen.collectAsStateWithLifecycle()
    val isStartingBalanceOpen by viewModel.isStartingBalanceDialogOpen.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800)
            viewModel.clearToast()
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
                                onResetData = { viewModel.resetAllData() },
                                onRestoreDemoData = { viewModel.restoreDemoData() }
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
            categories = uiState.categories,
            onDismiss = { viewModel.closeNewTransactionSheet() },
            onSave = { title, category, amount, isExpense, note ->
                viewModel.addTransaction(title, category, amount, isExpense, note)
            }
        )

        IncomeEngineSheet(
            isOpen = isIncomeEngineOpen,
            currentConfig = uiState.incomeEngine,
            onDismiss = { viewModel.closeIncomeEngine() },
            onSave = { expectedIncome, cadence, nextDate ->
                viewModel.updateIncomeEngine(expectedIncome, cadence, nextDate)
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

        StartingBalanceDialog(
            isOpen = isStartingBalanceOpen,
            currentOffset = uiState.startingBalanceOffset,
            onDismiss = { viewModel.closeStartingBalanceDialog() },
            onConfirm = { newOffset ->
                viewModel.updateStartingBalanceOffset(newOffset)
            }
        )
    }
}
