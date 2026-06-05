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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.LinkedBank
import com.example.data.database.SyncLog
import com.example.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountsScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val banks by viewModel.banks.collectAsState()
    val isAutoSync by viewModel.isAutoSyncRunning.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()

    var showLinkDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Action Bar with Add Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Linked Institutions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Manage auto-syncing bank credentials",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showLinkDialog = true },
                    modifier = Modifier.testTag("add_account_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Bank Link")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Link Bank")
                }
            }
        }

        // Live Background Sync config card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Banking Synchronizer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Fetch transactions automatically in background every 20s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isAutoSync,
                        onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                        modifier = Modifier.testTag("auto_sync_switch")
                    )
                }
            }
        }

        // Configuration status tip if Plaid API isn't set
        if (!viewModel.isPlaidConfigured) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Info Tag",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Using Local Bank Simulator",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "The system is currently running in zero-dependency Demo Bank sync mode. " +
                                    "To connect your live banking feed, register a free developer login at (dashboard.plaid.com) " +
                                    "and configure PLAID_CLIENT_ID and PLAID_SECRET inside the AI Studio Secrets panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Active Links Listing
        if (banks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No bank accounts linked yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap 'Link Bank' above to start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(banks) { bank ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Institution symbol",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bank.bankName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${bank.accountName} (${bank.accountNumber})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("$%,.2f", bank.balance),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (bank.balance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (bank.status == "CONNECTED") Color(0xFF10B981) else Color(0xFFF59E0B),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = bank.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // SQLite Daemon Sync Logs
        item {
            Text(
                text = "Live Sync Audit Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (syncLogs.isEmpty()) {
            item {
                Text(
                    text = "Sync audit log has not been generated yet. Sync a credential to see audit trails.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        } else {
            items(syncLogs.take(8)) { log ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (log.status == "SUCCESS") Color(0xFF10B981) else Color(0xFFEF4444),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (log.status == "SUCCESS") Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }

                            val timeStr = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.resetAllData() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_data_button")
            ) {
                Text("Clear All Local Budgets & Link Data")
            }
        }
    }

    // SIMULATED PLAID SECURE FLOW LINK MODAL DIALOG
    if (showLinkDialog) {
        Dialog(
            onDismissRequest = { showLinkDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            ) {
                var currentStep by remember { mutableStateOf(1) } // 1: Select Bank, 2: Login flow, 3: Link Accounts complete

                var selectedBank by remember { mutableStateOf("") }
                var bankUser by remember { mutableStateOf("") }
                var bankPass by remember { mutableStateOf("") }

                var accountNickname by remember { mutableStateOf("") }
                var selectedAccountType by remember { mutableStateOf("Checking Account") }
                var mockInitialBalance by remember { mutableStateOf("3250.00") }
                var customAccountNumber by remember { mutableStateOf("4829") }

                val bankList = listOf(
                    "Chase Bank",
                    "Wells Fargo",
                    "Bank of America",
                    "Capital One",
                    "Navy Federal CU",
                    "Fidelity Investments"
                )

                AnimatedContent(
                    targetState = currentStep,
                    label = "LinkBankTransition"
                ) { step ->
                    when (step) {
                        1 -> { // STEP 1: Select Institution
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Link Bank (Plaid)",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { showLinkDialog = false }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Close Connector")
                                    }
                                }

                                Text(
                                    text = "Select your institution to establish a secure, live transaction synchronizer link.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                bankList.forEach { bank ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedBank = bank
                                                accountNickname = "$bank Checking"
                                                currentStep = 2
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedBank == bank) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (selectedBank == bank) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = bank,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedBank == bank) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Or custom bank name input
                                var customBankName by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = customBankName,
                                    onValueChange = { customBankName = it },
                                    label = { Text("Or Type Custom Bank Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (customBankName.isNotBlank()) {
                                            selectedBank = customBankName
                                            accountNickname = "$customBankName Account"
                                            currentStep = 2
                                        }
                                    },
                                    enabled = customBankName.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Link Custom Bank")
                                }
                            }
                        }

                        2 -> { // STEP 2: Login and Credentials Mock
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Log In: $selectedBank",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { currentStep = 1 }) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                }

                                Text(
                                    text = "Enter secure credentials to establish authentication. Credentials are encrypted and never stored.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = bankUser,
                                    onValueChange = { bankUser = it },
                                    label = { Text("Online ID / Username") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("username_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = bankPass,
                                    onValueChange = { bankPass = it },
                                    label = { Text("Password/PIN") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Demo hint: In sandboxed environment you can enter any username and password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { currentStep = 3 },
                                    enabled = bankUser.isNotBlank() && bankPass.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verify & Authenticate")
                                }
                            }
                        }

                        3 -> { // STEP 3: Configure account type, starting deposits, and account number
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "Link Account: $selectedBank",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select account type and configure initial parameters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = accountNickname,
                                    onValueChange = { accountNickname = it },
                                    label = { Text("Account Nickname") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Account Type", style = MaterialTheme.typography.labelMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ElevatedFilterChip(
                                        selected = selectedAccountType == "Checking Account",
                                        onClick = { selectedAccountType = "Checking Account" },
                                        label = { Text("Checking") }
                                    )
                                    ElevatedFilterChip(
                                        selected = selectedAccountType == "Savings Account",
                                        onClick = { selectedAccountType = "Savings Account" },
                                        label = { Text("Savings") }
                                    )
                                    ElevatedFilterChip(
                                        selected = selectedAccountType == "Credit Card",
                                        onClick = { selectedAccountType = "Credit Card" },
                                        label = { Text("Credit") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = mockInitialBalance,
                                        onValueChange = { mockInitialBalance = it },
                                        label = { Text("Initial Balance ($)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = customAccountNumber,
                                        onValueChange = { customAccountNumber = it },
                                        label = { Text("Last 4 digits") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        val bal = mockInitialBalance.toDoubleOrNull() ?: 0.0
                                        viewModel.linkBank(
                                            bankName = selectedBank,
                                            accountName = selectedAccountType,
                                            accountNumber = "**** $customAccountNumber",
                                            initialBalance = bal
                                        )
                                        showLinkDialog = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_bank_link")
                                ) {
                                    Text("Link and Sync Ledger")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
