package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Counter
import com.example.data.model.Expense
import com.example.data.model.Item
import com.example.data.model.PurchaseReceipt
import com.example.data.model.Reconciliation
import com.example.data.model.User
import com.example.ui.components.MetricStatCard
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface

@Composable
fun ReceiptsScreen(
    currentUser: User,
    reconciliations: List<Reconciliation>,
    attendants: List<User>,
    selectedAttendantId: Long?,
    purchaseReceipts: List<PurchaseReceipt>,
    counters: List<Counter> = emptyList(),
    items: List<Item> = emptyList(),
    expenses: List<Expense> = emptyList(),
    onViewShiftReceipt: (Long) -> Unit = {},
    onSelectAttendantFilter: (Long?) -> Unit,
    onAddExpense: (category: String, description: String, amount: String, paymentMethod: String, refNumber: String, notes: String?) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteExpense: (Long) -> Unit = {},
    onReceivePurchase: ((itemId: Long, purchaseQuantity: Int, unitsPerPurchaseUnit: Int, purchaseUnitType: String, supplierName: String, receiptNumber: String, unitCost: Double, notes: String) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Sales Receipts", "Purchase Receipts", "Expenses")

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = AmberPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AmberPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (selectedTab == index) AmberPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("receipts_tab_${index}")
                )
            }
        }

        when (selectedTab) {
            0 -> {
                LedgerScreen(
                    currentUser = currentUser,
                    reconciliations = reconciliations,
                    attendants = attendants,
                    selectedAttendantId = selectedAttendantId,
                    onViewShiftReceipt = onViewShiftReceipt,
                    onSelectAttendantFilter = onSelectAttendantFilter
                )
            }
            1 -> {
                PurchaseReceiptsScreen(
                    receipts = purchaseReceipts,
                    counters = counters,
                    items = items,
                    onReceivePurchase = onReceivePurchase
                )
            }
            2 -> {
                val totalExpenses = expenses.sumOf { it.amount }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    MetricStatCard(
                        title = "Total Expenses Recorded",
                        value = formatCurrency(totalExpenses),
                        icon = Icons.Default.TrendingDown,
                        iconColor = CrimsonRed,
                        subtitle = "${expenses.size} total entries"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EXPENSE RECORDS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Recorded operational expenses & payments",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showAddExpenseDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            modifier = Modifier.testTag("add_expense_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (expenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No expenses recorded yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Managers and Owners can record expenses anytime.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(expenses, key = { it.id }) { expense ->
                                ExpenseItemCard(
                                    expense = expense,
                                    onDelete = { onDeleteExpense(expense.id) }
                                )
                            }
                        }
                    }
                }

                if (showAddExpenseDialog) {
                    AddExpenseDialog(
                        onDismiss = { showAddExpenseDialog = false },
                        onSubmit = { cat, desc, amt, payMethod, ref, notes ->
                            onAddExpense(cat, desc, amt, payMethod, ref, notes)
                            showAddExpenseDialog = false
                        }
                    )
                }
            }
        }
    }
}
