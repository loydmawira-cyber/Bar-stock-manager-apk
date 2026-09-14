package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : PurchasesUpdatedListener {

    private val tag = "BillingManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("google_billing_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PRODUCT_ID_PREMIUM_ANNUAL = "bar_premium_annual"
        const val PRODUCT_ID_LIFETIME = "bar_lifetime_license"

        const val TRIAL_DURATION_DAYS = 180L
        const val TRIAL_DURATION_MS = TRIAL_DURATION_DAYS * 24L * 60L * 60L * 1000L

        val DEFAULT_PLANS = listOf(
            BillingPlan(
                productId = PRODUCT_ID_PREMIUM_ANNUAL,
                title = "Bar Stock Premium Annual",
                subtitle = "6 Months Free Trial included, then $10.00/year",
                formattedPrice = "$10.00 / yr",
                priceAmountMicros = 10000000L,
                currencyCode = "USD",
                billingPeriod = "Annual",
                isSubscription = true,
                badge = "6-MONTH FREE TRIAL",
                isPopular = true,
                trialPeriodDescription = "6 Months Free Trial",
                features = listOf(
                    "6 Months 100% Free Trial on all features",
                    "Unlimited selling counters & attendant stations",
                    "Real-time cloud database backup & auto-sync",
                    "Stock auditing, physical counts & discrepancy reports",
                    "Shift closing receipts, thermal printouts & logs",
                    "Only $10.00 / year ($0.83/mo) after 6 months"
                )
            ),
            BillingPlan(
                productId = PRODUCT_ID_LIFETIME,
                title = "Lifetime Enterprise License",
                subtitle = "One-time payment, never pay again",
                formattedPrice = "$49.99",
                priceAmountMicros = 49990000L,
                currencyCode = "USD",
                billingPeriod = "One-Time",
                isSubscription = false,
                badge = "LIFETIME PASS",
                isPopular = false,
                trialPeriodDescription = null,
                features = listOf(
                    "Pay once, use forever on all Android devices",
                    "Zero recurring annual subscription fees",
                    "Unlimited bars, counters, shifts & attendant accounts",
                    "Permanent lifetime cloud backup & database sync",
                    "All future updates & POS feature additions included"
                )
            )
        )
    }

    private val _connectionState = MutableStateFlow(BillingConnectionState.CONNECTING)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _billingStatus = MutableStateFlow(calculateCurrentStatus())
    val billingStatus: StateFlow<BillingStatus> = _billingStatus.asStateFlow()

    private val _availablePlans = MutableStateFlow(DEFAULT_PLANS)
    val availablePlans: StateFlow<List<BillingPlan>> = _availablePlans.asStateFlow()

    private val _billingMessages = MutableSharedFlow<String>()
    val billingMessages: SharedFlow<String> = _billingMessages.asSharedFlow()

    private var billingClient: BillingClient? = null
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    init {
        ensureTrialInitialized()
        _billingStatus.value = calculateCurrentStatus()
        initializeBillingClient()
    }

    private fun ensureTrialInitialized() {
        if (!prefs.contains("trial_start_timestamp")) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong("trial_start_timestamp", now).apply()
            Log.d(tag, "Initialized 6-month free trial starting at $now")
        }
    }

    private fun calculateCurrentStatus(): BillingStatus {
        val isPaidActive = prefs.getBoolean("is_paid_active", false)
        val isForceExpired = prefs.getBoolean("trial_force_expired", false)
        val trialStart = prefs.getLong("trial_start_timestamp", System.currentTimeMillis())
        val trialEnd = trialStart + TRIAL_DURATION_MS
        val now = System.currentTimeMillis()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val trialStartFormatted = dateFormat.format(Date(trialStart))
        val trialEndFormatted = dateFormat.format(Date(trialEnd))

        val diffMs = trialEnd - now
        val daysRemaining = max(0, (diffMs / (1000L * 60L * 60L * 24L)).toInt())
        val isTrialStillActive = diffMs > 0 && !isForceExpired

        if (isPaidActive) {
            val tierStr = prefs.getString("tier", SubscriptionTier.PREMIUM_ANNUAL.name) ?: SubscriptionTier.PREMIUM_ANNUAL.name
            val tier = try {
                SubscriptionTier.valueOf(tierStr)
            } catch (e: Exception) {
                SubscriptionTier.PREMIUM_ANNUAL
            }
            val activePlanTitle = prefs.getString("active_plan_title", "Premium Annual Pass") ?: "Premium Annual Pass"
            val orderId = prefs.getString("active_order_id", null)
            val purchaseTime = prefs.getString("purchase_time", null)
            val autoRenewing = prefs.getBoolean("is_auto_renewing", true)
            val isSandbox = prefs.getBoolean("is_sandbox", false)

            return BillingStatus(
                isProActive = true,
                isTrialActive = false,
                isTrialExpired = false,
                trialDaysRemaining = 0,
                trialStartDateFormatted = trialStartFormatted,
                trialExpiryFormatted = trialEndFormatted,
                tier = tier,
                activePlanTitle = activePlanTitle,
                activeOrderId = orderId,
                purchaseTimeFormatted = purchaseTime,
                isAutoRenewing = autoRenewing,
                isSandboxActive = isSandbox
            )
        }

        if (isTrialStillActive) {
            return BillingStatus(
                isProActive = true,
                isTrialActive = true,
                isTrialExpired = false,
                trialDaysRemaining = daysRemaining,
                trialStartDateFormatted = trialStartFormatted,
                trialExpiryFormatted = trialEndFormatted,
                tier = SubscriptionTier.FREE_TRIAL,
                activePlanTitle = "6-Month Free Trial ($daysRemaining days remaining)",
                activeOrderId = null,
                purchaseTimeFormatted = null,
                isAutoRenewing = false,
                isSandboxActive = false
            )
        } else {
            return BillingStatus(
                isProActive = false,
                isTrialActive = false,
                isTrialExpired = true,
                trialDaysRemaining = 0,
                trialStartDateFormatted = trialStartFormatted,
                trialExpiryFormatted = trialEndFormatted,
                tier = SubscriptionTier.EXPIRED,
                activePlanTitle = "6-Month Free Trial Expired",
                activeOrderId = null,
                purchaseTimeFormatted = null,
                isAutoRenewing = false,
                isSandboxActive = false
            )
        }
    }

    private fun initializeBillingClient() {
        try {
            val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()

            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(pendingPurchasesParams)
                .build()

            startBillingConnection()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize BillingClient: ${e.message}", e)
            _connectionState.value = BillingConnectionState.PLAY_STORE_UNAVAILABLE
        }
    }

    fun startBillingConnection() {
        val client = billingClient ?: return
        _connectionState.value = BillingConnectionState.CONNECTING

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(tag, "BillingClient connected successfully")
                    _connectionState.value = BillingConnectionState.CONNECTED
                    queryAvailableProducts()
                    queryActivePurchases()
                } else {
                    Log.w(tag, "Billing setup failed code: ${billingResult.responseCode}, ${billingResult.debugMessage}")
                    _connectionState.value = BillingConnectionState.PLAY_STORE_UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(tag, "Billing service disconnected")
                _connectionState.value = BillingConnectionState.DISCONNECTED
            }
        })
    }

    private fun queryAvailableProducts() {
        val client = billingClient ?: return
        if (!client.isReady) return

        // 1. Query Subscriptions (Annual $10 with 6-month free trial offer)
        val subProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PREMIUM_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subProductList)
            .build()

        client.queryProductDetailsAsync(subParams) { billingResult, queryProductDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryProductDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                }
                updatePlansWithPlayDetails()
            }
        }

        // 2. Query In-App Purchases (Lifetime License)
        val inAppProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProductList)
            .build()

        client.queryProductDetailsAsync(inAppParams) { billingResult, queryProductDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryProductDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                }
                updatePlansWithPlayDetails()
            }
        }
    }

    private fun updatePlansWithPlayDetails() {
        val updated = DEFAULT_PLANS.map { plan ->
            val details = productDetailsMap[plan.productId]
            if (details != null) {
                val formattedPrice = when {
                    details.productType == BillingClient.ProductType.SUBS -> {
                        val offer = details.subscriptionOfferDetails?.firstOrNull()
                        val pricingPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        pricingPhase?.formattedPrice ?: plan.formattedPrice
                    }
                    details.productType == BillingClient.ProductType.INAPP -> {
                        details.oneTimePurchaseOfferDetails?.formattedPrice ?: plan.formattedPrice
                    }
                    else -> plan.formattedPrice
                }
                plan.copy(
                    formattedPrice = formattedPrice,
                    title = details.name.ifBlank { plan.title },
                    subtitle = details.description.ifBlank { plan.subtitle }
                )
            } else {
                plan
            }
        }
        _availablePlans.value = updated
    }

    fun queryActivePurchases() {
        val client = billingClient ?: return
        if (!client.isReady) return

        // Query Active Subs
        val subParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        client.queryPurchasesAsync(subParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchasesList(purchases)
            }
        }

        // Query In-App Purchases
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(inAppParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchasesList(purchases)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    handlePurchasesList(purchases)
                    emitMessage("Google Play subscription activated successfully!")
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                emitMessage("Purchase cancelled.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryActivePurchases()
                emitMessage("Subscription already owned. Restored successfully.")
            }
            else -> {
                emitMessage("Purchase failed: ${billingResult.debugMessage.ifBlank { "Code ${billingResult.responseCode}" }}")
            }
        }
    }

    private fun handlePurchasesList(purchases: List<Purchase>) {
        var highestTier = SubscriptionTier.FREE_TRIAL
        var isPaid = false
        var activePlanTitle = "Premium Annual Pass"
        var activeOrderId: String? = null
        var isAutoRenewing = false
        var purchaseTime = 0L

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }

                val product = purchase.products.firstOrNull() ?: continue
                val orderId = purchase.orderId ?: "GPA.PLAY-${purchase.purchaseTime}"
                val autoRenewing = purchase.isAutoRenewing

                when (product) {
                    PRODUCT_ID_LIFETIME -> {
                        highestTier = SubscriptionTier.LIFETIME
                        isPaid = true
                        activePlanTitle = "Lifetime Enterprise License"
                        activeOrderId = orderId
                        purchaseTime = purchase.purchaseTime
                    }
                    PRODUCT_ID_PREMIUM_ANNUAL -> {
                        if (highestTier != SubscriptionTier.LIFETIME) {
                            highestTier = SubscriptionTier.PREMIUM_ANNUAL
                            isPaid = true
                            activePlanTitle = "Premium Annual Pass ($10/yr)"
                            activeOrderId = orderId
                            isAutoRenewing = autoRenewing
                            purchaseTime = purchase.purchaseTime
                        }
                    }
                }
            }
        }

        if (isPaid) {
            val dateStr = if (purchaseTime > 0) {
                SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(purchaseTime))
            } else null

            prefs.edit().apply {
                putBoolean("is_paid_active", true)
                putString("tier", highestTier.name)
                putString("active_plan_title", activePlanTitle)
                putString("active_order_id", activeOrderId)
                putString("purchase_time", dateStr)
                putBoolean("is_auto_renewing", isAutoRenewing)
                putBoolean("is_sandbox", false)
                apply()
            }
            _billingStatus.value = calculateCurrentStatus()
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        client.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(tag, "Purchase acknowledged: ${purchase.orderId}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, plan: BillingPlan) {
        val client = billingClient
        val details = productDetailsMap[plan.productId]

        if (client != null && client.isReady && details != null) {
            val productDetailsParamsList = when (details.productType) {
                BillingClient.ProductType.SUBS -> {
                    val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offerToken)
                            .build()
                    )
                }
                else -> {
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build()
                    )
                }
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val result = client.launchBillingFlow(activity, billingFlowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                emitMessage("Failed to start Google Play purchase: ${result.debugMessage}")
            }
        } else {
            emitMessage("Google Play Billing is unavailable. Please try again later.")
        }
    }

    fun activateSandboxPlan(plan: BillingPlan) {
        val tier = when (plan.productId) {
            PRODUCT_ID_LIFETIME -> SubscriptionTier.LIFETIME
            else -> SubscriptionTier.PREMIUM_ANNUAL
        }

        val dateStr = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date())
        prefs.edit().apply {
            putBoolean("is_paid_active", true)
            putString("tier", tier.name)
            putString("active_plan_title", "${plan.title} ($10.00/yr)")
            putString("active_order_id", "GPA.DEV-TEST-${System.currentTimeMillis() % 1000000}")
            putString("purchase_time", dateStr)
            putBoolean("is_auto_renewing", plan.isSubscription)
            putBoolean("is_sandbox", true)
            apply()
        }
        _billingStatus.value = calculateCurrentStatus()
        emitMessage("Activated ${plan.title} successfully ($10/yr)!")
    }

    fun simulateTrialActive() {
        prefs.edit().apply {
            putBoolean("is_paid_active", false)
            putBoolean("trial_force_expired", false)
            putLong("trial_start_timestamp", System.currentTimeMillis())
            putString("tier", SubscriptionTier.FREE_TRIAL.name)
            putString("active_plan_title", "6-Month Free Trial (180 days remaining)")
            remove("active_order_id")
            remove("purchase_time")
            putBoolean("is_auto_renewing", false)
            putBoolean("is_sandbox", false)
            apply()
        }
        _billingStatus.value = calculateCurrentStatus()
        emitMessage("6-Month Free Trial activated (180 days remaining).")
    }

    fun simulateTrialExpired() {
        prefs.edit().apply {
            putBoolean("is_paid_active", false)
            putBoolean("trial_force_expired", true)
            putString("tier", SubscriptionTier.EXPIRED.name)
            putString("active_plan_title", "6-Month Free Trial Expired")
            remove("active_order_id")
            remove("purchase_time")
            putBoolean("is_auto_renewing", false)
            putBoolean("is_sandbox", false)
            apply()
        }
        _billingStatus.value = calculateCurrentStatus()
        emitMessage("Simulating: 6-Month Free Trial has expired. Subscribe for $10/year.")
    }

    fun restorePurchases() {
        val client = billingClient
        if (client != null && client.isReady) {
            queryActivePurchases()
            emitMessage("Checking Google Play for active subscriptions...")
        } else {
            _billingStatus.value = calculateCurrentStatus()
            if (_billingStatus.value.isProActive && !_billingStatus.value.isTrialActive) {
                emitMessage("Restored active license: ${_billingStatus.value.activePlanTitle}")
            } else if (_billingStatus.value.isTrialActive) {
                emitMessage("Active: 6-Month Free Trial (${_billingStatus.value.trialDaysRemaining} days remaining).")
            } else {
                emitMessage("Free trial expired. Please subscribe to Premium for $10/year.")
            }
        }
    }

    fun openGooglePlaySubscriptions(activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/account/subscriptions")
                setPackage("com.android.vending")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                activity.startActivity(webIntent)
            } catch (ex: Exception) {
                emitMessage("Unable to open Google Play Store.")
            }
        }
    }

    fun cancelOrResetSubscription() {
        prefs.edit().apply {
            putBoolean("is_paid_active", false)
            putBoolean("trial_force_expired", false)
            remove("active_order_id")
            remove("purchase_time")
            putBoolean("is_auto_renewing", false)
            putBoolean("is_sandbox", false)
            apply()
        }
        _billingStatus.value = calculateCurrentStatus()
        emitMessage("Subscription reset. Current status updated.")
    }

    private fun emitMessage(msg: String) {
        coroutineScope.launch(Dispatchers.Main) {
            _billingMessages.emit(msg)
        }
    }

    fun destroy() {
        try {
            billingClient?.endConnection()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
