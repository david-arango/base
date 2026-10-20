package com.arango.base.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.arango.base.data.model.CategoryItem
import com.arango.base.data.model.CategoryVisuals
import com.arango.base.data.model.DefaultCategories
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerSheet(
    isOpen: Boolean,
    categories: List<CategoryItem> = DefaultCategories.list,
    onDismiss: () -> Unit,
    onCreateCategory: (name: String, iconKey: String, colorHex: Long) -> Unit = { _, _, _ -> },
    onUpdateCategory: (id: Long, name: String, iconKey: String, colorHex: Long, oldName: String) -> Unit = { _, _, _, _, _ -> },
    onDeleteCategory: (id: Long, name: String) -> Unit = { _, _ -> },
    onCreateCategoryWithTarget: ((name: String, iconKey: String, colorHex: Long, spendingTarget: Double?) -> Unit)? = null,
    onUpdateCategoryWithTarget: ((id: Long, name: String, iconKey: String, colorHex: Long, oldName: String, spendingTarget: Double?) -> Unit)? = null,
    onSaveCategoryOrder: (List<CategoryItem>) -> Unit = {}
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isCreating by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }

    var currentList by remember(categories) { mutableStateOf(categories) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val currentListState by rememberUpdatedState(currentList)
    val onSaveCategoryOrderState by rememberUpdatedState(onSaveCategoryOrder)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { 68.dp.toPx() }

    LaunchedEffect(categories) {
        if (draggedItemIndex == null) {
            currentList = categories
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
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            if (isCreating || editingCategory != null) {
                val isNew = isCreating
                val editing = editingCategory
                val currentInitialName = if (isNew) "" else editing?.name ?: ""
                val currentInitialIconKey = if (isNew) "shopping_bag" else editing?.iconKey ?: "category"
                val currentInitialColor = if (isNew) null else editing?.color
                val currentInitialTarget = if (isNew) null else editing?.spendingTarget

                CategoryEditorContent(
                    title = if (isNew) "New Category" else "Edit Category",
                    initialName = currentInitialName,
                    initialIconKey = currentInitialIconKey,
                    initialColor = currentInitialColor,
                    initialSpendingTarget = currentInitialTarget,
                    confirmButtonText = if (isNew) "Create Category" else "Save Changes",
                    confirmButtonTag = if (isNew) "save_category_button" else "update_category_button",
                    onDismiss = {
                        isCreating = false
                        editingCategory = null
                    },
                    onConfirmWithTarget = { name, iconKey, colorHex, target ->
                        if (isNew) {
                            if (onCreateCategoryWithTarget != null) {
                                onCreateCategoryWithTarget(name, iconKey, colorHex, target)
                            } else {
                                onCreateCategory(name, iconKey, colorHex)
                            }
                            isCreating = false
                        } else if (editing != null) {
                            if (onUpdateCategoryWithTarget != null) {
                                onUpdateCategoryWithTarget(editing.dbId, name, iconKey, colorHex, editing.name, target)
                            } else {
                                onUpdateCategory(editing.dbId, name, iconKey, colorHex, editing.name)
                            }
                            editingCategory = null
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MANAGE CATEGORIES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Custom Categories",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_category_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { isCreating = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_add_category")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add New Category",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        currentList,
                        key = { _, item -> item.dbId.toString() + "_" + item.name }
                    ) { index, category ->
                        val isDragging = draggedItemIndex == index
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = if (isDragging) 8.dp else 0.dp,
                            tonalElevation = if (isDragging) 4.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationY = dragOffset
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                                }
                                .testTag("category_row_${category.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("drag_handle_${category.id}")
                                            .pointerInput(category.id) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        val startIndex = currentListState.indexOfFirst { it.id == category.id }.takeIf { it != -1 } ?: index
                                                        draggedItemIndex = startIndex
                                                        dragOffset = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedItemIndex = null
                                                        dragOffset = 0f
                                                        onSaveCategoryOrderState(currentListState)
                                                    },
                                                    onDragCancel = {
                                                        draggedItemIndex = null
                                                        dragOffset = 0f
                                                        onSaveCategoryOrderState(currentListState)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount.y
                                                        val threshold = itemHeightPx * 0.7f
                                                        while (dragOffset > threshold && (draggedItemIndex ?: 0) < currentListState.lastIndex) {
                                                            val currentIndex = draggedItemIndex ?: break
                                                            val nextIndex = currentIndex + 1
                                                            val updated = currentListState.toMutableList().apply {
                                                                val item = removeAt(currentIndex)
                                                                add(nextIndex, item)
                                                            }
                                                            currentList = updated
                                                            draggedItemIndex = nextIndex
                                                            dragOffset -= itemHeightPx
                                                        }
                                                        while (dragOffset < -threshold && (draggedItemIndex ?: 0) > 0) {
                                                            val currentIndex = draggedItemIndex ?: break
                                                            val prevIndex = currentIndex - 1
                                                            val updated = currentListState.toMutableList().apply {
                                                                val item = removeAt(currentIndex)
                                                                add(prevIndex, item)
                                                            }
                                                            currentList = updated
                                                            draggedItemIndex = prevIndex
                                                            dragOffset += itemHeightPx
                                                        }
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = "Reorder ${category.name}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (category.spendingTarget != null && category.spendingTarget > 0.0) {
                                            Text(
                                                text = String.format(Locale.US, "Target: $%,.2f / paycheck", category.spendingTarget),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.testTag("category_row_target_${category.id}")
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { editingCategory = category },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("edit_category_${category.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit ${category.name}",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { categoryToDelete = category },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_category_${category.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete ${category.name}",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(9999.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("done_category_manager_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Done", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = "Delete Category?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${cat.name}\"? Any transactions in this category will be reassigned to \"Other\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.dbId, cat.name)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_category_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_category_button")
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorContent(
    title: String,
    initialName: String = "",
    initialIconKey: String = "category",
    initialColor: Color? = null,
    initialSpendingTarget: Double? = null,
    confirmButtonText: String,
    confirmButtonTag: String,
    onDismiss: () -> Unit,
    onConfirmWithTarget: (name: String, iconKey: String, colorHex: Long, spendingTarget: Double?) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var targetInput by remember(initialSpendingTarget) {
        mutableStateOf(
            if (initialSpendingTarget != null && initialSpendingTarget > 0.0) {
                if (initialSpendingTarget % 1.0 == 0.0) {
                    initialSpendingTarget.toLong().toString()
                } else {
                    String.format(Locale.US, "%.2f", initialSpendingTarget)
                }
            } else ""
        )
    }
    var selectedIconKey by remember(initialIconKey) { mutableStateOf(initialIconKey) }
    var hasManuallyPickedIcon by remember { mutableStateOf(initialIconKey != "category" && initialIconKey != "shopping_bag") }
    var selectedColor by remember(initialColor, initialName, initialIconKey) {
        mutableStateOf(initialColor ?: CategoryVisuals.findMatchingColor(initialName, initialIconKey))
    }
    var hasManuallyPickedColor by remember { mutableStateOf(initialColor != null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_category_editor_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { newName ->
                name = newName
                if (!hasManuallyPickedIcon) {
                    CategoryVisuals.findMatchingIconKey(newName)?.let { matchedKey ->
                        selectedIconKey = matchedKey
                    }
                }
                if (!hasManuallyPickedColor) {
                    selectedColor = CategoryVisuals.findMatchingColor(newName, selectedIconKey)
                }
            },
            label = { Text("Category Name") },
            placeholder = { Text("e.g. Subscriptions") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_name_input")
        )

        OutlinedTextField(
            value = targetInput,
            onValueChange = { targetInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Spending Target / Paycheck (Optional)") },
            placeholder = { Text("e.g. 150.00") },
            leadingIcon = { Text("$", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_target_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cancel_category_editor_btn")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = {
                    val colorHex = try {
                        selectedColor.toArgb().toLong() and 0xFFFFFFFFL
                    } catch (_: Exception) {
                        selectedColor.value.toLong()
                    }
                    val parsedTarget = targetInput.toDoubleOrNull()?.takeIf { it > 0.0 }
                    onConfirmWithTarget(name.trim(), selectedIconKey, colorHex, parsedTarget)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag(confirmButtonTag)
            ) {
                Text(confirmButtonText)
            }
        }

        // Color Selection Row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Color",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 5
            ) {
                CategoryVisuals.colorOptions.forEachIndexed { index, color ->
                    val isColorSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable {
                                selectedColor = color
                                hasManuallyPickedColor = true
                            }
                            .then(
                                if (isColorSelected) {
                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("color_choice_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isColorSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected color",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Icon Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose Icon",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(selectedColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryVisuals.getIcon(selectedIconKey),
                        contentDescription = "Selected icon",
                        tint = selectedColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 5
                ) {
                    CategoryVisuals.iconOptions.forEach { (iconKey, vector) ->
                        val isSelected = selectedIconKey == iconKey
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable {
                                    selectedIconKey = iconKey
                                    hasManuallyPickedIcon = true
                                }
                                .testTag("icon_choice_$iconKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = CategoryVisuals.getIconLabel(iconKey),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
    title: String,
    initialName: String = "",
    initialIconKey: String = "category",
    initialColor: Color? = null,
    initialSpendingTarget: Double? = null,
    confirmButtonText: String,
    confirmButtonTag: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconKey: String, colorHex: Long) -> Unit = { _, _, _ -> },
    onConfirmWithTarget: ((name: String, iconKey: String, colorHex: Long, spendingTarget: Double?) -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        CategoryEditorContent(
            title = title,
            initialName = initialName,
            initialIconKey = initialIconKey,
            initialColor = initialColor,
            initialSpendingTarget = initialSpendingTarget,
            confirmButtonText = confirmButtonText,
            confirmButtonTag = confirmButtonTag,
            onDismiss = onDismiss,
            onConfirmWithTarget = { name, iconKey, colorHex, target ->
                if (onConfirmWithTarget != null) {
                    onConfirmWithTarget(name, iconKey, colorHex, target)
                } else {
                    onConfirm(name, iconKey, colorHex)
                }
            }
        )
    }
}

@Composable
fun CategoryEditorDialog(
    title: String,
    initialName: String = "",
    initialIconKey: String = "category",
    confirmButtonText: String,
    confirmButtonTag: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconKey: String) -> Unit
) {
    CategoryEditorDialog(
        title = title,
        initialName = initialName,
        initialIconKey = initialIconKey,
        initialColor = null,
        confirmButtonText = confirmButtonText,
        confirmButtonTag = confirmButtonTag,
        onDismiss = onDismiss,
        onConfirm = { name, iconKey, _ -> onConfirm(name, iconKey) }
    )
}
