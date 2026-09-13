package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCategories
import com.example.ui.theme.CalmDividerHairline
import com.example.ui.theme.CalmNegativeDeep
import com.example.ui.theme.CalmOutlineVariant
import com.example.ui.theme.CalmPositiveDeep
import com.example.ui.theme.CalmPrimary
import com.example.ui.theme.CalmSage
import com.example.ui.theme.CalmSurface
import com.example.ui.theme.CalmSurfaceContainer
import com.example.ui.theme.CalmSurfaceContainerHigh
import com.example.ui.theme.CalmSurfaceContainerLow
import com.example.ui.theme.CalmSurfaceContainerLowest
import com.example.ui.theme.CalmTerracotta
import com.example.ui.theme.CalmTextMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionSheet(
    isOpen: Boolean,
    categories: List<CategoryItem> = DefaultCategories.list,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, amount: Double, isExpense: Boolean, note: String, timestamp: Long) -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isExpense by remember { mutableStateOf(true) }
    var rawAmount by remember { mutableStateOf("") }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()?.name ?: "Groceries") }
    var noteText by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedDate = remember(selectedTimestamp) {
        val nowCal = Calendar.getInstance()
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val isToday = nowCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) {
            val sdf = SimpleDateFormat("MMM d", Locale.US)
            "Today, " + sdf.format(Date(selectedTimestamp))
        } else {
            val sdf = SimpleDateFormat("EEE, MMM d", Locale.US)
            sdf.format(Date(selectedTimestamp))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
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
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header Row: Close, Title, Expense/Income Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_transaction_sheet_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "New Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
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
                    // Expense Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isExpense) CalmTerracotta else Color.Transparent)
                            .clickable { isExpense = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("toggle_expense_tab"),
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

                    // Income Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (!isExpense) CalmPositiveDeep else Color.Transparent)
                            .clickable { isExpense = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("toggle_income_tab"),
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

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // Subtitle + Date Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpense) "ENTER EXPENSE AMOUNT" else "ENTER INCOME AMOUNT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isExpense) CalmTerracotta else CalmPositiveDeep
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("select_transaction_date_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Massive Typographic Amount Display with Blinking Cursor
                val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
                val cursorAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "cursor_alpha"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpense) "-$" else "+$",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isExpense) CalmTerracotta else CalmPositiveDeep
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (rawAmount.isEmpty()) "0.00" else rawAmount,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("display_amount_text")
                    )
                    // Active Pulsing Cursor
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .width(2.5.dp)
                            .height(30.dp)
                            .alpha(cursorAlpha)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                    )
                }

                // Horizontal Category Chips
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category.name
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { selectedCategory = category.name }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("cat_chip_${category.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.name,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .padding(top = 8.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("note_input_field")
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

                // Save Transaction Primary Button
                val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0
                val amountStr = String.format(Locale.US, "%.2f", parsedAmount)
                Button(
                    onClick = {
                        if (parsedAmount > 0) {
                            onSave(
                                noteText.ifBlank { selectedCategory },
                                selectedCategory,
                                parsedAmount,
                                isExpense,
                                noteText,
                                selectedTimestamp
                            )
                        }
                    },
                    enabled = parsedAmount > 0.0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_transaction_button")
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

            // Realistic Touch Numeric Keypad
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Mini Gboard header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Keyboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Google Keyboard",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.MoreHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Keypad rows
                    val keyRows = listOf(
                        listOf(Pair("1", ""), Pair("2", "abc"), Pair("3", "def")),
                        listOf(Pair("4", "ghi"), Pair("5", "jkl"), Pair("6", "mno")),
                        listOf(Pair("7", "pqrs"), Pair("8", "tuv"), Pair("9", "wxyz")),
                        listOf(Pair(".", ""), Pair("0", ""), Pair("backspace", ""))
                    )

                    keyRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowKeys.forEach { (primary, sub) ->
                                val isAction = primary == "." || primary == "backspace"
                                Surface(
                                    color = if (isAction) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable {
                                            when (primary) {
                                                "backspace" -> {
                                                    if (rawAmount.isNotEmpty()) {
                                                        rawAmount = rawAmount.dropLast(1)
                                                    }
                                                }
                                                "." -> {
                                                    if (!rawAmount.contains(".")) {
                                                        rawAmount = if (rawAmount.isEmpty()) "0." else "$rawAmount."
                                                    }
                                                }
                                                else -> {
                                                    if (rawAmount == "0") {
                                                        rawAmount = primary
                                                    } else {
                                                        val parts = rawAmount.split(".")
                                                        if (parts.size <= 1 || parts[1].length < 2) {
                                                            if (rawAmount.length < 8) {
                                                                rawAmount += primary
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("keypad_key_${if (primary == ".") "dot" else primary}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        if (primary == "backspace") {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        } else if (sub.isNotEmpty()) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = primary,
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Normal
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = primary,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontSize = if (primary == ".") 22.sp else 18.sp,
                                                    fontWeight = if (primary == ".") FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedTimestamp = it + (12 * 60 * 60 * 1000)
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("confirm_date_picker_button")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.testTag("cancel_date_picker_button")
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
