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
import com.example.ui.components.optionalLabel
import com.example.ui.components.requiredLabel
import com.example.ui.theme.CrimsonRed
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
fun ReceivePurchaseDialog(
    items: List<Item>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int, Int, String, String, String, Double, String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(items.firstOrNull()) }
    var itemMenuExpanded by remember { mutableStateOf(false) }
    var purchaseUnitMenuExpanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var unitsPerPack by remember { mutableStateOf("24") }
    var purchaseUnit by remember { mutableStateOf("Crate") }
    var supplier by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val isItemValid = selectedItem != null
    val qtyVal = quantity.toIntOrNull()
    val isQtyValid = qtyVal != null && qtyVal > 0

    val packVal = unitsPerPack.toIntOrNull()
    val isPackValid = packVal != null && packVal > 0

    val isSupplierValid = supplier.trim().isNotBlank()
    val isReceiptValid = receipt.trim().isNotBlank()

    val costVal = cost.toDoubleOrNull()
    val isCostValid = costVal != null && costVal > 0.0

    val isFormValid = isItemValid && isQtyValid && isPackValid && isSupplierValid && isReceiptValid && isCostValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Purchase into Store", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text("Select Item *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { itemMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedItem?.name ?: "Select item")
                    }
                    DropdownMenu(expanded = itemMenuExpanded, onDismissRequest = { itemMenuExpanded = false }) {
                        items.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { selectedItem = item; itemMenuExpanded = false }) }
                    }
                }
                Column {
                    Text("Purchase Unit *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = { purchaseUnitMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Unit: $purchaseUnit") }
                    DropdownMenu(
                        expanded = purchaseUnitMenuExpanded,
                        onDismissRequest = { purchaseUnitMenuExpanded = false }
                    ) {
                        listOf("Crate", "Case", "Bottle", "Other").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    purchaseUnit = option
                                    unitsPerPack = when (option) {
                                        "Crate" -> "24"
                                        "Case" -> "6"
                                        "Bottle" -> "1"
                                        else -> "1"
                                    }
                                    purchaseUnitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = requiredLabel("Quantity purchased"),
                    isError = quantity.isNotEmpty() && !isQtyValid,
                    supportingText = if (quantity.isNotEmpty() && !isQtyValid) { { Text("Must be > 0", color = CrimsonRed) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitsPerPack,
                    onValueChange = { unitsPerPack = it },
                    label = requiredLabel("Units per $purchaseUnit"),
                    isError = unitsPerPack.isNotEmpty() && !isPackValid,
                    supportingText = if (unitsPerPack.isNotEmpty() && !isPackValid) { { Text("Must be > 0", color = CrimsonRed) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = requiredLabel("Supplier"),
                    isError = supplier.isNotEmpty() && !isSupplierValid,
                    supportingText = if (supplier.isNotEmpty() && !isSupplierValid) { { Text("Supplier name required", color = CrimsonRed) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = receipt,
                    onValueChange = { receipt = it },
                    label = requiredLabel("Receipt / Invoice number"),
                    isError = receipt.isNotEmpty() && !isReceiptValid,
                    supportingText = if (receipt.isNotEmpty() && !isReceiptValid) { { Text("Receipt number required", color = CrimsonRed) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = requiredLabel("Cost per purchase unit"),
                    isError = cost.isNotEmpty() && !isCostValid,
                    supportingText = if (cost.isNotEmpty() && !isCostValid) { { Text("Cost must be > 0", color = CrimsonRed) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = optionalLabel("Notes"),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Stock added: ${(qtyVal ?: 0) * (packVal ?: 0)} ${selectedItem?.unitType ?: "units"}", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedItem ?: return@Button
                    if (isFormValid) {
                        onConfirm(item.id, qtyVal ?: 0, packVal ?: 0, purchaseUnit, supplier.trim(), receipt.trim(), costVal ?: 0.0, notes.trim())
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) { Text("Save Receipt", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
