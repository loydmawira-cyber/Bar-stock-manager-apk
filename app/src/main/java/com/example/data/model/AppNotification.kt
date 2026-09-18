package com.example.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

enum class NotificationType {
    NEW_SIGNUP,
    ACCOUNT_APPROVED,
    ACCOUNT_REJECTED,
    ACCOUNT_REVOKED,
    DISPUTE_RAISED,
    DISPUTE_RESOLVED,
    SHORTAGE_ACCEPTANCE_REQUIRED,
    SHORTAGE_ACCEPTED,
    SHORTAGE_REJECTED,
    MID_SHIFT_ADJUSTMENT,
    SHIFT_CLOSED_RECONCILIATION,
    LOW_STOCK
}

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetUserId: Long? = null,      // null = broadcast to role
    val targetRole: UserRole? = null,    // ADMIN or ATTENDANT
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedId: Long? = null,
    val read: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val isRead: Boolean get() = read

    @get:Ignore
    val createdAt: Long get() = timestamp
}
