package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                  // e.g., "Counter 1 – Main Bar"
    val location: String,              // e.g., "Ground Floor"
    val activeAttendantId: Long? = null,
    val activeAttendantName: String? = null,
    val activeShiftId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
