package com.example.data.repository

import com.example.data.dao.BarStockDao
import com.example.data.model.AdjustmentStatus
import com.example.data.model.AppNotification
import com.example.data.model.BarProfile
import com.example.data.model.Counter
import com.example.data.model.CounterStock
import com.example.data.model.CounterStockWithItem
import com.example.data.model.Dispute
import com.example.data.model.DisputeStatus
import com.example.data.model.Item
import com.example.data.model.ItemCategory
import com.example.data.model.NotificationType
import com.example.data.model.Reconciliation
import com.example.data.model.ReconciliationType
import com.example.data.model.Shift
import com.example.data.model.ShiftClosing
import com.example.data.model.ShiftStatus
import com.example.data.model.StockAdjustment
import com.example.data.model.StockVerification
import com.example.data.model.StoreStock
import com.example.data.model.StoreStockWithItem
import com.example.data.model.PurchaseReceipt
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.model.VerificationStatus
import com.example.data.util.PasswordValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class BarStockRepository(private val dao: BarStockDao) {
    

    // --- Bar Profile ---
    val barProfile: Flow<BarProfile> = dao.getBarProfile().map {
        it ?: BarProfile()
    }

    suspend fun getBarProfileSync(): BarProfile {
        return dao.getBarProfileSync() ?: BarProfile()
    }

    suspend fun saveBarProfile(profile: BarProfile) {
        dao.insertOrUpdateBarProfile(profile)
    }

    suspend fun clearAllDataForNewBar() {
        dao.clearBarProfile()
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
    }

    suspend fun seedInitialCounterForNewBar() {
        dao.insertCounter(
            com.example.data.model.Counter(
                id = 1L,
                name = "Main Bar Counter",
                location = "Main Bar Area"
            )
        )
    }

    suspend fun seedSampleBeersForNewBar(counterId: Long = 1L) {
        val beers = listOf(
            Item(name = "Tusker Lager (500ml)", category = ItemCategory.BEER, unitPrice = 250.0, casePrice = 6000.0, unitType = "Bottle", description = "Classic Kenyan Lager"),
            Item(name = "Tusker Malt (500ml)", category = ItemCategory.BEER, unitPrice = 280.0, casePrice = 6720.0, unitType = "Bottle", description = "Premium Malt Lager"),
            Item(name = "Tusker Cider (500ml)", category = ItemCategory.BEER, unitPrice = 300.0, casePrice = 7200.0, unitType = "Bottle", description = "Crisp Apple Cider"),
            Item(name = "Tusker Lite (500ml)", category = ItemCategory.BEER, unitPrice = 280.0, casePrice = 6720.0, unitType = "Bottle", description = "Low carb light lager"),
            Item(name = "Guinness Foreign Extra Stout (500ml)", category = ItemCategory.BEER, unitPrice = 300.0, casePrice = 7200.0, unitType = "Bottle", description = "Rich dark stout"),
            Item(name = "Heineken Lager (330ml)", category = ItemCategory.BEER, unitPrice = 320.0, casePrice = 7680.0, unitType = "Bottle", description = "International premium malt"),
            Item(name = "White Cap Lager (500ml)", category = ItemCategory.BEER, unitPrice = 260.0, casePrice = 6240.0, unitType = "Bottle", description = "Smooth crisp lager"),
            Item(name = "White Cap Light (500ml)", category = ItemCategory.BEER, unitPrice = 260.0, casePrice = 6240.0, unitType = "Bottle", description = "Light refreshing lager"),
            Item(name = "Balozi Lager (500ml)", category = ItemCategory.BEER, unitPrice = 230.0, casePrice = 5520.0, unitType = "Bottle", description = "Rich unpasteurized lager"),
            Item(name = "Pilsner Lager (500ml)", category = ItemCategory.BEER, unitPrice = 250.0, casePrice = 6000.0, unitType = "Bottle", description = "Bold Simba lager"),
            Item(name = "Pilsner Ice (500ml)", category = ItemCategory.BEER, unitPrice = 260.0, casePrice = 6240.0, unitType = "Bottle", description = "Ice filtered lager"),
            Item(name = "Corona Extra (355ml)", category = ItemCategory.BEER, unitPrice = 380.0, casePrice = 9120.0, unitType = "Bottle", description = "Mexican pale lager"),
            Item(name = "Stella Artois (330ml)", category = ItemCategory.BEER, unitPrice = 350.0, casePrice = 8400.0, unitType = "Bottle", description = "Belgian pilsner"),
            Item(name = "Budweiser (330ml)", category = ItemCategory.BEER, unitPrice = 320.0, casePrice = 7680.0, unitType = "Bottle", description = "American king of beers"),
            Item(name = "Carlsberg Elephant (330ml)", category = ItemCategory.BEER, unitPrice = 340.0, casePrice = 8160.0, unitType = "Bottle", description = "Strong Danish lager"),
            Item(name = "Windhoek Lager (330ml)", category = ItemCategory.BEER, unitPrice = 300.0, casePrice = 7200.0, unitType = "Bottle", description = "100% Pure Namibian beer"),
            Item(name = "Windhoek Draught (440ml)", category = ItemCategory.BEER, unitPrice = 330.0, casePrice = 7920.0, unitType = "Can", description = "Smooth draught beer"),
            Item(name = "Savanna Dry Cider (330ml)", category = ItemCategory.BEER, unitPrice = 320.0, casePrice = 7680.0, unitType = "Bottle", description = "Dry apple cider"),
            Item(name = "Hunters Gold Cider (330ml)", category = ItemCategory.BEER, unitPrice = 300.0, casePrice = 7200.0, unitType = "Bottle", description = "Real golden cider"),
            Item(name = "Desperados Tequila Beer (330ml)", category = ItemCategory.BEER, unitPrice = 360.0, casePrice = 8640.0, unitType = "Bottle", description = "Tequila flavored beer")
        )

        val stocks = mutableListOf<CounterStock>()
        for (beer in beers) {
            val itemId = dao.insertItem(beer)
            stocks.add(
                CounterStock(
                    counterId = counterId,
                    itemId = itemId,
                    currentQuantity = 24, // 1 case initial stock
                    minThreshold = 6
                )
            )
        }
        dao.insertCounterStocks(stocks)
    }


    // --- Users ---
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val pendingUsers: Flow<List<User>> = dao.getUsersByStatus(UserStatus.PENDING)
    val attendants: Flow<List<User>> = dao.getUsersByRole(UserRole.ATTENDANT)

    suspend fun getUserById(userId: Long): User? = dao.getUserById(userId)
    suspend fun getUserByEmail(email: String): User? = dao.getUserByEmail(email)
    suspend fun getUserByIdentifier(identifier: String): User? = dao.getUserByIdentifier(identifier.trim())

    suspend fun authenticateUser(identifier: String, password: String): User? {
        val trimmedIdentifier = identifier.trim()
        val trimmedPassword = password.trim()
        val user = dao.getUserByIdentifier(trimmedIdentifier)

        if (user != null) {
            // Block revoked or pending accounts from authenticating
            if (user.status == UserStatus.REVOKED || user.status == UserStatus.PENDING) {
                return null
            }

            // Check local password match
            if (user.password == trimmedPassword) {
                // Cloud backup requires a real Firebase session; do not fall back to local-only login for email accounts.
                if (user.email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
                    val currentFbEmail = FirebaseAuth.getInstance().currentUser?.email
                    if (!currentFbEmail.equals(user.email, ignoreCase = true)) {
                        try {
                            FirebaseAuth.getInstance().signInWithEmailAndPassword(user.email, trimmedPassword).await()
                        } catch (e: Exception) {
                            return null
                        }
                    }
                }
                return user
            } else {
                // Local password didn't match directly. Try Firebase Auth (in case password was updated elsewhere)
                try {
                    if (user.email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(user.email, trimmedPassword).await()
                        // Firebase sign-in succeeded! Update local password
                        dao.updateUserPassword(user.id, trimmedPassword)
                        return user.copy(password = trimmedPassword)
                    }
                } catch (e: Exception) {
                    // Firebase sign-in failed
                }
                return null
            }
        } else {
            // Authenticate the Firebase account, but do not create a local user yet.
            // The caller must restore the matching bar backup first; otherwise a
            // missing local user could incorrectly be created as ADMIN.
            try {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedIdentifier).matches()) {
                    val currentFbEmail = FirebaseAuth.getInstance().currentUser?.email
                    if (!currentFbEmail.equals(trimmedIdentifier, ignoreCase = true)) {
                        FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(trimmedIdentifier, trimmedPassword)
                            .await()
                    }
                }
            } catch (e: Exception) {
                // Firebase sign-in failed
            }
        }
        return null
    }

    suspend fun registerUser(name: String, email: String, phone: String, role: UserRole, password: String): Long {
        val validation = PasswordValidator.validate(password)
        if (!validation.isValid) {
            throw IllegalArgumentException("Password does not meet requirements: ${validation.missingRequirementsMessage}")
        }
        val finalEmail = email.ifBlank { "${name.lowercase().replace(" ", "")}@thebar.com" }.trim()

        // 1. Check if user already exists locally
        val existingByEmail = dao.getUserByIdentifier(finalEmail)
        if (existingByEmail != null) {
            throw IllegalArgumentException("The email address '$finalEmail' is already registered to an account.")
        }
        if (phone.isNotBlank()) {
            val existingByPhone = dao.getUserByIdentifier(phone.trim())
            if (existingByPhone != null) {
                throw IllegalArgumentException("The phone number '${phone.trim()}' is already registered to an account.")
            }
        }

        // 2. Create user in Firebase Authentication
        try {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(finalEmail).matches()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(finalEmail, password).await()
            }
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            throw IllegalArgumentException("The email address '$finalEmail' is already in use by an existing account.")
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || e.message?.contains("already in use", ignoreCase = true) == true) {
                throw IllegalArgumentException("The email address '$finalEmail' is already in use by an existing account.")
            } else {
                throw IllegalArgumentException(e.localizedMessage ?: "Failed to create account.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("already in use", ignoreCase = true) == true ||
                e.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true) {
                throw IllegalArgumentException("The email address '$finalEmail' is already in use by an existing account.")
            }
        }

        val user = User(
            name = name,
            role = role,
            status = if (role == UserRole.ADMIN) UserStatus.APPROVED else UserStatus.PENDING,
            email = finalEmail,
            phone = phone,
            password = password
        )
        val id = dao.insertUser(user)
        if (role == UserRole.ATTENDANT) {
            dao.insertNotification(
                AppNotification(
                    targetRole = UserRole.ADMIN,
                    type = NotificationType.NEW_SIGNUP,
                    title = "New Attendant Registration",
                    message = "$name ($phone) registered as Bar Attendant and awaits approval.",
                    relatedId = id
                )
            )
        }
        return id
    }

    suspend fun createAttendantByAdmin(name: String, email: String, phone: String, initialPassword: String): Long {
        val validation = PasswordValidator.validate(initialPassword)
        if (!validation.isValid) {
            throw IllegalArgumentException("Password does not meet requirements: ${validation.missingRequirementsMessage}")
        }
        val finalEmail = email.ifBlank { "${name.lowercase().replace(" ", "")}@thebar.com" }.trim()
        val pass = initialPassword

        // Check local DB
        val existingByEmail = dao.getUserByIdentifier(finalEmail)
        if (existingByEmail != null) {
            throw IllegalArgumentException("The email address '$finalEmail' is already registered to an account.")
        }
        if (phone.isNotBlank()) {
            val existingByPhone = dao.getUserByIdentifier(phone.trim())
            if (existingByPhone != null) {
                throw IllegalArgumentException("The phone number '${phone.trim()}' is already registered to an account.")
            }
        }

        // Firebase signs the current device into the newly created account.
        // Save the admin session so creating an attendant does not log the admin out.
        val auth = FirebaseAuth.getInstance()
        val adminEmail = auth.currentUser?.email
        val adminPassword = adminEmail?.let { dao.getUserByEmail(it)?.password }

        try {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(finalEmail).matches()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(finalEmail, pass).await()
            }
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            throw IllegalArgumentException("The email address '$finalEmail' is already in use by an existing account.")
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || e.message?.contains("already in use", ignoreCase = true) == true) {
                throw IllegalArgumentException("The email address '$finalEmail' is already in use by an existing account.")
            } else {
                throw IllegalArgumentException(e.localizedMessage ?: "Failed to create account.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("already in use", ignoreCase = true) == true ||
                e.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true) {
                throw IllegalArgumentException("The email address '$finalEmail' is already in use by an existing account.")
            }
        }

        if (!adminEmail.isNullOrBlank() && !adminPassword.isNullOrBlank()) {
            auth.signInWithEmailAndPassword(adminEmail, adminPassword).await()
        }

        val user = User(
            name = name,
            role = UserRole.ATTENDANT,
            status = UserStatus.APPROVED,
            email = finalEmail,
            phone = phone,
            password = pass
        )
        val id = dao.insertUser(user)
        dao.insertNotification(
            AppNotification(
                targetUserId = id,
                targetRole = UserRole.ATTENDANT,
                type = NotificationType.ACCOUNT_APPROVED,
                title = "Attendant Account Created",
                message = "Admin created your account for $name. You can now login and change your password anytime.",
                relatedId = id
            )
        )
        return id
    }


    suspend fun changeUserPassword(userId: Long, oldPassword: String?, newPassword: String): Result<Unit> {
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        if (oldPassword != null && user.password != oldPassword) {
            return Result.failure(Exception("Current password is incorrect"))
        }
        if (newPassword.length < 4) {
            return Result.failure(Exception("New password must be at least 4 characters"))
        }
        dao.updateUserPassword(userId, newPassword)
        return Result.success(Unit)
        
}


    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(trimmedEmail).await()
            Result.success("If an account exists for $trimmedEmail, a password reset link has been sent to your email. Please check your inbox.")
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email format. Please enter a valid email address."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            // Prevent user enumeration attack by returning generic success message
            Result.success("If an account exists for $trimmedEmail, a password reset link has been sent to your email. Please check your inbox.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again."))
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            if (e.errorCode == "ERROR_TOO_MANY_REQUESTS" || e.message?.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) == true) {
                Result.failure(Exception("Too many reset attempts. Please wait a few minutes before trying again."))
            } else if (e.errorCode == "ERROR_USER_NOT_FOUND") {
                Result.success("If an account exists for $trimmedEmail, a password reset link has been sent to your email. Please check your inbox.")
            } else {
                Result.failure(Exception(e.localizedMessage ?: "Failed to send reset email."))
            }
        } catch (e: Exception) {
            if (e.message?.contains("user-not-found", ignoreCase = true) == true ||
                e.message?.contains("NO_SUCH_USER", ignoreCase = true) == true) {
                Result.success("If an account exists for $trimmedEmail, a password reset link has been sent to your email. Please check your inbox.")
            } else {
                Result.failure(Exception(e.localizedMessage ?: "Failed to send reset email."))
            }
        }
    }

    suspend fun resetPasswordByIdentifier(identifier: String, newPassword: String): Result<User> {
        val trimmed = identifier.trim()
        val user = dao.getUserByIdentifier(trimmed)
            ?: return Result.failure(Exception("No account registered with '$trimmed'"))
        if (newPassword.length < 4) {
            return Result.failure(Exception("Password must be at least 4 characters"))
        }
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            return Result.failure(
                Exception("Email passwords must be reset using the password-reset link sent to ${user.email}.")
            )
        }
        dao.updateUserPassword(user.id, newPassword)
        return Result.success(user)
        
}


    suspend fun resetPasswordByPhone(phone: String, newPassword: String): Result<User> {
        return resetPasswordByIdentifier(phone, newPassword)
        
}


    suspend fun getUserByPhone(phone: String): User? = dao.getUserByIdentifier(phone.trim())

    suspend fun deleteUser(userId: Long) {
        dao.deleteUser(userId)
        
}


    suspend fun updateUserStatus(userId: Long, newStatus: UserStatus) {
        val user = dao.getUserById(userId)
        dao.updateUserStatus(userId, newStatus)
        if (user != null) {
            val title = when (newStatus) {
                UserStatus.APPROVED -> "Account Approved"
                UserStatus.REVOKED -> "Account Revoked"
                UserStatus.PENDING -> "Account Set to Pending"
            }
            val message = when (newStatus) {
                UserStatus.APPROVED -> "Your attendant account has been approved by the Admin. You may now select a counter and start shifts."
                UserStatus.REVOKED -> "Your attendant account has been revoked by management."
                UserStatus.PENDING -> "Your account status is currently pending review."
            }
            dao.insertNotification(
                AppNotification(
                    targetUserId = userId,
                    targetRole = UserRole.ATTENDANT,
                    type = if (newStatus == UserStatus.APPROVED) NotificationType.ACCOUNT_APPROVED else NotificationType.ACCOUNT_REVOKED,
                    title = title,
                    message = message,
                    relatedId = userId
                )
            )
        }
        
}


    // --- Items & Catalog ---
    val allItems: Flow<List<Item>> = dao.getAllItems()

    suspend fun addItem(
        name: String,
        category: ItemCategory,
        unitPrice: Double,
        casePrice: Double,
        unitType: String,
        description: String = ""
    ): Long {
        return dao.insertItem(
            Item(
                name = name,
                category = category,
                unitPrice = unitPrice,
                casePrice = casePrice,
                unitType = unitType,
                description = description
            )
        )
    }

    suspend fun updateItem(item: Item) = dao.updateItem(item).also {  }
    suspend fun deleteItem(itemId: Long) = dao.deleteItem(itemId).also {  }

    // --- Counters & Inventory ---
    val allCounters: Flow<List<Counter>> = dao.getAllCounters()

    fun getCounterByIdFlow(counterId: Long): Flow<Counter?> = dao.getCounterByIdFlow(counterId)
    suspend fun getCounterById(counterId: Long): Counter? = dao.getCounterById(counterId)

    suspend fun addCounter(name: String, location: String): Long {
        return dao.insertCounter(Counter(name = name, location = location))
        
}


    suspend fun updateCounter(counter: Counter) = dao.updateCounter(counter).also {  }
    suspend fun deleteCounter(counterId: Long) = dao.deleteCounter(counterId).also {  }

    fun getCounterStocksWithItems(counterId: Long): Flow<List<CounterStockWithItem>> =
        dao.getCounterStocksWithItems(counterId)

    fun getStoreStockWithItems(): Flow<List<StoreStockWithItem>> = dao.getAllStoreStockWithItems()
    val allPurchaseReceipts: Flow<List<PurchaseReceipt>> = dao.getAllPurchaseReceipts()

    suspend fun receivePurchaseToStore(
        itemId: Long,
        purchaseQuantity: Int,
        unitsPerPurchaseUnit: Int,
        purchaseUnitType: String,
        supplierName: String,
        receiptNumber: String,
        unitCost: Double,
        receivedByUserId: Long?,
        receivedByName: String,
        notes: String = ""
    ) {
        require(purchaseQuantity > 0) { "Purchase quantity must be greater than zero." }
        require(unitsPerPurchaseUnit > 0) { "Units per purchase unit must be greater than zero." }
        val item = dao.getItemById(itemId) ?: error("Item not found")
        val unitsReceived = purchaseQuantity * unitsPerPurchaseUnit
        val existing = dao.getStoreStock(itemId)
        dao.insertStoreStock(
            StoreStock(
                id = existing?.id ?: 0L,
                itemId = itemId,
                currentQuantity = (existing?.currentQuantity ?: 0) + unitsReceived,
                minThreshold = existing?.minThreshold ?: 0
            )
        )
        val enteredReceiptNumber = receiptNumber.trim()
        val finalReceiptNumber = enteredReceiptNumber.ifBlank {
            "AUTO-${System.currentTimeMillis()}"
        }
        dao.insertPurchaseReceipt(
            PurchaseReceipt(
                receiptNumber = finalReceiptNumber,
                supplierName = supplierName,
                itemId = itemId,
                itemName = item.name,
                purchaseQuantity = purchaseQuantity,
                purchaseUnitType = purchaseUnitType,
                unitsReceived = unitsReceived,
                stockUnitType = item.unitType,
                unitCost = unitCost,
                totalCost = unitCost * purchaseQuantity,
                receivedByUserId = receivedByUserId,
                receivedByName = receivedByName,
                notes = notes
            )
        )
    }

    suspend fun setCounterItemStock(
        counterId: Long,
        itemId: Long,
        quantity: Int,
        minThreshold: Int = 5,
        adminName: String = "Admin"
    ) {
        val existing = dao.getCounterStock(counterId, itemId)
        val isNewAssignment = existing == null
        val current = existing?.currentQuantity ?: 0
        val newQuantity = current + quantity
        val stockId = existing?.id ?: 0L

        dao.insertCounterStock(
            CounterStock(
                id = stockId,
                counterId = counterId,
                itemId = itemId,
                currentQuantity = newQuantity,
                minThreshold = existing?.minThreshold ?: minThreshold
            )
        )

        val activeShift = dao.getActiveShiftForCounter(counterId)
        val counter = dao.getCounterById(counterId)
        val item = dao.getItemById(itemId)
        if (counter != null && item != null) {
            val adjustment = StockAdjustment(
                counterId = counterId,
                counterName = counter.name,
                itemId = itemId,
                itemName = item.name,
                qtyAddedOrRemoved = quantity,
                addedByAdminName = adminName,
                shiftId = activeShift?.id,
                attendantConfirmed = activeShift == null,
                status = if (activeShift == null) AdjustmentStatus.CONFIRMED else AdjustmentStatus.PENDING_CONFIRMATION,
                reason = if (isNewAssignment) "Assigned Item to Counter" else "Stock Addition"
            )
            val adjId = dao.insertStockAdjustment(adjustment)
            if (activeShift != null) {
                val actionWord = if (isNewAssignment) "assigned" else "added"
                dao.insertNotification(
                    AppNotification(
                        targetUserId = activeShift.attendantId,
                        targetRole = UserRole.ATTENDANT,
                        type = NotificationType.MID_SHIFT_ADJUSTMENT,
                        title = if (isNewAssignment) "Item Assigned: ${item.name}" else "Stock Added: ${item.name}",
                        message = "Admin $adminName $actionWord +$quantity units of ${item.name} at ${counter.name}. Total stock is now $newQuantity. Please confirm receipt.",
                        relatedId = adjId
                    )
                )
            }
        }
    }


    suspend fun removeCounterStock(counterId: Long, itemId: Long) {
        dao.deleteCounterStock(counterId, itemId)
        
}


    // --- Shift Lifecycle & Handover ---
    val allShifts: Flow<List<Shift>> = dao.getAllShifts()
    val allShiftClosings: Flow<List<ShiftClosing>> = dao.getAllShiftClosings()
    fun getShiftsByAttendant(attendantId: Long): Flow<List<Shift>> = dao.getShiftsByAttendant(attendantId)
    fun getActiveShiftForAttendantFlow(attendantId: Long): Flow<Shift?> = dao.getActiveShiftForAttendantFlow(attendantId)
    fun getShiftByIdFlow(shiftId: Long): Flow<Shift?> = dao.getShiftByIdFlow(shiftId)
    suspend fun getShiftById(shiftId: Long): Shift? = dao.getShiftById(shiftId)

    suspend fun canStartShiftAtCounter(counterId: Long): Pair<Boolean, String?> {
        val counter = dao.getCounterById(counterId) ?: return Pair(false, "Counter not found.")
        if (counter.activeAttendantId != null && counter.activeShiftId != null) {
            return Pair(false, "Shift not closed by previous attendant (${counter.activeAttendantName ?: "Attendant"}).")
        }
        val activeShift = dao.getActiveShiftForCounter(counterId)
        if (activeShift != null) {
            return Pair(false, "Shift #${activeShift.id} is still active on this counter by ${activeShift.attendantName}.")
        }
        return Pair(true, null)
    }

    data class OpeningVerificationItemInput(
        val itemId: Long,
        val itemName: String,
        val category: ItemCategory,
        val unitType: String,
        val unitPrice: Double,
        val systemQty: Int,
        val countedQty: Int,
        val isApproved: Boolean
    )

    suspend fun startShiftWithVerification(
        counterId: Long,
        attendantId: Long,
        attendantName: String,
        verifications: List<OpeningVerificationItemInput>
    ): Long {
        val counter = dao.getCounterById(counterId) ?: throw IllegalStateException("Counter not found")
        val lastClosedShift = dao.getLastClosedShiftForCounter(counterId)

        var disputeCount = 0
        verifications.forEach { item ->
            if (!item.isApproved || item.countedQty != item.systemQty) {
                disputeCount++
            }
        }

        // 1. Create Shift Record
        val shiftId = dao.insertShift(
            Shift(
                counterId = counterId,
                counterName = counter.name,
                attendantId = attendantId,
                attendantName = attendantName,
                startTime = System.currentTimeMillis(),
                status = ShiftStatus.ACTIVE,
                disputeCount = disputeCount
            )
        )

        // 2. Lock Counter to this attendant
        dao.updateCounterActiveShift(counterId, attendantId, attendantName, shiftId)

        // 3. Save Stock Verifications & Create Disputes if any
        val verificationEntities = verifications.map { item ->
            val isDisputed = !item.isApproved || (item.countedQty != item.systemQty)
            StockVerification(
                shiftId = shiftId,
                itemId = item.itemId,
                itemName = item.itemName,
                category = item.category,
                unitType = item.unitType,
                unitPrice = item.unitPrice,
                systemQty = item.systemQty,
                attendantEnteredQty = item.countedQty,
                status = if (isDisputed) VerificationStatus.DISPUTED else VerificationStatus.CONFIRMED
            )
        }
        dao.insertStockVerifications(verificationEntities)

        // 4. Create Dispute records & Push Notifications
        verifications.filter { !it.isApproved || it.countedQty != it.systemQty }.forEach { disputedItem ->
            val disputeId = dao.insertDispute(
                Dispute(
                    shiftId = shiftId,
                    counterId = counterId,
                    counterName = counter.name,
                    itemId = disputedItem.itemId,
                    itemName = disputedItem.itemName,
                    expectedQty = disputedItem.systemQty,
                    reportedQty = disputedItem.countedQty,
                    discrepancy = disputedItem.countedQty - disputedItem.systemQty,
                    raisedByAttendantId = attendantId,
                    raisedByAttendantName = attendantName,
                    involvesPreviousAttendantId = lastClosedShift?.attendantId,
                    involvesPreviousAttendantName = lastClosedShift?.attendantName,
                    status = DisputeStatus.OPEN
                )
            )

            // Notify Admin
            val diffStr = if (disputedItem.countedQty > disputedItem.systemQty) "+${disputedItem.countedQty - disputedItem.systemQty}" else "${disputedItem.countedQty - disputedItem.systemQty}"
            dao.insertNotification(
                AppNotification(
                    targetRole = UserRole.ADMIN,
                    type = NotificationType.DISPUTE_RAISED,
                    title = "Dispute Raised at ${counter.name}",
                    message = "$attendantName flagged ${disputedItem.itemName}: Expected ${disputedItem.systemQty}, Counted ${disputedItem.countedQty} (Diff $diffStr).",
                    relatedId = disputeId
                )
            )

            // Notify Previous Attendant if exists
            lastClosedShift?.attendantId?.let { prevId ->
                dao.insertNotification(
                    AppNotification(
                        targetUserId = prevId,
                        targetRole = UserRole.ATTENDANT,
                        type = NotificationType.DISPUTE_RAISED,
                        title = "Dispute on Handover (${counter.name})",
                        message = "Incoming attendant $attendantName disputed ${disputedItem.itemName} stock (Expected: ${disputedItem.systemQty}, Counted: ${disputedItem.countedQty}).",
                        relatedId = disputeId
                    )
                )
            }
        }

        return shiftId
    }

    // --- Mid-Shift Adjustments (Admin Adds/Removes Stock) ---
    val allStockAdjustments: Flow<List<StockAdjustment>> = dao.getAllStockAdjustments()

    fun getAdjustmentsForShift(shiftId: Long): Flow<List<StockAdjustment>> = dao.getAdjustmentsForShift(shiftId)
    fun getPendingAdjustmentsForShift(shiftId: Long): Flow<List<StockAdjustment>> = dao.getPendingAdjustmentsForShift(shiftId)

    suspend fun addMidShiftAdjustment(
        counterId: Long,
        itemId: Long,
        quantityToAddOrRemove: Int,
        adminName: String,
        reason: String
    ): Long {
        val counter = dao.getCounterById(counterId) ?: throw IllegalStateException("Counter not found")
        val item = dao.getItemById(itemId) ?: throw IllegalStateException("Item not found")
        val activeShift = dao.getActiveShiftForCounter(counterId)

        val adjustment = StockAdjustment(
            counterId = counterId,
            counterName = counter.name,
            itemId = itemId,
            itemName = item.name,
            qtyAddedOrRemoved = quantityToAddOrRemove,
            addedByAdminName = adminName,
            shiftId = activeShift?.id,
            attendantConfirmed = activeShift == null, // auto confirm if no active shift
            status = if (activeShift == null) AdjustmentStatus.CONFIRMED else AdjustmentStatus.PENDING_CONFIRMATION,
            reason = reason
        )
        val id = dao.insertStockAdjustment(adjustment)

        // Always update the counter stock directly so both admin and attendant immediately see existing + added stock
        val stock = dao.getCounterStock(counterId, itemId)
        val current = stock?.currentQuantity ?: 0
        val newQty = maxOf(0, current + quantityToAddOrRemove)
        dao.insertCounterStock(
            CounterStock(
                id = stock?.id ?: 0,
                counterId = counterId,
                itemId = itemId,
                currentQuantity = newQty,
                minThreshold = stock?.minThreshold ?: 5
            )
        )

        if (activeShift != null) {
            // Notify active attendant
            val actionWord = if (quantityToAddOrRemove > 0) "added (+$quantityToAddOrRemove)" else "removed ($quantityToAddOrRemove)"
            dao.insertNotification(
                AppNotification(
                    targetUserId = activeShift.attendantId,
                    targetRole = UserRole.ATTENDANT,
                    type = NotificationType.MID_SHIFT_ADJUSTMENT,
                    title = "Stock Adjustment: ${item.name}",
                    message = "Admin $adminName $actionWord units of ${item.name}. Total stock is now $newQty. Please confirm receipt.",
                    relatedId = id
                )
            )
        }

        return id
    }

    suspend fun respondToAdjustment(adjustmentId: Long, isConfirmed: Boolean, attendantId: Long) {
        val adjustments = dao.getAllStockAdjustments()
        // We'll update via query / direct find
        // Note: For simplicity, we create helper in DAO or update directly
        
}


    suspend fun confirmStockAdjustment(adjustment: StockAdjustment) {
        val updated = adjustment.copy(
            attendantConfirmed = true,
            status = AdjustmentStatus.CONFIRMED
        )
        dao.updateStockAdjustment(updated)

        // Notify Admin
        dao.insertNotification(
            AppNotification(
                targetRole = UserRole.ADMIN,
                type = NotificationType.MID_SHIFT_ADJUSTMENT,
                title = "Adjustment Confirmed",
                message = "Attendant confirmed ${adjustment.qtyAddedOrRemoved} units of ${adjustment.itemName} at ${adjustment.counterName}."
            )
        )
        
}


    suspend fun disputeStockAdjustment(adjustment: StockAdjustment, attendantName: String) {
        val updated = adjustment.copy(
            attendantConfirmed = false,
            status = AdjustmentStatus.DISPUTED
        )
        dao.updateStockAdjustment(updated)

        // Revert the disputed added stock from the counter
        val stock = dao.getCounterStock(adjustment.counterId, adjustment.itemId)
        if (stock != null) {
            val revertedQty = maxOf(0, stock.currentQuantity - adjustment.qtyAddedOrRemoved)
            dao.insertCounterStock(stock.copy(currentQuantity = revertedQty))
        }

        // Create dispute record
        val disputeId = dao.insertDispute(
            Dispute(
                shiftId = adjustment.shiftId ?: 0,
                counterId = adjustment.counterId,
                counterName = adjustment.counterName,
                itemId = adjustment.itemId,
                itemName = adjustment.itemName,
                expectedQty = adjustment.qtyAddedOrRemoved,
                reportedQty = 0,
                discrepancy = -adjustment.qtyAddedOrRemoved,
                raisedByAttendantId = 0,
                raisedByAttendantName = attendantName,
                status = DisputeStatus.OPEN,
                resolutionNotes = "Mid-shift adjustment disputed by attendant."
            )
        )

        dao.insertNotification(
            AppNotification(
                targetRole = UserRole.ADMIN,
                type = NotificationType.DISPUTE_RAISED,
                title = "Mid-Shift Adjustment Disputed",
                message = "$attendantName disputed stock adjustment of ${adjustment.qtyAddedOrRemoved} units for ${adjustment.itemName}.",
                relatedId = disputeId
            )
        )
        
}


    // --- Shift Ending & Reconciliation Computation ---
    data class ClosingItemInput(
        val itemId: Long,
        val itemName: String,
        val category: ItemCategory,
        val unitType: String,
        val unitPrice: Double,
        val openingQty: Int,
        val adjustmentQty: Int,
        val closingQty: Int
    ) {
        val effectiveOpening: Int = openingQty + adjustmentQty
        val unitsSold: Int = maxOf(0, effectiveOpening - closingQty)
        val expectedAmount: Double = unitsSold * unitPrice
    }

    suspend fun getOpeningAndAdjustmentDataForClosing(shiftId: Long, counterId: Long): List<ClosingItemInput> {
        val verifications = dao.getStockVerificationsForShiftSync(shiftId)
        val confirmedAdjustments = dao.getConfirmedAdjustmentsForShiftSync(shiftId)

        val closingList = verifications.map { ver ->
            val netAdj = confirmedAdjustments.filter { it.itemId == ver.itemId }.sumOf { it.qtyAddedOrRemoved }
            ClosingItemInput(
                itemId = ver.itemId,
                itemName = ver.itemName,
                category = ver.category,
                unitType = ver.unitType,
                unitPrice = ver.unitPrice,
                openingQty = ver.attendantEnteredQty,
                adjustmentQty = netAdj,
                closingQty = maxOf(0, ver.attendantEnteredQty + netAdj)
            )
        }.toMutableList()

        val existingItemIds = verifications.map { it.itemId }.toSet()
        val extraAdjustments = confirmedAdjustments.filter { it.itemId !in existingItemIds }
        extraAdjustments.groupBy { it.itemId }.forEach { (itemId, adjs) ->
            val item = dao.getItemById(itemId)
            if (item != null) {
                val netAdj = adjs.sumOf { it.qtyAddedOrRemoved }
                closingList.add(
                    ClosingItemInput(
                        itemId = itemId,
                        itemName = item.name,
                        category = item.category,
                        unitType = item.unitType,
                        unitPrice = item.unitPrice,
                        openingQty = 0,
                        adjustmentQty = netAdj,
                        closingQty = maxOf(0, netAdj)
                    )
                )
            }
        }

        return closingList
    }

    suspend fun closeShiftAndReconcile(
        shiftId: Long,
        submittedCash: Double,
        closingInputs: List<ClosingItemInput>,
        closingNotes: String = ""
    ): Pair<Shift, List<ShiftClosing>> {
        val shift = dao.getShiftById(shiftId) ?: throw IllegalStateException("Shift not found")
        val counter = dao.getCounterById(shift.counterId)

        // 1. Calculate totals
        var totalExpected = 0.0
        val closingEntities = closingInputs.map { item ->
            val effectiveOpening = item.openingQty + item.adjustmentQty
            val unitsSold = maxOf(0, effectiveOpening - item.closingQty)
            val expectedAmount = unitsSold * item.unitPrice
            totalExpected += expectedAmount

            ShiftClosing(
                shiftId = shiftId,
                itemId = item.itemId,
                itemName = item.itemName,
                category = item.category,
                unitType = item.unitType,
                unitPrice = item.unitPrice,
                openingQty = item.openingQty,
                adjustmentsQty = item.adjustmentQty,
                effectiveOpeningQty = effectiveOpening,
                closingQty = item.closingQty,
                unitsSold = unitsSold,
                expectedAmount = expectedAmount
            )
        }
        dao.insertShiftClosings(closingEntities)

        // 2. Update Counter stock quantities to closing quantities
        closingInputs.forEach { item ->
            dao.updateStockQuantity(shift.counterId, item.itemId, item.closingQty)
        }

        // 3. Compute variance
        val variance = submittedCash - totalExpected
        val recType = when {
            variance < -0.01 -> ReconciliationType.LOSS
            variance > 0.01 -> ReconciliationType.EXTRA
            else -> ReconciliationType.BALANCED
        }

        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthYear = monthFormat.format(Date())

        // 4. Update Shift
        val updatedShift = shift.copy(
            endTime = System.currentTimeMillis(),
            status = ShiftStatus.CLOSED,
            totalExpectedSales = totalExpected,
            totalSubmittedCash = submittedCash,
            variance = variance,
            reconciliationType = recType,
            notes = closingNotes
        )
        dao.updateShift(updatedShift)

        // 5. Unlock Counter (now available for next attendant)
        dao.updateCounterActiveShift(shift.counterId, null, null, null)

        // 6. Record Reconciliation in Ledger
        dao.insertReconciliation(
            Reconciliation(
                shiftId = shiftId,
                attendantId = shift.attendantId,
                attendantName = shift.attendantName,
                counterName = shift.counterName,
                expectedTotal = totalExpected,
                submittedTotal = submittedCash,
                variance = variance,
                type = recType,
                date = System.currentTimeMillis(),
                monthYear = currentMonthYear,
                notes = "Shift #$shiftId (${shift.counterName}) - $closingNotes"
            )
        )

        // 7. Push Notifications
        val varianceFormatted = String.format(Locale.US, "$%.2f", Math.abs(variance))
        val statusMessage = when (recType) {
            ReconciliationType.LOSS -> "Closed with LOSS of $varianceFormatted. Expected: $${String.format(Locale.US, "%.2f", totalExpected)}, Submitted: $${String.format(Locale.US, "%.2f", submittedCash)}."
            ReconciliationType.EXTRA -> "Closed with EXTRA of $varianceFormatted. Expected: $${String.format(Locale.US, "%.2f", totalExpected)}, Submitted: $${String.format(Locale.US, "%.2f", submittedCash)}."
            ReconciliationType.BALANCED, ReconciliationType.NONE -> "Closed cleanly with balanced cash ($${String.format(Locale.US, "%.2f", totalExpected)})."
        }

        // Notify Admin
        dao.insertNotification(
            AppNotification(
                targetRole = UserRole.ADMIN,
                type = NotificationType.SHIFT_CLOSED_RECONCILIATION,
                title = "Shift Ended: ${shift.attendantName} at ${shift.counterName}",
                message = statusMessage,
                relatedId = shiftId
            )
        )

        // Notify Attendant
        dao.insertNotification(
            AppNotification(
                targetUserId = shift.attendantId,
                targetRole = UserRole.ATTENDANT,
                type = NotificationType.SHIFT_CLOSED_RECONCILIATION,
                title = "Shift Reconciled",
                message = statusMessage,
                relatedId = shiftId
            )
        )

        return Pair(updatedShift, closingEntities)
    }

    suspend fun getShiftClosings(shiftId: Long): List<ShiftClosing> {
        return dao.getShiftClosingsForShiftSync(shiftId)
    }

    // --- Disputes ---
    val allDisputes: Flow<List<Dispute>> = dao.getAllDisputes()
    val openDisputes: Flow<List<Dispute>> = dao.getDisputesByStatus(DisputeStatus.OPEN)
    fun getDisputesForAttendant(attendantId: Long): Flow<List<Dispute>> = dao.getDisputesForAttendant(attendantId)

    suspend fun resolveDispute(
        disputeId: Long,
        resolutionNotes: String,
        adminName: String,
        adjustedStockQty: Int?
    ) {
        val dispute = dao.getDisputeById(disputeId) ?: return
        val updated = dispute.copy(
            status = DisputeStatus.RESOLVED,
            resolutionNotes = resolutionNotes,
            resolvedByAdminName = adminName,
            resolvedAt = System.currentTimeMillis(),
            adjustedStockQty = adjustedStockQty
        )
        dao.updateDispute(updated)

        // If admin adjusted the official stock quantity
        if (adjustedStockQty != null && adjustedStockQty >= 0) {
            dao.updateStockQuantity(dispute.counterId, dispute.itemId, adjustedStockQty)
        }

        // Notify Raised Attendant
        dao.insertNotification(
            AppNotification(
                targetUserId = dispute.raisedByAttendantId,
                targetRole = UserRole.ATTENDANT,
                type = NotificationType.DISPUTE_RESOLVED,
                title = "Dispute Resolved: ${dispute.itemName}",
                message = "Admin $adminName resolved dispute on ${dispute.itemName} at ${dispute.counterName}. Notes: $resolutionNotes",
                relatedId = disputeId
            )
        )

        // Notify Previous Attendant if involved
        dispute.involvesPreviousAttendantId?.let { prevId ->
            dao.insertNotification(
                AppNotification(
                    targetUserId = prevId,
                    targetRole = UserRole.ATTENDANT,
                    type = NotificationType.DISPUTE_RESOLVED,
                    title = "Dispute Resolved: ${dispute.itemName}",
                    message = "Admin $adminName resolved dispute regarding ${dispute.itemName} at ${dispute.counterName}. Notes: $resolutionNotes",
                    relatedId = disputeId
                )
            )
        }
    }

    // --- Losses & Extras Ledger ---
    val allReconciliations: Flow<List<Reconciliation>> = dao.getAllReconciliations()
    fun getReconciliationsByAttendant(attendantId: Long): Flow<List<Reconciliation>> =
        dao.getReconciliationsByAttendant(attendantId)
    fun getReconciliationsByMonth(monthYear: String): Flow<List<Reconciliation>> =
        dao.getReconciliationsByMonth(monthYear)

    // --- Notifications ---
    val allNotifications: Flow<List<AppNotification>> = dao.getAllNotifications()
    fun getNotificationsForUser(userId: Long, userRole: UserRole): Flow<List<AppNotification>> =
        dao.getNotificationsForUser(userId, userRole)

    suspend fun markNotificationAsRead(id: Long) = dao.markNotificationAsRead(id).also {  }
    suspend fun markAllNotificationsAsRead() = dao.markAllNotificationsAsRead().also {  }

    fun getShiftVerifications(shiftId: Long): Flow<List<StockVerification>> =
        dao.getStockVerificationsForShift(shiftId)

    fun getShiftClosingsFlow(shiftId: Long): Flow<List<ShiftClosing>> =
        dao.getShiftClosingsForShift(shiftId)
}
