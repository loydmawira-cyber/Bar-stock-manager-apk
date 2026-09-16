package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val description: String,
    val amount: Double,
    val paymentMethod: String = "Cash",
    val referenceNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val recordedBy: String = "",
    val notes: String? = null
)
