package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.util.PasswordValidator
import com.example.ui.components.StrongPasswordField
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.formatDateTime
import com.example.ui.components.requiredLabel
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    currentUserRole: UserRole,
    allUsers: List<User>,
    onApproveUser: (Long) -> Unit,
    onRevokeUser: (Long) -> Unit,
    onRestoreUser: (Long) -> Unit,
    onCreateAttendant: (name: String, email: String, phone: String, password: String) -> Unit,
    onCreateManager: (name: String, email: String, phone: String, password: String) -> Unit,
    onDeleteUser: (Long) -> Unit
) {
    var selectedStatusTab by remember { mutableIntStateOf(0) } // 0: Active, 1: Pending, 2: Revoked
    var selectedRoleFilter by remember { mutableStateOf(if (currentUserRole == UserRole.OWNER) "ALL" else "ATTENDANTS") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var targetRoleToCreate by remember { mutableStateOf(UserRole.ATTENDANT) }

    // Form inputs
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    // Filter users based on current user role
    val visibleUsers = if (currentUserRole == UserRole.MANAGER) {
        // Managers can ONLY view Attendants
        allUsers.filter { it.role == UserRole.ATTENDANT }
    } else {
        // Owner can view according to role filter
        when (selectedRoleFilter) {
            "MANAGERS" -> allUsers.filter { it.role == UserRole.MANAGER }
            "ATTENDANTS" -> allUsers.filter { it.role == UserRole.ATTENDANT }
            "OWNERS" -> allUsers.filter { it.role == UserRole.OWNER }
            else -> allUsers
        }
    }

    val approvedList = visibleUsers.filter { it.status == UserStatus.APPROVED }
    val pendingList = visibleUsers.filter { it.status == UserStatus.PENDING }
    val revokedList = visibleUsers.filter { it.status == UserStatus.REVOKED }

    val displayedUsers = when (selectedStatusTab) {
        0 -> approvedList
        1 -> pendingList
        else -> revokedList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header & Quick Add Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (currentUserRole == UserRole.OWNER) "BUSINESS USER HIERARCHY" else "BAR ATTENDANT MANAGEMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (currentUserRole == UserRole.OWNER) "Manage Owners, Managers, and Attendants" else "Manage Attendant accounts and shift access",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentUserRole == UserRole.OWNER) {
                    Button(
                        onClick = {
                            newName = ""
                            newEmail = ""
                            newPhone = ""
                            newPassword = "MgrPass@" + (1000..9999).random()
                            targetRoleToCreate = UserRole.MANAGER
                            showCreateDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_manager_button")
                    ) {
                        Text("+ Manager", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        newName = ""
                        newEmail = ""
                        newPhone = ""
                        newPassword = "BarPass@" + (1000..9999).random()
                        targetRoleToCreate = UserRole.ATTENDANT
                        showCreateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("create_attendant_button")
                ) {
                    Text("+ Attendant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Role Filter Row for Owner
        if (currentUserRole == UserRole.OWNER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "MANAGERS", "ATTENDANTS", "OWNERS").forEach { roleName ->
                    FilterChip(
                        selected = selectedRoleFilter == roleName,
                        onClick = { selectedRoleFilter = roleName },
                        label = { Text(roleName, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Tabs: Approved / Pending / Revoked
        TabRow(
            selectedTabIndex = selectedStatusTab,
            containerColor = DarkSurface,
            contentColor = AmberPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedStatusTab]),
                    color = AmberPrimary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedStatusTab == 0,
                onClick = { selectedStatusTab = 0 },
                text = { Text("Active (${approvedList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("tab_users_approved")
            )
            Tab(
                selected = selectedStatusTab == 1,
                onClick = { selectedStatusTab = 1 },
                text = { Text("Pending (${pendingList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("tab_users_pending")
            )
            Tab(
                selected = selectedStatusTab == 2,
                onClick = { selectedStatusTab = 2 },
                text = { Text("Revoked (${revokedList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("tab_users_revoked")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (displayedUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (selectedStatusTab) {
                        0 -> "No active accounts in this view."
                        1 -> "No pending applications."
                        else -> "No revoked accounts."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedUsers, key = { it.id }) { user ->
                    UserCardItem(
                        user = user,
                        currentUserRole = currentUserRole,
                        onApprove = { onApproveUser(user.id) },
                        onRevoke = { onRevokeUser(user.id) },
                        onRestore = { onRestoreUser(user.id) },
                        onDelete = { onDeleteUser(user.id) }
                    )
                }
            }
        }
    }

    // Modal Dialog: Creating New Account
    if (showCreateDialog) {
        var submittedAttempt by remember { mutableStateOf(false) }

        val isNameValid = newName.trim().isNotBlank()
        val isPhoneValid = newPhone.trim().isNotBlank()
        val isEmailValid = newEmail.trim().isNotBlank() && newEmail.contains("@")
        val passwordValid = PasswordValidator.validate(newPassword).isValid
        val isFormValid = isNameValid && isPhoneValid && isEmailValid && passwordValid

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (targetRoleToCreate == UserRole.MANAGER) "Create Manager Account" else "Create Attendant Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Fill in all compulsory fields (*). Every account requires a unique email, phone number, and strong password.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = requiredLabel("Full Name"),
                        isError = submittedAttempt && !isNameValid,
                        supportingText = {
                            if (submittedAttempt && !isNameValid) {
                                Text("Full name is required", color = CrimsonRed)
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_user_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = requiredLabel("Phone Number"),
                        isError = submittedAttempt && !isPhoneValid,
                        supportingText = {
                            if (submittedAttempt && !isPhoneValid) {
                                Text("Phone number is required", color = CrimsonRed)
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_user_phone"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = requiredLabel("Email Address"),
                        isError = submittedAttempt && !isEmailValid,
                        supportingText = {
                            if (submittedAttempt && !isEmailValid) {
                                Text("Valid email address is required", color = CrimsonRed)
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_user_email"),
                        singleLine = true
                    )

                    StrongPasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "Initial Password *",
                        placeholder = "Min 8 chars, 1 uppercase, 1 digit, 1 special",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "new_user_password"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        submittedAttempt = true
                        if (isFormValid) {
                            if (targetRoleToCreate == UserRole.MANAGER) {
                                onCreateManager(newName.trim(), newEmail.trim(), newPhone.trim(), newPassword)
                            } else {
                                onCreateAttendant(newName.trim(), newEmail.trim(), newPhone.trim(), newPassword)
                            }
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("submit_create_user_button")
                ) {
                    Text("Create Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCardItem(
    user: User,
    currentUserRole: UserRole,
    onApprove: () -> Unit,
    onRevoke: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val canManageThisUser = when {
        currentUserRole == UserRole.OWNER -> user.role != UserRole.OWNER // Owner can manage Manager & Attendants
        currentUserRole == UserRole.MANAGER -> user.role == UserRole.ATTENDANT // Manager can ONLY manage Attendants
        else -> false
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                when (user.status) {
                                    UserStatus.APPROVED -> EmeraldGreen.copy(alpha = 0.15f)
                                    UserStatus.REVOKED -> CrimsonRed.copy(alpha = 0.15f)
                                    UserStatus.PENDING -> AmberPrimary.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = when (user.status) {
                                UserStatus.APPROVED -> EmeraldGreen
                                UserStatus.REVOKED -> CrimsonRed
                                UserStatus.PENDING -> AmberPrimary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = when (user.role) {
                                    UserRole.OWNER -> AmberPrimary.copy(alpha = 0.2f)
                                    UserRole.MANAGER -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                    UserRole.ATTENDANT -> Color(0xFF10B981).copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = user.role.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (user.role) {
                                        UserRole.OWNER -> AmberPrimary
                                        UserRole.MANAGER -> Color(0xFF60A5FA)
                                        UserRole.ATTENDANT -> Color(0xFF34D399)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${user.phone} · ${user.email}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                UserStatusBadge(status = user.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Added: ${formatDateTime(user.createdAt)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (canManageThisUser) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = CrimsonRed)
                        }

                        when (user.status) {
                            UserStatus.PENDING -> {
                                Button(
                                    onClick = onApprove,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Approve", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            UserStatus.APPROVED -> {
                                OutlinedButton(
                                    onClick = onRevoke,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Revoke", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            UserStatus.REVOKED -> {
                                Button(
                                    onClick = onRestore,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Restore", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
