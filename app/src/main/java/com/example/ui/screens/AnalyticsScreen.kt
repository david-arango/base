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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.AnalyticsPeriod
import com.example.ui.CategorySpend
import com.example.ui.FinanceUiState
import com.example.ui.theme.CalmNegativeDeep
import com.example.ui.theme.CalmOnPrimary
import com.example.ui.theme.CalmOnSecondaryContainer
import com.example.ui.theme.CalmOutlineVariant
import com.example.ui.theme.CalmPositiveDeep
import com.example.ui.theme.CalmPrimary
import com.example.ui.theme.CalmSecondary
import com.example.ui.theme.CalmSecondaryContainer
import com.example.ui.theme.CalmSurfaceContainer
import com.example.ui.theme.CalmSurfaceContainerHigh
import com.example.ui.theme.CalmSurfaceContainerLow
import com.example.ui.theme.CalmSurfaceContainerLowest
import com.example.ui.theme.CalmTextMuted
import java.util.Locale

@Composable
fun AnalyticsScreen(
    uiState: FinanceUiState,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    var isInfoBannerVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section with Title & Info Dialog Trigger
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FLOW OVERVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = CalmTextMuted
                    )
                    Text(
                        text = "Analytics & Flow",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CalmSurfaceContainer)
                        .clickable { isInfoBannerVisible = !isInfoBannerVisible }
                        .testTag("toggle_info_banner_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "About rolling budget flow",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Soft Explanatory Tooltip Toast (Toggleable)
        item {
            AnimatedVisibility(
                visible = isInfoBannerVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = CalmSurfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = null,
                            tint = CalmSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Non-Judgmental Tracking",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Cash flow rolls smoothly with no artificial month-end cliff. Stay aware of your daily pace without arbitrary restrictions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = { isInfoBannerVisible = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = CalmTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Segment Toggle Pill (Material 3 Calm Style)
        item {
            Surface(
                color = CalmSurfaceContainer,
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
                            .background(if (isCurrent) CalmPrimary else Color.Transparent)
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
                            color = if (isCurrent) CalmOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Tab: Last 30 Days
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (!isCurrent) CalmPrimary else Color.Transparent)
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
                            color = if (!isCurrent) CalmOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
            ExpenseFlowCard(breakdown = uiState.rankedBreakdown)
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
                    color = CalmTextMuted
                )
                Text(
                    text = "By expenditure",
                    style = MaterialTheme.typography.labelSmall,
                    color = CalmTextMuted
                )
            }
        }

        // Ranked Breakdown Items
        item {
            Surface(
                color = CalmSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.rankedBreakdown.isEmpty()) {
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
                        uiState.rankedBreakdown.forEachIndexed { index, item ->
                            RankedCategoryItemCard(
                                rank = index + 1,
                                item = item
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
                    tint = CalmTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Calculated completely on-device • zero third-party sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = CalmTextMuted
                )
            }
        }
    }
}

@Composable
fun PeriodSummaryCard(uiState: FinanceUiState) {
    Surface(
        color = CalmSurfaceContainer,
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
                        tint = CalmSecondary,
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
                        .background(CalmSecondaryContainer)
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
                        color = CalmOnSecondaryContainer
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
                        color = CalmTextMuted
                    )
                    Text(
                        text = String.format(Locale.US, "$%,.2f", uiState.periodOut),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("analytics_total_spent")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPECTED IN",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = CalmTextMuted
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
                        .background(CalmSurfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(uiState.burnProgress.coerceIn(0.01f, 1f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(CalmSecondary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val pct = (uiState.burnProgress * 100).toInt()
                    val remaining = (uiState.periodIn - uiState.periodOut).coerceAtLeast(0.0)
                    Text(
                        text = "$pct% committed",
                        style = MaterialTheme.typography.labelSmall,
                        color = CalmTextMuted
                    )
                    Text(
                        text = String.format(Locale.US, "$%,.2f remaining", remaining),
                        style = MaterialTheme.typography.labelSmall,
                        color = CalmTextMuted
                    )
                }
            }

            // Calming Pace Indicator Note
            Surface(
                color = CalmSurfaceContainerLow,
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
                            .background(CalmSecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = CalmOnSecondaryContainer,
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

@Composable
fun ExpenseFlowCard(breakdown: List<CategorySpend>) {
    Surface(
        color = CalmSurfaceContainer,
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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${breakdown.size} Categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = CalmTextMuted
                )
            }

            // Horizontal Proportional Segment Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(CalmSurfaceContainerHigh)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    breakdown.forEach { item ->
                        val weight = item.percentageOfSpend.coerceAtLeast(0.02f)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(14.dp)
                                .background(item.category.color)
                        )
                    }
                }
            }

            // Legend Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                breakdown.take(3).forEach { item ->
                    Row(
                        modifier = Modifier.weight(1f),
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

@Composable
fun RankedCategoryItemCard(
    rank: Int,
    item: CategorySpend
) {
    Surface(
        color = CalmSurfaceContainerLowest,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
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
                            color = MaterialTheme.colorScheme.primary
                        )
                        val pct = (item.percentageOfSpend * 100).toInt()
                        Text(
                            text = "$pct% of spend • ${item.transactionCount} transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = CalmTextMuted
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
                        color = CalmTextMuted
                    )
                }
            }

            // Proportional mini gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(CalmSurfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.percentageOfSpend.coerceIn(0.01f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(item.category.color)
                )
            }
        }
    }
}
