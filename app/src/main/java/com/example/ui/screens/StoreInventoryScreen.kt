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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.data.model.StoreStockWithItem
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen

@Composable
fun StoreInventoryScreen(
    items: List<Item>,
    storeStock: List<StoreStockWithItem>,
    onReceivePurchase: (itemId: Long, purchaseQuantity: Int, unitsPerPurchaseUnit: Int, purchaseUnitType: String, supplierName: String, receiptNumber: String, unitCost: Double, notes: String) -> Unit,
    onOpenCounterStock: () -> Unit = {}
) {
    var showReceiveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF101114)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("STORE STOCK", color = AmberPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Balances are stored in the item's selling unit.", color = Color.LightGray, fontSize = 12.sp)
                }
                Button(
                    onClick = { showReceiveDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    modifier = Modifier.testTag("receive_store_purchase_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black)
                    Text("Receive", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenCounterStock, modifier = Modifier.fillMaxWidth()) {
                Text("Open Counter Stock", fontWeight = FontWeight.Bold)
            }
        }
        if (storeStock.isEmpty()) {
            item { Text("No store stock recorded yet. Receive a purchase to begin.", color = Color.LightGray, modifier = Modifier.padding(vertical = 24.dp)) }
        } else {
            items(storeStock) { stock ->
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stock.itemName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(stock.category.name, color = Color.LightGray, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("${stock.currentQuantity} ${stock.unitType}s", color = AmberPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Store balance", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showReceiveDialog) {
        ReceivePurchaseDialog(
            items = items,
            onDismiss = { showReceiveDialog = false },
            onConfirm = { itemId, quantity, unitsPerPurchaseUnit, purchaseUnit, supplier, receipt, cost, notes ->
                onReceivePurchase(itemId, quantity, unitsPerPurchaseUnit, purchaseUnit, supplier, receipt, cost, notes)
                showReceiveDialog = false
            }
        )
    }
}

@Composable
private fun ReceivePurchaseDialog(
    items: List<Item>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int, Int, String, String, String, Double, String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(items.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var unitsPerPack by remember { mutableStateOf("1") }
    var purchaseUnit by remember { mutableStateOf("Crate") }
    var supplier by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Purchase into Store", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedItem?.name ?: "Select item") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { selectedItem = item; expanded = false }) }
                }
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity purchased") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(unitsPerPack, { unitsPerPack = it }, label = { Text("Selling units per $purchaseUnit") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(purchaseUnit, { purchaseUnit = it }, label = { Text("Purchase unit (crate/case/bottle)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(supplier, { supplier = it }, label = { Text("Supplier") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(receipt, { receipt = it }, label = { Text("Receipt / invoice number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(cost, { cost = it }, label = { Text("Cost per purchase unit") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                Text("Stock added: ${(quantity.toIntOrNull() ?: 0) * (unitsPerPack.toIntOrNull() ?: 0)} ${selectedItem?.unitType ?: "units"}", color = EmeraldGreen, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val item = selectedItem ?: return@Button
                onConfirm(item.id, quantity.toIntOrNull() ?: 0, unitsPerPack.toIntOrNull() ?: 0, purchaseUnit, supplier, receipt, cost.toDoubleOrNull() ?: 0.0, notes)
            }, colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)) { Text("Save Receipt", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
