package com.arango.base.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arango.base.data.local.IncomeEngineEntity
import com.arango.base.ui.theme.CalmDividerHairline
import com.arango.base.ui.theme.CalmPositiveDeep
import com.arango.base.ui.theme.CalmPrimary
import com.arango.base.ui.theme.CalmSecondaryContainer
import com.arango.base.ui.theme.CalmSurfaceContainer
import com.arango.base.ui.theme.CalmSurfaceContainerHigh
import com.arango.base.ui.theme.CalmSurfaceContainerLowest
import com.arango.base.ui.theme.CalmTextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizard(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onComplete: (startingBalance: Double, incomeConfig: IncomeEngineEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    // Wizard mutable draft state
    var startingBalanceText by remember { mutableStateOf("") }
    var incomeAmountText by remember { mutableStateOf("") }
    var selectedCadence by remember { mutableStateOf("Bi-Weekly") }
    var nextPaydayTimestamp by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 14)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_wizard_surface"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar: Step Indicators + Skip/Close Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "step_indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(width)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) CalmPrimary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                // Top Right Action: "Skip" button or Close
                if (pagerState.currentPage < 3) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = CalmTextMuted
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("onboarding_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CalmTextMuted
                        )
                    }
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPhilosophySlide(
                        onContinue = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                    1 -> OnboardingBalanceSlide(
                        amountText = startingBalanceText,
                        onAmountChange = { startingBalanceText = it },
                        onContinue = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        onSkip = {
                            startingBalanceText = ""
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                    2 -> OnboardingIncomeSlide(
                        incomeAmountText = incomeAmountText,
                        onIncomeAmountChange = { incomeAmountText = it },
                        cadence = selectedCadence,
                        onCadenceChange = { selectedCadence = it },
                        nextPayday = nextPaydayTimestamp,
                        onOpenDatePicker = { showDatePicker = true },
                        onContinue = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        onSkip = {
                            incomeAmountText = ""
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                    )
                    3 -> OnboardingSummarySlide(
                        onFinish = {
                            val balance = startingBalanceText.replace(",", "").toDoubleOrNull() ?: 0.0
                            val incomeAmount = incomeAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
                            val incomeConfig = if (incomeAmount > 0.0) {
                                IncomeEngineEntity(
                                    id = 1,
                                    expectedIncome = incomeAmount,
                                    cadence = selectedCadence,
                                    nextPaydayTimestamp = nextPaydayTimestamp,
                                    isActive = true
                                )
                            } else null

                            onComplete(balance, incomeConfig)
                        }
                    )
                }
            }
        }
    }

    // Payday DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextPaydayTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            nextPaydayTimestamp = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = CalmPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = CalmTextMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 1: Welcome & Philosophy
// ---------------------------------------------------------------------------
@Composable
private fun OnboardingPhilosophySlide(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CalmSecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CalmPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Private by Design,\nPerpetual by Nature",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Base is built differently from traditional finance apps, centered on two core principles:",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = CalmTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Pillar 1: Offline
            FeaturePillarCard(
                icon = Icons.Default.WifiOff,
                title = "100% Offline & Private",
                description = "Your financial data stays exclusively on your device in local SQLite storage. No accounts, no cloud sync, and zero tracking."
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pillar 2: Perpetual balance
            FeaturePillarCard(
                icon = Icons.Default.Timeline,
                title = "Perpetual Rolling Balance",
                description = "Financial reality doesn't reset when the calendar flips to the 1st. Base tracks continuous cash runway so you always know your true liquidity."
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalmPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_philosophy_continue_button")
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 2: Starting Balance (Runway Offset)
// ---------------------------------------------------------------------------
@Composable
private fun OnboardingBalanceSlide(
    amountText: String,
    onAmountChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CalmSecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = CalmPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "What is your current balance?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter your current checking or liquid cash balance. Base uses discrete balance adjustments to keep your expense analytics clean.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = CalmTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        onAmountChange(filtered)
                    }
                },
                label = { Text("Starting Balance") },
                prefix = {
                    Text(
                        text = "$ ",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                        color = CalmPrimary
                    )
                },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CalmPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = CalmPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_starting_balance_field")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalmPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_balance_continue_button")
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("onboarding_balance_skip_button")
            ) {
                Text(
                    text = "Skip this step",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    color = CalmTextMuted
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 3: Income Engine Schedule
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingIncomeSlide(
    incomeAmountText: String,
    onIncomeAmountChange: (String) -> Unit,
    cadence: String,
    onCadenceChange: (String) -> Unit,
    nextPayday: Long,
    onOpenDatePicker: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val formattedDate = remember(nextPayday) {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
        sdf.format(Date(nextPayday))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CalmSecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = CalmPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Automate your paycheck schedule",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Base automatically books your income and computes remaining runway days until next payday.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = CalmTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Irregular income note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CalmSurfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "For predictable paychecks, set up your schedule here. Irregular or variable income can always be added manually directly in the ledger.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    ),
                    color = CalmTextMuted,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Net amount field
            OutlinedTextField(
                value = incomeAmountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        onIncomeAmountChange(filtered)
                    }
                },
                label = { Text("Expected Net Paycheck") },
                prefix = {
                    Text(
                        text = "$ ",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = CalmPrimary
                    )
                },
                placeholder = { Text("2,500.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CalmPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = CalmPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_income_amount_field")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cadence Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Pay Cadence",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Weekly", "Bi-Weekly", "Monthly").forEach { cadenceOption ->
                        val isSelected = cadence.equals(cadenceOption, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCadenceChange(cadenceOption) },
                            label = { Text(cadenceOption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmSecondaryContainer,
                                selectedLabelColor = CalmPrimary
                            ),
                            shape = RoundedCornerShape(9999.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Next Payday Field (Clickable)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CalmSurfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDatePicker() }
                    .testTag("onboarding_payday_picker_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "First / Next Payday",
                            style = MaterialTheme.typography.labelSmall,
                            color = CalmTextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date",
                        tint = CalmPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalmPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_income_continue_button")
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("onboarding_income_skip_button")
            ) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    color = CalmTextMuted
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 4: All Set & Feature Overview
// ---------------------------------------------------------------------------
@Composable
private fun OnboardingSummarySlide(
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CalmSecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CalmPositiveDeep,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "You're ready to take control",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 23.sp,
                    letterSpacing = (-0.4).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Three essential features to guide your daily spending:",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = CalmTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            FeaturePillarCard(
                icon = Icons.Default.Timeline,
                title = "Perpetual Runway",
                description = "Monitor your daily burn pace and real-time cash runway buffer between deposits."
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeaturePillarCard(
                icon = Icons.Default.PieChart,
                title = "Flow Analytics",
                description = "Clear, perceptually tuned category breakdowns for trailing 30 days or current cycle."
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeaturePillarCard(
                icon = Icons.Default.Tune,
                title = "Custom Categories & CSV Import",
                description = "Customize 50+ Material category icons or import your bank CSV statement anytime in Settings."
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalmPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_start_tracking_button")
            ) {
                Text(
                    text = "Start Tracking",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Reusable Feature Pillar Card
// ---------------------------------------------------------------------------
@Composable
private fun FeaturePillarCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CalmSurfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CalmSecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CalmPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = CalmTextMuted
                )
            }
        }
    }
}
