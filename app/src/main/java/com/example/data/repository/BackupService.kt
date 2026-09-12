package com.example.data.repository

import com.example.data.dao.BarStockDao
import com.example.data.model.BackupData
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.tasks.await

class BackupService(private val dao: BarStockDao) {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupData::class.java)

    suspend fun backupData(backupId: String = "default_bar"): Result<Unit> {
        return try {
            val db = firestore ?: return Result.failure(Exception("Firebase not initialized"))
            val data = BackupData(
                users = dao.getAllUsersSync(),
                items = dao.getAllItemsSync(),
                counters = dao.getAllCountersSync(),
                counterStocks = dao.getAllCounterStocksSync(),
                shifts = dao.getAllShiftsSync(),
                stockVerifications = dao.getAllStockVerificationsSync(),
                shiftClosings = dao.getAllShiftClosingsSync(),
                disputes = dao.getAllDisputesSync(),
                stockAdjustments = dao.getAllStockAdjustmentsSync(),
                reconciliations = dao.getAllReconciliationsSync(),
                notifications = dao.getAllNotificationsSync()
            )

            val json = adapter.toJson(data)

            // Compress or chunk if needed, but for a simple bar stock app,
            // JSON will stay well under 1MB for a while.
            val document = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "data" to json
            )

            db.collection("backups").document(backupId).set(document).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreData(backupId: String = "default_bar"): Result<Unit> {
        return try {
            val db = firestore ?: return Result.failure(Exception("Firebase not initialized"))
            val snapshot = db.collection("backups").document(backupId).get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("No backup found in cloud."))
            }

            val json = snapshot.getString("data") ?: return Result.failure(Exception("Backup data is empty."))
            val data = adapter.fromJson(json) ?: return Result.failure(Exception("Failed to parse backup."))

            // Restore all data (Clear existing and insert)
            dao.clearNotifications()
            dao.clearReconciliations()
            dao.clearStockAdjustments()
            dao.clearDisputes()
            dao.clearShiftClosings()
            dao.clearStockVerifications()
            dao.clearShifts()
            dao.clearCounterStocks()
            dao.clearCounters()
            dao.clearItems()
            dao.clearUsers()

            // Insert new ones (Since they are REPLACE on conflict and have IDs, it restores perfectly)
            data.users.forEach { dao.insertUser(it) }
            data.items.forEach { dao.insertItem(it) }
            data.counters.forEach { dao.insertCounter(it) }
            dao.insertCounterStocks(data.counterStocks)
            data.shifts.forEach { dao.insertShift(it) }
            dao.insertStockVerifications(data.stockVerifications)
            dao.insertShiftClosings(data.shiftClosings)
            data.disputes.forEach { dao.insertDispute(it) }
            data.stockAdjustments.forEach { dao.insertStockAdjustment(it) }
            data.reconciliations.forEach { dao.insertReconciliation(it) }
            data.notifications.forEach { dao.insertNotification(it) }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
