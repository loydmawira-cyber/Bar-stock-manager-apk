package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counter_stocks",
    indices = [Index(value = ["counterId", "itemId"], unique = true)]
)
data class CounterStock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val itemId: Long,
    val currentQuantity: Int,
    val minThreshold: Int = 5
)

data class CounterStockWithItem(
    val stockId: Long,
    val counterId: Long,
    val itemId: Long,
    val itemName: String,
    val category: ItemCategory,
    val unitPrice: Double,
    val casePrice: Double,
    val unitType: String,
    val currentQuantity: Int,
    val minThreshold: Int
)
