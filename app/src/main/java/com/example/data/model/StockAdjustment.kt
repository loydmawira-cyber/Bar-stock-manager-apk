package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AdjustmentStatus {
    PENDING_CONFIRMATION,
    CONFIRMED,
    DISPUTED
}

@Entity(tableName = "stock_adjustments")
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val counterName: String,
    val itemId: Long,
    val itemName: String,
    val qtyAddedOrRemoved: Int,          // +5 or -2
    val addedByAdminName: String,
    val shiftId: Long? = null,           // Linked if shift active
    val attendantConfirmed: Boolean = false,
    val status: AdjustmentStatus = AdjustmentStatus.PENDING_CONFIRMATION,
    val reason: String = "Restock / Admin Adjustment",
    val timestamp: Long = System.currentTimeMillis()
)
