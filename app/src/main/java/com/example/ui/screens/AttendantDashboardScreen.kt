package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BarProfile
import com.example.data.model.Counter
import com.example.data.model.Dispute
import com.example.data.model.Reconciliation
import com.example.data.model.Shift
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.ui.components.BarLogoIcon
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ReconciliationBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SkyBlue
import com.example.ui.viewmodel.AppScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

enum class AttendantDashboardTab { OVERVIEW, HISTORY }
private enum class HistoryPeriod { TODAY, MONTH, YEAR }

private fun formatShiftElapsedTime(startTime: Long, currentTime: Long): String {
    val elapsedMinutes = maxOf(0L, (currentTime - startTime) / 60_000L)
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return "Shift time: ${hours}h ${minutes.toString().padStart(2, '0')}m"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendantDashboardScreen(
    barProfile: BarProfile,
    currentUser: User,
    activeShift: Shift?,
    counters: List<Counter>,
    shifts: List<Shift>,
    disputes: List<Dispute>,
    reconciliations: List<Reconciliation>,
    unreadNotificationsCount: Int = 0,
    onSelectCounterForShift: (Counter) -> Unit,
    onResumeActiveShift: () -> Unit,
    onViewShiftReceipt: (Shift) -> Unit = {},
    onChangePassword: (oldPass: String, newPass: String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    selectedTab: AttendantDashboardTab = AttendantDashboardTab.OVERVIEW
) {
    val myShifts = shifts.filter { it.attendantId == currentUser.id }
    val myReconciliations = reconciliations.filter { it.attendantId == currentUser.id }
    val myDisputes = disputes.filter { it.raisedByAttendantId == currentUser.id || it.involvesPreviousAttendantId == currentUser.id }

    val myTotalLosses = myReconciliations.filter { it.variance < 0 }.sumOf { Math.abs(it.variance) }
    val myTotalExtras = myReconciliations.filter { it.variance > 0 }.sumOf { it.variance }

    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.TODAY) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var periodMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activeShift?.id) {
        while (activeShift != null) {
            currentTime = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    val now = Calendar.getInstance()
    val filteredShifts = myShifts.filter { shift ->
        val date = Calendar.getInstance().apply { timeInMillis = shift.endTime ?: shift.startTime }
        when (selectedPeriod) {
            HistoryPeriod.TODAY -> date.get(Calendar.YEAR) == now.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            HistoryPeriod.MONTH -> date.get(Calendar.YEAR) == selectedYear && date.get(Calendar.MONTH) == selectedMonth
            HistoryPeriod.YEAR -> date.get(Calendar.YEAR) == selectedYear
        }
    }
    val filteredReconciliations = myReconciliations.filter { rec ->
        val date = Calendar.getInstance().apply { timeInMillis = rec.date }
        when (selectedPeriod) {
            HistoryPeriod.TODAY -> date.get(Calendar.YEAR) == now.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            HistoryPeriod.MONTH -> date.get(Calendar.YEAR) == selectedYear && date.get(Calendar.MONTH) == selectedMonth
            HistoryPeriod.YEAR -> date.get(Calendar.YEAR) == selectedYear
        }
    }
    val historySales = filteredShifts.sumOf { it.totalSubmittedCash }
    val historyVariance = filteredReconciliations.sumOf { it.variance }
    val monthLabels = (0..11).map { month -> SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, month) }.time) }
    val availableYears = ((now.get(Calendar.YEAR) - 4)..now.get(Calendar.YEAR)).toList().reversed()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Establishment & Attendant Profile Card
        stickyHeader {
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AmberPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("attendant_bar_profile_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!barProfile.customPhotoUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        ) {
                            AsyncImage(
                                model = barProfile.customPhotoUri,
                                contentDescription = barProfile.barName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.15f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    border = BorderStroke(2.dp, AmberPrimary),
                                    color = Color.Black.copy(alpha = 0.5f)
                                ) {
                                    BarLogoIcon(
                                        iconType = barProfile.iconType,
                                        size = 44.dp,
                                        iconSize = 24.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = barProfile.barName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("attendant_bar_name_hero_title")
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = barProfile.location.ifBlank { "Main Branch" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Horizontal Banner Header Strip stretching left to right
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            AmberPrimary.copy(alpha = 0.20f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            AmberPrimary.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, AmberPrimary),
                                    color = Color.Black.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(end = 10.dp)
                                ) {
                                    BarLogoIcon(
                                        iconType = barProfile.iconType,
                                        size = 44.dp,
                                        iconSize = 24.dp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = barProfile.barName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("attendant_bar_name_hero_title")
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = barProfile.location.ifBlank { "Main Branch" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Bottom Action Row: Stock Prices button on Left, Notifications & Settings on Right below banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onNavigate(AppScreen.ITEMS_MANAGEMENT) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("banner_view_prices_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalBar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stock Prices", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onNavigate(AppScreen.NOTIFICATIONS) },
                                modifier = Modifier.testTag("attendant_banner_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotificationsCount > 0) {
                                            Badge(
                                                containerColor = AmberPrimary,
                                                contentColor = Color.Black
                                            ) {
                                                Text(
                                                    text = "$unreadNotificationsCount",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onNavigate(AppScreen.BAR_PROFILE) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, AmberPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("attendant_banner_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == AttendantDashboardTab.OVERVIEW) {
        // Active Shift Card (if already in progress)
        if (activeShift != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2A4A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SkyBlue, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(
                                text = "ACTIVE SHIFT IN PROGRESS",
                                containerColor = Color(0xFF0C4A6E),
                                contentColor = SkyBlue
                            )
                            Text(
                                text = "Shift #${activeShift.id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SkyBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = activeShift.counterName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Opened: ${formatDateTime(activeShift.startTime)}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onResumeActiveShift,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resume_active_shift_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Active Counter Point of Sale",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Accountability & Losses Stat Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "My Shifts Completed",
                    value = "${myShifts.size}",
                    icon = Icons.Outlined.History,
                    iconColor = AmberPrimary,
                    subtitle = "Historical shifts",
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "Accountability Ledger",
                    value = formatCurrency(myTotalLosses),
                    icon = Icons.Filled.AttachMoney,
                    iconColor = if (myTotalLosses > 0) CrimsonRed else EmeraldGreen,
                    subtitle = "Extras: ${formatCurrency(myTotalExtras)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Available Counters to Start Shift
        item {
            Text(
                text = "AVAILABLE COUNTERS TO START SHIFT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (currentUser.status != UserStatus.APPROVED) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentUser.status == UserStatus.PENDING) "Account Pending Admin Approval" else "Account Access Revoked",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentUser.status == UserStatus.PENDING)
                                "The bar manager must approve your account before you can open shifts."
                            else "Your account has been revoked by the bar administrator. Please contact your manager.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(counters) { counter ->
                val isOccupied = counter.activeAttendantId != null
                val isMyShift = counter.activeAttendantId == currentUser.id

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMyShift) Color(0xFF0F2A4A) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Store,
                                        contentDescription = null,
                                        tint = if (isMyShift) SkyBlue else AmberPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = counter.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = counter.location,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isOccupied) {
                                StatusBadge(
                                    text = if (isMyShift) "My Active Shift" else "Active (Occupied)",
                                    containerColor = if (isMyShift) Color(0xFF0C4A6E) else DarkSurfaceVariant,
                                    contentColor = if (isMyShift) SkyBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                StatusBadge(
                                    text = "Ready to Open",
                                    containerColor = EmeraldGreen.copy(alpha = 0.15f),
                                    contentColor = EmeraldGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isMyShift) {
                            Text(
                                formatShiftElapsedTime(activeShift?.startTime ?: currentTime, currentTime),
                                fontSize = 12.sp,
                                color = SkyBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (isOccupied) {
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("In Use by ${counter.activeAttendantName ?: "Another Attendant"}")
                            }
                        } else {
                            Button(
                                onClick = { onSelectCounterForShift(counter) },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_shift_button_counter_${counter.id}")
                            ) {
                                Text(
                                    text = "Inspect Stock & Open Shift",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        }

        // History: recent shifts and reconciliation with period filters.
        if (selectedTab == AttendantDashboardTab.HISTORY) {
            item {
                Text("SHIFT HISTORY & RECONCILIATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { selectedPeriod = HistoryPeriod.TODAY },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (selectedPeriod == HistoryPeriod.TODAY) AmberPrimary else Color.Transparent,
                            contentColor = if (selectedPeriod == HistoryPeriod.TODAY) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Today", fontWeight = FontWeight.Bold) }

                    Box(modifier = Modifier.weight(1f)) {
                        TextButton(
                            onClick = { selectedPeriod = HistoryPeriod.MONTH; monthMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (selectedPeriod == HistoryPeriod.MONTH) AmberPrimary else Color.Transparent,
                                contentColor = if (selectedPeriod == HistoryPeriod.MONTH) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(monthLabels[selectedMonth], fontWeight = FontWeight.Bold) }
                        DropdownMenu(expanded = monthMenuExpanded, onDismissRequest = { monthMenuExpanded = false }) {
                            monthLabels.forEachIndexed { index, label ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { selectedMonth = index; selectedPeriod = HistoryPeriod.MONTH; monthMenuExpanded = false })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        TextButton(
                            onClick = { selectedPeriod = HistoryPeriod.YEAR; yearMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (selectedPeriod == HistoryPeriod.YEAR) AmberPrimary else Color.Transparent,
                                contentColor = if (selectedPeriod == HistoryPeriod.YEAR) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(selectedYear.toString(), fontWeight = FontWeight.Bold) }
                        DropdownMenu(expanded = yearMenuExpanded, onDismissRequest = { yearMenuExpanded = false }) {
                            availableYears.forEach { year ->
                                DropdownMenuItem(text = { Text(year.toString()) }, onClick = { selectedYear = year; selectedPeriod = HistoryPeriod.YEAR; yearMenuExpanded = false })
                            }
                        }
                    }
                }
                if (selectedPeriod == HistoryPeriod.MONTH) {
                    Text("Selected month: ${monthLabels[selectedMonth]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selectedPeriod == HistoryPeriod.YEAR) {
                    Text("Selected year: $selectedYear", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricStatCard(title = "Sales subtotal", value = formatCurrency(historySales), icon = Icons.Filled.AttachMoney, iconColor = EmeraldGreen, subtitle = "Submitted cash", modifier = Modifier.weight(1f))
                    MetricStatCard(title = "Variance subtotal", value = formatCurrency(historyVariance), icon = Icons.Filled.Warning, iconColor = if (historyVariance < 0) CrimsonRed else EmeraldGreen, subtitle = if (historyVariance < 0) "Shortage" else "Extra / balanced", modifier = Modifier.weight(1f))
                }
            }
            if (filteredShifts.isEmpty()) {
                item { Text("No shifts found for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp)) }
            } else {
            items(filteredShifts) { shift ->
                val rec = filteredReconciliations.firstOrNull { it.shiftId == shift.id }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewShiftReceipt(shift) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${shift.counterName} · Shift #${shift.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (rec != null) {
                                ReconciliationBadge(type = rec.type, variance = rec.variance)
                            } else {
                                StatusBadge(text = shift.status.name, containerColor = DarkSurfaceVariant, contentColor = AmberPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sales: ${formatCurrency(shift.totalSubmittedCash)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDateTime(shift.startTime),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Receipt >",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberPrimary
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
