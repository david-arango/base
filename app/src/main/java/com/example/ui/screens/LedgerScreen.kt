package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.model.DefaultCategories
import com.example.ui.FinanceUiState
import com.example.ui.theme.CalmDividerHairline
import com.example.ui.theme.CalmErrorContainer
import com.example.ui.theme.CalmNegativeDeep
import com.example.ui.theme.CalmOnPrimary
import com.example.ui.theme.CalmPositiveDeep
import com.example.ui.theme.CalmPrimary
import com.example.ui.theme.CalmPrimaryContainer
import com.example.ui.theme.CalmSecondaryContainer
import com.example.ui.theme.CalmSurfaceContainer
import com.example.ui.theme.CalmSurfaceContainerHighest
import com.example.ui.theme.CalmSurfaceContainerLowest
import com.example.ui.theme.CalmTextMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen(
    uiState: FinanceUiState,
    onOpenNewTransaction: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val groupedTransactions = remember(uiState.transactions) {
            groupTransactionsByDay(uiState.transactions)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Balance Overview Card (Warm Organic Material Card)
            item {
                PerpetualRollingBalanceCard(uiState = uiState)
            }

            // Ledger Stream Header & Filter Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Continuous Stream",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(CalmSurfaceContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${uiState.transactions.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = CalmTextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(CalmSurfaceContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Live Sync Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Empty State
            if (uiState.transactions.isEmpty()) {
                item {
                    Surface(
                        color = CalmSurfaceContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CalmSurfaceContainerLowest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Spa,
                                    contentDescription = null,
                                    tint = CalmPositiveDeep,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Ledger is Clear",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Balance is at $0.00. Tap the + button to log an expense or income, or configure your starting balance in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Grouped Transaction Cards
            groupedTransactions.forEach { (header, transactionsInGroup) ->
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = CalmTextMuted,
                            modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                        )

                        Surface(
                            color = CalmSurfaceContainer,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                transactionsInGroup.forEachIndexed { index, tx ->
                                    TransactionItemRow(
                                        transaction = tx,
                                        onDelete = { onDeleteTransaction(tx.id) }
                                    )
                                    if (index < transactionsInGroup.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = CalmDividerHairline.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Calm Mindset Quote / Footer Reflection
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Spa,
                        contentDescription = null,
                        tint = CalmPositiveDeep,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"Money is a neutral current. Track with clarity, rest without concern.\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = CalmTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Floating Action Button (FAB)
        FloatingActionButton(
            onClick = onOpenNewTransaction,
            containerColor = CalmPrimary,
            contentColor = CalmOnPrimary,
            shape = RoundedCornerShape(20.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .size(64.dp)
                .testTag("fab_add_transaction")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Log new expense",
                    modifier = Modifier.size(30.dp)
                )
                // Small subtle positive-deep indicator dot on top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CalmPositiveDeep)
                )
            }
        }
    }
}

@Composable
fun PerpetualRollingBalanceCard(uiState: FinanceUiState) {
    Surface(
        color = CalmSurfaceContainer,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Label with pulsing green dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CalmPositiveDeep)
                )
                Text(
                    text = "PERPETUAL ROLLING BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = CalmTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hero Amount Display
            Text(
                text = String.format(Locale.US, "$%,.2f", uiState.rollingBalance),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = CalmPositiveDeep,
                modifier = Modifier.testTag("hero_balance_display")
            )

            // Projected cash flow micro-copy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = CalmTextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = uiState.nextPaydayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // In/Out Period Flow Pill Deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Period In
                Surface(
                    color = CalmSurfaceContainerLowest.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CalmSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = "Period In",
                                tint = CalmPositiveDeep,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PERIOD IN",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                                color = CalmTextMuted
                            )
                            Text(
                                text = String.format(Locale.US, "+$%,.2f", uiState.periodIn),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = CalmPositiveDeep,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Period Out
                Surface(
                    color = CalmSurfaceContainerLowest.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CalmErrorContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = "Period Out",
                                tint = CalmNegativeDeep,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PERIOD OUT",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                                color = CalmTextMuted
                            )
                            Text(
                                text = String.format(Locale.US, "-$%,.2f", uiState.periodOut),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = CalmNegativeDeep,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onDelete: () -> Unit
) {
    val category = remember(transaction.category) {
        DefaultCategories.getCategoryByName(transaction.category)
    }

    val timeFormatted = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        sdf.format(Date(transaction.timestamp))
    }

    val isExpense = transaction.isExpense
    val isIncomeHighlight = !isExpense && transaction.isAuto

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isIncomeHighlight) CalmSecondaryContainer.copy(alpha = 0.25f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isIncomeHighlight) CalmSecondaryContainer else CalmSurfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (isIncomeHighlight) CalmPositiveDeep else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isIncomeHighlight) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.isAuto) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CalmPositiveDeep.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CalmPositiveDeep
                            )
                        }
                    }
                }

                val subtext = if (transaction.note.isNotBlank()) {
                    "${transaction.category} • ${transaction.note}"
                } else {
                    "${transaction.category} • $timeFormatted"
                }

                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CalmTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Amount & Status
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            val amountFormatted = String.format(Locale.US, "%,.2f", transaction.amount)
            Text(
                text = if (isExpense) "-$$amountFormatted" else "+$$amountFormatted",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isExpense) CalmNegativeDeep else CalmPositiveDeep
            )
            Text(
                text = transaction.statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isIncomeHighlight) CalmPositiveDeep.copy(alpha = 0.8f) else CalmTextMuted
            )
        }
    }
}

private fun groupTransactionsByDay(transactions: List<TransactionEntity>): Map<String, List<TransactionEntity>> {
    val sdfDay = SimpleDateFormat("yyyyMMdd", Locale.US)
    val now = Calendar.getInstance()
    val todayKey = sdfDay.format(now.time)
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayKey = sdfDay.format(now.time)

    val map = LinkedHashMap<String, MutableList<TransactionEntity>>()
    val dayHeaderFormatter = SimpleDateFormat("EEEE, MMM d", Locale.US)

    for (tx in transactions) {
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        val key = sdfDay.format(txCal.time)
        val header = when (key) {
            todayKey -> "TODAY"
            yesterdayKey -> "YESTERDAY"
            else -> dayHeaderFormatter.format(txCal.time).uppercase(Locale.US)
        }
        map.getOrPut(header) { mutableListOf() }.add(tx)
    }

    return map
}
