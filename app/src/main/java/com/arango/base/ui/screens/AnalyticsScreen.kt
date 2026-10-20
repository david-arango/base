package com.arango.base.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arango.base.data.model.CategoryItem
import com.arango.base.ui.AnalyticsPeriod
import com.arango.base.ui.CategorySpend
import com.arango.base.ui.FinanceUiState
import com.arango.base.ui.components.CategorySpendingTargetDialog
import com.arango.base.ui.theme.AnalyticsColorPalette
import com.arango.base.ui.theme.CalmNegativeDeep
import com.arango.base.ui.theme.CalmPositiveDeep
import com.arango.base.ui.theme.CalmSageSubtle
import com.arango.base.ui.theme.CalmTerracottaSubtle
import java.util.Locale

@Composable
fun AnalyticsScreen(
    uiState: FinanceUiState,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    onUpdateCategoryTarget: (CategoryItem, Double?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var editingTargetCategory by remember { mutableStateOf<CategoryItem?>(null) }

    val distinctBreakdown = remember(uiState.rankedBreakdown) {
        AnalyticsColorPalette.resolveDistinctCategoryColors(uiState.rankedBreakdown)
    }

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_lazy_column"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section with Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "FLOW OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Analytics & Flow",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Segment Toggle Pill (Material 3 Calm Style)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(9999.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isCurrent = uiState.currentPeriod == AnalyticsPeriod.CURRENT_PAYCHECK
                    // Tab: Current Paycheck
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onPeriodChange(AnalyticsPeriod.CURRENT_PAYCHECK) }
                            .padding(vertical = 10.dp)
                            .testTag("tab_current_paycheck"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Current Paycheck",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Tab: Last 30 Days
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (!isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onPeriodChange(AnalyticsPeriod.LAST_30_DAYS) }
                            .padding(vertical = 10.dp)
                            .testTag("tab_last_30_days"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Last 30 Days",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (!isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Period Summary Card (Tonal Elevation Level 1)
        item {
            PeriodSummaryCard(uiState = uiState)
        }

        // Expense Distribution Card (Flow bar & legend pills)
        item {
            ExpenseFlowCard(breakdown = distinctBreakdown)
        }

        // Ranked Breakdown Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RANKED BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "By expenditure",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Ranked Breakdown Items
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (distinctBreakdown.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenditures recorded for this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distinctBreakdown.forEachIndexed { index, item ->
                            RankedCategoryItemCard(
                                rank = index + 1,
                                item = item,
                                onClick = {
                                    val matchingCat = uiState.categories.firstOrNull {
                                        it.name.equals(item.category.name, ignoreCase = true)
                                    } ?: item.category
                                    editingTargetCategory = matchingCat
                                }
                            )
                        }
                    }
                }
            }
        }

        // Micro Note at Bottom
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Calculated completely on-device • zero third-party sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (editingTargetCategory != null) {
        val cat = editingTargetCategory!!
        CategorySpendingTargetDialog(
            category = cat,
            onDismiss = { editingTargetCategory = null },
            onSaveTarget = { newTarget ->
                onUpdateCategoryTarget(cat, newTarget)
                editingTargetCategory = null
            }
        )
    }
    }
}

@Composable
fun PeriodSummaryCard(uiState: FinanceUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dates row + days left pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = uiState.periodDateRangeStr,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val badgeText = if (uiState.currentPeriod == AnalyticsPeriod.CURRENT_PAYCHECK) {
                        "${uiState.daysLeftInPeriod} days left"
                    } else {
                        "Full cycle"
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Spent vs Income Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "TOTAL SPENT",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "$%,.2f", uiState.periodOut),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("analytics_total_spent")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL IN",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "$%,.2f", uiState.periodIn),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Calm Total Gauge Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(uiState.burnProgress.coerceIn(0.01f, 1f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val pct = (uiState.burnProgress * 100).toInt()
                    val net = uiState.periodIn - uiState.periodOut
                    val netText = if (net >= 0) {
                        String.format(Locale.US, "$%,.2f remaining", net)
                    } else {
                        String.format(Locale.US, "-$%,.2f deficit", -net)
                    }
                    Text(
                        text = "$pct% committed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = netText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (net >= 0) MaterialTheme.colorScheme.onSurfaceVariant else CalmNegativeDeep
                    )
                }
            }

            // Calming Pace Indicator Note
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = uiState.burnPaceNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseFlowCard(breakdown: List<CategorySpend>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expense Flow",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${breakdown.size} Categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Horizontal Proportional Segment Bar with Hairline Dividers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (breakdown.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        breakdown.forEachIndexed { index, item ->
                            val weight = item.percentageOfSpend.coerceAtLeast(0.02f)
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .height(16.dp)
                                    .background(item.category.color)
                            )
                            if (index < breakdown.lastIndex) {
                                Spacer(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(16.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                )
                            }
                        }
                    }
                }
            }

            // Legend Pills showing all active categories with distinct colors
            if (breakdown.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    breakdown.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(item.category.color)
                            )
                            val pct = (item.percentageOfSpend * 100).toInt()
                            Text(
                                text = "${item.category.name} $pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankedCategoryItemCard(
    rank: Int,
    item: CategorySpend,
    onClick: () -> Unit = {}
) {
    val effectiveTarget = item.effectiveTarget
    val hasTarget = effectiveTarget != null && effectiveTarget > 0.0
    val barFraction = if (hasTarget) {
        (item.totalAmount / effectiveTarget!!).toFloat().coerceIn(0.01f, 1f)
    } else {
        item.percentageOfSpend.coerceIn(0.01f, 1f)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("ranked_category_card_${item.category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(item.category.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.category.icon,
                            contentDescription = null,
                            tint = item.category.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "$rank. ${item.category.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val pct = (item.percentageOfSpend * 100).toInt()
                        Text(
                            text = "$pct% of spend • ${item.transactionCount} transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "-$%,.2f", item.totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = CalmNegativeDeep
                    )
                    Text(
                        text = String.format(Locale.US, "Avg $%,.2f", item.averageAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Proportional mini gauge (target progress when target is set, otherwise % of total spend)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(item.category.color)
                )
            }

            // Spending Target Row + Under/Over Pill Badge
            if (hasTarget) {
                val targetVal = effectiveTarget!!
                val diff = targetVal - item.totalAmount
                val isUnder = diff > 0.005
                val isOver = diff < -0.005
                val badgeBg = if (isOver) CalmTerracottaSubtle else CalmSageSubtle
                val badgeTextColor = if (isOver) CalmNegativeDeep else CalmPositiveDeep
                val badgeText = when {
                    isUnder -> String.format(Locale.US, "$%,.2f under target", diff)
                    isOver -> String.format(Locale.US, "$%,.2f over target", -diff)
                    else -> "On target"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_status_row_${item.category.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "Target: $%,.2f", targetVal),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("target_amount_text_${item.category.id}")
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("target_badge_${item.category.id}")
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = badgeTextColor
                        )
                    }
                }
            }
        }
    }
}
