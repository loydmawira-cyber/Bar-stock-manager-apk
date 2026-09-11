package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    ATTENDANT
}

enum class UserStatus {
    PENDING,
    APPROVED,
    REVOKED
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: UserRole,
    val status: UserStatus,
    val email: String,
    val phone: String,
    val password: String = "123456",
    val createdAt: Long = System.currentTimeMillis()
)
