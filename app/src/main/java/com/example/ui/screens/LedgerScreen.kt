package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.model.Reconciliation
import com.example.data.model.ReconciliationType
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.LocalCurrencySymbol
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ReconciliationBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import java.util.Calendar

val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

enum class TimeframeMode {
    ALL_TIME,
    TODAY,
    MONTH,
    YEAR
}

@Composable
fun LedgerScreen(
    currentUser: User,
    reconciliations: List<Reconciliation>,
    attendants: List<User>,
    selectedAttendantId: Long?,
    onViewShiftReceipt: (Long) -> Unit = {},
    onSelectAttendantFilter: (Long?) -> Unit
) {
    val currency = LocalCurrencySymbol.current
    var showAttendantDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }

    val currentCal = remember { Calendar.getInstance() }
    var timeframeMode by remember { mutableStateOf(TimeframeMode.ALL_TIME) }
    var selectedMonth by remember { mutableStateOf(currentCal.get(Calendar.MONTH)) } // 0..11
    var selectedYear by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }

    // Collect available years from reconciliations data + default range
    val availableYears = remember(reconciliations) {
        val dataYears = reconciliations.map { recon ->
            Calendar.getInstance().apply { timeInMillis = recon.date }.get(Calendar.YEAR)
        }
        val currentYr = Calendar.getInstance().get(Calendar.YEAR)
        val defaultRange = listOf(currentYr + 1, currentYr, currentYr - 1, currentYr - 2, currentYr - 3)
        (dataYears + defaultRange).distinct().sortedDescending()
    }

    // If attendant is viewing, restrict to their own records automatically
    val effectiveFilterId = if (currentUser.role == UserRole.ATTENDANT) currentUser.id else selectedAttendantId

    // Filter reconciliations based on Attendant + Timeframe Mode
    val filteredList = reconciliations.filter { recon ->
        val matchesAttendant = effectiveFilterId == null || recon.attendantId == effectiveFilterId

        val reconCal = Calendar.getInstance().apply { timeInMillis = recon.date }
        val reconYear = reconCal.get(Calendar.YEAR)
        val reconMonth = reconCal.get(Calendar.MONTH)

        val matchesTimeframe = when (timeframeMode) {
            TimeframeMode.ALL_TIME -> true
            TimeframeMode.TODAY -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                recon.date >= startOfDay
            }
            TimeframeMode.MONTH -> {
                reconMonth == selectedMonth && reconYear == selectedYear
            }
            TimeframeMode.YEAR -> {
                reconYear == selectedYear
            }
        }

        matchesAttendant && matchesTimeframe
    }

    val totalExpected = filteredList.sumOf { it.expectedTotal }
    val totalSubmitted = filteredList.sumOf { it.submittedTotal }
    val totalLosses = filteredList.filter { it.variance < 0.0 }.sumOf { Math.abs(it.variance) }
    val totalExtras = filteredList.filter { it.variance > 0.0 }.sumOf { it.variance }
    val netVariance = totalSubmitted - totalExpected

    val balancedCount = filteredList.count { it.type == ReconciliationType.BALANCED }
    val lossCount = filteredList.count { it.type == ReconciliationType.LOSS }
    val extraCount = filteredList.count { it.type == ReconciliationType.EXTRA }

    val selectedAttendantName = attendants.firstOrNull { it.id == effectiveFilterId }?.name ?: "All Attendants"

    val timeframeSummaryLabel = when (timeframeMode) {
        TimeframeMode.ALL_TIME -> "All Time"
        TimeframeMode.TODAY -> "Today"
        TimeframeMode.MONTH -> "${MONTH_NAMES[selectedMonth]} $selectedYear"
        TimeframeMode.YEAR -> "Year $selectedYear"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filter Controls Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val isManagement = currentUser.role == UserRole.OWNER || currentUser.role == UserRole.MANAGER
                    val isFiltered = timeframeMode != TimeframeMode.ALL_TIME || (isManagement && selectedAttendantId != null)

                    if (isManagement || isFiltered) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isManagement) {
                                Box {
                                    OutlinedButton(
                                        onClick = { showAttendantDropdown = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("filter_attendant_button")
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedAttendantName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showAttendantDropdown,
                                        onDismissRequest = { showAttendantDropdown = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "All Attendants",
                                                    fontWeight = if (selectedAttendantId == null) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedAttendantId == null) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                onSelectAttendantFilter(null)
                                                showAttendantDropdown = false
                                            }
                                        )

                                        attendants.forEach { att ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = att.name,
                                                        fontWeight = if (selectedAttendantId == att.id) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedAttendantId == att.id) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    onSelectAttendantFilter(att.id)
                                                    showAttendantDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            if (isFiltered) {
                                TextButton(
                                    onClick = {
                                        timeframeMode = TimeframeMode.ALL_TIME
                                        if (currentUser.role == UserRole.OWNER || currentUser.role == UserRole.MANAGER) {
                                            onSelectAttendantFilter(null)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Filters", fontSize = 11.sp, color = AmberPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Timeframe Filter Chips Row (Quick presets + Month picker + Year picker)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All Time
                        FilterChip(
                            selected = timeframeMode == TimeframeMode.ALL_TIME,
                            onClick = { timeframeMode = TimeframeMode.ALL_TIME },
                            label = { Text("All Time", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Today
                        FilterChip(
                            selected = timeframeMode == TimeframeMode.TODAY,
                            onClick = { timeframeMode = TimeframeMode.TODAY },
                            label = { Text("Today", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Select Month Dropdown Chip
                        Box {
                            val isMonthSelected = timeframeMode == TimeframeMode.MONTH
                            FilterChip(
                                selected = isMonthSelected,
                                onClick = { showMonthDropdown = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isMonthSelected) "Month: ${MONTH_NAMES[selectedMonth]}" else "Select Month",
                                            fontSize = 12.sp,
                                            fontWeight = if (isMonthSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Select Month",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("filter_month_chip")
                            )

                            DropdownMenu(
                                expanded = showMonthDropdown,
                                onDismissRequest = { showMonthDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "Select Month ($selectedYear)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )

                                MONTH_NAMES.forEachIndexed { index, monthName ->
                                    val isCurrent = index == selectedMonth && timeframeMode == TimeframeMode.MONTH
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = monthName,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isCurrent) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = AmberPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedMonth = index
                                            timeframeMode = TimeframeMode.MONTH
                                            showMonthDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Select Year Dropdown Chip
                        Box {
                            val isYearSelected = timeframeMode == TimeframeMode.YEAR
                            FilterChip(
                                selected = isYearSelected,
                                onClick = { showYearDropdown = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isYearSelected) "Year: $selectedYear" else "Select Year",
                                            fontSize = 12.sp,
                                            fontWeight = if (isYearSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Select Year",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("filter_year_chip")
                            )

                            DropdownMenu(
                                expanded = showYearDropdown,
                                onDismissRequest = { showYearDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "Select Year",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )

                                availableYears.forEach { yr ->
                                    val isCurrent = yr == selectedYear && (timeframeMode == TimeframeMode.YEAR || timeframeMode == TimeframeMode.MONTH)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$yr",
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isCurrent) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = AmberPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedYear = yr
                                            if (timeframeMode != TimeframeMode.MONTH) {
                                                timeframeMode = TimeframeMode.YEAR
                                            }
                                            showYearDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Active filters tag / count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing ${filteredList.size} shift settlements ($timeframeSummaryLabel)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filteredList.isNotEmpty()) {
                            Text(
                                text = "$balancedCount balanced · $lossCount losses · $extraCount surplus",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Expected Sales",
                    value = formatCurrency(totalExpected),
                    icon = Icons.Filled.ReceiptLong,
                    iconColor = AmberPrimary,
                    subtitle = "System tallied",
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "Cash Collected",
                    value = formatCurrency(totalSubmitted),
                    icon = Icons.Filled.AttachMoney,
                    iconColor = EmeraldGreen,
                    subtitle = "Handover cash",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Shortage / Losses",
                    value = formatCurrency(totalLosses),
                    icon = Icons.Filled.ArrowDownward,
                    iconColor = if (totalLosses > 0.0) CrimsonRed else EmeraldGreen,
                    subtitle = if (lossCount > 0) "$lossCount shift deficits" else "No deficits",
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "Surplus / Extras",
                    value = formatCurrency(totalExtras),
                    icon = Icons.Filled.ArrowUpward,
                    iconColor = EmeraldGreen,
                    subtitle = if (extraCount > 0) "$extraCount shift overages" else "No overages",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Net Balance & Accuracy Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Net Variance Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (netVariance < 0.0) "-${formatCurrency(Math.abs(netVariance))}" else if (netVariance > 0.0) "+${formatCurrency(netVariance)}" else formatCurrency(0.0),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = if (netVariance < -0.01) CrimsonRed else if (netVariance > 0.01) EmeraldGreen else Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Accuracy Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val rate = if (filteredList.isNotEmpty()) (balancedCount.toDouble() / filteredList.size * 100).toInt() else 100
                        Text(
                            text = "$rate% Balanced",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rate >= 80) EmeraldGreen else AmberPrimary
                        )
                    }
                }
            }
        }

        // Itemized Reconciliations List
        item {
            Text(
                text = "SHIFT-BY-SHIFT SETTLEMENT HISTORY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No closed shift settlements found for this filter criteria.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                timeframeMode = TimeframeMode.ALL_TIME
                                if (currentUser.role == UserRole.OWNER || currentUser.role == UserRole.MANAGER) {
                                    onSelectAttendantFilter(null)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Show All Records", fontSize = 12.sp, color = AmberPrimary)
                        }
                    }
                }
            }
        } else {
            items(filteredList) { recon ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewShiftReceipt(recon.shiftId) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Shift #${recon.shiftId} · ${recon.attendantName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatDateTime(recon.date),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "· View Receipt >",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberPrimary
                                    )
                                }
                            }

                            ReconciliationBadge(type = recon.type, variance = recon.variance)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Expected: ${formatCurrency(recon.expectedTotal)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Handover Cash: ${formatCurrency(recon.submittedTotal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        if (recon.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: ${recon.notes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
