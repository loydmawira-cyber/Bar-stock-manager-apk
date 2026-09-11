package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.BarStockDatabase
import com.example.data.model.UserRole
import com.example.data.repository.BarStockRepository
import com.example.ui.components.AdminBottomNavigationBar
import com.example.ui.components.LocalCurrencySymbol
import com.example.ui.components.BarAppTopBar
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.screens.ActiveShiftScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AttendantDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BarProfileScreen
import com.example.ui.screens.CounterManagementScreen
import com.example.ui.screens.DisputeManagementScreen
import com.example.ui.screens.ItemManagementScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ShiftEndClosingScreen
import com.example.ui.screens.ShiftStartVerificationScreen
import com.example.ui.screens.ShiftSummaryScreen
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

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val viewModel: BarStockViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return BarStockViewModel(repository) as T
                        }
                    }
                )

                BarStockApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BarStockApp(viewModel: BarStockViewModel) {
    val barProfile by viewModel.barProfile.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val attendants by viewModel.attendants.collectAsState()

    val allItems by viewModel.allItems.collectAsState()
    val allCounters by viewModel.allCounters.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val allDisputes by viewModel.allDisputes.collectAsState()
    val allReconciliations by viewModel.allReconciliations.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val activeShift by viewModel.activeShift.collectAsState()
    val selectedCounterForShift by viewModel.selectedCounterForShift.collectAsState()
    val verificationItems by viewModel.verificationItems.collectAsState()
    val closingItems by viewModel.closingItems.collectAsState()
    val submittedCashInput by viewModel.submittedCashInput.collectAsState()
    val closingNotesInput by viewModel.closingNotesInput.collectAsState()
    val lastClosedShift by viewModel.lastClosedShift.collectAsState()
    val lastShiftClosings by viewModel.lastShiftClosings.collectAsState()
    val activeShiftPendingAdjustments by viewModel.activeShiftPendingAdjustments.collectAsState()

    val selectedCounterId by viewModel.selectedCounterId.collectAsState()
    val selectedCounterStocks by viewModel.selectedCounterStocks.collectAsState()
    val selectedAttendantFilterId by viewModel.selectedAttendantFilterId.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }

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
        AppScreen.LEDGER -> "Losses & Extras Ledger"
        AppScreen.COUNTERS_MANAGEMENT -> "Selling Counters"
        AppScreen.ITEMS_MANAGEMENT -> "Stock Prices"
        AppScreen.USERS_MANAGEMENT -> "Attendant Logins"
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
                    currentScreen = currentScreen,
                    onBackClick = {
                        val user = currentUser
                        if (user != null) {
                            if (currentScreen == AppScreen.SHIFT_VERIFICATION || currentScreen == AppScreen.SHIFT_SUMMARY) {
                                viewModel.navigateTo(if (user.role == UserRole.ADMIN) AppScreen.ADMIN_DASHBOARD else AppScreen.ATTENDANT_DASHBOARD)
                            } else if (currentScreen == AppScreen.ACTIVE_SHIFT) {
                                viewModel.navigateTo(AppScreen.ATTENDANT_DASHBOARD)
                            } else if (currentScreen == AppScreen.SHIFT_CLOSING) {
                                viewModel.navigateTo(AppScreen.ACTIVE_SHIFT)
                            } else {
                                viewModel.navigateTo(if (user.role == UserRole.ADMIN) AppScreen.ADMIN_DASHBOARD else AppScreen.ATTENDANT_DASHBOARD)
                            }
                        }
                    },
                    onNotificationClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) },
                    onBarProfileClick = { viewModel.navigateTo(AppScreen.BAR_PROFILE) },
                    onChangePasswordClick = { showPasswordDialog = true },
                    onSwitchUser = { user -> viewModel.loginUser(user) },
                    onLogout = { viewModel.logout() }
                )
            }
        },
        bottomBar = {
            if (currentUser?.role == UserRole.ADMIN && currentScreen != AppScreen.AUTH) {
                val openDisputesCount = allDisputes.count { it.status == com.example.data.model.DisputeStatus.OPEN }
                val pendingUsersCount = pendingUsers.size
                AdminBottomNavigationBar(
                    currentScreen = currentScreen,
                    openDisputesCount = openDisputesCount,
                    pendingUsersCount = pendingUsersCount,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
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
                        onLoginCredentials = { identifier, password ->
                            viewModel.loginWithCredentials(identifier, password)
                        },
                        onRegisterBarAndAdmin = { barName, location, adminName, phone, email, password ->
                            viewModel.registerBarAndAdmin(barName, location, adminName, phone, email, password)
                        },
                        onSelfResetPassword = { phone, newPassword ->
                            viewModel.selfResetPassword(phone, newPassword)
                        }
                    )
                }

                AppScreen.ADMIN_DASHBOARD -> {
                    AdminDashboardScreen(
                        barProfile = barProfile,
                        counters = allCounters,
                        items = allItems,
                        shifts = allShifts,
                        disputes = allDisputes,
                        pendingUsers = pendingUsers,
                        reconciliations = allReconciliations,
                        unreadNotificationsCount = unreadNotificationsCount,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onAddStockAdjustment = { counterId, itemId, qty, reason ->
                            viewModel.addStockAdjustment(counterId, itemId, qty, reason)
                        },
                        onSelectCounterForManagement = { counterId ->
                            viewModel.setSelectedCounterForManagement(counterId)
                        }
                    )
                }

                AppScreen.BAR_PROFILE -> {
                    BarProfileScreen(
                        barProfile = barProfile,
                        currentUser = currentUser,
                        onSaveProfile = { name, location, icon, photo, phone, currency, manager, hours ->
                            viewModel.updateBarProfile(name, location, icon, photo, phone, currency, manager, hours)
                        },
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onChangePasswordClick = { showPasswordDialog = true }
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
                        onConfirmAndStartShift = {
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
                        submittedCashInput = submittedCashInput,
                        closingNotesInput = closingNotesInput,
                        onUpdateClosingQty = { itemId, count ->
                            viewModel.updateClosingQty(itemId, count)
                        },
                        onUpdateSubmittedCash = { cash ->
                            viewModel.updateSubmittedCashInput(cash)
                        },
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
                            if (user?.role == UserRole.ADMIN) {
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
                            onResolveDispute = { disputeId, notes, stockQty ->
                                viewModel.resolveDispute(disputeId, notes, stockQty)
                            }
                        )
                    }
                }

                AppScreen.LEDGER -> {
                    currentUser?.let { user ->
                        LedgerScreen(
                            currentUser = user,
                            reconciliations = allReconciliations,
                            attendants = attendants,
                            selectedAttendantId = selectedAttendantFilterId,
                            onSelectAttendantFilter = { id ->
                                viewModel.setAttendantLedgerFilter(id)
                            }
                        )
                    }
                }

                AppScreen.COUNTERS_MANAGEMENT -> {
                    CounterManagementScreen(
                        currentUser = currentUser,
                        counters = allCounters,
                        allItems = allItems,
                        selectedCounterId = selectedCounterId,
                        counterStocks = selectedCounterStocks,
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
                        }
                    )
                }

                AppScreen.ITEMS_MANAGEMENT -> {
                    ItemManagementScreen(
                        currentUser = currentUser,
                        items = allItems,
                        onCreateItem = { name, category, unitPrice, casePrice, unitType, desc ->
                            viewModel.createStockItem(name, category, unitPrice, casePrice, unitType, desc)
                        },
                        onUpdateItem = { itemId, name, category, unitPrice, casePrice, unitType, desc ->
                            viewModel.updateStockItem(itemId, name, category, unitPrice, casePrice, unitType, desc)
                        },
                        onDeleteItem = { itemId ->
                            viewModel.deleteStockItem(itemId)
                        }
                    )
                }

                AppScreen.USERS_MANAGEMENT -> {
                    UserManagementScreen(
                        allUsers = allUsers,
                        onApproveUser = { userId ->
                            viewModel.approveUser(userId)
                        },
                        onRevokeUser = { userId ->
                            viewModel.revokeUser(userId)
                        },
                        onCreateAttendant = { name, email, phone, password ->
                            viewModel.createAttendantByAdmin(name, email, phone, password)
                        },
                        onDeleteUser = { userId ->
                            viewModel.deleteUser(userId)
                        }
                    )
                }

                AppScreen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        notifications = notifications,
                        onMarkAsRead = { id ->
                            viewModel.markNotificationAsRead(id)
                        },
                        onMarkAllAsRead = {
                            viewModel.markAllNotificationsAsRead()
                        }
                    )
                }
            }
        }
    }
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
