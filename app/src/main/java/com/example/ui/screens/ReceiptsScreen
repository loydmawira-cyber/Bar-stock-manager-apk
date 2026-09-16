package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.PurchaseReceipt
import com.example.data.model.Counter
import com.example.data.model.Reconciliation
import com.example.data.model.User

@Composable
fun ReceiptsScreen(
    currentUser: User,
    reconciliations: List<Reconciliation>,
    attendants: List<User>,
    selectedAttendantId: Long?,
    purchaseReceipts: List<PurchaseReceipt>,
    counters: List<Counter> = emptyList(),
    onViewShiftReceipt: (Long) -> Unit = {},
    onSelectAttendantFilter: (Long?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sales Receipts", "Purchase Receipts")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (selectedTab == 0) {
            LedgerScreen(
                currentUser = currentUser,
                reconciliations = reconciliations,
                attendants = attendants,
                selectedAttendantId = selectedAttendantId,
                onViewShiftReceipt = onViewShiftReceipt,
                onSelectAttendantFilter = onSelectAttendantFilter
            )
        } else {
            PurchaseReceiptsScreen(receipts = purchaseReceipts, counters = counters)
        }
    }
}
