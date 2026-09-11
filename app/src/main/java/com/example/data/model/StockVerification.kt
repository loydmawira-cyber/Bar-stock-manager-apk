package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VerificationStatus {
    CONFIRMED,
    DISPUTED
}

@Entity(tableName = "stock_verifications")
data class StockVerification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val itemId: Long,
    val itemName: String,
    val category: ItemCategory,
    val unitType: String,
    val unitPrice: Double,
    val systemQty: Int,
    val attendantEnteredQty: Int,
    val status: VerificationStatus = VerificationStatus.CONFIRMED,
    val verifiedAt: Long = System.currentTimeMillis()
)
