package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "store_stocks",
    indices = [Index(value = ["itemId"], unique = true)]
)
data class StoreStock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val currentQuantity: Int,
    val minThreshold: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class StoreStockWithItem(
    val stockId: Long,
    val itemId: Long,
    val itemName: String,
    val category: ItemCategory,
    val unitType: String,
    val currentQuantity: Int,
    val minThreshold: Int
)
