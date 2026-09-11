package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DisputeStatus {
    OPEN,
    RESOLVED
}

@Entity(tableName = "disputes")
data class Dispute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val counterId: Long,
    val counterName: String,
    val itemId: Long,
    val itemName: String,
    val expectedQty: Int,
    val reportedQty: Int,
    val discrepancy: Int,                // reportedQty - expectedQty
    val raisedByAttendantId: Long,
    val raisedByAttendantName: String,
    val involvesPreviousAttendantId: Long? = null,
    val involvesPreviousAttendantName: String? = null,
    val status: DisputeStatus = DisputeStatus.OPEN,
    val resolutionNotes: String = "",
    val resolvedByAdminName: String? = null,
    val resolvedAt: Long? = null,
    val adjustedStockQty: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
