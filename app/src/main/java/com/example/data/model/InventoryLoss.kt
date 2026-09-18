package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ShortageAcceptanceStatus { PENDING, ACCEPTED, REJECTED }

@Entity(tableName = "inventory_losses", indices = [Index(value = ["disputeId"], unique = true)])
data class InventoryLoss(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val disputeId: Long,
    val shiftId: Long,
    val counterId: Long,
    val counterName: String,
    val itemId: Long,
    val itemName: String,
    val missingBottles: Int,
    val missingLooseMl: Int = 0,
    val bottleVolumeMl: Int = 0,
    val unitValue: Double,
    val totalValue: Double,
    val reason: String,
    val resolvedBy: String,
    val acceptanceStatus: ShortageAcceptanceStatus = ShortageAcceptanceStatus.PENDING,
    val acceptedByAttendantId: Long? = null,
    val acceptedByAttendantName: String? = null,
    val acceptedAt: Long? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
