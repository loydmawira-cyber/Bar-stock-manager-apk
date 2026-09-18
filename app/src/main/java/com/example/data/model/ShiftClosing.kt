package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_closings")
data class ShiftClosing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val itemId: Long,
    val itemName: String,
    val category: ItemCategory,
    val unitType: String,
    val unitPrice: Double,
    val openingQty: Int,
    val openingLooseMl: Int = 0,
    val adjustmentsQty: Int = 0,        // Mid-shift confirmed stock added/removed
    val effectiveOpeningQty: Int = 0,   // openingQty + adjustmentsQty
    val closingQty: Int,
    val unitsSold: Int,                 // effectiveOpeningQty - closingQty
    val expectedAmount: Double,         // unitsSold * unitPrice
    val closingLooseMl: Int = 0,
    val bottleVolumeMl: Int = 0
)
