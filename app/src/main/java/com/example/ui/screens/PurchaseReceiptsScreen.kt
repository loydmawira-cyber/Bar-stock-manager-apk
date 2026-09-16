package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PurchaseReceipt
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkSurface

@Composable
fun PurchaseReceiptsScreen(receipts: List<PurchaseReceipt>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF101114)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("PURCHASE RECEIPTS", color = AmberPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Read-only receipts generated from Store Stock receiving.", color = Color.LightGray, fontSize = 12.sp)
            }
        }
        if (receipts.isEmpty()) {
            item { Text("No purchase receipts have been generated yet.", color = Color.LightGray, modifier = Modifier.padding(vertical = 24.dp)) }
        } else {
            items(receipts) { receipt ->
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(receipt.itemName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(receipt.totalCost), color = AmberPrimary, fontWeight = FontWeight.Bold)
                        }
                        Text("Receipt: ${receipt.receiptNumber.ifBlank { "Not supplied" }}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Supplier: ${receipt.supplierName.ifBlank { "Not supplied" }}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Received: ${receipt.purchaseQuantity} ${receipt.purchaseUnitType} = ${receipt.unitsReceived} ${receipt.stockUnitType}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Recorded ${formatDateTime(receipt.purchaseDate)} by ${receipt.receivedByName.ifBlank { "Admin" }}", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
