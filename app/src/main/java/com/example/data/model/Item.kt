package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemCategory {
    BEER,
    SPIRIT,
    TOT,
    WINE,
    SOFT_DRINK,
    OTHER
}

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ItemCategory,
    val unitPrice: Double,       // Selling price per single unit/bottle/shot
    val casePrice: Double,       // Total price per case / bottle equivalent
    val unitType: String,        // "Bottle", "Shot / Tot", "Can", "Glass"
    val description: String = "",
    val totEnabled: Boolean = false,
    val bottleVolumeMl: Int = 0,
    val totSizeMl: Int = 0,
    val totPrice: Double = 0.0
)
