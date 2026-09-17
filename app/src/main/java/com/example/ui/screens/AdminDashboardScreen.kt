package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.TrendingDown
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.ShiftClosing
import com.example.data.model.StoreStockWithItem
import com.example.data.model.User
import com.example.ui.components.BarLogoIcon
import com.example.ui.components.DisputeBadge
import com.example.ui.components.optionalLabel
import com.example.ui.components.requiredLabel
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
import java.util.Calendar
import kotlinx.coroutines.delay

private fun formatAdminShiftElapsedTime(startTime: Long, currentTime: Long): String {
    val elapsedMinutes = maxOf(0L, (currentTime - startTime) / 60_000L)
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return "Shift time: ${hours}h ${minutes.toString().padStart(2, '0')}m"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminDashboardScreen(
    barProfile: BarProfile,
    counters: List<Counter>,
    items: List<Item>,
    shifts: List<Shift>,
    currentUserRole: com.example.data.model.UserRole = com.example.data.model.UserRole.OWNER,
    storeStock: List<StoreStockWithItem> = emptyList(),
    shiftClosings: List<ShiftClosing> = emptyList(),
    disputes: List<Dispute>,
    pendingUsers: List<User>,
    reconciliations: List<Reconciliation>,
    unreadNotificationsCount: Int = 0,
    isSyncing: Boolean = false,
    onNavigate: (AppScreen) -> Unit,
    onViewShiftReceipt: (Shift) -> Unit = {},
    onAddStockAdjustment: (counterId: Long, itemId: Long, qty: Int, reason: String) -> Unit,
    onRestockCounterFromSource: (counterId: Long, itemId: Long, qty: Int, unitsPerPurchaseUnit: Int, purchaseUnitType: String, source: String, supplier: String, receiptNumber: String, unitCost: Double, reason: String) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onSelectCounterForManagement: (Long) -> Unit
) {
    var showRestockDialog by remember { mutableStateOf(false) }
    var restockSource by remember { mutableStateOf("STORE") }
    var expandedRestockSource by remember { mutableStateOf(false) }
    var selectedCounterForRestock by remember { mutableStateOf<Counter?>(null) }
    var selectedItemForRestock by remember { mutableStateOf<Item?>(null) }
    var restockQtyText by remember { mutableStateOf("12") }
    var restockReason by remember { mutableStateOf("Mid-Shift Restock") }
    var restockSupplier by remember { mutableStateOf("") }
    var restockReceiptNumber by remember { mutableStateOf("") }
    var restockUnitCost by remember { mutableStateOf("0") }
    var restockPurchaseUnit by remember { mutableStateOf("Crate") }
    var restockUnitsPerPurchaseUnit by remember { mutableStateOf("24") }
    var expandedPurchaseUnit by remember { mutableStateOf(false) }

    val activeShiftsCount = counters.count { it.activeAttendantId != null }
    val openDisputesCount = disputes.count { it.status == com.example.data.model.DisputeStatus.OPEN }
    val pendingApprovalsCount = pendingUsers.size

    val netLosses = reconciliations.filter { it.variance < 0 }.sumOf { Math.abs(it.variance) }
    val netExtras = reconciliations.filter { it.variance > 0 }.sumOf { it.variance }
    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
    val shiftDates = shifts.associate { it.id to (it.endTime ?: it.startTime) }
    val topSellingItems = shiftClosings
        .filter { (shiftDates[it.shiftId] ?: 0L) >= sevenDaysAgo }
        .groupBy { it.itemName }
        .mapValues { (_, closings) -> closings.sumOf { it.unitsSold } }
        .toList()
        .sortedByDescending { it.second }
        .take(3)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(60_000L)
        }
    }

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
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AmberPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_bar_profile_card")
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
                                            .testTag("bar_name_hero_title")
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
                                            .testTag("bar_name_hero_title")
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
                            modifier = Modifier.testTag("banner_set_prices_button")
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
                                border = BorderStroke(1.5.dp, AmberPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
            }
        }

        item {
            val tickerText = if (topSellingItems.isEmpty()) {
                "TOP SELLING ITEMS · LAST 7 DAYS    No completed sales recorded in the last 7 days"
            } else {
                "TOP SELLING ITEMS · LAST 7 DAYS    " +
                    topSellingItems.joinToString("     •     ") { (itemName, quantity) ->
                        "$itemName: $quantity units"
                    }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("admin_top_selling_items_ticker")
            ) {
                Text(
                    text = tickerText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .basicMarquee()
                )
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
            val activeShiftForCounter = shifts.firstOrNull {
                it.counterId == counter.id && it.status == com.example.data.model.ShiftStatus.ACTIVE
            }
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
                            if (isActive && activeShiftForCounter != null) {
                                Text(
                                    text = formatAdminShiftElapsedTime(activeShiftForCounter.startTime, currentTime),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBlue
                                )
                            }
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
    }

    // Mid-Shift Restock / Adjustment Modal
    if (showRestockDialog && selectedCounterForRestock != null) {
        var expandedCounter by remember { mutableStateOf(false) }
        var expandedItem by remember { mutableStateOf(false) }

        val availableAtStore = storeStock.firstOrNull { it.itemId == selectedItemForRestock?.id }?.currentQuantity ?: 0
        val qtyVal = restockQtyText.toIntOrNull() ?: 0
        val isQtyValid = qtyVal > 0
        val unitsPerPurchaseUnitVal = restockUnitsPerPurchaseUnit.toIntOrNull() ?: 0
        val isPurchasePackValid = unitsPerPurchaseUnitVal > 0
        val unitsToAdd = if (restockSource == "SUPPLIER") qtyVal * unitsPerPurchaseUnitVal else qtyVal
        val isStoreQtyValid = restockSource != "STORE" || (availableAtStore > 0 && unitsToAdd <= availableAtStore)

        val isCounterValid = selectedCounterForRestock != null
        val isItemValid = selectedItemForRestock != null

        val isSupplierValid = restockSupplier.trim().isNotBlank()
        val isReceiptValid = restockReceiptNumber.trim().isNotBlank()
        val costVal = restockUnitCost.toDoubleOrNull()
        val isCostValid = costVal != null && costVal > 0.0

        val isSupplierFormValid = restockSource != "SUPPLIER" || (isSupplierValid && isReceiptValid && isCostValid && isPurchasePackValid)
        val isFormValid = isCounterValid && isItemValid && isQtyValid && isStoreQtyValid && isSupplierFormValid

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
                            label = requiredLabel("Counter"),
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

                    ExposedDropdownMenuBox(
                        expanded = expandedRestockSource,
                        onExpandedChange = { expandedRestockSource = it }
                    ) {
                        OutlinedTextField(
                            value = if (restockSource == "STORE") "From Store" else "Direct from Supplier",
                            onValueChange = {},
                            readOnly = true,
                            label = requiredLabel("Restock Source"),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRestockSource) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRestockSource,
                            onDismissRequest = { expandedRestockSource = false }
                        ) {
                            DropdownMenuItem(text = { Text("From Store") }, onClick = { restockSource = "STORE"; expandedRestockSource = false })
                            DropdownMenuItem(text = { Text("Direct from Supplier") }, onClick = { restockSource = "SUPPLIER"; expandedRestockSource = false })
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
                            label = requiredLabel("Item to Restock"),
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
                    val hasNoStoreStock = restockSource == "STORE" && availableAtStore <= 0
                    val isExceedingStoreStock = restockSource == "STORE" && availableAtStore > 0 && qtyVal > availableAtStore
                    val hasQtyError = (restockQtyText.isNotEmpty() && !isQtyValid) || hasNoStoreStock || (restockQtyText.isNotEmpty() && isExceedingStoreStock)

                    OutlinedTextField(
                        value = restockQtyText,
                        onValueChange = { restockQtyText = it },
                        label = requiredLabel(if (restockSource == "SUPPLIER") "Quantity purchased" else "Quantity Added (+ Units)"),
                        isError = hasQtyError,
                        supportingText = {
                            if (hasNoStoreStock) {
                                Text("No stock available in Store for this item.", color = CrimsonRed)
                            } else if (restockQtyText.isNotEmpty() && isExceedingStoreStock) {
                                Text("Insufficient Store Stock. Available: $availableAtStore.", color = CrimsonRed)
                            } else if (restockQtyText.isNotEmpty() && !isQtyValid) {
                                Text("Quantity must be greater than 0", color = CrimsonRed)
                            } else {
                                Text(if (restockSource == "SUPPLIER") "Counter stock added: $qtyVal × $unitsPerPurchaseUnitVal = $unitsToAdd units" else "Adds to existing counter stock (Existing + Added = New Total)")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restock_qty_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    if (restockSource == "SUPPLIER") {
                        ExposedDropdownMenuBox(
                            expanded = expandedPurchaseUnit,
                            onExpandedChange = { expandedPurchaseUnit = it }
                        ) {
                            OutlinedTextField(
                                value = restockPurchaseUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = requiredLabel("Purchase Unit"),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPurchaseUnit) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandedPurchaseUnit, onDismissRequest = { expandedPurchaseUnit = false }) {
                                listOf("Crate", "Case", "Bottle", "Other").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        restockPurchaseUnit = option
                                        restockUnitsPerPurchaseUnit = when (option) { "Crate" -> "24"; "Case" -> "6"; "Bottle", "Other" -> "1"; else -> "1" }
                                        expandedPurchaseUnit = false
                                    })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = restockUnitsPerPurchaseUnit,
                            onValueChange = { restockUnitsPerPurchaseUnit = it },
                            label = requiredLabel("Units per $restockPurchaseUnit"),
                            isError = restockUnitsPerPurchaseUnit.isNotEmpty() && !isPurchasePackValid,
                            supportingText = if (restockUnitsPerPurchaseUnit.isNotEmpty() && !isPurchasePackValid) { { Text("Must be > 0", color = CrimsonRed) } } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = restockSupplier,
                            onValueChange = { restockSupplier = it },
                            label = requiredLabel("Supplier"),
                            isError = restockSupplier.isNotEmpty() && !isSupplierValid,
                            supportingText = if (restockSupplier.isNotEmpty() && !isSupplierValid) { { Text("Supplier name required", color = CrimsonRed) } } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = restockReceiptNumber,
                            onValueChange = { restockReceiptNumber = it },
                            label = requiredLabel("Receipt / Invoice Number"),
                            isError = restockReceiptNumber.isNotEmpty() && !isReceiptValid,
                            supportingText = if (restockReceiptNumber.isNotEmpty() && !isReceiptValid) { { Text("Receipt number required", color = CrimsonRed) } } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = restockUnitCost,
                            onValueChange = { restockUnitCost = it },
                            label = requiredLabel("Cost per $restockPurchaseUnit"),
                            isError = restockUnitCost.isNotEmpty() && !isCostValid,
                            supportingText = if (restockUnitCost.isNotEmpty() && !isCostValid) { { Text("Cost must be > 0", color = CrimsonRed) } } else { { Text("Enter the purchase price for one $restockPurchaseUnit") } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Text("Available at Store: $availableAtStore ${selectedItemForRestock?.unitType ?: "units"}", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = restockReason,
                        onValueChange = { restockReason = it },
                        label = optionalLabel("Reason / Note"),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val counter = selectedCounterForRestock
                        val item = selectedItemForRestock
                        if (isFormValid && counter != null && item != null) {
                            onRestockCounterFromSource(
                                counter.id,
                                item.id,
                                qtyVal,
                                if (restockSource == "SUPPLIER") unitsPerPurchaseUnitVal else 1,
                                if (restockSource == "SUPPLIER") restockPurchaseUnit else item.unitType,
                                restockSource,
                                restockSupplier.trim(),
                                restockReceiptNumber.trim(),
                                costVal ?: 0.0,
                                restockReason.trim()
                            )
                            showRestockDialog = false
                        }
                    },
                    enabled = isFormValid,
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
