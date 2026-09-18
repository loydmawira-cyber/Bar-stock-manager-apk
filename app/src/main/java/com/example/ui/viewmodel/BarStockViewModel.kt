package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
import com.example.data.billing.BillingConnectionState
import com.example.data.billing.BillingManager
import com.example.data.billing.BillingPlan
import com.example.data.billing.BillingStatus
import com.example.data.model.AdjustmentStatus
import com.example.data.model.AppNotification
import com.example.data.model.Counter
import com.example.data.model.CounterStockWithItem
import com.example.data.model.Dispute
import com.example.data.model.DisputeStatus
import com.example.data.model.Expense
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
import com.example.data.model.MonthlyReport
import com.example.data.repository.BarStockRepository
import com.example.data.repository.BackupService
import com.example.data.repository.MonthlyReportService
import com.example.data.util.PasswordValidator
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
    NOTIFICATIONS,
    STORE_INVENTORY,
    EXPENSES_MANAGEMENT,
    PROFIT_LOSS_REPORT,
    MONTHLY_REPORTS
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
    val closingQty: Int,
    val totEnabled: Boolean = false,
    val bottleVolumeMl: Int = 0,
    val closingLooseMl: Int = 0,
    val totSizeMl: Int = 0,
    val totPrice: Double = 0.0
) {
    val effectiveOpening: Int get() = openingQty + adjustmentQty
    val unitsSold: Int get() = maxOf(0, effectiveOpening - closingQty)
    val closingVolumeMl: Int get() = if (totEnabled && bottleVolumeMl > 0) closingQty * bottleVolumeMl + closingLooseMl else closingQty
    val openingVolumeMl: Int get() = if (totEnabled && bottleVolumeMl > 0) effectiveOpening * bottleVolumeMl else effectiveOpening
    val volumeSoldMl: Int get() = maxOf(0, openingVolumeMl - closingVolumeMl)
    val expectedAmount: Double get() = if (totEnabled && bottleVolumeMl > 0) {
        val fullBottles = volumeSoldMl / bottleVolumeMl
        val remainderMl = volumeSoldMl % bottleVolumeMl
        fullBottles * unitPrice + if (totSizeMl > 0) (remainderMl / totSizeMl) * totPrice else 0.0
    } else unitsSold * unitPrice
}

class BarStockViewModel(
    private val repository: BarStockRepository,
    private val backupService: BackupService,
    private val billingManager: BillingManager,
    private val monthlyReportService: MonthlyReportService = MonthlyReportService()
) : ViewModel() {

    // Billing state & actions
    val billingStatus = billingManager.billingStatus
    val availableBillingPlans = billingManager.availablePlans
    val billingConnectionState = billingManager.connectionState

    // Must be initialized before init{} because startup synchronization
    // updates this state immediately.
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        viewModelScope.launch {
            billingManager.billingMessages.collect { msg ->
                _toastMessage.emit(msg)
            }
        }
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
    val allShiftClosings = repository.allShiftClosings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDisputes = repository.allDisputes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val openDisputes = repository.openDisputes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReconciliations = repository.allReconciliations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStockAdjustments = repository.allStockAdjustments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val storeStock = repository.getStoreStockWithItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCounterStocks = repository.allCounterStocks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchaseReceipts = repository.allPurchaseReceipts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<AppNotification>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getNotificationsForUser(user.id, user.role)
        } else {
            repository.allNotifications
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active shift for currently logged in user
    val activeShift: StateFlow<Shift?> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getActiveShiftForAttendantFlow(user.id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Expenses
    val allExpenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Managers & Owners
    val owners = repository.owners.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val managers = repository.managers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // P&L Report
    private val _selectedPnlTimeframe = MutableStateFlow(com.example.data.model.PnlTimeframe.THIS_MONTH)
    val selectedPnlTimeframe: StateFlow<com.example.data.model.PnlTimeframe> = _selectedPnlTimeframe.asStateFlow()

    private val _pnlReport = MutableStateFlow<com.example.data.model.ProfitAndLossReport?>(null)
    val pnlReport: StateFlow<com.example.data.model.ProfitAndLossReport?> = _pnlReport.asStateFlow()

    fun loadProfitAndLossReport(timeframe: com.example.data.model.PnlTimeframe = _selectedPnlTimeframe.value) {
        _selectedPnlTimeframe.value = timeframe
        viewModelScope.launch {
            _pnlReport.value = repository.generateProfitAndLossReport(timeframe)
        }
    }

    fun addExpense(
        category: String,
        description: String,
        amountText: String,
        paymentMethod: String,
        referenceNumber: String,
        notes: String?
    ) {
        viewModelScope.launch {
            if (category.isBlank() || description.isBlank() || amountText.isBlank()) {
                _toastMessage.emit("Category, description, and amount are required.")
                return@launch
            }
            val amt = amountText.toDoubleOrNull()
            if (amt == null || amt <= 0) {
                _toastMessage.emit("Amount must be a positive number greater than zero.")
                return@launch
            }
            if (paymentMethod.isBlank() || referenceNumber.isBlank()) {
                _toastMessage.emit("Payment method and reference number are required.")
                return@launch
            }
            val user = _currentUser.value
            val expense = com.example.data.model.Expense(
                category = category.trim(),
                description = description.trim(),
                amount = amt,
                paymentMethod = paymentMethod.trim(),
                referenceNumber = referenceNumber.trim(),
                recordedBy = user?.name ?: "Owner",
                notes = notes?.trim()?.takeIf { it.isNotBlank() }
            )
            repository.addExpense(expense)
            autoBackup()
            _toastMessage.emit("Expense recorded successfully: ${expense.category}")
            loadProfitAndLossReport(_selectedPnlTimeframe.value)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            autoBackup()
            _toastMessage.emit("Expense deleted.")
            loadProfitAndLossReport(_selectedPnlTimeframe.value)
        }
    }

    fun deletePurchaseReceipt(id: Long) {
        viewModelScope.launch {
            repository.deletePurchaseReceipt(id)
            autoBackup()
            _toastMessage.emit("Purchase receipt deleted and stock reversed.")
        }
    }

    fun createManagerByOwner(name: String, email: String, phone: String, initialPassword: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || phone.isBlank() || initialPassword.isBlank()) {
                _toastMessage.emit("Manager name, email, phone, and password are required.")
                return@launch
            }
            val validation = PasswordValidator.validate(initialPassword)
            if (!validation.isValid) {
                _toastMessage.emit("Password error: ${validation.missingRequirementsMessage}")
                return@launch
            }
            try {
                repository.createManagerByOwner(name.trim(), email.trim(), phone.trim(), initialPassword.trim())
                backupService.backupData().getOrThrow()
                _toastMessage.emit("Manager account created for $name.")
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Manager creation failed")
            }
        }
    }

    fun restoreUser(userId: Long) {
        viewModelScope.launch {
            repository.restoreUser(userId)
            autoBackup()
            _toastMessage.emit("User account restored.")
        }
    }

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
    private val _submittedMpesaInput = MutableStateFlow("0")
    val submittedMpesaInput: StateFlow<String> = _submittedMpesaInput.asStateFlow()
    private val _submittedCardInput = MutableStateFlow("0")
    val submittedCardInput: StateFlow<String> = _submittedCardInput.asStateFlow()
    private val _submittedBankInput = MutableStateFlow("0")
    val submittedBankInput: StateFlow<String> = _submittedBankInput.asStateFlow()
    private val _submittedPaymentInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val submittedPaymentInputs: StateFlow<Map<String, String>> = _submittedPaymentInputs.asStateFlow()

    private val _closingNotesInput = MutableStateFlow("")
    val closingNotesInput: StateFlow<String> = _closingNotesInput.asStateFlow()

    private val _monthlyReports = MutableStateFlow<List<MonthlyReport>>(emptyList())
    val monthlyReports: StateFlow<List<MonthlyReport>> = _monthlyReports.asStateFlow()
    private val _monthlyReportsLoading = MutableStateFlow(false)
    val monthlyReportsLoading: StateFlow<Boolean> = _monthlyReportsLoading.asStateFlow()

    fun loadMonthlyReports() {
        if (_currentUser.value?.role != UserRole.OWNER) return
        viewModelScope.launch {
            _monthlyReportsLoading.value = true
            try {
                val barId = repository.getBarProfileSync()?.let { "bar_${it.barId}" } ?: return@launch
                _monthlyReports.value = monthlyReportService.listReports(barId)
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Unable to load monthly reports.")
            } finally {
                _monthlyReportsLoading.value = false
            }
        }
    }

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

    fun receivePurchaseToStore(
        itemId: Long,
        purchaseQuantity: Int,
        unitsPerPurchaseUnit: Int,
        purchaseUnitType: String,
        supplierName: String,
        receiptNumber: String,
        unitCost: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val user = _currentUser.value ?: error("Please sign in first")
                repository.receivePurchaseToStore(
                    itemId = itemId,
                    purchaseQuantity = purchaseQuantity,
                    unitsPerPurchaseUnit = unitsPerPurchaseUnit,
                    purchaseUnitType = purchaseUnitType,
                    supplierName = supplierName,
                    receiptNumber = receiptNumber,
                    unitCost = unitCost,
                    receivedByUserId = user.id,
                    receivedByName = user.name,
                    notes = notes
                )
                backupService.backupData()
                _toastMessage.emit("Purchase receipt recorded and store stock updated.")
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Could not record purchase receipt.")
            }
        }
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
        if (user.role == UserRole.OWNER || user.role == UserRole.MANAGER) {
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
                // Restore the bar identified by this phone before reading the
                // local user; otherwise an old local database can win.
                backupService.syncData(phone.trim())
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

    private fun extractFirebaseErrorCode(e: Throwable): String {
        return when (e) {
            is com.google.firebase.auth.FirebaseAuthException -> e.errorCode
            is com.google.firebase.FirebaseNetworkException -> "auth/network-request-failed"
            else -> {
                val msg = e.message ?: ""
                when {
                    msg.contains("invalid-credential", ignoreCase = true) || msg.contains("ERROR_INVALID_CREDENTIAL", ignoreCase = true) -> "auth/invalid-credential"
                    msg.contains("user-not-found", ignoreCase = true) || msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) -> "auth/user-not-found"
                    msg.contains("wrong-password", ignoreCase = true) || msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) -> "auth/wrong-password"
                    msg.contains("user-disabled", ignoreCase = true) || msg.contains("ERROR_USER_DISABLED", ignoreCase = true) -> "auth/user-disabled"
                    msg.contains("invalid-email", ignoreCase = true) || msg.contains("ERROR_INVALID_EMAIL", ignoreCase = true) -> "auth/invalid-email"
                    msg.contains("network", ignoreCase = true) -> "auth/network-request-failed"
                    msg.contains("too-many-requests", ignoreCase = true) || msg.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) -> "auth/too-many-requests"
                    msg.contains("api-key-not-valid", ignoreCase = true) -> "auth/api-key-not-valid"
                    else -> "auth/unknown"
                }
            }
        }
    }

    private fun formatFirebaseAuthError(e: Throwable): String {
        val code = extractFirebaseErrorCode(e)
        val friendly = when {
            e is com.google.firebase.FirebaseNetworkException || code.contains("network", ignoreCase = true) ->
                "Network error. Please check your internet connection."
            code.contains("wrong-password", ignoreCase = true) || code.equals("ERROR_WRONG_PASSWORD", ignoreCase = true) ->
                "Wrong password. Please verify your password."
            code.contains("user-not-found", ignoreCase = true) || code.equals("ERROR_USER_NOT_FOUND", ignoreCase = true) ->
                "User not found. No account registered with this email or phone."
            code.contains("user-disabled", ignoreCase = true) || code.equals("ERROR_USER_DISABLED", ignoreCase = true) ->
                "Account disabled. Please contact support or management."
            code.contains("invalid-email", ignoreCase = true) || code.equals("ERROR_INVALID_EMAIL", ignoreCase = true) ->
                "Invalid email address format."
            code.contains("invalid-credential", ignoreCase = true) || code.equals("ERROR_INVALID_CREDENTIAL", ignoreCase = true) ->
                "Invalid credentials (wrong password or unverified account)."
            code.contains("too-many-requests", ignoreCase = true) || code.equals("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) ->
                "Too many failed attempts. Please try again in a few minutes."
            code.contains("api-key", ignoreCase = true) || code.contains("config", ignoreCase = true) ->
                "Firebase configuration error. Please verify project settings."
            else ->
                e.localizedMessage ?: "Authentication failed."
        }
        return "$friendly [$code]"
    }

    fun loginWithCredentials(identifier: String, password: String) {
        viewModelScope.launch {
            val cleanId = identifier.trim()
            val cleanPass = password.trim()

            if (cleanId.isBlank() || cleanPass.isBlank()) {
                _toastMessage.emit("Please enter your email/phone and password.")
                return@launch
            }

            _isSyncing.value = true

            try {
                val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(cleanId).matches() || cleanId.contains("@")

                if (isEmail) {
                    // 1. Authenticate against Firebase Authentication with existing credentials
                    val auth = FirebaseAuth.getInstance()
                    val authResult = try {
                        auth.signInWithEmailAndPassword(cleanId, cleanPass).await()
                    } catch (e: Exception) {
                        _isSyncing.value = false
                        val errorMsg = formatFirebaseAuthError(e)
                        _toastMessage.emit(errorMsg)
                        return@launch
                    }

                    val fbUser = authResult.user
                    val fbEmail = fbUser?.email ?: cleanId

                    // 2. Restore or load the matching Firestore bar backup
                    try {
                        backupService.syncData(fbEmail)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 3. Match the local user record using Firebase email, phone, or UID where available
                    val allUsers = repository.getAllUsersSync()
                    var localUser = repository.getUserByEmail(fbEmail)
                        ?: repository.getUserByIdentifier(cleanId)
                        ?: allUsers.firstOrNull { it.email.equals(fbEmail, ignoreCase = true) }
                        ?: allUsers.firstOrNull { it.email.equals(cleanId, ignoreCase = true) }
                        ?: allUsers.firstOrNull { it.role == UserRole.OWNER }

                    if (localUser != null) {
                        // 4. ADMIN -> OWNER migration (confirm migrated once only, status APPROVED)
                        if (localUser.role == UserRole.OWNER || localUser.role.name == "ADMIN") {
                            if (localUser.role != UserRole.OWNER || localUser.status != UserStatus.APPROVED) {
                                val updatedUser = localUser.copy(role = UserRole.OWNER, status = UserStatus.APPROVED)
                                repository.updateUser(updatedUser)
                                localUser = updatedUser
                            }
                        }
                        _isSyncing.value = false
                        loginUser(localUser)
                    } else {
                        _isSyncing.value = false
                        _toastMessage.emit("Account verified with Firebase, but no bar profile found in cloud backup.")
                    }
                } else {
                    // Phone / Attendant login
                    var localUser = repository.getUserByIdentifier(cleanId)
                    if (localUser == null) {
                        try {
                            backupService.syncData(cleanId)
                            localUser = repository.getUserByIdentifier(cleanId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (localUser != null) {
                        if (localUser.password == cleanPass) {
                            if (localUser.email.isNotBlank() && localUser.email.contains("@")) {
                                try {
                                    val currentFbEmail = FirebaseAuth.getInstance().currentUser?.email
                                    if (!currentFbEmail.equals(localUser.email.trim(), ignoreCase = true)) {
                                        FirebaseAuth.getInstance().signInWithEmailAndPassword(localUser.email.trim(), cleanPass).await()
                                    }
                                } catch (e: Exception) {
                                    // non-blocking
                                }
                            }
                            if (localUser.role == UserRole.OWNER || localUser.role.name == "ADMIN") {
                                if (localUser.role != UserRole.OWNER || localUser.status != UserStatus.APPROVED) {
                                    val updated = localUser.copy(role = UserRole.OWNER, status = UserStatus.APPROVED)
                                    repository.updateUser(updated)
                                    localUser = updated
                                }
                            }
                            _isSyncing.value = false
                            loginUser(localUser)
                        } else {
                            _isSyncing.value = false
                            _toastMessage.emit("Wrong password. Please verify your password. [auth/wrong-password]")
                        }
                    } else {
                        _isSyncing.value = false
                        _toastMessage.emit("User not found. No account registered with '$cleanId'. [auth/user-not-found]")
                    }
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                val errorMsg = formatFirebaseAuthError(e)
                _toastMessage.emit("Login failed: $errorMsg")
            }
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
                repository.clearAllDataForNewBar()
                FirebaseAuth.getInstance().signOut()
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
            val validation = PasswordValidator.validate(password.trim())
            if (!validation.isValid) {
                _toastMessage.emit("Password error: ${validation.missingRequirementsMessage}")
                return@launch
            }
            try {
                // Clear previous bar's local data to ensure total data isolation for the new bar
                repository.clearAllDataForNewBar()

                val updated = com.example.data.model.BarProfile(
                    id = 1L,
                    barName = barName.trim(),
                    location = location.trim(),
                    managerName = adminName.trim(),
                    contactPhone = phone.trim(),
                    isRegistered = true,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveBarProfile(updated)
                repository.seedInitialCounterForNewBar()
                repository.seedSampleBeersForNewBar()

                val adminId = repository.registerUser(
                    name = adminName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    role = UserRole.OWNER,
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
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Registration failed")
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
        paymentMethods: String,
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
                paymentMethods = paymentMethods.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(","),
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

    fun registerAttendant(name: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || phone.isBlank()) {
                _toastMessage.emit("Please enter your name, unique email, and phone number.")
                return@launch
            }
            val validation = PasswordValidator.validate(password)
            if (!validation.isValid) {
                _toastMessage.emit("Password error: ${validation.missingRequirementsMessage}")
                return@launch
            }
            try {
                repository.registerUser(name, email, phone, UserRole.ATTENDANT, password)
                autoBackup()
                _toastMessage.emit("Registration submitted! Account is pending admin approval.")
                _currentScreen.value = AppScreen.AUTH
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Registration failed")
            }
        }
    }

    // --- Admin User Actions ---
    fun createAttendantByAdmin(name: String, email: String, phone: String, initialPassword: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || phone.isBlank()) {
                _toastMessage.emit("Attendant name, unique email, and phone number are required.")
                return@launch
            }
            val validation = PasswordValidator.validate(initialPassword)
            if (!validation.isValid) {
                _toastMessage.emit("Password error: ${validation.missingRequirementsMessage}")
                return@launch
            }
            try {
                repository.createAttendantByAdmin(name.trim(), email.trim(), phone.trim(), initialPassword.trim())
                // Do not let the attendant log in before the updated bar
                // snapshot (including this attendant) is in Firestore.
                backupService.backupData().getOrThrow()
                _toastMessage.emit("Attendant account created for $name with strong password.")
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Attendant creation failed")
            }
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
            try {
                backupService.backupData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _toastMessage.emit("Attendant login access revoked.")
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            try {
                backupService.backupData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _toastMessage.emit("Attendant account deleted from system. All sales history preserved.")
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

    fun sendPasswordResetEmail(
        email: String,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (email.isBlank()) {
                val msg = "Please enter your registered account email."
                _toastMessage.emit(msg)
                onError(msg)
                return@launch
            }
            val result = repository.sendPasswordResetEmail(email)
            result.onSuccess { successMsg ->
                _toastMessage.emit(successMsg)
                onSuccess(successMsg)
            }.onFailure { error ->
                val msg = error.message ?: "Failed to send password reset email."
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
                    closingQty = input.closingQty,
                    totEnabled = input.totEnabled,
                    bottleVolumeMl = input.bottleVolumeMl,
                    closingLooseMl = input.closingLooseMl,
                    totSizeMl = input.totSizeMl,
                    totPrice = input.totPrice
                )
            }
            _submittedCashInput.value = ""
            _submittedMpesaInput.value = "0"
            _submittedCardInput.value = "0"
            _submittedBankInput.value = "0"
            _submittedPaymentInputs.value = barProfile.value.paymentMethods.split(",").map { it.trim() }.filter { it.isNotBlank() }.associateWith { "0" }
            _closingNotesInput.value = ""
            _currentScreen.value = AppScreen.SHIFT_CLOSING
        }
    }

    fun updateClosingQty(itemId: Long, count: Int, looseMl: Int? = null) {
        _closingItems.value = _closingItems.value.map {
            if (it.itemId == itemId) {
                it.copy(closingQty = maxOf(0, count), closingLooseMl = looseMl?.coerceAtLeast(0) ?: it.closingLooseMl)
            } else it
        }
    }

    fun updateSubmittedCashInput(value: String) {
        _submittedCashInput.value = value
    }

    fun updateSubmittedMpesaInput(value: String) { _submittedMpesaInput.value = value }
    fun updateSubmittedCardInput(value: String) { _submittedCardInput.value = value }
    fun updateSubmittedBankInput(value: String) { _submittedBankInput.value = value }
    fun updateSubmittedPayment(method: String, value: String) {
        _submittedPaymentInputs.value = _submittedPaymentInputs.value.toMutableMap().apply { put(method, value) }
    }

    fun updateClosingNotes(value: String) {
        _closingNotesInput.value = value
    }

    fun submitShiftClosing() {
        val shift = activeShift.value ?: return
        val submittedCash = _submittedPaymentInputs.value.entries
            .firstOrNull { it.key.trim().equals("cash", ignoreCase = true) }
            ?.value
            ?.toDoubleOrNull()
            ?: 0.0
        val submittedMpesa = _submittedMpesaInput.value.toDoubleOrNull() ?: 0.0
        val submittedCard = _submittedCardInput.value.toDoubleOrNull() ?: 0.0
        val submittedBank = _submittedBankInput.value.toDoubleOrNull() ?: 0.0
        val configuredPayments = _submittedPaymentInputs.value
        val submittedPaymentTotal = configuredPayments.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
        if (submittedCash == null || submittedCash < 0 || submittedMpesa < 0 || submittedCard < 0 || submittedBank < 0 || configuredPayments.values.any { (it.toDoubleOrNull() ?: -1.0) < 0 }) {
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
                    ,closingLooseMl = item.closingLooseMl
                )
            }

            val (closedShift, closings) = repository.closeShiftAndReconcile(
                shiftId = shift.id,
                submittedCash = submittedCash,
                submittedMpesa = submittedMpesa,
                submittedCard = submittedCard,
                submittedBank = submittedBank,
                submittedPayments = configuredPayments.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
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

    fun restockCounterFromSource(
        counterId: Long,
        itemId: Long,
        qty: Int,
        unitsPerPurchaseUnit: Int,
        purchaseUnitType: String,
        source: String,
        supplierName: String,
        receiptNumber: String,
        unitCost: Double,
        reason: String
    ) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                repository.restockCounterFromSource(
                    counterId, itemId, qty, unitsPerPurchaseUnit, purchaseUnitType, source, supplierName, receiptNumber,
                    unitCost, admin.name, reason
                )
                autoBackup()
                _toastMessage.emit("Counter restock recorded from ${if (source == "STORE") "Store" else "Direct Supplier"}.")
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Could not record counter restock.")
            }
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

    fun deleteCounter(counterId: Long, destinationCounterId: Long? = null) {
        viewModelScope.launch {
            val counter = repository.getCounterById(counterId)
            if (counter == null) return@launch
            if (counter.activeShiftId != null) {
                _toastMessage.emit("Cannot delete '${counter.name}' while an attendant shift is active!")
                return@launch
            }
            repository.deleteCounterAndMoveStock(counterId, destinationCounterId)
            if (_selectedCounterId.value == counterId) {
                val remaining = allCounters.value.filter { it.id != counterId }
                _selectedCounterId.value = remaining.firstOrNull()?.id
            }
            autoBackup()
            val destination = if (destinationCounterId == null) "Store" else allCounters.value.firstOrNull { it.id == destinationCounterId }?.name ?: "the selected counter"
            _toastMessage.emit("Counter '${counter.name}' deleted. Stock moved to $destination.")
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
        description: String = "",
        openingStockQuantity: Int = 0,
        openingStockCostPerUnit: Double = 0.0,
        totEnabled: Boolean = false,
        bottleVolumeMl: Int = 0,
        totSizeMl: Int = 0,
        totPrice: Double = 0.0
    ) {
        viewModelScope.launch {
            if (name.isBlank() || unitPrice <= 0) {
                _toastMessage.emit("Please enter valid item details.")
                return@launch
            }
            if (openingStockQuantity > 0 && openingStockCostPerUnit <= 0) {
                _toastMessage.emit("Please enter the cost per unit for the opening stock.")
                return@launch
            }
            repository.addItemWithOpeningStock(
                name = name,
                category = category,
                unitPrice = unitPrice,
                casePrice = casePrice,
                unitType = unitType,
                description = description,
                openingStockQuantity = openingStockQuantity,
                openingStockCostPerUnit = openingStockCostPerUnit,
                totEnabled = totEnabled,
                bottleVolumeMl = bottleVolumeMl,
                totSizeMl = totSizeMl,
                totPrice = totPrice,
                receivedByUserId = currentUser.value?.id,
                receivedByName = currentUser.value?.name ?: "Admin"
            )
            _toastMessage.emit(
                if (openingStockQuantity > 0) "Stock item added with opening stock." else "Stock item added."
            )
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
        description: String = "",
        totEnabled: Boolean = false,
        bottleVolumeMl: Int = 0,
        totSizeMl: Int = 0,
        totPrice: Double = 0.0
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
                    description = description.trim(),
                    totEnabled = totEnabled,
                    bottleVolumeMl = bottleVolumeMl,
                    totSizeMl = totSizeMl,
                    totPrice = totPrice
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

    // --- Google Play Billing Actions ---

    fun launchBillingPurchase(activity: android.app.Activity, plan: BillingPlan) {
        billingManager.launchPurchaseFlow(activity, plan)
    }

    fun restoreBillingPurchases() {
        billingManager.restorePurchases()
    }

    fun openGooglePlaySubscriptions(activity: android.app.Activity) {
        billingManager.openGooglePlaySubscriptions(activity)
    }

    fun activateSandboxBillingPlan(plan: BillingPlan) {
        billingManager.activateSandboxPlan(plan)
    }

    fun simulateTrialActive() {
        billingManager.simulateTrialActive()
    }

    fun simulateTrialExpired() {
        billingManager.simulateTrialExpired()
    }

    fun resetSubscriptionToFree() {
        billingManager.cancelOrResetSubscription()
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }
}
