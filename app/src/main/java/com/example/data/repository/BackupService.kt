package com.example.data.repository

import com.example.data.dao.BarStockDao
import com.example.data.model.BackupData
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class BackupService(private val dao: BarStockDao) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(BackupData::class.java)

    private suspend fun tenantId(): String {
        val admin = dao.getAllUsersSync().firstOrNull { it.role.name == "ADMIN" }
        val profile = dao.getBarProfileSync()
        val identity = admin?.email?.trim()?.lowercase() ?: profile?.let { "${it.barName}|${it.location}" }
            ?: error("Cannot identify this bar")
        val hash = MessageDigest.getInstance("SHA-256").digest(identity.lowercase().toByteArray())
        return "bar_" + hash.joinToString("") { "%02x".format(it) }.take(48)
    }

    private suspend fun snapshot() = BackupData(
        barProfile = dao.getBarProfileSync(), users = dao.getAllUsersSync(), items = dao.getAllItemsSync(),
        counters = dao.getAllCountersSync(), counterStocks = dao.getAllCounterStocksSync(), shifts = dao.getAllShiftsSync(),
        stockVerifications = dao.getAllStockVerificationsSync(), shiftClosings = dao.getAllShiftClosingsSync(),
        disputes = dao.getAllDisputesSync(), stockAdjustments = dao.getAllStockAdjustmentsSync(),
        reconciliations = dao.getAllReconciliationsSync(), notifications = dao.getAllNotificationsSync()
    )

    suspend fun backupData(backupId: String? = null): Result<Unit> = runCatching {
        val json = adapter.toJson(snapshot())
        firestore.collection("backups").document(backupId ?: tenantId()).set(
            mapOf("schemaVersion" to 2, "updatedAt" to System.currentTimeMillis(), "data" to json)
        ).await()
    }

    suspend fun syncData(): Result<Unit> = runCatching {
        val ref = firestore.collection("backups").document(tenantId())
        val remote = ref.get().await()
        if (remote.exists()) restoreSnapshot(remote.getString("data") ?: error("Cloud data is empty"))
        else ref.set(mapOf("schemaVersion" to 2, "updatedAt" to System.currentTimeMillis(), "data" to adapter.toJson(snapshot()))).await()
    }

    suspend fun restoreData(backupId: String? = null): Result<Unit> = runCatching {
        val json = firestore.collection("backups").document(backupId ?: tenantId()).get().await().getString("data")
            ?: error("No cloud backup found")
        restoreSnapshot(json)
    }

    private suspend fun restoreSnapshot(json: String) {
        val data = adapter.fromJson(json) ?: error("Cloud data could not be parsed")
        data.barProfile?.let { dao.insertOrUpdateBarProfile(it) }
        dao.clearNotifications(); dao.clearReconciliations(); dao.clearStockAdjustments(); dao.clearDisputes()
        dao.clearShiftClosings(); dao.clearStockVerifications(); dao.clearShifts(); dao.clearCounterStocks()
        dao.clearCounters(); dao.clearItems(); dao.clearUsers()
        data.users.forEach { dao.insertUser(it) }; data.items.forEach { dao.insertItem(it) }
        data.counters.forEach { dao.insertCounter(it) }; dao.insertCounterStocks(data.counterStocks)
        data.shifts.forEach { dao.insertShift(it) }; dao.insertStockVerifications(data.stockVerifications)
        dao.insertShiftClosings(data.shiftClosings); data.disputes.forEach { dao.insertDispute(it) }
        data.stockAdjustments.forEach { dao.insertStockAdjustment(it) }; data.reconciliations.forEach { dao.insertReconciliation(it) }
        data.notifications.forEach { dao.insertNotification(it) }
    }
}

private fun String.lowercase() = lowercase(java.util.Locale.ROOT)
