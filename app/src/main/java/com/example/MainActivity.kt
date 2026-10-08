package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.billing.BillingManager
import com.example.data.db.BarStockDatabase
import com.example.data.model.UserRole
import com.example.data.repository.BarStockRepository
import com.example.ui.components.AdminBottomNavigationBar
import com.example.ui.components.LocalCurrencySymbol
import com.example.ui.components.BarAppTopBar
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.screens.ActiveShiftScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminInventoryScreen
import com.example.ui.screens.AttendantDashboardScreen
import com.example.ui.screens.AttendantDashboardTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BarProfileScreen
import com.example.ui.screens.CounterManagementScreen
import com.example.ui.screens.DisputeManagementScreen
import com.example.ui.screens.ItemManagementScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.MonthlyReportsScreen
import com.example.ui.screens.ShiftEndClosingScreen
import com.example.ui.screens.ShiftStartVerificationScreen
import com.example.ui.screens.ShiftSummaryScreen
import com.example.ui.screens.StoreInventoryScreen
import com.example.ui.screens.ReceiptsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.ProfitAndLossScreen
import com.example.ui.screens.UserManagementScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BarStockViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = BarStockDatabase.getDatabase(applicationContext)
        val repository = BarStockRepository(database.barStockDao())
        val backupService = com.example.data.repository.BackupService(database.barStockDao())
        val billingManager = BillingManager(applicationContext, lifecycleScope)
        val pinManager = com.example.data.util.PinManager(applicationContext)

        var syncJob: kotlinx.coroutines.Job? = null
        database.invalidationTracker.addObserver(object : androidx.room.InvalidationTracker.Observer(
            "bar_profile", "users", "items", "counters", "counter_stocks", "shifts", 
            "stock_verifications", "shift_closings", "disputes", 
            "stock_adjustments", "reconciliations", "app_notifications", "store_stocks", "purchase_receipts"
        ) {
            override fun onInvalidated(tables: Set<String>) {
                syncJob?.cancel()
                syncJob = this@MainActivity.lifecycleScope.launch {
                    kotlinx.coroutines.delay(1000)
                    backupService.backupData()
                }
            }
        })

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val viewModel: BarStockViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return BarStockViewModel(repository, backupService, billingManager, pinManager) as T
                        }
                    }
                )

                BarStockApp(viewModel = viewModel)
            }
        }
    }
}

private const val AUTO_LOCK_MS = 60_000L

@Composable
fun BarStockApp(viewModel: BarStockViewModel) {
    val barProfile by viewModel.barProfile.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val pinUserIds by viewModel.pinUserIds.collectAsState()
    val showChangePinDialog by viewModel.showChangePinDialog.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val attendants by viewModel.attendants.collectAsState()

    val allItems by viewModel.allItems.collectAsState()
    val allCounters by viewModel.allCounters.collectAsState()
    val allCounterStocks by viewModel.allCounterStocks.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val allShiftClosings by viewModel.allShiftClosings.collectAsState()
    val storeStock by viewModel.storeStock.collectAsState()
    val purchaseReceipts by viewModel.purchaseReceipts.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allDisputes by viewModel.allDisputes.collectAsState()
    val allInventoryLosses by viewModel.allInventoryLosses.collectAsState()
    val allReconciliations by viewModel.allReconciliations.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val activeShift by viewModel.activeShift.collectAsState()
    val selectedCounterForShift by viewModel.selectedCounterForShift.collectAsState()
    val verificationItems by viewModel.verificationItems.collectAsState()
    val closingItems by viewModel.closingItems.collectAsState()
    val submittedCashInput by viewModel.submittedCashInput.collectAsState()
    val closingNotesInput by viewModel.closingNotesInput.collectAsState()
    val submittedPaymentInputs by viewModel.submittedPaymentInputs.collectAsState()
    val lastClosedShift by viewModel.lastClosedShift.collectAsState()
    val lastShiftClosings by viewModel.lastShiftClosings.collectAsState()
    val activeShiftPendingAdjustments by viewModel.activeShiftPendingAdjustments.collectAsState()

    val selectedCounterId by viewModel.selectedCounterId.collectAsState()
    val selectedCounterStocks by viewModel.selectedCounterStocks.collectAsState()
    val selectedAttendantFilterId by viewModel.selectedAttendantFilterId.collectAsState()
    val billingStatus by viewModel.billingStatus.collectAsState()
    val availableBillingPlans by viewModel.availableBillingPlans.collectAsState()
    val billingConnectionState by viewModel.billingConnectionState.collectAsState()
    val monthlyReports by viewModel.monthlyReports.collectAsState()
    val monthlyReportsLoading by viewModel.monthlyReportsLoading.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var attendantDashboardTab by remember { mutableStateOf(AttendantDashboardTab.OVERVIEW) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val unreadNotificationsCount = notifications.count { !it.isRead }
    val currencySymbol = barProfile.currencySymbol.ifBlank { "$" }

    CompositionLocalProvider(LocalCurrencySymbol provides currencySymbol) {
        val screenTitle = when (currentScreen) {
            AppScreen.AUTH -> "Sign In"
            AppScreen.ADMIN_DASHBOARD -> barProfile.barName.ifBlank { "Bar Dashboard" }
            AppScreen.ATTENDANT_DASHBOARD -> "Attendant Station"
            AppScreen.BAR_PROFILE -> "Bar Profile & Branding"
            AppScreen.SHIFT_VERIFICATION -> "Verify Opening Stock"
            AppScreen.ACTIVE_SHIFT -> "Active Shift"
            AppScreen.SHIFT_CLOSING -> "Shift Closing"
            AppScreen.SHIFT_SUMMARY -> "Reconciliation Receipt"
            AppScreen.DISPUTES -> "Dispute Center"
            AppScreen.LEDGER -> "Receipts"
            AppScreen.STORE_INVENTORY -> "Store Inventory"
            AppScreen.COUNTERS_MANAGEMENT -> "Selling Counters"
            AppScreen.ITEMS_MANAGEMENT -> "Stock Prices"
            AppScreen.USERS_MANAGEMENT -> if (currentUser?.role == UserRole.OWNER) "User Hierarchy" else "Attendant Logins"
            AppScreen.EXPENSES_MANAGEMENT -> "Expense Management"
            AppScreen.PROFIT_LOSS_REPORT -> "P&L Statement"
            AppScreen.MONTHLY_REPORTS -> "Monthly Reports"
            AppScreen.NOTIFICATIONS -> "Notification Center"
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentScreen != AppScreen.AUTH && currentScreen != AppScreen.ADMIN_DASHBOARD && currentScreen != AppScreen.ATTENDANT_DASHBOARD) {
                BarAppTopBar(
                    title = screenTitle,
                    barProfile = barProfile,
                    currentUser = currentUser,
                    allUsers = allUsers,
                    unreadCount = unreadNotificationsCount,
                    isSyncing = isSyncing,
                    currentScreen = currentScreen,
                    onBackClick = {
                        val user = currentUser
                        if (user != null) {
                            if (currentScreen == AppScreen.SHIFT_VERIFICATION || currentScreen == AppScreen.SHIFT_SUMMARY) {
                                viewModel.navigateTo(if (user.role == UserRole.OWNER || user.role == UserRole.MANAGER) AppScreen.ADMIN_DASHBOARD else AppScreen.ATTENDANT_DASHBOARD)
                            } else if (currentScreen == AppScreen.ACTIVE_SHIFT) {
                                viewModel.navigateTo(AppScreen.ATTENDANT_DASHBOARD)
                            } else if (currentScreen == AppScreen.SHIFT_CLOSING) {
                                viewModel.navigateTo(AppScreen.ACTIVE_SHIFT)
                            } else {
                                viewModel.navigateTo(if (user.role == UserRole.OWNER || user.role == UserRole.MANAGER) AppScreen.ADMIN_DASHBOARD else AppScreen.ATTENDANT_DASHBOARD)
                            }
                        }
                    },
                    onNotificationClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) },
                    onBarProfileClick = { viewModel.navigateTo(AppScreen.BAR_PROFILE) },
                    onChangePasswordClick = { showPasswordDialog = true },
                    onLogout = { viewModel.logout() }
                )
            }
        },
        bottomBar = {
            if ((currentUser?.role == UserRole.OWNER || currentUser?.role == UserRole.MANAGER) && currentScreen != AppScreen.AUTH) {
                val openDisputesCount = allDisputes.count { it.status == com.example.data.model.DisputeStatus.OPEN }
                val pendingUsersCount = pendingUsers.size
                AdminBottomNavigationBar(
                    currentScreen = currentScreen,
                    openDisputesCount = openDisputesCount,
                    pendingUsersCount = pendingUsersCount,
                    isOwner = currentUser?.role == UserRole.OWNER,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            } else if (currentUser?.role == UserRole.ATTENDANT && currentScreen == AppScreen.ATTENDANT_DASHBOARD) {
                NavigationBar(
                    containerColor = com.example.ui.theme.DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = attendantDashboardTab == AttendantDashboardTab.OVERVIEW,
                        onClick = { attendantDashboardTab = AttendantDashboardTab.OVERVIEW },
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Dashboard, contentDescription = "Overview") },
                        label = { androidx.compose.material3.Text("Overview") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.ui.theme.AmberPrimary,
                            selectedTextColor = com.example.ui.theme.AmberPrimary,
                            indicatorColor = com.example.ui.theme.AmberPrimary.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier.testTag("attendant_tab_overview")
                    )
                    NavigationBarItem(
                        selected = attendantDashboardTab == AttendantDashboardTab.HISTORY,
                        onClick = { attendantDashboardTab = AttendantDashboardTab.HISTORY },
                        icon = { androidx.compose.material3.Icon(Icons.Outlined.History, contentDescription = "History") },
                        label = { androidx.compose.material3.Text("History") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.ui.theme.AmberPrimary,
                            selectedTextColor = com.example.ui.theme.AmberPrimary,
                            indicatorColor = com.example.ui.theme.AmberPrimary.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier.testTag("attendant_tab_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                AppScreen.AUTH -> {
                    AuthScreen(
                        barProfile = barProfile,
                        users = allUsers,
                        onLoginUser = { user -> viewModel.loginUser(user) },
                        pinUserIds = pinUserIds,
                        onLoginWithPin = { userId, pin, onResult -> viewModel.loginWithPin(userId, pin, onResult) },
                        onLoginCredentials = { identifier, password ->
                            viewModel.loginWithCredentials(identifier, password)
                        },
                        onRegisterBarAndAdmin = { barName, location, adminName, phone, email, password ->
                            viewModel.registerBarAndAdmin(barName, location, adminName, phone, email, password)
                        },
                        onSelfResetPassword = { phone, newPassword ->
                            viewModel.selfResetPassword(phone, newPassword)
                        },
                        onSendPasswordResetEmail = { email, onSuccess, onError ->
                            viewModel.sendPasswordResetEmail(email, onSuccess, onError)
                        }
                    )
                }

                        AppScreen.ADMIN_DASHBOARD -> {
                    AdminDashboardScreen(
                        barProfile = barProfile,
                        counters = allCounters,
                        items = allItems,
                        shifts = allShifts,
                        currentUserRole = currentUser?.role ?: UserRole.OWNER,
                        storeStock = storeStock,
                        counterStockQuantities = allCounterStocks.groupBy { it.counterId }.mapValues { (_, stocks) -> stocks.sumOf { it.currentQuantity } },
                        shiftClosings = allShiftClosings,
                        disputes = allDisputes,
                        pendingUsers = pendingUsers,
                        reconciliations = allReconciliations,
                        unreadNotificationsCount = unreadNotificationsCount,
                        isSyncing = isSyncing,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onViewShiftReceipt = { shift -> viewModel.viewShiftReceipt(shift) },
                        onAddStockAdjustment = { counterId, itemId, qty, reason ->
                            viewModel.addStockAdjustment(counterId, itemId, qty, reason)
                        },
                        onRestockCounterFromSource = { counterId, itemId, qty, unitsPerPurchaseUnit, purchaseUnitType, source, supplier, receiptNumber, unitCost, reason ->
                            viewModel.restockCounterFromSource(counterId, itemId, qty, unitsPerPurchaseUnit, purchaseUnitType, source, supplier, receiptNumber, unitCost, reason)
                        },
                        onSelectCounterForManagement = { counterId ->
                            viewModel.setSelectedCounterForManagement(counterId)
                        },
                        onUpdateCounter = { counterId, name, location ->
                            viewModel.updateCounter(counterId, name, location)
                        },
                        onDeleteCounter = { counterId, destinationCounterId ->
                            viewModel.deleteCounter(counterId, destinationCounterId)
                        }
                    )
                }

                AppScreen.BAR_PROFILE -> {
                    BarProfileScreen(
                        barProfile = barProfile,
                        currentUser = currentUser,
                        billingStatus = billingStatus,
                        availablePlans = availableBillingPlans,
                        billingConnectionState = billingConnectionState,
                        onLaunchPurchase = { activity, plan -> viewModel.launchBillingPurchase(activity, plan) },
                        onRestorePurchases = { viewModel.restoreBillingPurchases() },
                        onOpenSubscriptions = { activity -> viewModel.openGooglePlaySubscriptions(activity) },
                        onSaveProfile = { name, location, icon, photo, phone, currency, paymentMethods, manager, hours ->
                            viewModel.updateBarProfile(name, location, icon, photo, phone, currency, paymentMethods, manager, hours)
                        },
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onChangePasswordClick = { showPasswordDialog = true },
                        hasPin = currentUser?.id?.let { it in pinUserIds } == true,
                        onSetPinClick = { viewModel.openPinSetup() },
                        onRemovePinClick = { viewModel.removePinForCurrentUser() }
                    )
                }

                AppScreen.ATTENDANT_DASHBOARD -> {
                    currentUser?.let { user ->
                        AttendantDashboardScreen(
                            barProfile = barProfile,
                            currentUser = user,
                            activeShift = activeShift,
                            counters = allCounters,
                            shifts = allShifts,
                            disputes = allDisputes,
                            allItems = allItems,
                            reconciliations = allReconciliations,
                            unreadNotificationsCount = notifications.filter { !it.isRead }.size,
                            onSelectCounterForShift = { counter ->
                                viewModel.selectCounterForShiftStart(counter)
                            },
                            onResumeActiveShift = {
                                viewModel.navigateTo(AppScreen.ACTIVE_SHIFT)
                            },
                            onChangePassword = { oldPass, newPass ->
                                viewModel.changePassword(user.id, oldPass, newPass)
                            },
                            selectedTab = attendantDashboardTab,
                            onViewShiftReceipt = { shift ->
                                viewModel.viewShiftReceipt(shift)
                            },
                            onNavigate = { screen -> viewModel.navigateTo(screen) }
                        )
                    }
                }

                AppScreen.SHIFT_VERIFICATION -> {
                    ShiftStartVerificationScreen(
                        counter = selectedCounterForShift,
                        items = verificationItems,
                        onUpdateApproval = { itemId, approved ->
                            viewModel.updateVerificationItemApproval(itemId, approved)
                        },
                        onUpdateCountedQty = { itemId, count ->
                            viewModel.updateVerificationItemCountedQty(itemId, count)
                        },
                        onUpdateCountedLooseMl = { itemId, looseMl ->
                            viewModel.updateVerificationItemCountedLooseMl(itemId, looseMl)
                        },
                        onConfirmAndStartShift = {
                            attendantDashboardTab = AttendantDashboardTab.OVERVIEW
                            viewModel.confirmOpeningStockAndStartShift()
                        },
                        onCancel = {
                            viewModel.navigateTo(AppScreen.ATTENDANT_DASHBOARD)
                        }
                    )
                }

                AppScreen.ACTIVE_SHIFT -> {
                    ActiveShiftScreen(
                        shift = activeShift,
                        stockItems = selectedCounterStocks,
                        pendingAdjustments = activeShiftPendingAdjustments,
                        onConfirmAdjustment = { adjustment ->
                            viewModel.confirmAdjustment(adjustment)
                        },
                        onDisputeAdjustment = { adjustment ->
                            viewModel.disputeAdjustment(adjustment)
                        },
                        onEndShiftClick = {
                            viewModel.prepareShiftClosing()
                        }
                    )
                }

                AppScreen.SHIFT_CLOSING -> {
                    ShiftEndClosingScreen(
                        closingItems = closingItems,
                        paymentMethods = barProfile.paymentMethods.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        submittedPaymentInputs = submittedPaymentInputs,
                        submittedCashInput = submittedCashInput,
                        submittedMpesaInput = viewModel.submittedMpesaInput.collectAsState().value,
                        submittedCardInput = viewModel.submittedCardInput.collectAsState().value,
                        submittedBankInput = viewModel.submittedBankInput.collectAsState().value,
                        closingNotesInput = closingNotesInput,
                        onUpdateClosingQty = { itemId, count, looseMl ->
                            viewModel.updateClosingQty(itemId, count, looseMl)
                        },
                        onUpdatePayment = { method, value -> viewModel.updateSubmittedPayment(method, value) },
                        onUpdateSubmittedCash = { cash ->
                            viewModel.updateSubmittedCashInput(cash)
                        },
                        onUpdateSubmittedMpesa = viewModel::updateSubmittedMpesaInput,
                        onUpdateSubmittedCard = viewModel::updateSubmittedCardInput,
                        onUpdateSubmittedBank = viewModel::updateSubmittedBankInput,
                        onUpdateNotes = { notes ->
                            viewModel.updateClosingNotes(notes)
                        },
                        onSubmitClosing = {
                            viewModel.submitShiftClosing()
                        }
                    )
                }

                AppScreen.SHIFT_SUMMARY -> {
                    ShiftSummaryScreen(
                        shift = lastClosedShift,
                        closings = lastShiftClosings,
                        onReturnToDashboard = {
                            val user = currentUser
                            if (user?.role == UserRole.OWNER || user?.role == UserRole.MANAGER) {
                                viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD)
                            } else {
                                viewModel.navigateTo(AppScreen.ATTENDANT_DASHBOARD)
                            }
                        }
                    )
                }

                AppScreen.DISPUTES -> {
                    currentUser?.let { user ->
                        DisputeManagementScreen(
                            currentUser = user,
                            disputes = allDisputes,
                            items = allItems,
                            onResolveDispute = { disputeId, notes, stockQty, looseMl ->
                                viewModel.resolveDispute(disputeId, notes, stockQty, looseMl)
                            }
                        )
                    }
                }

                AppScreen.LEDGER -> {
                    currentUser?.let { user ->
                        ReceiptsScreen(
                            currentUser = user,
                            reconciliations = allReconciliations,
                            attendants = attendants,
                            selectedAttendantId = selectedAttendantFilterId,
                            purchaseReceipts = purchaseReceipts,
                            counters = allCounters,
                            items = allItems,
                            expenses = allExpenses,
                            onViewShiftReceipt = { shiftId -> viewModel.viewShiftReceiptById(shiftId) },
                            onSelectAttendantFilter = { id -> viewModel.setAttendantLedgerFilter(id) },
                            onAddExpense = { category, description, amount, paymentMethod, referenceNumber, notes ->
                                viewModel.addExpense(category, description, amount, paymentMethod, referenceNumber, notes)
                            },
                            onDeleteExpense = { id ->
                                viewModel.deleteExpense(id)
                            },
                            onReceivePurchase = { itemId, purchaseQuantity, unitsPerPurchaseUnit, purchaseUnitType, supplierName, receiptNumber, unitCost, notes ->
                                viewModel.receivePurchaseToStore(itemId, purchaseQuantity, unitsPerPurchaseUnit, purchaseUnitType, supplierName, receiptNumber, unitCost, notes)
                            },
                            onDeletePurchaseReceipt = { id ->
                                viewModel.deletePurchaseReceipt(id)
                            }
                        )
                    }
                }

                AppScreen.STORE_INVENTORY -> {
                    AdminInventoryScreen(
                        counters = allCounters,
                        onSelectCounter = { viewModel.setSelectedCounterForManagement(it) },
                        countersContent = {
                            CounterManagementScreen(
                                currentUser = currentUser,
                                counters = allCounters,
                                allItems = allItems,
                                selectedCounterId = selectedCounterId,
                                counterStocks = selectedCounterStocks,
                                storeStock = storeStock,
                                onSelectCounter = { viewModel.setSelectedCounterForManagement(it) },
                                onCreateCounter = { name, location -> viewModel.createCounter(name, location) },
                                onUpdateCounter = { id, name, location -> viewModel.updateCounter(id, name, location) },
                                onDeleteCounter = { viewModel.deleteCounter(it) },
                                onAssignItemToCounter = { id, itemId, qty -> viewModel.assignItemToCounter(id, itemId, qty) },
                                onRestockClick = { viewModel.setSelectedCounterForManagement(it.id) },
                                onRestockCounterFromSource = { id, itemId, qty, unitsPerPurchaseUnit, purchaseUnitType, source, supplier, receipt, cost, reason -> viewModel.restockCounterFromSource(id, itemId, qty, unitsPerPurchaseUnit, purchaseUnitType, source, supplier, receipt, cost, reason) },
                                showCounterSelector = false
                            )
                        },
                        storeContent = {
                            StoreInventoryScreen(
                                items = allItems,
                                storeStock = storeStock,
                                onReceivePurchase = { itemId, purchaseQuantity, unitsPerPurchaseUnit, purchaseUnitType, supplierName, receiptNumber, unitCost, notes ->
                                    viewModel.receivePurchaseToStore(itemId, purchaseQuantity, unitsPerPurchaseUnit, purchaseUnitType, supplierName, receiptNumber, unitCost, notes)
                                }
                            )
                        }
                    )
                }

                AppScreen.COUNTERS_MANAGEMENT -> {
                    CounterManagementScreen(
                        currentUser = currentUser,
                        counters = allCounters,
                        allItems = allItems,
                        selectedCounterId = selectedCounterId,
                        counterStocks = selectedCounterStocks,
                        storeStock = storeStock,
                        onSelectCounter = { id ->
                            viewModel.setSelectedCounterForManagement(id)
                        },
                        onCreateCounter = { name, location ->
                            viewModel.createCounter(name, location)
                        },
                        onUpdateCounter = { counterId, name, location ->
                            viewModel.updateCounter(counterId, name, location)
                        },
                        onDeleteCounter = { counterId ->
                            viewModel.deleteCounter(counterId)
                        },
                        onAssignItemToCounter = { counterId, itemId, qty ->
                            viewModel.assignItemToCounter(counterId, itemId, qty)
                        },
                        onRestockClick = { counter ->
                            viewModel.setSelectedCounterForManagement(counter.id)
                        },
                        onRestockCounterFromSource = { counterId, itemId, qty, unitsPerPurchaseUnit, purchaseUnitType, source, supplier, receiptNumber, unitCost, reason ->
                            viewModel.restockCounterFromSource(counterId, itemId, qty, unitsPerPurchaseUnit, purchaseUnitType, source, supplier, receiptNumber, unitCost, reason)
                        }
                    )
                }

                AppScreen.ITEMS_MANAGEMENT -> {
                    ItemManagementScreen(
                        currentUser = currentUser,
                        items = allItems,
                        onCreateItem = { name, category, unitPrice, casePrice, unitType, desc, openingQty, openingCost, totEnabled, bottleVolumeMl, totSizeMl, totPrice ->
                            viewModel.createStockItem(name, category, unitPrice, casePrice, unitType, desc, openingQty, openingCost, totEnabled, bottleVolumeMl, totSizeMl, totPrice)
                        },
                        onUpdateItem = { itemId, name, category, unitPrice, casePrice, unitType, desc, totEnabled, bottleVolumeMl, totSizeMl, totPrice ->
                            viewModel.updateStockItem(itemId, name, category, unitPrice, casePrice, unitType, desc, totEnabled, bottleVolumeMl, totSizeMl, totPrice)
                        },
                        onDeleteItem = { itemId ->
                            viewModel.deleteStockItem(itemId)
                        },
                        onSyncData = {
                            viewModel.syncData()
                        }
                    )
                }

                AppScreen.USERS_MANAGEMENT -> {
                    UserManagementScreen(
                        currentUserRole = currentUser?.role ?: UserRole.ATTENDANT,
                        allUsers = allUsers,
                        onApproveUser = { userId ->
                            viewModel.approveUser(userId)
                        },
                        onRevokeUser = { userId ->
                            viewModel.revokeUser(userId)
                        },
                        onRestoreUser = { userId ->
                            viewModel.approveUser(userId)
                        },
                        onCreateAttendant = { name, email, phone, password ->
                            viewModel.createAttendantByAdmin(name, email, phone, password)
                        },
                        onCreateManager = { name, email, phone, password ->
                            viewModel.createManagerByOwner(name, email, phone, password)
                        },
                        onDeleteUser = { userId ->
                            viewModel.deleteUser(userId)
                        }
                    )
                }

                AppScreen.EXPENSES_MANAGEMENT -> {
                    ExpensesScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) }
                    )
                }

                AppScreen.PROFIT_LOSS_REPORT -> {
                    if (currentUser?.role == UserRole.OWNER) {
                        ProfitAndLossScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) }
                        )
                    } else {
                        // Managers should not see Profit & Loss — bounce back to the dashboard.
                        LaunchedEffect(Unit) {
                            viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD)
                        }
                    }
                }

                AppScreen.MONTHLY_REPORTS -> {
                    if (currentUser?.role == UserRole.OWNER) {
                        MonthlyReportsScreen(
                            reports = monthlyReports,
                            loading = monthlyReportsLoading,
                            onLoad = { viewModel.loadMonthlyReports() },
                            onBack = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) }
                        )
                    } else {
                        LaunchedEffect(Unit) { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) }
                    }
                }

                AppScreen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        notifications = notifications,
                        losses = allInventoryLosses,
                        onMarkAsRead = { id ->
                            viewModel.markNotificationAsRead(id)
                        },
                        onMarkAllAsRead = {
                            viewModel.markAllNotificationsAsRead()
                        },
                        onAcceptShortage = { lossId -> viewModel.acceptShortage(lossId) },
                        onRejectShortage = { lossId, reason -> viewModel.rejectShortage(lossId, reason) },
                        onPostShortage = { lossId -> viewModel.postRejectedShortage(lossId) },
                        onWaiveShortage = { lossId -> viewModel.waiveRejectedShortage(lossId) }
                    )
                }
            }
        }
    }
    }

    // Drop PINs for accounts that were deleted.
    LaunchedEffect(allUsers) {
        viewModel.pruneStalePins(allUsers.map { it.id }.toSet())
    }

    if (currentUser != null && showChangePinDialog) {
        com.example.ui.components.SetPinDialog(
            userName = currentUser!!.name,
            isChange = currentUser?.id?.let { it in pinUserIds } == true,
            onSetPin = { pin -> viewModel.setPinForCurrentUser(pin) },
            onDismiss = { viewModel.dismissPinSetup() }
        )
    }

    // Auto-lock: ask for the PIN again after the app has been in the background for a minute.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        var leftAt = 0L
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> leftAt = android.os.SystemClock.elapsedRealtime()
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    if (leftAt != 0L && android.os.SystemClock.elapsedRealtime() - leftAt >= AUTO_LOCK_MS) {
                        viewModel.lockApp()
                    }
                    leftAt = 0L
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showPasswordDialog && currentUser != null) {
        ChangePasswordDialog(
            userName = currentUser!!.name,
            onDismiss = { showPasswordDialog = false },
            onConfirmChange = { oldPass, newPass ->
                viewModel.changePassword(currentUser!!.id, oldPass, newPass)
                showPasswordDialog = false
            }
        )
    }
}
