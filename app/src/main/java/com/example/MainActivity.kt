package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BudgetViewModel
import com.example.viewmodel.BudgetViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels {
        BudgetViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(Tab.Dashboard) }

                Scaffold(
                    modifier = Modifier.fillMaxSize().testTag("app_root_scaffold"),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .testTag("bottom_nav_bar")
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    label = { Text(tab.title, fontWeight = FontWeight.Bold) },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (currentTab) {
                            Tab.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAccounts = { currentTab = Tab.Accounts }
                            )
                            Tab.Accounts -> AccountsScreen(
                                viewModel = viewModel
                            )
                            Tab.Transactions -> TransactionsScreen(
                                viewModel = viewModel
                            )
                            Tab.Budgets -> BudgetsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class Tab(val title: String, val icon: ImageVector) {
    Dashboard("Overview", Icons.Default.Home),
    Accounts("Link Feed", Icons.Default.Star),
    Transactions("Ledger", Icons.Default.List),
    Budgets("Limits", Icons.Default.Warning)
}
