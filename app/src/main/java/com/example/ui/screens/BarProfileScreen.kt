package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import android.app.Activity
import com.example.data.billing.BillingConnectionState
import com.example.data.billing.BillingPlan
import com.example.data.billing.BillingStatus
import com.example.data.model.BarProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.BAR_ICON_PRESETS
import com.example.ui.components.BarLogoIcon
import com.example.ui.components.GoogleBillingAdminSection
import com.example.ui.components.StatusBadge
import com.example.ui.components.requiredLabel
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.AppScreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarProfileScreen(
    barProfile: BarProfile,
    currentUser: User?,
    billingStatus: BillingStatus = BillingStatus(),
    availablePlans: List<BillingPlan> = emptyList(),
    billingConnectionState: BillingConnectionState = BillingConnectionState.CONNECTED,
    onLaunchPurchase: (Activity, BillingPlan) -> Unit = { _, _ -> },
    onRestorePurchases: () -> Unit = {},
    onOpenSubscriptions: (Activity) -> Unit = {},
    onSaveProfile: (
        name: String,
        location: String,
        iconType: String,
        customPhotoUri: String?,
        contactPhone: String,
        currencySymbol: String,
        paymentMethods: String,
        managerName: String,
        openingHours: String
    ) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onChangePasswordClick: () -> Unit = {},
    hasPin: Boolean = false,
    onSetPinClick: () -> Unit = {},
    onRemovePinClick: () -> Unit = {}
) {
    var barName by remember(barProfile) { mutableStateOf(barProfile.barName) }
    var location by remember(barProfile) { mutableStateOf(barProfile.location) }
    var selectedIcon by remember(barProfile) { mutableStateOf(barProfile.iconType) }
    var customPhotoUri by remember(barProfile) { mutableStateOf(barProfile.customPhotoUri ?: "") }
    var contactPhone by remember(barProfile) { mutableStateOf(barProfile.contactPhone) }
    var currencySymbol by remember(barProfile) { mutableStateOf(barProfile.currencySymbol) }
    var paymentMethods by remember(barProfile) { mutableStateOf(barProfile.paymentMethods) }
    var managerName by remember(barProfile) { mutableStateOf(barProfile.managerName) }
    var openingHours by remember(barProfile) { mutableStateOf(barProfile.openingHours) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header & Branding Preview Card (Always visible for context)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (currentUser?.role == UserRole.OWNER) "LIVE BRANDING PREVIEW" else "MY STATION PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentUser?.role == UserRole.OWNER) "Multi-Bar Ready" else "Active Attendant",
                            color = EmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero Branding Area
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
                            iconType = selectedIcon,
                            size = 52.dp,
                            iconSize = 28.dp,
                            tint = AmberPrimary,
                            backgroundColor = AmberPrimary.copy(alpha = 0.15f),
                            borderColor = AmberPrimary.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = barName.ifBlank { "Bar Name" },
                                style = MaterialTheme.typography.titleLarge,
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
                                    text = location.ifBlank { "Main Branch" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (currentUser?.role == UserRole.ATTENDANT) {
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Active Station",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (currentUser?.role == UserRole.ATTENDANT) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            text = "Manager: ${barProfile.managerName.ifBlank { "Admin" }}",
                            containerColor = DarkSurfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = { onNavigate(AppScreen.ITEMS_MANAGEMENT) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.LocalBar, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stock Prices", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(AppScreen.ATTENDANT_DASHBOARD) },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attendant Station", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onChangePasswordClick() },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AmberPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Password", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                }
            }
        }

        // Security & Help: visible to every role
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Security & Help",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sign in faster with a 4-digit PIN, or read the guide for your role.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSetPinClick() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasPin) "Change Quick PIN" else "Set Quick PIN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (hasPin) {
                        OutlinedButton(
                            onClick = { onRemovePinClick() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { onNavigate(AppScreen.USER_MANUAL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_user_manual_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AmberPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("User Manual", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (currentUser?.role == UserRole.OWNER) {
            // Section: Visual Identity & Icon / Photo Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "1. Choose Bar Icon & Visual Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select an emblem style representing your establishment:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BAR_ICON_PRESETS.forEach { preset ->
                            val isSelected = selectedIcon == preset.id
                            Surface(
                                color = if (isSelected) AmberPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) AmberPrimary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .clickable { selectedIcon = preset.id }
                                    .testTag("icon_preset_${preset.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = preset.name,
                                        tint = if (isSelected) AmberPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Establishment Details Form
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "2. Bar Details & Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "This information prints on shift end summaries and stock handover receipts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    val isBarNameValid = barName.trim().isNotBlank()
                    val isLocationValid = location.trim().isNotBlank()
                    val isProfileFormValid = isBarNameValid && isLocationValid

                    OutlinedTextField(
                        value = barName,
                        onValueChange = { barName = it },
                        label = requiredLabel("Bar / Establishment Name"),
                        isError = barName.isNotEmpty() && !isBarNameValid,
                        supportingText = if (barName.isNotEmpty() && !isBarNameValid) { { Text("Bar name required", color = CrimsonRed) } } else null,
                        leadingIcon = { Icon(Icons.Outlined.Storefront, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bar_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            focusedLabelColor = AmberPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = requiredLabel("Physical Location / Address"),
                        isError = location.isNotEmpty() && !isLocationValid,
                        supportingText = if (location.isNotEmpty() && !isLocationValid) { { Text("Location required", color = CrimsonRed) } } else null,
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bar_location_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            focusedLabelColor = AmberPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Contact Phone") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bar_phone_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                focusedLabelColor = AmberPrimary
                            )
                        )

                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("Currency (e.g. $, KSh, UGX)") },
                            leadingIcon = { Icon(Icons.Filled.MonetizationOn, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bar_currency_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                focusedLabelColor = AmberPrimary
                            )
                        )
                    }

                    OutlinedTextField(
                        value = paymentMethods,
                        onValueChange = { paymentMethods = it },
                        label = { Text("Payment methods") },
                        supportingText = { Text("Comma separated: Cash, M-Pesa, Card") },
                        modifier = Modifier.fillMaxWidth().testTag("bar_payment_methods_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            focusedLabelColor = AmberPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = managerName,
                            onValueChange = { managerName = it },
                            label = { Text("Manager Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bar_manager_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                focusedLabelColor = AmberPrimary
                            )
                        )

                        OutlinedTextField(
                            value = openingHours,
                            onValueChange = { openingHours = it },
                            label = { Text("Operating Hours") },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bar_hours_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                focusedLabelColor = AmberPrimary
                            )
                        )
                    }
                }
            }
        }

        if (currentUser?.role == UserRole.OWNER) {
            val isBarNameValid = barName.trim().isNotBlank()
            val isLocationValid = location.trim().isNotBlank()
            val isProfileFormValid = isBarNameValid && isLocationValid

            // Save & Quick Links
            Button(
                onClick = {
                    if (isProfileFormValid) {
                        onSaveProfile(
                            barName.trim(),
                            location.trim(),
                            selectedIcon,
                            customPhotoUri.trim().ifBlank { null },
                            contactPhone.trim(),
                            currencySymbol.trim(),
                            paymentMethods.trim(),
                            managerName.trim(),
                            openingHours.trim()
                        )
                    }
                },
                enabled = isProfileFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bar_profile_button")
            ) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Bar Profile & Settings",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Secondary Button: Manage Attendant Logins
            OutlinedButton(
                onClick = { onNavigate(AppScreen.USERS_MANAGEMENT) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("navigate_to_attendants_button")
            ) {
                Icon(imageVector = Icons.Filled.People, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Proceed to Manage Attendant Logins",
                    color = EmeraldGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Play In-App Billing Section (Admin Only)
            GoogleBillingAdminSection(
                billingStatus = billingStatus,
                availablePlans = availablePlans,
                connectionState = billingConnectionState,
                onLaunchPurchase = onLaunchPurchase,
                onRestorePurchases = onRestorePurchases,
                onOpenSubscriptions = onOpenSubscriptions,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
