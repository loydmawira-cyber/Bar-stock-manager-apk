package com.example.ui.screens

import com.example.data.model.UserRole

data class ManualSection(
    val title: String,
    val steps: List<String>,
    val tip: String? = null
)

/** In-app user manuals, one per role. Owners can read all of them; managers also read the attendant one. */
object UserManualContent {

    fun rolesVisibleTo(role: UserRole?): List<UserRole> = when (role) {
        UserRole.OWNER -> listOf(UserRole.OWNER, UserRole.MANAGER, UserRole.ATTENDANT)
        UserRole.MANAGER -> listOf(UserRole.MANAGER, UserRole.ATTENDANT)
        else -> listOf(UserRole.ATTENDANT)
    }

    fun title(role: UserRole): String = when (role) {
        UserRole.OWNER -> "Owner Guide"
        UserRole.MANAGER -> "Manager Guide"
        UserRole.ATTENDANT -> "Attendant Guide"
    }

    fun summary(role: UserRole): String = when (role) {
        UserRole.OWNER -> "Set up the bar, control stock and staff, and read the money reports."
        UserRole.MANAGER -> "Run the day: stock, shifts, disputes, expenses and attendants."
        UserRole.ATTENDANT -> "Start your shift, sell, count, and hand over cleanly."
    }

    fun sections(role: UserRole): List<ManualSection> = when (role) {
        UserRole.OWNER -> ownerSections + commonSections
        UserRole.MANAGER -> managerSections + commonSections
        UserRole.ATTENDANT -> attendantSections + commonSections
    }

    fun asPlainText(role: UserRole): String = buildString {
        appendLine(title(role).uppercase())
        appendLine(summary(role))
        sections(role).forEachIndexed { i, section ->
            appendLine()
            appendLine("${i + 1}. ${section.title}")
            section.steps.forEachIndexed { j, step -> appendLine("   ${j + 1}) $step") }
            section.tip?.let { appendLine("   Tip: $it") }
        }
    }

    private val commonSections = listOf(
        ManualSection(
            title = "Signing in and Quick PIN",
            steps = listOf(
                "Sign in with your email (or phone number) and password.",
                "Forgot your password? Tap Forgot Password on the sign-in screen and a reset link is emailed to you.",
                "To sign in faster, open Settings, then Security & Help, then Set Quick PIN. Choose a 4-digit PIN and enter it twice.",
                "Next time the app opens, enter your PIN instead of your email and password. On a shared phone, tap your name first.",
                "If the app stays in the background for about a minute, it asks for your PIN again. You come back to the same screen.",
                "After 5 wrong PIN tries the PIN is removed. Sign in with your password and set a new one."
            ),
            tip = "Avoid easy PINs like 0000 or 1234. The app will not accept them."
        ),
        ManualSection(
            title = "Your account",
            steps = listOf(
                "Change your password from the profile menu (Change Password) or from Settings.",
                "New accounts must be approved before they can sign in. Pending or revoked accounts cannot enter.",
                "Log Out backs your data up to the cloud and then clears it from this phone. It also removes all Quick PINs on this phone, so you must sign in with email and password afterwards."
            ),
            tip = "To leave the app without typing your password next time, just close it instead of logging out."
        ),
        ManualSection(
            title = "Notifications",
            steps = listOf(
                "The bell shows unread alerts such as stock adjustments, shortages and disputes.",
                "Tap an alert to mark it read, or use Mark all as read."
            )
        )
    )

    private val attendantSections = listOf(
        ManualSection(
            title = "Start your shift (opening stock handover)",
            steps = listOf(
                "On your dashboard, choose the counter you are working at.",
                "The app shows each item with its system balance, one item at a time.",
                "Look at the real stock on the shelf. If it matches, tap Verified OK.",
                "If it does not match, enter the actual count (bottles, plus loose ml for tots). A dispute is logged for management.",
                "Tap Confirm Count & Next Item until every item is done.",
                "Tap All Confirmed OK · Start Shift. You are now responsible for this stock."
            ),
            tip = "Never confirm a count you did not physically check. Once you start, the stock is yours."
        ),
        ManualSection(
            title = "During your shift",
            steps = listOf(
                "The screen shows Selling Active. Stock prices are view-only while you work.",
                "If management adds or removes stock during your shift, an Action Required card appears.",
                "Check the stock really arrived, then tap Confirm Receipt. If it is wrong, dispute it."
            )
        ),
        ManualSection(
            title = "End your shift (closing and handover)",
            steps = listOf(
                "Tap Stop Selling & Enter Closing Stock.",
                "Enter the stock remaining on the counter for each item (bottles and loose ml).",
                "The app works out units sold: (opening + adjustments) minus closing, and the cash you should have.",
                "Enter the sales you collected for each payment method (cash, M-Pesa, card, bank and so on).",
                "Add handover notes for the next person if needed.",
                "The app shows a shortage, a surplus, or Clean handover. Check it carefully.",
                "Tap Submit Closing & Finalize Reconciliation. You get a Shift Reconciliation Receipt and the counter is free for the next attendant."
            ),
            tip = "A shortage is posted to your attendant loss ledger. If you disagree, reject it with a reason (see Shortages)."
        ),
        ManualSection(
            title = "Shortages and disputes",
            steps = listOf(
                "Open Notifications to see a shortage charged to you. You can accept it, or reject it with a reason.",
                "Rejected shortages go to management, who either post it to your shift or waive it (nothing charged).",
                "Under History you can see My Disputes and your Shift History & Reconciliation receipts."
            )
        )
    )

    private val managerSections = listOf(
        ManualSection(
            title = "Daily routine",
            steps = listOf(
                "Start on Overview: check counters, active shifts, open disputes and alerts.",
                "Make sure each counter has stock before attendants start shifts.",
                "During the day, handle restocks, adjustments and disputes.",
                "After each shift, open the Reconciliation Receipt (from Overview or Receipts) and review sales, cash and variance."
            )
        ),
        ManualSection(
            title = "Stock prices and items",
            steps = listOf(
                "Open Stock Prices to add a beverage: name, category, unit type, unit price and case price.",
                "For sale by tot, turn on Enable sale by tot and enter bottle volume (ml), tot size (ml) and tot price.",
                "If stock is already on hand, enter Opening Stock Qty and its cost so day-one cost is accurate.",
                "Tap an item to edit its price or details."
            ),
            tip = "Prices cannot be changed by attendants, and they are locked during a shift."
        ),
        ManualSection(
            title = "Store purchases and restocking",
            steps = listOf(
                "Under Inventory, Store, tap Receive Purchase into Store.",
                "Enter the item, quantity purchased, units per pack, supplier name, receipt or invoice number, and unit cost.",
                "To restock a counter, open it and choose Restock. Pick From Store or Direct from Supplier.",
                "Direct from Supplier needs the supplier name, receipt number and unit cost.",
                "Use Add / Remove Stock to correct a counter. Tap Apply & Notify Attendant so the attendant must confirm it."
            ),
            tip = "Store balances are kept in the item's selling unit."
        ),
        ManualSection(
            title = "Counters",
            steps = listOf(
                "Create a counter with a name (for example Rooftop Bar) and a physical location.",
                "Long-press a counter to edit or delete it.",
                "A counter can only be deleted when it has no stock, or you choose where its stock goes (Store or another counter)."
            )
        ),
        ManualSection(
            title = "Attendants",
            steps = listOf(
                "Open Users to see Approved, Pending and Revoked attendants.",
                "Tap Create Attendant Account. Each account needs a unique email, phone number and a strong password.",
                "Approve people who registered themselves, or revoke access when someone leaves.",
                "Revoked attendants can be restored later. Managers can only manage attendants."
            )
        ),
        ManualSection(
            title = "Disputes and shortages",
            steps = listOf(
                "Open Disputes. Open and Resolved tabs show what is waiting and what is done.",
                "Compare System Expected with the Physical Count, and note whether it came from the previous attendant or a mid-shift adjustment.",
                "Tap Investigate & Resolve Dispute, write your notes, and enter the official counter stock (bottles and loose ml). Confirm.",
                "When an attendant rejects a shortage, you will find it in Notifications. Choose Post to Attendant or Waive."
            )
        ),
        ManualSection(
            title = "Receipts and expenses",
            steps = listOf(
                "Receipts shows shift reconciliations, purchase receipts and recorded expenses.",
                "To record an expense, enter the category, description or vendor, amount, payment method and reference or receipt number. Notes are optional.",
                "Managers and owners can record expenses at any time."
            ),
            tip = "Profit & Loss and Monthly Reports are for the Owner only."
        )
    )

    private val ownerSections = listOf(
        ManualSection(
            title = "First-time setup checklist",
            steps = listOf(
                "Register your bar. This creates your Owner account.",
                "Open Settings and fill in the bar name, location, currency, payment methods and opening hours.",
                "Add your beverages and prices under Stock Prices.",
                "Create your selling counters.",
                "Receive your first purchase into the Store, then restock each counter.",
                "Create manager and attendant accounts under Users.",
                "Set your own Quick PIN under Settings, Security & Help."
            )
        ),
        ManualSection(
            title = "Stock prices and items",
            steps = listOf(
                "Open Stock Prices and tap Add Beverage & Set Prices.",
                "Enter name, category, unit type, unit price and case price.",
                "For sale by tot, turn on Enable sale by tot and enter bottle volume (ml), tot size (ml) and tot price.",
                "If stock is already on hand, enter Opening Stock Qty and cost so your profit figures start correctly.",
                "Tap an item to edit it."
            ),
            tip = "Use Sync on this screen to push your data to the cloud right away."
        ),
        ManualSection(
            title = "Store, purchases and counters",
            steps = listOf(
                "Receive Purchase into Store: item, quantity, units per pack, supplier, receipt or invoice number and unit cost.",
                "Create counters with a name and location. Long-press a counter to edit or delete it.",
                "Restock a counter From Store or Direct from Supplier (supplier name, receipt number and unit cost required).",
                "Use Add / Remove Stock to correct a counter. Tap Apply & Notify Attendant so the attendant must confirm."
            ),
            tip = "A counter can only be deleted when empty, or after you move its stock to the Store or another counter."
        ),
        ManualSection(
            title = "People and permissions",
            steps = listOf(
                "Open Users (User Hierarchy). Only you can create Manager accounts.",
                "Tap Create Manager Account or Create Attendant Account. Each needs a unique email, phone and a strong password.",
                "Use the Approved, Pending and Revoked tabs to approve, revoke, restore or delete accounts.",
                "Managers can manage attendants, stock, disputes and expenses. They cannot see Profit & Loss or Monthly Reports."
            )
        ),
        ManualSection(
            title = "Shifts, disputes and shortages",
            steps = listOf(
                "Overview shows counters, active shifts and open disputes. Tap a shift to see its Reconciliation Receipt.",
                "Open Disputes to investigate and resolve stock differences. Enter notes and the official counter stock.",
                "Rejected shortages appear in Notifications. Choose Post to Attendant to charge it, or Waive to charge nothing."
            )
        ),
        ManualSection(
            title = "Money: receipts, expenses and Profit & Loss",
            steps = listOf(
                "Receipts shows shift reconciliations, purchase receipts and expenses.",
                "Record each expense with category, description or vendor, amount, payment method and reference number.",
                "Open P&L for the Executive Financial Summary: cost of goods (weighted cost), gross margin, operating expenses and net profit or loss.",
                "The same report shows stock valuation and movements, and an item profitability breakdown."
            ),
            tip = "Record expenses and purchases promptly. Reports are only as accurate as the entries."
        ),
        ManualSection(
            title = "Monthly reports",
            steps = listOf(
                "Open Reports from the Overview screen.",
                "Generated monthly reports can be downloaded as PDF and CSV.",
                "Reports are visible to the Owner only."
            )
        ),
        ManualSection(
            title = "Bar profile, backup and subscription",
            steps = listOf(
                "Settings lets you change the bar icon, photo, name, location, phone, currency, payment methods and hours.",
                "Your data is backed up to the cloud automatically as you work.",
                "Subscription plans and billing status are in Settings, and you can manage them through Google Play."
            )
        )
    )
}
