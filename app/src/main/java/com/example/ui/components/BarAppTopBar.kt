package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.BarProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarAppTopBar(
    title: String,
    barProfile: BarProfile,
    currentUser: User?,
    allUsers: List<User> = emptyList(),
    unreadCount: Int,
    isSyncing: Boolean,
    currentScreen: AppScreen,
    onBackClick: (() -> Unit)? = null,
    onNotificationClick: () -> Unit,
    onBarProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onLogout: () -> Unit
) {
    var showUserMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BarLogoIcon(
                    iconType = barProfile.iconType,
                    size = 32.dp,
                    iconSize = 18.dp,
                    modifier = Modifier.padding(end = 10.dp)
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (currentUser != null) {
                        Text(
                            text = "${barProfile.barName} · ${currentUser.role.name} (${currentUser.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (currentUser.role) {
                                UserRole.OWNER -> AmberPrimary
                                UserRole.MANAGER -> Color(0xFF60A5FA)
                                UserRole.ATTENDANT -> EmeraldGreen
                            },
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null && currentScreen != AppScreen.ADMIN_DASHBOARD && currentScreen != AppScreen.ATTENDANT_DASHBOARD && currentScreen != AppScreen.AUTH) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            if (currentUser != null) {
                Surface(
                    color = if (isSyncing) AmberPrimary.copy(alpha = 0.14f) else EmeraldGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(end = 6.dp).testTag("sync_status_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("✓", color = if (isSyncing) AmberPrimary else EmeraldGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSyncing) "Synchronizing..." else "Synced",
                            color = if (isSyncing) AmberPrimary else EmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Notification Bell with Badge
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.testTag("notification_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = AmberPrimary,
                                    contentColor = Color.Black
                                ) {
                                    Text(text = "$unreadCount", fontWeight = FontWeight.Bold)
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

                // User Role & Switcher Pill
                Box {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { showUserMenu = true }
                            .testTag("user_persona_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (currentUser.role == UserRole.OWNER || currentUser.role == UserRole.MANAGER) Icons.Outlined.Shield else Icons.Outlined.Person,
                                contentDescription = null,
                                tint = when (currentUser.role) {
                                    UserRole.OWNER -> AmberPrimary
                                    UserRole.MANAGER -> Color(0xFF60A5FA)
                                    UserRole.ATTENDANT -> EmeraldGreen
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentUser.name.split(" ").firstOrNull() ?: "User",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Dropdown for settings / passwords / logout
                    DropdownMenu(
                        expanded = showUserMenu,
                        onDismissRequest = { showUserMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = if (currentUser.role == UserRole.OWNER || currentUser.role == UserRole.MANAGER) "Bar Profile & Settings" else "Settings & Station",
                                    fontWeight = FontWeight.SemiBold 
                                ) 
                            },
                            onClick = {
                                showUserMenu = false
                                onBarProfileClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = AmberPrimary
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Change Password", fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showUserMenu = false
                                onChangePasswordClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LockReset,
                                    contentDescription = null,
                                    tint = AmberPrimary
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showUserMenu = false
                                onLogout()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ExitToApp,
                                    contentDescription = "Log Out",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
