package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.DonutChart
import com.example.ui.components.getCategoryColor
import com.example.viewmodel.BudgetStatus
import com.example.viewmodel.BudgetViewModel
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val budgetStatusList by viewModel.budgetStatusList.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCategoryToEdit by remember { mutableStateOf("") }
    var currentLimitToEdit by remember { mutableStateOf(200.0) }

    val categoriesWithNoBudgets = remember(budgetStatusList) {
        val activeCats = budgetStatusList.map { it.category }
        listOf("Food & Dining", "Shopping", "Groceries", "Transport", "Bills & Utilities", "Health & Fitness", "Entertainment")
            .filter { !activeCats.contains(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budgets_screen_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Headers
        item {
            Column {
                Text(
                    text = "Spending Budgets",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Track and shape automated spending categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Budget Allocated Donut Chart Panel
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DonutChart(
                        data = categorySpending,
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp),
                        thickness = 30f
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Core Categories",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        categorySpending.keys.take(3).forEach { cat ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(getCategoryColor(cat), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(cat, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Add Limit option if any categories don't have limit
        if (categoriesWithNoBudgets.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Untracked Spent Categories Available",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shape limits to prevent budget overrun warnings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick buttons to click and track
                        FlowRow(
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoriesWithNoBudgets.forEach { cat ->
                                Button(
                                    onClick = {
                                        selectedCategoryToEdit = cat
                                        currentLimitToEdit = 200.0
                                        showEditDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("add_budget_chip_$cat")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(cat, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active tracked budgets list
        if (budgetStatusList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No budget rules configured yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(budgetStatusList, key = { it.category }) { status ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_row_${status.category}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(getCategoryColor(status.category), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = status.category,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedCategoryToEdit = status.category
                                        currentLimitToEdit = status.limit
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.size(28.dp).testTag("edit_budget_${status.category}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Edit limits boundary",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.removeBudget(status.category) },
                                    modifier = Modifier.size(28.dp).testTag("delete_budget_${status.category}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove category constraint",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress limit statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("Spent: $%.2f", status.spent),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isAlertTriggered) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("Limit: $%.2f", status.limit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Spent visual bar
                        val targetColor = when {
                            status.percent >= 1.0f -> Color(0xFFEF4444)     // Exceeded
                            status.percent >= 0.8f -> Color(0xFFF59E0B)     // Alert zone
                            else -> Color(0xFF10B981)                       // Healthy emerald
                        }

                        LinearProgressIndicator(
                            progress = status.percent.coerceIn(0f, 1f),
                            color = targetColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Alarm feedback
                        if (status.percent >= 1.0f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Breach Logo",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("Overlimit by $%.2f!", status.spent - status.limit),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        } else if (status.isAlertTriggered) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning Logo",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Approaching limits boundary! (>=80% consumed)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        } else {
                            Text(
                                text = String.format("$%.2f remaining this month", status.remaining),
                                fontSize = 11.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }
    }

    // LIMIT EDIT MODAL SHEET
    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set Budget: $selectedCategoryToEdit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    var limitStr by remember { mutableStateOf(currentLimitToEdit.roundToInt().toString()) }

                    OutlinedTextField(
                        value = limitStr,
                        onValueChange = { limitStr = it },
                        label = { Text("Monthly Alert Cap ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_limit_input")
                    )

                    Text(
                        text = "Slide to adjust threshold:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val sliderValue = limitStr.toFloatOrNull() ?: 100f
                    Slider(
                        value = sliderValue.coerceIn(10f, 1000f),
                        onValueChange = { limitStr = it.roundToInt().toString() },
                        valueRange = 10f..1000f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth().testTag("budget_threshold_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val limitVal = limitStr.toDoubleOrNull() ?: 0.0
                                if (limitVal > 0) {
                                    viewModel.setBudgetLimit(selectedCategoryToEdit, limitVal)
                                    showEditDialog = false
                                }
                            },
                            enabled = limitStr.toDoubleOrNull() != null && (limitStr.toDoubleOrNull() ?: 0.0) > 0,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_budget_button")
                        ) {
                            Text("Save Cap")
                        }
                    }
                }
            }
        }
    }
}

// Inline FlowRow helper since standard FlowRow can be absent or experimental in basic material layouts
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val mainAxisSpacingPx = mainAxisSpacing.roundToPx()
        val crossAxisSpacingPx = crossAxisSpacing.roundToPx()

        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        val rowHeights = mutableListOf<Int>()
        val rowWidths = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                rowHeights.add(currentRowHeight)
                rowWidths.add(currentRowWidth - mainAxisSpacingPx)

                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainAxisSpacingPx
            currentRowHeight = kotlin.math.max(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowHeights.add(currentRowHeight)
            rowWidths.add(currentRowWidth - mainAxisSpacingPx)
        }

        val totalHeight = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * crossAxisSpacingPx
        val totalWidth = rowWidths.maxOrNull() ?: 0

        layout(
            width = constraints.maxWidth,
            height = totalHeight
        ) {
            var currentY = 0
            rows.forEachIndexed { rowIndex, row ->
                var currentX = 0
                val rowHeight = rowHeights[rowIndex]
                row.forEach { placeable ->
                    placeable.placeRelative(x = currentX, y = currentY)
                    currentX += placeable.width + mainAxisSpacingPx
                }
                currentY += rowHeight + crossAxisSpacingPx
            }
        }
    }
}
