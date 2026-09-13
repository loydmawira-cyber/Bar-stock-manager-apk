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
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BarProfile
import com.example.data.model.Counter
import com.example.data.model.Dispute
import com.example.data.model.Item
import com.example.data.model.Reconciliation
import com.example.data.model.Shift
import com.example.data.model.User
import com.example.ui.components.BarLogoIcon
import com.example.ui.components.DisputeBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ReconciliationBadge
import com.example.ui.components.ShiftStatusBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.SkyBlue
import com.example.ui.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminDashboardScreen(
    barProfile: BarProfile,
    counters: List<Counter>,
    items: List<Item>,
    shifts: List<Shift>,
    disputes: List<Dispute>,
    pendingUsers: List<User>,
    reconciliations: List<Reconciliation>,
    unreadNotificationsCount: Int = 0,
    isSyncing: Boolean = false,
    onNavigate: (AppScreen) -> Unit,
    onViewShiftReceipt: (Shift) -> Unit = {},
    onAddStockAdjustment: (counterId: Long, itemId: Long, qty: Int, reason: String) -> Unit,
    onSelectCounterForManagement: (Long) -> Unit
) {
    var showRestockDialog by remember { mutableStateOf(false) }
    var selectedCounterForRestock by remember { mutableStateOf<Counter?>(null) }
    var selectedItemForRestock by remember { mutableStateOf<Item?>(null) }
    var restockQtyText by remember { mutableStateOf("12") }
    var restockReason by remember { mutableStateOf("Mid-Shift Restock") }

    val activeShiftsCount = counters.count { it.activeAttendantId != null }
    val openDisputesCount = disputes.count { it.status == com.example.data.model.DisputeStatus.OPEN }
    val pendingApprovalsCount = pendingUsers.size

    val netLosses = reconciliations.filter { it.variance < 0 }.sumOf { Math.abs(it.variance) }
    val netExtras = reconciliations.filter { it.variance > 0 }.sumOf { it.variance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        stickyHeader {
            Spacer(modifier = Modifier.height(4.dp))

            // Bar Profile Brand Hero Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_bar_profile_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (!barProfile.customPhotoUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp))
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
                                                Color.Black.copy(alpha = 0.05f),
                                                Color.Black.copy(alpha = 0.78f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { onNavigate(AppScreen.NOTIFICATIONS) },
                                    modifier = Modifier.testTag("admin_banner_notifications_button")
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
                                            tint = Color.White
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onNavigate(AppScreen.BAR_PROFILE) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.testTag("edit_bar_settings_button")
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
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = barProfile.barName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = barProfile.location.ifBlank { "Main Branch" },
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                BarLogoIcon(
                                    iconType = barProfile.iconType,
                                    size = 52.dp,
                                    iconSize = 28.dp
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = barProfile.barName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = barProfile.location.ifBlank { "Main Branch" },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Actions in Banner Header (Notifications & Settings)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onNavigate(AppScreen.NOTIFICATIONS) },
                                    modifier = Modifier.testTag("admin_banner_notifications_button")
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
                                    border = BorderStroke(1.dp, AmberPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                                    modifier = Modifier.testTag("edit_bar_settings_button")
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

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(
                                text = "Manager: ${barProfile.managerName.ifBlank { "Admin" }}",
                                containerColor = DarkSurfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { onNavigate(AppScreen.ITEMS_MANAGEMENT) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("banner_set_prices_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalBar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stock Prices", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Live Selling Counters Section (Direct operational overview)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE SELLING COUNTERS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            selectedCounterForRestock = counters.firstOrNull()
                            selectedItemForRestock = items.firstOrNull()
                            showRestockDialog = true
                        }
                    ) {
                        Text("+ Restock", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { onNavigate(AppScreen.COUNTERS_MANAGEMENT) }) {
                        Text("Counters", color = AmberPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        items(counters) { counter ->
            val isActive = counter.activeAttendantId != null
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) DarkSurface else DarkSurface.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isActive) SkyBlue.copy(alpha = 0.4f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = counter.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "📍 ${counter.location}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isActive) {
                            StatusBadge(
                                text = "On Duty: ${counter.activeAttendantName ?: "Attendant"}",
                                containerColor = Color(0xFF0C4A6E),
                                contentColor = SkyBlue
                            )
                        } else {
                            StatusBadge(
                                text = "Counter Available",
                                containerColor = Color(0xFF064E3B),
                                contentColor = EmeraldGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSelectCounterForManagement(counter.id)
                                onNavigate(AppScreen.COUNTERS_MANAGEMENT)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View Stock", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                selectedCounterForRestock = counter
                                selectedItemForRestock = items.firstOrNull()
                                showRestockDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Restock / Adjust", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Shifts Section
        val activeShifts = shifts.filter { it.status == com.example.data.model.ShiftStatus.ACTIVE }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE SHIFTS (${activeShifts.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                TextButton(onClick = { onNavigate(AppScreen.LEDGER) }) {
                    Text("View Sales", color = AmberPrimary, fontSize = 12.sp)
                }
            }
        }

        if (activeShifts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No active shifts currently running",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Active attendant counter shifts will appear here in real-time.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(activeShifts) { shift ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${shift.counterName} · Shift #${shift.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Attendant: ${shift.attendantName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Started: ${formatDateTime(shift.startTime)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            ShiftStatusBadge(status = shift.status)
                        }
                    }
                }
            }
        }


    }

    // Mid-Shift Restock / Adjustment Modal
    if (showRestockDialog && selectedCounterForRestock != null) {
        var expandedCounter by remember { mutableStateOf(false) }
        var expandedItem by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRestockDialog = false },
            title = {
                Text("Add / Remove Stock (Restock)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "If the counter has an active attendant, a notification will be pushed to them to confirm the received quantity.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Select Counter
                    ExposedDropdownMenuBox(
                        expanded = expandedCounter,
                        onExpandedChange = { expandedCounter = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCounterForRestock?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Counter") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCounter) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCounter,
                            onDismissRequest = { expandedCounter = false }
                        ) {
                            counters.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedCounterForRestock = c
                                        expandedCounter = false
                                    }
                                )
                            }
                        }
                    }

                    // Select Item
                    ExposedDropdownMenuBox(
                        expanded = expandedItem,
                        onExpandedChange = { expandedItem = it }
                    ) {
                        OutlinedTextField(
                            value = selectedItemForRestock?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Item to Restock") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItem) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedItem,
                            onDismissRequest = { expandedItem = false }
                        ) {
                            items.forEach { itm ->
                                DropdownMenuItem(
                                    text = { Text("${itm.name} (${itm.category.name})") },
                                    onClick = {
                                        selectedItemForRestock = itm
                                        expandedItem = false
                                    }
                                )
                            }
                        }
                    }

                    // Quantity input
                    OutlinedTextField(
                        value = restockQtyText,
                        onValueChange = { restockQtyText = it },
                        label = { Text("Quantity Added (+/-)") },
                        supportingText = {
                            Text("Adds to existing counter stock (Existing + Added = New Total)")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restock_qty_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    OutlinedTextField(
                        value = restockReason,
                        onValueChange = { restockReason = it },
                        label = { Text("Reason / Note") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = restockQtyText.toIntOrNull() ?: 0
                        val counter = selectedCounterForRestock
                        val item = selectedItemForRestock
                        if (counter != null && item != null && qty != 0) {
                            onAddStockAdjustment(counter.id, item.id, qty, restockReason)
                            showRestockDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Apply & Notify Attendant", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminModuleButton(
    title: String,
    badge: String?,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(95.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }

                if (badge != null) {
                    Surface(
                        color = color,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp
            )
        }
    }
}
