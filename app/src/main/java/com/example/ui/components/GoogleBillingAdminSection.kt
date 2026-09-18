package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.billing.BillingConnectionState
import com.example.data.billing.BillingPlan
import com.example.data.billing.BillingStatus
import com.example.data.billing.SubscriptionTier
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EmeraldGreen

@Composable
fun GoogleBillingAdminSection(
    billingStatus: BillingStatus,
    availablePlans: List<BillingPlan>,
    connectionState: BillingConnectionState,
    onLaunchPurchase: (Activity, BillingPlan) -> Unit,
    onRestorePurchases: () -> Unit,
    onOpenSubscriptions: (Activity) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showFeatureDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header Card with Connection State
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AmberPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LICENSING & SUBSCRIPTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "6-Month Free Trial & $10/Yr Premium",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(14.dp))

                // Current Status Overview
                when {
                    billingStatus.isTrialActive -> {
                        // Trial Active Banner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AmberPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, AmberPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "6-MONTH FREE TRIAL ACTIVE",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = AmberPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Surface(
                                    color = AmberPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${billingStatus.trialDaysRemaining} DAYS LEFT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress bar for 180 days
                            val progress = ((180 - billingStatus.trialDaysRemaining).coerceIn(0, 180) / 180f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = AmberPrimary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Started: ${billingStatus.trialStartDateFormatted ?: "First install"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Valid until: ${billingStatus.trialExpiryFormatted ?: "In 6 months"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Full premium access is unlocked during your 6-month trial. After 6 months, maintain full access for just $10.00/year.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    billingStatus.isTrialExpired && !billingStatus.isProActive -> {
                        // Trial Expired Warning Banner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "6-MONTH FREE TRIAL HAS ENDED",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your 6-month trial expired on ${billingStatus.trialExpiryFormatted}. Subscribe to the Premium Annual Plan for $10.00/year to continue enjoying multi-counter syncing, reports & cloud backup.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    else -> {
                        // Paid Subscription Active Banner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = billingStatus.activePlanTitle.uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = EmeraldGreen
                                    )
                                }
                                Surface(
                                    color = EmeraldGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (billingStatus.activeOrderId != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Order Ref: ${billingStatus.activeOrderId}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (billingStatus.purchaseTimeFormatted != null) {
                                Text(
                                    text = "Activated: ${billingStatus.purchaseTimeFormatted}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Restore & Manage Subscriptions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onRestorePurchases() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("restore_purchases_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Purchases", fontSize = 11.sp)
                    }

                    if (activity != null) {
                        TextButton(
                            onClick = { onOpenSubscriptions(activity) },
                            modifier = Modifier.testTag("manage_play_subs_button")
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = AmberPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Play Subscriptions", fontSize = 11.sp, color = AmberPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Available Google Play Plans List
        Text(
            text = "GOOGLE PLAY SUBSCRIPTION PLAN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        availablePlans.forEach { plan ->
            val isCurrentPaidPlan = when (plan.productId) {
                com.example.data.billing.BillingManager.PRODUCT_ID_LIFETIME -> billingStatus.tier == SubscriptionTier.LIFETIME
                com.example.data.billing.BillingManager.PRODUCT_ID_PREMIUM_ANNUAL -> billingStatus.tier == SubscriptionTier.PREMIUM_ANNUAL
                else -> false
            }

            PlanCard(
                plan = plan,
                isCurrentPlan = isCurrentPaidPlan,
                isTrialActive = billingStatus.isTrialActive,
                onSubscribe = {
                    activity?.let { onLaunchPurchase(it, plan) }
                }
            )
        }

        // Terms & Google Play Disclosure
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFeatureDetails = !showFeatureDetails },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Play Security & 6-Month Free Trial Terms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (showFeatureDetails) "Hide" else "Details >",
                        fontSize = 11.sp,
                        color = AmberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showFeatureDetails) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• 6-Month Free Trial: Full access to all bar stock management, selling stations, attendant shifts, and real-time database sync for 180 days with zero upfront commitment.\n" +
                               "• Premium Annual Subscription: Billed at $10.00/year after the 6-month trial period ends ($0.83/month equivalent).\n" +
                               "• Auto-Renewal & Cancellation: Subscriptions automatically renew through Google Play unless cancelled at least 24 hours before the end of the current period.\n" +
                               "• You can manage or cancel your subscription anytime in the Google Play Store under Account > Payments & Subscriptions.\n" +
                               "• Purchases automatically restore across all your Android devices signed into your Google account.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: BillingPlan,
    isCurrentPlan: Boolean,
    isTrialActive: Boolean,
    onSubscribe: () -> Unit
) {
    val isAnnual = plan.productId == com.example.data.billing.BillingManager.PRODUCT_ID_PREMIUM_ANNUAL
    val borderColor = when {
        isCurrentPlan -> EmeraldGreen
        isAnnual -> AmberPrimary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isCurrentPlan || isAnnual) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.productId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Badge & Pricing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (plan.badge != null) {
                        Surface(
                            color = if (isAnnual) AmberPrimary else Color(0xFF3B82F6),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = plan.badge,
                                color = if (isAnnual) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = plan.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = plan.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = plan.formattedPrice,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAnnual) AmberPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (plan.isSubscription) "Billed annually" else "One-time purchase",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Feature List
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                plan.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isCurrentPlan) EmeraldGreen else AmberPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = feature,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            if (isCurrentPlan) {
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Subscription",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAnnual) AmberPrimary else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("subscribe_button_${plan.productId}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint = if (isAnnual) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAnnual) {
                            if (isTrialActive) "Subscribe ($10.00/yr after 6-Mo Trial)" else "Subscribe for $10.00 / Year"
                        } else {
                            "Purchase Lifetime Pass ($49.99)"
                        },
                        color = if (isAnnual) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
