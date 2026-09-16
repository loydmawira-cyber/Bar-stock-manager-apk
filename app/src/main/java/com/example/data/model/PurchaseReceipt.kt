package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_receipts")
data class PurchaseReceipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val supplierName: String,
    val itemId: Long,
    val itemName: String,
    val purchaseQuantity: Int,
    val purchaseUnitType: String,
    val unitsReceived: Int,
    val stockUnitType: String,
    val unitCost: Double,
    val totalCost: Double,
    val destination: String = "STORE",
    val receivedByUserId: Long? = null,
    val receivedByName: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)
