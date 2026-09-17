package com.example.data.model

import androidx.room.Entity

/**
 * A frozen "cost per unit" for one item, effective for one calendar day.
 *
 * The cost recorded here is the unit cost from the most recent
 * [PurchaseReceipt] received on or before [dayStart]. Snapshots are written
 * once per item per day and never rewritten by later purchases, so reports
 * for past days keep the cost that was actually in effect back then even if
 * a new delivery today changes today's price.
 */
@Entity(tableName = "item_cost_snapshots", primaryKeys = ["itemId", "dayStart"])
data class ItemCostSnapshot(
    val itemId: Long,
    val dayStart: Long,             // Midnight (local time) for the day this snapshot covers
    val costPerUnit: Double,        // Unit cost in effect on this day
    val sourceReceiptId: Long? = null // The purchase receipt this cost was taken from, if any
)
