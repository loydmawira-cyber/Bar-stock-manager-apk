package com.example.data.billing

enum class BillingConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    PLAY_STORE_UNAVAILABLE
}

enum class SubscriptionTier {
    FREE_TRIAL,
    PREMIUM_ANNUAL,
    LIFETIME,
    EXPIRED
}

data class BillingPlan(
    val productId: String,
    val title: String,
    val subtitle: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val currencyCode: String,
    val billingPeriod: String, // "Annual", "One-Time"
    val isSubscription: Boolean,
    val badge: String? = null,
    val isPopular: Boolean = false,
    val trialPeriodDescription: String? = null,
    val features: List<String>
)

data class BillingStatus(
    val isProActive: Boolean = true,
    val isTrialActive: Boolean = true,
    val isTrialExpired: Boolean = false,
    val trialDaysRemaining: Int = 180,
    val trialStartDateFormatted: String? = null,
    val trialExpiryFormatted: String? = null,
    val tier: SubscriptionTier = SubscriptionTier.FREE_TRIAL,
    val activePlanTitle: String = "6-Month Free Trial",
    val activeOrderId: String? = null,
    val purchaseTimeFormatted: String? = null,
    val isAutoRenewing: Boolean = false,
    val isSandboxActive: Boolean = false
)
