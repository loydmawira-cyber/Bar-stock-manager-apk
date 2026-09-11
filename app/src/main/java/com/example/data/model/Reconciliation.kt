package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reconciliations")
data class Reconciliation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val attendantId: Long,
    val attendantName: String,
    val counterName: String,
    val expectedTotal: Double,
    val submittedTotal: Double,
    val variance: Double,                // submittedTotal - expectedTotal (<0: loss, >0: extra)
    val type: ReconciliationType,        // LOSS, EXTRA, BALANCED
    val date: Long = System.currentTimeMillis(),
    val monthYear: String = "",          // "YYYY-MM"
    val notes: String = ""
)
