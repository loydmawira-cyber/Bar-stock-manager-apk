package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.ui.components.MetricStatCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.viewmodel.BarStockViewModel

val EXPENSE_CATEGORIES = listOf(
    "Water",
    "Electricity",
    "Rent",
    "Permits",
    "Wages and salaries",
    "Security",
    "Transport",
    "Repairs",
    "Internet",
    "Cleaning",
    "Bank charges",
    "Other"
)

val PAYMENT_METHODS = listOf("Cash", "M-Pesa", "Bank Transfer", "Card", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: BarStockViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.allExpenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val totalExpenses = expenses.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record Expense") },
                containerColor = AmberPrimary,
                contentColor = Color.Black
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            MetricStatCard(
                title = "Total Expenses Recorded",
                value = formatCurrency(totalExpenses),
                icon = Icons.Default.TrendingDown,
                iconColor = CrimsonRed,
                subtitle = "${expenses.size} total entries"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Expense History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No expenses recorded yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onDelete = { viewModel.deleteExpense(expense.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { cat, desc, amt, payMethod, ref, notes ->
                viewModel.addExpense(cat, desc, amt, payMethod, ref, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AmberPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = expense.category,
                            color = AmberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = expense.paymentMethod,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = expense.description,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                if (expense.referenceNumber.isNotBlank()) {
                    Text(
                        text = "Ref: ${expense.referenceNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!expense.notes.isNullOrBlank()) {
                    Text(
                        text = "Note: ${expense.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${formatDateTime(expense.date)} · By ${expense.recordedBy}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(expense.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CrimsonRed
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CrimsonRed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, description: String, amount: String, paymentMethod: String, refNumber: String, notes: String?) -> Unit
) {
    var category by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PAYMENT_METHODS.first()) }
    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    var submittedAttempt by remember { mutableStateOf(false) }

    val isDescValid = description.trim().isNotBlank()
    val parsedAmt = amountText.toDoubleOrNull()
    val isAmountValid = parsedAmt != null && parsedAmt > 0
    val isRefValid = referenceNumber.trim().isNotBlank()

    val isFormValid = isDescValid && isAmountValid && isRefValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record New Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(buildAnnotatedString {
                                append("Expense Category")
                                withStyle(SpanStyle(color = CrimsonRed)) { append(" *") }
                            })
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        EXPENSE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text(buildAnnotatedString {
                            append("Description / Vendor")
                            withStyle(SpanStyle(color = CrimsonRed)) { append(" *") }
                        })
                    },
                    isError = submittedAttempt && !isDescValid,
                    supportingText = {
                        if (submittedAttempt && !isDescValid) {
                            Text("Description is required", color = CrimsonRed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = {
                        Text(buildAnnotatedString {
                            append("Amount")
                            withStyle(SpanStyle(color = CrimsonRed)) { append(" *") }
                        })
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = submittedAttempt && !isAmountValid,
                    supportingText = {
                        if (submittedAttempt && !isAmountValid) {
                            Text("Enter a valid positive amount > 0", color = CrimsonRed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Method Dropdown
                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = !paymentExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(buildAnnotatedString {
                                append("Payment Method")
                                withStyle(SpanStyle(color = CrimsonRed)) { append(" *") }
                            })
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        PAYMENT_METHODS.forEach { pm ->
                            DropdownMenuItem(
                                text = { Text(pm) },
                                onClick = {
                                    paymentMethod = pm
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                // Reference Number
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = {
                        Text(buildAnnotatedString {
                            append("Reference / Receipt No.")
                            withStyle(SpanStyle(color = CrimsonRed)) { append(" *") }
                        })
                    },
                    isError = submittedAttempt && !isRefValid,
                    supportingText = {
                        if (submittedAttempt && !isRefValid) {
                            Text("Reference or receipt number is required", color = CrimsonRed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reason (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submittedAttempt = true
                    if (isFormValid) {
                        onSubmit(category, description, amountText, paymentMethod, referenceNumber, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black)
            ) {
                Text("Save Expense", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
