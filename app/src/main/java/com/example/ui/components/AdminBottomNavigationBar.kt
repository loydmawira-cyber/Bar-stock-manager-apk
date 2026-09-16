package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.viewmodel.AppScreen

data class AdminTabItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val badgeColor: Color = AmberPrimary,
    val testTag: String
)

@Composable
fun AdminBottomNavigationBar(
    currentScreen: AppScreen,
    openDisputesCount: Int,
    pendingUsersCount: Int,
    onNavigate: (AppScreen) -> Unit
) {
    val tabs = listOf(
        AdminTabItem(
            screen = AppScreen.ADMIN_DASHBOARD,
            label = "Overview",
            icon = Icons.Filled.Dashboard,
            testTag = "admin_tab_overview"
        ),
        AdminTabItem(
            screen = AppScreen.STORE_INVENTORY,
            label = "Inventory",
            icon = Icons.Filled.Store,
            testTag = "admin_tab_counters"
        ),
        AdminTabItem(
            screen = AppScreen.USERS_MANAGEMENT,
            label = "Users",
            icon = Icons.Filled.People,
            badgeCount = pendingUsersCount,
            badgeColor = AmberPrimary,
            testTag = "admin_tab_users"
        ),
        AdminTabItem(
            screen = AppScreen.LEDGER,
            label = "Receipts",
            icon = Icons.Filled.ReceiptLong,
            testTag = "admin_tab_receipts"
        ),
        AdminTabItem(
            screen = AppScreen.DISPUTES,
            label = "Disputes",
            icon = Icons.Filled.Warning,
            badgeCount = openDisputesCount,
            badgeColor = CrimsonRed,
            testTag = "admin_tab_disputes"
        )
    )

    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        tabs.forEach { tab ->
            val isSelected = currentScreen == tab.screen

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(tab.screen) },
                icon = {
                    if (tab.badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = tab.badgeColor,
                                    contentColor = Color.Black
                                ) {
                                    Text(
                                        text = "${tab.badgeCount}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AmberPrimary,
                    selectedTextColor = AmberPrimary,
                    indicatorColor = AmberPrimary.copy(alpha = 0.18f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
