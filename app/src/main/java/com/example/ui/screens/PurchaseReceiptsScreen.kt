package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Item
import com.example.data.model.PurchaseReceipt
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import java.util.Calendar

private enum class PurchaseTimeFilter { ALL_TIME, TODAY, MONTH, YEAR }

@Composable
fun PurchaseReceiptsScreen(
    receipts: List<PurchaseReceipt>,
    counters: List<Counter> = emptyList(),
    items: List<Item> = emptyList(),
    onReceivePurchase: ((itemId: Long, purchaseQuantity: Int, unitsPerPurchaseUnit: Int, purchaseUnitType: String, supplierName: String, receiptNumber: String, unitCost: Double, notes: String) -> Unit)? = null,
    isOwner: Boolean = false,
    onDeleteReceipt: (Long) -> Unit = {}
) {
    val now = remember { Calendar.getInstance() }
    var showReceiveDialog by remember { mutableStateOf(false) }
    var receiptPendingDelete by remember { mutableStateOf<PurchaseReceipt?>(null) }
    var selectedDestination by remember { mutableStateOf("All") }
    var destinationExpanded by remember { mutableStateOf(false) }
    var selectedTimeFilter by remember { mutableStateOf(PurchaseTimeFilter.ALL_TIME) }
    var selectedMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val availableYears = remember(receipts) {
        val receiptYears = receipts.map {
            Calendar.getInstance().apply { timeInMillis = it.purchaseDate }.get(Calendar.YEAR)
        }
        (receiptYears + now.get(Calendar.YEAR) + (now.get(Calendar.YEAR) - 1)).distinct().sortedDescending()
    }
    val destinationOptions = listOf("All", "Store") + counters.map { it.name }

    fun matchesTime(receipt: PurchaseReceipt): Boolean {
        val receiptCal = Calendar.getInstance().apply { timeInMillis = receipt.purchaseDate }
        return when (selectedTimeFilter) {
            PurchaseTimeFilter.ALL_TIME -> true
            PurchaseTimeFilter.TODAY -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                receipt.purchaseDate >= start
            }
            PurchaseTimeFilter.MONTH -> receiptCal.get(Calendar.MONTH) == selectedMonth && receiptCal.get(Calendar.YEAR) == selectedYear
            PurchaseTimeFilter.YEAR -> receiptCal.get(Calendar.YEAR) == selectedYear
        }
    }

    val filteredReceipts = receipts.filter { receipt ->
        val matchesDestination = selectedDestination == "All" ||
            (selectedDestination == "Store" && receipt.destination.equals("STORE", ignoreCase = true)) ||
            receipt.destination.equals(selectedDestination, ignoreCase = true)
        matchesDestination && matchesTime(receipt)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF101114)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PURCHASE RECEIPTS", color = AmberPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Individual receipts generated from Inventory receiving.", color = Color.LightGray, fontSize = 12.sp)
                    }
                    if (onReceivePurchase != null && items.isNotEmpty()) {
                        Button(
                            onClick = { showReceiveDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            modifier = Modifier.testTag("add_purchase_receipt_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receive", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { destinationExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedDestination, fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = destinationExpanded, onDismissRequest = { destinationExpanded = false }) {
                            destinationOptions.distinct().forEach { destination ->
                                DropdownMenuItem(text = { Text(destination) }, onClick = {
                                    selectedDestination = destination
                                    destinationExpanded = false
                                })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Time
                    FilterChip(
                        selected = selectedTimeFilter == PurchaseTimeFilter.ALL_TIME,
                        onClick = { selectedTimeFilter = PurchaseTimeFilter.ALL_TIME },
                        label = { Text("All Time", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Today
                    FilterChip(
                        selected = selectedTimeFilter == PurchaseTimeFilter.TODAY,
                        onClick = { selectedTimeFilter = PurchaseTimeFilter.TODAY },
                        label = { Text("Today", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Select Month Dropdown Chip
                    Box {
                        val isMonthSelected = selectedTimeFilter == PurchaseTimeFilter.MONTH
                        FilterChip(
                            selected = isMonthSelected,
                            onClick = { monthExpanded = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isMonthSelected) "Month: ${monthNames[selectedMonth]}" else "Select Month",
                                        fontSize = 12.sp,
                                        fontWeight = if (isMonthSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select Month",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("purchase_filter_month_chip")
                        )

                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Select Month ($selectedYear)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )

                            monthNames.forEachIndexed { index, monthName ->
                                val isCurrent = index == selectedMonth && selectedTimeFilter == PurchaseTimeFilter.MONTH
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = monthName,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = AmberPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedMonth = index
                                        selectedTimeFilter = PurchaseTimeFilter.MONTH
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Select Year Dropdown Chip
                    Box {
                        val isYearSelected = selectedTimeFilter == PurchaseTimeFilter.YEAR
                        FilterChip(
                            selected = isYearSelected,
                            onClick = { yearExpanded = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isYearSelected) "Year: $selectedYear" else "Select Year",
                                        fontSize = 12.sp,
                                        fontWeight = if (isYearSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select Year",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("purchase_filter_year_chip")
                        )

                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Select Year",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )

                            availableYears.forEach { year ->
                                val isCurrent = year == selectedYear && selectedTimeFilter == PurchaseTimeFilter.YEAR
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = year.toString(),
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = AmberPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedYear = year
                                        selectedTimeFilter = PurchaseTimeFilter.YEAR
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (filteredReceipts.isEmpty()) {
            item { Text("No purchase receipts match the selected filters.", color = Color.LightGray, modifier = Modifier.padding(vertical = 24.dp)) }
        } else {
            items(filteredReceipts) { receipt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = { if (isOwner) receiptPendingDelete = receipt }
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(receipt.itemName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(receipt.totalCost), color = AmberPrimary, fontWeight = FontWeight.Bold)
                        }
                        Text("Receipt: ${receipt.receiptNumber.ifBlank { "Not supplied" }}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Supplier: ${receipt.supplierName.ifBlank { "Not supplied" }}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Destination: ${if (receipt.destination.equals("STORE", true)) "Store" else receipt.destination}", color = AmberPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Received: ${receipt.purchaseQuantity} ${receipt.purchaseUnitType} = ${receipt.unitsReceived} ${receipt.stockUnitType}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Recorded ${formatDateTime(receipt.purchaseDate)} by ${receipt.receivedByName.ifBlank { "Admin" }}", color = Color.Gray, fontSize = 11.sp)
                        if (receipt.notes.isNotBlank()) Text("Notes: ${receipt.notes}", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showReceiveDialog && onReceivePurchase != null) {
        ReceivePurchaseDialog(
            items = items,
            onDismiss = { showReceiveDialog = false },
            onConfirm = { itemId, qty, pack, unit, supplier, receipt, cost, notes ->
                onReceivePurchase(itemId, qty, pack, unit, supplier, receipt, cost, notes)
                showReceiveDialog = false
            }
        )
    }

    receiptPendingDelete?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptPendingDelete = null },
            title = { Text("Delete Purchase Receipt?") },
            text = {
                Text("This will permanently delete the receipt for \"${receipt.itemName}\" (${receipt.receiptNumber.ifBlank { "No receipt number" }}) and reverse the ${receipt.unitsReceived} ${receipt.stockUnitType} it added to stock. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteReceipt(receipt.id)
                    receiptPendingDelete = null
                }) {
                    Text("Delete", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
