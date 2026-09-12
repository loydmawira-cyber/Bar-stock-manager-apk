package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdjustmentStatus
import com.example.data.model.AppNotification
import com.example.data.model.Counter
import com.example.data.model.CounterStockWithItem
import com.example.data.model.Dispute
import com.example.data.model.DisputeStatus
import com.example.data.model.Item
import com.example.data.model.ItemCategory
import com.example.data.model.Reconciliation
import com.example.data.model.Shift
import com.example.data.model.ShiftClosing
import com.example.data.model.StockAdjustment
import com.example.data.model.StockVerification
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.repository.BarStockRepository
import com.example.data.repository.BackupService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH,
    ADMIN_DASHBOARD,
    ATTENDANT_DASHBOARD,
    SHIFT_VERIFICATION,
    ACTIVE_SHIFT,
    SHIFT_CLOSING,
    SHIFT_SUMMARY,
    DISPUTES,
    LEDGER,
    COUNTERS_MANAGEMENT,
    ITEMS_MANAGEMENT,
    USERS_MANAGEMENT,
    BAR_PROFILE,
    NOTIFICATIONS
}

data class OpeningVerificationState(
    val itemId: Long,
    val itemName: String,
    val category: ItemCategory,
    val unitType: String,
    val unitPrice: Double,
    val systemQty: Int,
    val countedQty: Int,
    val isApproved: Boolean
)

data class ClosingCountState(
    val itemId: Long,
    val itemName: String,
    val category: ItemCategory,
    val unitType: String,
    val unitPrice: Double,
    val openingQty: Int,
    val adjustmentQty: Int,
    val closingQty: Int
) {
    val effectiveOpening: Int get() = openingQty + adjustmentQty
    val unitsSold: Int get() = maxOf(0, effectiveOpening - closingQty)
    val expectedAmount: Double get() = unitsSold * unitPrice
}

class BarStockViewModel(
    private val repository: BarStockRepository,
    private val backupService: BackupService
) : ViewModel() {

    // Must be initialized before init{} because startup synchronization
    // updates this state immediately.
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        autoSync()
    }

    private val _currentScreen = MutableStateFlow(AppScreen.AUTH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Bar Profile & Settings
    val barProfile = repository.barProfile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.data.model.BarProfile()
    )

    // Users
    val allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingUsers = repository.pendingUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attendants = repository.attendants.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Catalog & Counters
    val allItems = repository.allItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCounters = repository.allCounters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shifts & Disputes & Ledgers
    val allShifts = repository.allShifts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDisputes = repository.allDisputes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val openDisputes = repository.openDisputes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReconciliations = repository.allReconciliations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStockAdjustments = repository.allStockAdjustments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<AppNotification>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getNotificationsForUser(user.id, user.role)
        } else {
            repository.allNotifications
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active shift for currently logged in attendant
    val activeShift: StateFlow<Shift?> = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == UserRole.ATTENDANT) {
            repository.getActiveShiftForAttendantFlow(user.id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current shift verification state (Shift Start)
    private val _selectedCounterForShift = MutableStateFlow<Counter?>(null)
    val selectedCounterForShift: StateFlow<Counter?> = _selectedCounterForShift.asStateFlow()

    private val _verificationItems = MutableStateFlow<List<OpeningVerificationState>>(emptyList())
    val verificationItems: StateFlow<List<OpeningVerificationState>> = _verificationItems.asStateFlow()

    // Current shift closing state (Shift End)
    private val _closingItems = MutableStateFlow<List<ClosingCountState>>(emptyList())
    val closingItems: StateFlow<List<ClosingCountState>> = _closingItems.asStateFlow()

    private val _submittedCashInput = MutableStateFlow("")
    val submittedCashInput: StateFlow<String> = _submittedCashInput.asStateFlow()

    private val _closingNotesInput = MutableStateFlow("")
    val closingNotesInput: StateFlow<String> = _closingNotesInput.asStateFlow()

    // Shift summary receipt
    private val _lastClosedShift = MutableStateFlow<Shift?>(null)
    val lastClosedShift: StateFlow<Shift?> = _lastClosedShift.asStateFlow()

    private val _lastShiftClosings = MutableStateFlow<List<ShiftClosing>>(emptyList())
    val lastShiftClosings: StateFlow<List<ShiftClosing>> = _lastShiftClosings.asStateFlow()

    // Active pending adjustments for current active shift
    val activeShiftPendingAdjustments: StateFlow<List<StockAdjustment>> = activeShift.flatMapLatest { shift ->
        if (shift != null) {
            repository.getPendingAdjustmentsForShift(shift.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stock for selected counter (Management view)
    private val _selectedCounterId = MutableStateFlow<Long?>(null)
    val selectedCounterId: StateFlow<Long?> = _selectedCounterId.asStateFlow()

    val selectedCounterStocks: StateFlow<List<CounterStockWithItem>> = combine(
        _selectedCounterId,
        activeShift
    ) { selId, shift ->
        selId ?: shift?.counterId
    }.flatMapLatest { id ->
        if (id != null) repository.getCounterStocksWithItems(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly ledger filter
    private val _selectedAttendantFilterId = MutableStateFlow<Long?>(null) // null = all
    val selectedAttendantFilterId: StateFlow<Long?> = _selectedAttendantFilterId.asStateFlow()

    fun setAttendantLedgerFilter(attendantId: Long?) {
        _selectedAttendantFilterId.value = attendantId
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun loginUser(user: User) {
        if (user.status == UserStatus.PENDING) {
            viewModelScope.launch {
                _toastMessage.emit("Account is pending Admin approval. Please wait for confirmation.")
            }
            return
        }
        if (user.status == UserStatus.REVOKED) {
            viewModelScope.launch {
                _toastMessage.emit("Account access has been revoked by management.")
            }
            return
        }

        _currentUser.value = user
        if (user.role == UserRole.ADMIN) {
            _currentScreen.value = AppScreen.ADMIN_DASHBOARD
        } else {
            _currentScreen.value = AppScreen.ATTENDANT_DASHBOARD
        }
    }

    val phoneAuthManager = PhoneAuthManager()

    fun loginWithPhoneCredential(credential: PhoneAuthCredential, phone: String) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().signInWithCredential(credential).await()
                val localUser = repository.getUserByPhone(phone)
                if (localUser != null) {
                    loginUser(localUser)
                } else {
                    _toastMessage.emit("Phone verified, but no local account found.")
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Phone sign-in failed")
            }
        }
    }

    fun loginWithCredentials(identifier: String, password: String) {
        viewModelScope.launch {
            if (identifier.isBlank() || password.isBlank()) {
                _toastMessage.emit("Please enter your email/phone and password.")
                return@launch
            }

            // 1. Try local authentication first (instant and reliable)
            val localUser = repository.authenticateUser(identifier.trim(), password.trim())
            if (localUser != null) {
                loginUser(localUser)
                // Sync cloud in background without blocking login
                autoSync()
                return@launch
            }

            // 2. If not found locally, attempt to retrieve cloud tenant data for this user
            _isSyncing.value = true
            try {
                val syncResult = backupService.syncData(identifier.trim())
                if (syncResult.isSuccess) {
                    val syncedUser = repository.authenticateUser(identifier.trim(), password.trim())
                    if (syncedUser != null) {
                        loginUser(syncedUser)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }

            _toastMessage.emit("Invalid credentials. Please verify your email/phone and password.")
        }
    }

    // --- Cloud Synchronization ---
    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = backupService.syncData()
            if (result.isSuccess) {
                _toastMessage.emit("Cloud data synchronized successfully!")
            } else {
                _toastMessage.emit("Sync failed: ${result.exceptionOrNull()?.message ?: "Check connection"}")
            }
            _isSyncing.value = false
        }
    }

    fun autoSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                backupService.syncData()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun autoBackup() {
        viewModelScope.launch {
            try {
                backupService.backupData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Ensure latest local snapshot is saved to cloud before clearing user
                backupService.backupData()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
                _currentUser.value = null
                _currentScreen.value = AppScreen.AUTH
                _toastMessage.emit("Logged out successfully.")
            }
        }
    }

    // --- Bar Profile Settings ---
    fun registerBarAndAdmin(
        barName: String,
        location: String,
        adminName: String,
        phone: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            if (barName.isBlank() || location.isBlank() || adminName.isBlank() || phone.isBlank() || password.isBlank()) {
                _toastMessage.emit("Bar Name, Location, Admin Name, Phone, and Password are required.")
                return@launch
            }
            if (email.isBlank() || !email.contains("@")) {
                _toastMessage.emit("Admin Email is compulsory and must be a valid email (used to reset password).")
                return@launch
            }
            val current = barProfile.value
            val updated = current.copy(
                barName = barName.trim(),
                location = location.trim(),
                managerName = adminName.trim(),
                contactPhone = phone.trim(),
                isRegistered = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBarProfile(updated)

            val adminId = repository.registerUser(
                name = adminName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                role = UserRole.ADMIN,
                password = password.trim()
            )

            autoBackup()

            val newAdminUser = repository.getUserById(adminId)
            if (newAdminUser != null) {
                _currentUser.value = newAdminUser
                _currentScreen.value = AppScreen.ADMIN_DASHBOARD
                _toastMessage.emit("Bar/Club '${barName.trim()}' registered! Welcome Admin ${adminName.trim()}.")
            } else {
                _toastMessage.emit("Bar/Club registered successfully! Please log in.")
            }
        }
    }

    fun updateBarProfile(
        name: String,
        location: String,
        iconType: String,
        customPhotoUri: String?,
        contactPhone: String,
        currencySymbol: String,
        managerName: String,
        openingHours: String
    ) {
        viewModelScope.launch {
            if (name.isBlank() || location.isBlank()) {
                _toastMessage.emit("Bar Name and Location are required.")
                return@launch
            }
            val current = barProfile.value
            val updated = current.copy(
                barName = name.trim(),
                location = location.trim(),
                iconType = iconType,
                customPhotoUri = customPhotoUri,
                contactPhone = contactPhone.trim(),
                currencySymbol = currencySymbol.ifBlank { "$" },
                managerName = managerName.trim(),
                openingHours = openingHours.trim(),
                isRegistered = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBarProfile(updated)
            autoBackup()
            _toastMessage.emit("Bar Profile updated successfully!")
        }
    }

    fun registerAttendant(name: String, email: String, phone: String, password: String = "123456") {
        viewModelScope.launch {
            if (name.isBlank() || phone.isBlank()) {
                _toastMessage.emit("Please enter your name and phone number.")
                return@launch
            }
            repository.registerUser(name, email, phone, UserRole.ATTENDANT, password)
            autoBackup()
            _toastMessage.emit("Registration submitted! Account is pending admin approval.")
            _currentScreen.value = AppScreen.AUTH
        }
    }

    // --- Admin User Actions ---
    fun createAttendantByAdmin(name: String, email: String, phone: String, initialPassword: String) {
        viewModelScope.launch {
            if (name.isBlank() || phone.isBlank()) {
                _toastMessage.emit("Attendant Name and Phone Number are required.")
                return@launch
            }
            val password = initialPassword.ifBlank { "123456" }
            repository.createAttendantByAdmin(name.trim(), email.trim(), phone.trim(), password.trim())
            autoBackup()
            _toastMessage.emit("Attendant account created for $name with password.")
        }
    }

    fun approveUser(userId: Long) {
        viewModelScope.launch {
            repository.updateUserStatus(userId, UserStatus.APPROVED)
            autoBackup()
            _toastMessage.emit("Attendant account approved.")
        }
    }

    fun revokeUser(userId: Long) {
        viewModelScope.launch {
            repository.updateUserStatus(userId, UserStatus.REVOKED)
            autoBackup()
            _toastMessage.emit("Attendant login access revoked.")
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            autoBackup()
            _toastMessage.emit("Attendant profile deleted.")
        }
    }

    // --- Password Management (Attendant Full Control) ---
    fun changePassword(
        userId: Long,
        oldPassword: String?,
        newPassword: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.changeUserPassword(userId, oldPassword, newPassword)
            result.onSuccess {
                // If the current user changed their password, reload currentUser if needed
                val user = repository.getUserById(userId)
                if (user != null && _currentUser.value?.id == userId) {
                    _currentUser.value = user
                }
                autoBackup()
                _toastMessage.emit("Password changed successfully!")
                onSuccess()
            }.onFailure { error ->
                val msg = error.message ?: "Failed to update password."
                _toastMessage.emit(msg)
                onError(msg)
            }
        }
    }

    fun selfResetPassword(
        identifier: String,
        newPassword: String,
        onSuccess: (User) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (identifier.isBlank()) {
                val msg = "Please enter your registered email or phone number."
                _toastMessage.emit(msg)
                onError(msg)
                return@launch
            }
            val result = repository.resetPasswordByIdentifier(identifier, newPassword)
            result.onSuccess { user ->
                autoBackup()
                _toastMessage.emit("Password reset successful! You can now log in.")
                onSuccess(user)
            }.onFailure { error ->
                val msg = error.message ?: "Failed to reset password."
                _toastMessage.emit(msg)
                onError(msg)
            }
        }
    }

    // --- Shift Start & Verification Flow ---
    fun selectCounterForShiftStart(counter: Counter) {
        viewModelScope.launch {
            val (canStart, reason) = repository.canStartShiftAtCounter(counter.id)
            if (!canStart) {
                _toastMessage.emit(reason ?: "Counter is currently unavailable.")
                return@launch
            }
            _selectedCounterForShift.value = counter

            // Load counter stock for verification
            val stocks = repository.getCounterStocksWithItems(counter.id)
            stocks.collect { stockList ->
                _verificationItems.value = stockList.map { item ->
                    OpeningVerificationState(
                        itemId = item.itemId,
                        itemName = item.itemName,
                        category = item.category,
                        unitType = item.unitType,
                        unitPrice = item.unitPrice,
                        systemQty = item.currentQuantity,
                        countedQty = item.currentQuantity,
                        isApproved = true
                    )
                }
                _currentScreen.value = AppScreen.SHIFT_VERIFICATION
                return@collect
            }
        }
    }

    fun updateVerificationItemApproval(itemId: Long, approved: Boolean) {
        _verificationItems.value = _verificationItems.value.map {
            if (it.itemId == itemId) it.copy(isApproved = approved) else it
        }
    }

    fun updateVerificationItemCountedQty(itemId: Long, count: Int) {
        _verificationItems.value = _verificationItems.value.map {
            if (it.itemId == itemId) {
                val clamped = maxOf(0, count)
                it.copy(
                    countedQty = clamped,
                    isApproved = clamped == it.systemQty
                )
            } else it
        }
    }

    fun confirmOpeningStockAndStartShift() {
        val user = _currentUser.value ?: return
        val counter = _selectedCounterForShift.value ?: return
        val items = _verificationItems.value

        viewModelScope.launch {
            val inputs = items.map { item ->
                BarStockRepository.OpeningVerificationItemInput(
                    itemId = item.itemId,
                    itemName = item.itemName,
                    category = item.category,
                    unitType = item.unitType,
                    unitPrice = item.unitPrice,
                    systemQty = item.systemQty,
                    countedQty = item.countedQty,
                    isApproved = item.isApproved
                )
            }

            val shiftId = repository.startShiftWithVerification(
                counterId = counter.id,
                attendantId = user.id,
                attendantName = user.name,
                verifications = inputs
            )

            val disputeCount = items.count { !it.isApproved || it.countedQty != it.systemQty }
            if (disputeCount > 0) {
                _toastMessage.emit("Shift #$shiftId started! $disputeCount discrepancy dispute(s) flagged to Admin.")
            } else {
                _toastMessage.emit("Shift #$shiftId started! All opening stock verified OK.")
            }

            autoBackup()
            _currentScreen.value = AppScreen.ACTIVE_SHIFT
        }
    }

    // --- Mid-Shift Attendant Responses ---
    fun confirmAdjustment(adjustment: StockAdjustment) {
        viewModelScope.launch {
            repository.confirmStockAdjustment(adjustment)
            autoBackup()
            _toastMessage.emit("Stock adjustment confirmed and added to your shift baseline.")
        }
    }

    fun disputeAdjustment(adjustment: StockAdjustment) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.disputeStockAdjustment(adjustment, user.name)
            autoBackup()
            _toastMessage.emit("Adjustment discrepancy reported as dispute to Admin.")
        }
    }

    // --- Shift End & Closing Flow ---
    fun prepareShiftClosing() {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            val closingInputs = repository.getOpeningAndAdjustmentDataForClosing(shift.id, shift.counterId)
            _closingItems.value = closingInputs.map { input ->
                ClosingCountState(
                    itemId = input.itemId,
                    itemName = input.itemName,
                    category = input.category,
                    unitType = input.unitType,
                    unitPrice = input.unitPrice,
                    openingQty = input.openingQty,
                    adjustmentQty = input.adjustmentQty,
                    closingQty = input.closingQty
                )
            }
            _submittedCashInput.value = ""
            _closingNotesInput.value = ""
            _currentScreen.value = AppScreen.SHIFT_CLOSING
        }
    }

    fun updateClosingQty(itemId: Long, count: Int) {
        _closingItems.value = _closingItems.value.map {
            if (it.itemId == itemId) {
                it.copy(closingQty = maxOf(0, count))
            } else it
        }
    }

    fun updateSubmittedCashInput(value: String) {
        _submittedCashInput.value = value
    }

    fun updateClosingNotes(value: String) {
        _closingNotesInput.value = value
    }

    fun submitShiftClosing() {
        val shift = activeShift.value ?: return
        val submittedCash = _submittedCashInput.value.toDoubleOrNull()
        if (submittedCash == null || submittedCash < 0) {
            viewModelScope.launch {
                _toastMessage.emit("Please enter a valid cash amount submitted.")
            }
            return
        }

        viewModelScope.launch {
            val inputs = _closingItems.value.map { item ->
                BarStockRepository.ClosingItemInput(
                    itemId = item.itemId,
                    itemName = item.itemName,
                    category = item.category,
                    unitType = item.unitType,
                    unitPrice = item.unitPrice,
                    openingQty = item.openingQty,
                    adjustmentQty = item.adjustmentQty,
                    closingQty = item.closingQty
                )
            }

            val (closedShift, closings) = repository.closeShiftAndReconcile(
                shiftId = shift.id,
                submittedCash = submittedCash,
                closingInputs = inputs,
                closingNotes = _closingNotesInput.value
            )

            _lastClosedShift.value = closedShift
            _lastShiftClosings.value = closings

            autoBackup()
            _toastMessage.emit("Shift closed successfully and reconciled.")
            _currentScreen.value = AppScreen.SHIFT_SUMMARY
        }
    }

    fun viewShiftReceipt(shift: Shift) {
        viewModelScope.launch {
            _lastClosedShift.value = shift
            _lastShiftClosings.value = repository.getShiftClosings(shift.id)
            _currentScreen.value = AppScreen.SHIFT_SUMMARY
        }
    }

    fun viewShiftReceiptById(shiftId: Long) {
        viewModelScope.launch {
            val shift = repository.getShiftById(shiftId)
            if (shift != null) {
                _lastClosedShift.value = shift
                _lastShiftClosings.value = repository.getShiftClosings(shift.id)
                _currentScreen.value = AppScreen.SHIFT_SUMMARY
            }
        }
    }

    // --- Admin Dispute Resolution ---
    fun resolveDispute(
        disputeId: Long,
        resolutionNotes: String,
        adjustedStockQty: Int?
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            if (resolutionNotes.isBlank()) {
                _toastMessage.emit("Please enter resolution notes.")
                return@launch
            }
            repository.resolveDispute(
                disputeId = disputeId,
                resolutionNotes = resolutionNotes,
                adminName = admin.name,
                adjustedStockQty = adjustedStockQty
            )
            autoBackup()
            _toastMessage.emit("Dispute resolved successfully.")
        }
    }

    // --- Admin Stock Adjustments (Restock) ---
    fun addStockAdjustment(
        counterId: Long,
        itemId: Long,
        qty: Int,
        reason: String
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            if (qty == 0) {
                _toastMessage.emit("Quantity cannot be zero.")
                return@launch
            }
            repository.addMidShiftAdjustment(
                counterId = counterId,
                itemId = itemId,
                quantityToAddOrRemove = qty,
                adminName = admin.name,
                reason = reason.ifBlank { "Restock" }
            )
            autoBackup()
            _toastMessage.emit("Stock adjustment recorded.")
        }
    }

    // --- Admin Counter & Stock Management ---
    fun setSelectedCounterForManagement(counterId: Long) {
        _selectedCounterId.value = counterId
    }

    fun createCounter(name: String, location: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toastMessage.emit("Please enter counter name.")
                return@launch
            }
            val newId = repository.addCounter(name, location)
            _selectedCounterId.value = newId
            autoBackup()
            _toastMessage.emit("Counter '${name.trim()}' created.")
        }
    }

    fun updateCounter(counterId: Long, name: String, location: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toastMessage.emit("Counter name cannot be blank.")
                return@launch
            }
            val existing = repository.getCounterById(counterId)
            if (existing != null) {
                repository.updateCounter(existing.copy(name = name.trim(), location = location.trim()))
                autoBackup()
                _toastMessage.emit("Counter '${name.trim()}' updated successfully.")
            }
        }
    }

    fun deleteCounter(counterId: Long) {
        viewModelScope.launch {
            val counter = repository.getCounterById(counterId)
            if (counter == null) return@launch
            if (counter.activeShiftId != null) {
                _toastMessage.emit("Cannot delete '${counter.name}' while an attendant shift is active!")
                return@launch
            }
            repository.deleteCounter(counterId)
            if (_selectedCounterId.value == counterId) {
                val remaining = allCounters.value.filter { it.id != counterId }
                _selectedCounterId.value = remaining.firstOrNull()?.id
            }
            autoBackup()
            _toastMessage.emit("Counter '${counter.name}' deleted.")
        }
    }

    fun assignItemToCounter(counterId: Long, itemId: Long, initialQty: Int) {
        val adminName = _currentUser.value?.name ?: "Admin"
        viewModelScope.launch {
            repository.setCounterItemStock(counterId, itemId, initialQty, adminName = adminName)
            autoBackup()
            _toastMessage.emit("Assigned/Added $initialQty units to counter stock.")
        }
    }

    fun createStockItem(
        name: String,
        category: ItemCategory,
        unitPrice: Double,
        casePrice: Double,
        unitType: String,
        description: String = ""
    ) {
        viewModelScope.launch {
            if (name.isBlank() || unitPrice <= 0) {
                _toastMessage.emit("Please enter valid item details.")
                return@launch
            }
            repository.addItem(name, category, unitPrice, casePrice, unitType, description)
            _toastMessage.emit("Stock item added.")
            autoBackup()
        }
    }

    fun updateStockItem(
        itemId: Long,
        name: String,
        category: ItemCategory,
        unitPrice: Double,
        casePrice: Double,
        unitType: String,
        description: String = ""
    ) {
        viewModelScope.launch {
            if (name.isBlank() || unitPrice <= 0) {
                _toastMessage.emit("Please enter a valid item name and unit price.")
                return@launch
            }
            repository.updateItem(
                Item(
                    id = itemId,
                    name = name.trim(),
                    category = category,
                    unitPrice = unitPrice,
                    casePrice = casePrice,
                    unitType = unitType.trim().ifBlank { "Unit" },
                    description = description.trim()
                )
            )
            _toastMessage.emit("Stock item '${name.trim()}' updated successfully.")
            autoBackup()
        }
    }

    fun deleteStockItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            _toastMessage.emit("Stock item deleted.")
            autoBackup()
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            _toastMessage.emit("All notifications marked as read.")
        }
    }
}
