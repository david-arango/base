package com.arango.base.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arango.base.data.local.OffsetTransactionEntity
import com.arango.base.ui.theme.CalmNegativeDeep
import com.arango.base.ui.theme.CalmPositiveDeep
import com.arango.base.ui.theme.CalmSageSubtle
import com.arango.base.ui.theme.CalmTerracottaSubtle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffsetBudgetSheet(
    isOpen: Boolean,
    currentOffset: Double,
    offsetTransactions: List<OffsetTransactionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAddOffset: (Double, String) -> Unit,
    onDeleteOffset: (Long) -> Unit
) {
    if (!isOpen) return

    var rawAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isNegative by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Balance Offset",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Add invisible offset transactions that adjust your current balance up or down. These adjustments do not appear in your ledger or analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Current Total Offset Summary Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT NET OFFSET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val offsetSign = if (currentOffset > 0) "+" else if (currentOffset < 0) "-" else ""
                        val offsetColor = if (currentOffset > 0) CalmPositiveDeep else if (currentOffset < 0) CalmNegativeDeep else MaterialTheme.colorScheme.onSurface
                        Text(
                            text = "$offsetSign$${String.format(Locale.US, "%,.2f", abs(currentOffset))}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = offsetColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${offsetTransactions.size} offset${if (offsetTransactions.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NEW OFFSET ADJUSTMENT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            // Up / Down toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9999.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (!isNegative) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isNegative = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Positive Balance",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (!isNegative) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (!isNegative) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isNegative) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isNegative = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "- Negative Balance",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isNegative) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isNegative) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Offset Amount Input Field
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
                label = { Text("Adjustment Amount") },
                leadingIcon = {
                    Text(
                        text = if (isNegative) "-$" else "+$",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isNegative) CalmNegativeDeep else CalmPositiveDeep,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                placeholder = {
                    Text(
                        "0.00",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
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
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("starting_balance_display")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Optional note
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 60) note = it },
                label = { Text("Reason or Note (Optional)") },
                placeholder = { Text("e.g. Starting balance, Bank discrepancy") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val parsed = rawAmount.toDoubleOrNull() ?: 0.0
                        if (parsed > 0) {
                            val signed = if (isNegative) -parsed else parsed
                            val finalNote = note.ifBlank { "Offset adjustment" }
                            onAddOffset(signed, finalNote)
                            rawAmount = ""
                            note = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offset_note_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        val parsed = rawAmount.toDoubleOrNull() ?: 0.0
                        if (parsed > 0.0) {
                            val signed = if (isNegative) -parsed else parsed
                            val finalNote = note.ifBlank { "Offset adjustment" }
                            onAddOffset(signed, finalNote)
                            rawAmount = ""
                            note = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(46.dp)
                        .testTag("save_starting_balance_btn")
                ) {
                    Text("Apply Offset")
                }
            }

            // Offset Transactions History
            if (offsetTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RECORDED OFFSETS (${offsetTransactions.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offset_transactions_list")
                ) {
                    Column {
                        offsetTransactions.forEachIndexed { index, tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("offset_row_${tx.id}")
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val isPos = tx.amount >= 0
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isPos) CalmSageSubtle else CalmTerracottaSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isPos) "+" else "-",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isPos) CalmPositiveDeep else CalmNegativeDeep
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = tx.note.ifBlank { "Offset adjustment" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = dateFormat.format(Date(tx.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isPos = tx.amount >= 0
                                    Text(
                                        text = "${if (isPos) "+" else "-"}$${String.format(Locale.US, "%.2f", abs(tx.amount))}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isPos) CalmPositiveDeep else CalmNegativeDeep
                                    )

                                    IconButton(
                                        onClick = { onDeleteOffset(tx.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_offset_btn_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete offset",
                                            tint = CalmNegativeDeep.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (index < offsetTransactions.size - 1) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Deprecated(
    message = "Use OffsetBudgetSheet instead. StartingBalanceDialog is kept as an adapter for backward compatibility.",
    replaceWith = ReplaceWith("OffsetBudgetSheet(isOpen, currentOffset, emptyList(), onDismiss, { amount, _ -> onConfirm(amount) }, {})")
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartingBalanceDialog(
    isOpen: Boolean,
    currentOffset: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    OffsetBudgetSheet(
        isOpen = isOpen,
        currentOffset = currentOffset,
        offsetTransactions = emptyList(),
        onDismiss = onDismiss,
        onAddOffset = { amount, _ -> onConfirm(amount) },
        onDeleteOffset = {}
    )
}
