package com.example.data.repository

import com.example.data.dao.BarStockDao
import com.example.data.model.BackupData
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class MoshiUserRoleAdapter {
    @FromJson
    fun fromJson(value: String): UserRole {
        return when (value.uppercase()) {
            "ADMIN" -> UserRole.OWNER
            "OWNER" -> UserRole.OWNER
            "MANAGER" -> UserRole.MANAGER
            "ATTENDANT" -> UserRole.ATTENDANT
            else -> UserRole.ATTENDANT
        }
    }

    @ToJson
    fun toJson(role: UserRole): String {
        return role.name
    }
}

class MoshiUserStatusAdapter {
    @FromJson
    fun fromJson(value: String): UserStatus {
        return when (value.uppercase()) {
            "APPROVED" -> UserStatus.APPROVED
            "REVOKED" -> UserStatus.REVOKED
            "PENDING" -> UserStatus.PENDING
            else -> UserStatus.APPROVED
        }
    }

    @ToJson
    fun toJson(status: UserStatus): String {
        return status.name
    }
}

class BackupService(private val dao: BarStockDao) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val moshi = Moshi.Builder()
        .add(MoshiUserRoleAdapter())
        .add(MoshiUserStatusAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(BackupData::class.java)

    private suspend fun ensureAuth() {
        check(FirebaseAuth.getInstance().currentUser != null) {
            "Please sign in with your admin or attendant account before using cloud backup."
        }
    }

    private fun hashString(input: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(input.trim().lowercase(java.util.Locale.ROOT).toByteArray())
        return "bar_" + hash.joinToString("") { "%02x".format(it) }.take(48)
    }

    private suspend fun tenantId(identityOverride: String? = null): String {
        if (identityOverride.isNullOrBlank()) {
            val ownProfile = dao.getBarProfileSync()
            if (!ownProfile?.barId.isNullOrBlank()) {
                return "bar_" + ownProfile!!.barId
            }
        }
        val admin = dao.getAllUsersSync().firstOrNull { it.role.name == "OWNER" || it.role.name == "ADMIN" }
        val profile = dao.getBarProfileSync()
        val identity = identityOverride?.trim()?.takeIf { it.isNotBlank() }
            ?: admin?.email?.trim()?.takeIf { it.isNotBlank() }
            ?: admin?.phone?.trim()?.takeIf { it.isNotBlank() }
            ?: profile?.let { "${it.barName}|${it.location}" }
            ?: error("This installation is not linked to a bar account")
        return hashString(identity)
    }

    private suspend fun snapshot(): BackupData {
        val profile = dao.getBarProfileSync()
        return BackupData(
            barProfile = profile?.copy(updatedAt = System.currentTimeMillis()),
            users = dao.getAllUsersSync(),
            items = dao.getAllItemsSync(),
            counters = dao.getAllCountersSync(),
            counterStocks = dao.getAllCounterStocksSync(),
            storeStocks = dao.getAllStoreStocksSync(),
            purchaseReceipts = dao.getAllPurchaseReceiptsSync(),
            shifts = dao.getAllShiftsSync(),
            stockVerifications = dao.getAllStockVerificationsSync(),
            shiftClosings = dao.getAllShiftClosingsSync(),
            disputes = dao.getAllDisputesSync(),
            stockAdjustments = dao.getAllStockAdjustmentsSync(),
            reconciliations = dao.getAllReconciliationsSync(),
            notifications = dao.getAllNotificationsSync(),
            expenses = dao.getAllExpensesSync()
        )
    }

    suspend fun backupData(backupId: String? = null): Result<Unit> = runCatching {
        ensureAuth()
        val snap = snapshot()
        val json = adapter.toJson(snap)
        val now = System.currentTimeMillis()
        val targetId = backupId ?: tenantId()

        val adminUser = snap.users.firstOrNull { it.role.name == "OWNER" || it.role.name == "ADMIN" }
        val activeUsers = snap.users.filter { it.status.name != "REVOKED" }
        val userEmails = activeUsers.mapNotNull { it.email.takeIf { e -> e.isNotBlank() }?.lowercase(java.util.Locale.ROOT) }
        val userPhones = activeUsers.mapNotNull { it.phone.takeIf { p -> p.isNotBlank() } }

        val docData = mapOf(
            "schemaVersion" to 3,
            "barId" to targetId,
            "barName" to (snap.barProfile?.barName ?: "Bar"),
            "location" to (snap.barProfile?.location ?: ""),
            "adminEmail" to (adminUser?.email?.lowercase(java.util.Locale.ROOT) ?: ""),
            "adminPhone" to (adminUser?.phone ?: ""),
            "userEmails" to userEmails,
            "userPhones" to userPhones,
            "updatedAt" to now,
            "data" to json
        )

        firestore.collection("backups").document(targetId).set(docData).await()
    }

    suspend fun syncData(identityOverride: String? = null): Result<Unit> = runCatching {
        ensureAuth()
        var remoteDoc: DocumentSnapshot? = null
        val cleanIdentity = identityOverride?.trim()?.lowercase(java.util.Locale.ROOT)

        // 1. If identity is provided, search Firestore by user email/phone or hash
        if (!cleanIdentity.isNullOrBlank()) {
            // Check by userEmails array
            try {
                val emailQuery = firestore.collection("backups")
                    .whereArrayContains("userEmails", cleanIdentity)
                    .get()
                    .await()
                if (!emailQuery.isEmpty) {
                    remoteDoc = emailQuery.documents.maxByOrNull { it.getLong("updatedAt") ?: 0L }
                }
            } catch (e: Exception) {
                // continue
            }

            // Check by adminEmail field
            if (remoteDoc == null || !remoteDoc.exists()) {
                try {
                    val adminEmailQuery = firestore.collection("backups")
                        .whereEqualTo("adminEmail", cleanIdentity)
                        .get()
                        .await()
                    if (!adminEmailQuery.isEmpty) {
                        remoteDoc = adminEmailQuery.documents.maxByOrNull { it.getLong("updatedAt") ?: 0L }
                    }
                } catch (e: Exception) {
                    // continue
                }
            }

            // Check by userPhones array
            if (remoteDoc == null || !remoteDoc.exists()) {
                try {
                    val phoneQuery = firestore.collection("backups")
                        .whereArrayContains("userPhones", identityOverride!!.trim())
                        .get()
                        .await()
                    if (!phoneQuery.isEmpty) {
                        remoteDoc = phoneQuery.documents.maxByOrNull { it.getLong("updatedAt") ?: 0L }
                    }
                } catch (e: Exception) {
                    // continue
                }
            }

            // Check by adminPhone field
            if (remoteDoc == null || !remoteDoc.exists()) {
                try {
                    val adminPhoneQuery = firestore.collection("backups")
                        .whereEqualTo("adminPhone", identityOverride!!.trim())
                        .get()
                        .await()
                    if (!adminPhoneQuery.isEmpty) {
                        remoteDoc = adminPhoneQuery.documents.maxByOrNull { it.getLong("updatedAt") ?: 0L }
                    }
                } catch (e: Exception) {
                    // continue
                }
            }

            // Check by hash ID
            if (remoteDoc == null || !remoteDoc.exists()) {
                try {
                    val docByHash = firestore.collection("backups").document(hashString(cleanIdentity)).get().await()
                    if (docByHash.exists()) {
                        remoteDoc = docByHash
                    }
                } catch (e: Exception) {
                    // continue
                }
            }
        }

        // 2. Try current local tenant document if no identity provided or identity didn't yield a document
        if (remoteDoc == null || !remoteDoc.exists()) {
            try {
                val localTenantId = tenantId()
                val doc = firestore.collection("backups").document(localTenantId).get().await()
                if (doc.exists()) {
                    remoteDoc = doc
                }
            } catch (e: Exception) {
                // continue
            }
        }

        val localProfile = dao.getBarProfileSync()
        val localUpdatedAt = localProfile?.updatedAt ?: 0L
        val localUsers = dao.getAllUsersSync()

        if (remoteDoc != null && remoteDoc.exists()) {
            val remoteUpdatedAt = remoteDoc.getLong("updatedAt") ?: 0L
            val remoteDataJson = remoteDoc.getString("data")

            if (!remoteDataJson.isNullOrBlank()) {
                // Check if the current local database actually belongs to the user/tenant in remoteDoc
                val remoteUserEmails = (remoteDoc.get("userEmails") as? List<*>)?.mapNotNull { it?.toString()?.lowercase(java.util.Locale.ROOT) } ?: emptyList()
                val remoteUserPhones = (remoteDoc.get("userPhones") as? List<*>)?.mapNotNull { it?.toString()?.trim() } ?: emptyList()

                val localMatchesRemote = if (!cleanIdentity.isNullOrBlank()) {
                    localUsers.any { u ->
                        u.email.lowercase(java.util.Locale.ROOT) == cleanIdentity || u.phone.trim() == identityOverride?.trim()
                    }
                } else {
                    localUsers.any { u ->
                        remoteUserEmails.contains(u.email.lowercase(java.util.Locale.ROOT)) || remoteUserPhones.contains(u.phone.trim())
                    }
                }

                if (!localMatchesRemote || localUsers.isEmpty() || remoteUpdatedAt > localUpdatedAt) {
                    // Local DB belongs to a different bar/account or is empty -> Wipe local DB and restore remote tenant snapshot
                    restoreSnapshot(remoteDataJson)
                } else if (localUpdatedAt > remoteUpdatedAt) {
                    // Local DB belongs to this account and has newer local changes -> Upload to cloud
                    backupData(remoteDoc.id)
                }
            }
        } else {
            // Document doesn't exist on cloud yet. If local DB belongs to this identity, back it up
            if (localUsers.isNotEmpty() && cleanIdentity.isNullOrBlank()) {
                backupData()
            }
        }
    }

    suspend fun restoreData(backupId: String? = null): Result<Unit> = runCatching {
        ensureAuth()
        val docId = backupId ?: tenantId()
        val doc = firestore.collection("backups").document(docId).get().await()
        if (doc.exists()) {
            val json = doc.getString("data") ?: error("No cloud backup found")
            restoreSnapshot(json)
        } else {
            error("No backup found for this account")
        }
    }

    private suspend fun restoreSnapshot(json: String) {
        val data = adapter.fromJson(json) ?: error("Cloud data could not be parsed")
        data.barProfile?.let { dao.insertOrUpdateBarProfile(it) }
        dao.clearNotifications()
        dao.clearReconciliations()
        dao.clearStockAdjustments()
        dao.clearDisputes()
        dao.clearShiftClosings()
        dao.clearStockVerifications()
        dao.clearShifts()
        dao.clearCounterStocks()
        dao.clearStoreStocks()
        dao.clearPurchaseReceipts()
        dao.clearCounters()
        dao.clearItems()
        dao.clearUsers()
        dao.clearExpenses()

        data.users.forEach { user ->
            val migratedUser = if (user.role == UserRole.OWNER || user.role.name == "ADMIN" || user.role.name == "OWNER") {
                user.copy(
                    role = UserRole.OWNER,
                    status = UserStatus.APPROVED
                )
            } else {
                user
            }
            dao.insertUser(migratedUser)
        }
        data.items.forEach { dao.insertItem(it) }
        data.counters.forEach { dao.insertCounter(it) }
        dao.insertCounterStocks(data.counterStocks)
        data.storeStocks.forEach { dao.insertStoreStock(it) }
        dao.insertPurchaseReceipts(data.purchaseReceipts)
        data.shifts.forEach { dao.insertShift(it) }
        dao.insertStockVerifications(data.stockVerifications)
        dao.insertShiftClosings(data.shiftClosings)
        data.disputes.forEach { dao.insertDispute(it) }
        data.stockAdjustments.forEach { dao.insertStockAdjustment(it) }
        data.reconciliations.forEach { dao.insertReconciliation(it) }
        data.notifications.forEach { dao.insertNotification(it) }
        data.expenses.forEach { dao.insertExpense(it) }
    }
}
