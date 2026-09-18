package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ShiftStatus {
    ACTIVE,
    CLOSED,
    DISPUTED
}

enum class ReconciliationType {
    NONE,
    LOSS,
    EXTRA,
    BALANCED
}

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val counterName: String,
    val attendantId: Long,
    val attendantName: String,
    val attendantRole: UserRole = UserRole.ATTENDANT,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: ShiftStatus = ShiftStatus.ACTIVE,
    val totalExpectedSales: Double = 0.0,
    val totalSubmittedCash: Double = 0.0,
    val totalSubmittedMpesa: Double = 0.0,
    val totalSubmittedCard: Double = 0.0,
    val totalSubmittedBank: Double = 0.0,
    val submittedPaymentBreakdown: String = "",
    val variance: Double = 0.0,
    val inventoryShortageValue: Double = 0.0,
    val totalAccountableVariance: Double = 0.0,
    val reconciliationType: ReconciliationType = ReconciliationType.NONE,
    val disputeCount: Int = 0,
    val notes: String = ""
)
