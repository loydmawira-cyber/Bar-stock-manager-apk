package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val barProfile: BarProfile? = null,
    val users: List<User> = emptyList(),
    val items: List<Item> = emptyList(),
    val counters: List<Counter> = emptyList(),
    val counterStocks: List<CounterStock> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    val stockVerifications: List<StockVerification> = emptyList(),
    val shiftClosings: List<ShiftClosing> = emptyList(),
    val disputes: List<Dispute> = emptyList(),
    val stockAdjustments: List<StockAdjustment> = emptyList(),
    val reconciliations: List<Reconciliation> = emptyList(),
    val notifications: List<AppNotification> = emptyList()
)
