package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.BankTransaction
import com.example.ui.components.getCategoryColor
import com.example.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val banks by viewModel.banks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "All", "Food & Dining", "Shopping", "Groceries",
        "Transport", "Bills & Utilities", "Health & Fitness", "Entertainment", "Income"
    )

    // Compute filtered list
    val filteredTransactions = remember(transactions, searchQuery, selectedCategoryFilter) {
        transactions.filter { tx ->
            val matchesQuery = tx.merchant.lowercase().contains(searchQuery.lowercase()) ||
                    tx.category.lowercase().contains(searchQuery.lowercase())
            val matchesCategory = selectedCategoryFilter == "All" || tx.category == selectedCategoryFilter
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        modifier = modifier.testTag("transactions_screen_root"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_transaction_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Ledger Entry")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search transactions...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_input")
            )

            // Category Filter Chips List
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category) },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("chip_filter_$category")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main List or Empty State
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Search empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions found",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Try refining filters or link account to pull transactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_card_${tx.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category colored circle representing icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(tx.category).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (tx.category) {
                                        "Food & Dining" -> Icons.Default.ShoppingCart
                                        "Groceries" -> Icons.Default.ShoppingCart
                                        "Shopping" -> Icons.Default.ShoppingCart
                                        "Transport" -> Icons.Default.PlayArrow
                                        "Bills & Utilities" -> Icons.Default.Settings
                                        "Health & Fitness" -> Icons.Default.Star
                                        "Entertainment" -> Icons.Default.Home
                                        "Income" -> Icons.Default.Check
                                        else -> Icons.Default.List
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tx.category,
                                        tint = getCategoryColor(tx.category),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tx.merchant,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (tx.isManual) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("Cash", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                        }
                                        if (tx.isPending) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("Pending", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.date))
                                        Text(
                                            text = "$dateStr • ${tx.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val isDeposit = tx.amount > 0
                                    Text(
                                        text = (if (isDeposit) "+" else "-") + String.format("$%.2f", kotlin.math.abs(tx.amount)),
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isDeposit) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                    )

                                    IconButton(
                                        onClick = { viewModel.deleteTransaction(tx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ledger item",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
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

    // MANUAL LOGGING SHEET DIALOG
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
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
                        text = "Add Ledger Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    var amountStr by remember { mutableStateOf("") }
                    var merchantStr by remember { mutableStateOf("") }
                    var isIncomeSelected by remember { mutableStateOf(false) } // False = Expense, True = Income
                    var selectedCategory by remember { mutableStateOf("Food & Dining") }
                    var selectedBankId by remember { mutableStateOf("manual") }

                    // Toggle Expense vs Income
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Expense",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = if (!isIncomeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isIncomeSelected = false
                                    if (selectedCategory == "Income") selectedCategory = "Food & Dining"
                                }
                                .background(if (!isIncomeSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 12.dp)
                        )
                        Text(
                            text = "Income Credit",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncomeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isIncomeSelected = true
                                    selectedCategory = "Income"
                                }
                                .background(if (isIncomeSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input")
                    )

                    OutlinedTextField(
                        value = merchantStr,
                        onValueChange = { merchantStr = it },
                        label = { Text(if (isIncomeSelected) "Depositor / Source" else "Merchant / Payee") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("merchant_input")
                    )

                    // Category dropdown list
                    if (!isIncomeSelected) {
                        Text("Spend Category", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        val nonIncomeCategories = categories.filter { it != "All" && it != "Income" }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(nonIncomeCategories) { cat ->
                                ElevatedFilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }

                    // Account source select
                    Text("Impact Bank Account", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            ElevatedFilterChip(
                                selected = selectedBankId == "manual",
                                onClick = { selectedBankId = "manual" },
                                label = { Text("Cash (No bank balance change)") }
                            )
                        }
                        items(banks) { bank ->
                            ElevatedFilterChip(
                                selected = selectedBankId == bank.id,
                                onClick = { selectedBankId = bank.id },
                                label = { Text(bank.bankName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val amtVal = amountStr.toDoubleOrNull() ?: 0.0
                                if (amtVal > 0 && merchantStr.isNotBlank()) {
                                    if (isIncomeSelected) {
                                        viewModel.addManualIncome(
                                            amount = amtVal,
                                            source = merchantStr,
                                            date = System.currentTimeMillis(),
                                            bankId = selectedBankId
                                        )
                                    } else {
                                        viewModel.addManualExpense(
                                            amount = amtVal,
                                            merchant = merchantStr,
                                            category = selectedCategory,
                                            date = System.currentTimeMillis(),
                                            bankId = selectedBankId
                                        )
                                    }
                                    showAddDialog = false
                                }
                            },
                            enabled = amountStr.toDoubleOrNull() != null && merchantStr.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_transaction_button")
                        ) {
                            Text("Save Entry")
                        }
                    }
                }
            }
        }
    }
}
