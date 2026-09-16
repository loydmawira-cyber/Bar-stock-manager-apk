package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Counter
import com.example.data.model.PurchaseReceipt
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkSurface
import java.util.Calendar

private enum class PurchaseTimeFilter { TODAY, MONTH, YEAR }

@Composable
fun PurchaseReceiptsScreen(
    receipts: List<PurchaseReceipt>,
    counters: List<Counter> = emptyList()
) {
    val now = remember { Calendar.getInstance() }
    var selectedDestination by remember { mutableStateOf("All") }
    var destinationExpanded by remember { mutableStateOf(false) }
    var selectedTimeFilter by remember { mutableStateOf(PurchaseTimeFilter.TODAY) }
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
                Text("PURCHASE RECEIPTS", color = AmberPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Individual receipts generated from Inventory receiving.", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
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
                    OutlinedButton(onClick = { selectedTimeFilter = PurchaseTimeFilter.TODAY }, modifier = Modifier.weight(1f)) { Text("Today", fontSize = 11.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { selectedTimeFilter = PurchaseTimeFilter.MONTH; monthExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (selectedTimeFilter == PurchaseTimeFilter.MONTH) monthNames[selectedMonth] else "Month", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                            monthNames.forEachIndexed { index, month ->
                                DropdownMenuItem(text = { Text(month) }, onClick = {
                                    selectedMonth = index
                                    selectedTimeFilter = PurchaseTimeFilter.MONTH
                                    monthExpanded = false
                                })
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { selectedTimeFilter = PurchaseTimeFilter.YEAR; yearExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (selectedTimeFilter == PurchaseTimeFilter.YEAR) selectedYear.toString() else "Year", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                            availableYears.forEach { year ->
                                DropdownMenuItem(text = { Text(year.toString()) }, onClick = {
                                    selectedYear = year
                                    selectedTimeFilter = PurchaseTimeFilter.YEAR
                                    yearExpanded = false
                                })
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
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
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
}
