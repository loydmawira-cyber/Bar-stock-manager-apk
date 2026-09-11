package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bar_profile")
data class BarProfile(
    @PrimaryKey val id: Long = 1L,
    val barName: String = "The Amber Taphouse & Lounge",
    val location: String = "42 High Street, Downtown",
    val iconType: String = "cocktail", // "cocktail", "beer", "wine", "whiskey", "neon", "vip"
    val customPhotoUri: String? = null,
    val contactPhone: String = "+1 (555) 234-5678",
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val managerName: String = "Alex Vance",
    val openingHours: String = "4:00 PM - 3:00 AM",
    val isRegistered: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
