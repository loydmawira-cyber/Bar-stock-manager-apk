package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BarStockDao
import com.example.data.model.AppNotification
import com.example.data.model.BarProfile
import com.example.data.model.Counter
import com.example.data.model.CounterStock
import com.example.data.model.Dispute
import com.example.data.model.Item
import com.example.data.model.ItemCategory
import com.example.data.model.NotificationType
import com.example.data.model.Reconciliation
import com.example.data.model.ReconciliationType
import com.example.data.model.Shift
import com.example.data.model.ShiftClosing
import com.example.data.model.ShiftStatus
import com.example.data.model.StockAdjustment
import com.example.data.model.StockVerification
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BarProfile::class,
        User::class,
        Counter::class,
        Item::class,
        CounterStock::class,
        Shift::class,
        StockVerification::class,
        ShiftClosing::class,
        Dispute::class,
        StockAdjustment::class,
        Reconciliation::class,
        AppNotification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BarStockDatabase : RoomDatabase() {
    abstract fun barStockDao(): BarStockDao

    companion object {
        @Volatile
        private var INSTANCE: BarStockDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): BarStockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BarStockDatabase::class.java,
                    "bar_stock_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(BarStockDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class BarStockDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.barStockDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val dao = database.barStockDao()
                        if (dao.getUserByEmail("admin@savannahbar.co.ke") == null) {
                            populateInitialData(dao)
                        }
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: BarStockDao) {
            // Seed Bar Profile
            dao.insertOrUpdateBarProfile(
                BarProfile(
                    id = 1L,
                    barName = "The Savannah Taphouse & Lounge",
                    location = "Waiyaki Way, Westlands, Nairobi",
                    iconType = "cocktail",
                    customPhotoUri = null,
                    contactPhone = "+254 700 000 000",
                    currencyCode = "KES",
                    currencySymbol = "KSh",
                    managerName = "Alex Mwangi",
                    openingHours = "2:00 PM - 4:00 AM",
                    isRegistered = true
                )
            )

            // Seed Users
            val adminId = dao.insertUser(
                User(
                    name = "Alex Mwangi (Admin)",
                    role = UserRole.ADMIN,
                    status = UserStatus.APPROVED,
                    email = "admin@savannahbar.co.ke",
                    phone = "+254 711 000111",
                    password = "admin"
                )
            )

            val attendant1Id = dao.insertUser(
                User(
                    name = "John Kamau",
                    role = UserRole.ATTENDANT,
                    status = UserStatus.APPROVED,
                    email = "john@savannahbar.co.ke",
                    phone = "+254 722 000222",
                    password = "pass"
                )
            )

            val attendant2Id = dao.insertUser(
                User(
                    name = "Sarah Wanjiku",
                    role = UserRole.ATTENDANT,
                    status = UserStatus.APPROVED,
                    email = "sarah@savannahbar.co.ke",
                    phone = "+254 733 000333",
                    password = "pass"
                )
            )

            dao.insertUser(
                User(
                    name = "Marcus Otieno",
                    role = UserRole.ATTENDANT,
                    status = UserStatus.PENDING,
                    email = "marcus@savannahbar.co.ke",
                    phone = "+254 744 000444",
                    password = "pass"
                )
            )

            // Seed Catalog Items - Typical Kenyan Bar Stock
            val itemTusker = dao.insertItem(
                Item(
                    name = "Tusker Lager 500ml",
                    category = ItemCategory.BEER,
                    unitPrice = 300.00,
                    casePrice = 7000.00,
                    unitType = "Bottle",
                    description = "Kenya's Finest Lager"
                )
            )

            val itemTuskerMalt = dao.insertItem(
                Item(
                    name = "Tusker Malt 330ml",
                    category = ItemCategory.BEER,
                    unitPrice = 350.00,
                    casePrice = 8000.00,
                    unitType = "Bottle",
                    description = "Premium Malt Lager"
                )
            )

            val itemWhiteCap = dao.insertItem(
                Item(
                    name = "White Cap Crisp 500ml",
                    category = ItemCategory.BEER,
                    unitPrice = 320.00,
                    casePrice = 7500.00,
                    unitType = "Bottle",
                    description = "Distinctive refreshing beer"
                )
            )

            val itemGuinness = dao.insertItem(
                Item(
                    name = "Guinness FES 500ml",
                    category = ItemCategory.BEER,
                    unitPrice = 350.00,
                    casePrice = 8200.00,
                    unitType = "Bottle",
                    description = "Rich African Stout"
                )
            )

            val itemGilbeys = dao.insertItem(
                Item(
                    name = "Gilbey's Gin 750ml",
                    category = ItemCategory.SPIRIT,
                    unitPrice = 1800.00,
                    casePrice = 20000.00,
                    unitType = "Bottle",
                    description = "London Dry Gin"
                )
            )

            val itemGilbeysTot = dao.insertItem(
                Item(
                    name = "Gilbey's Gin Tot",
                    category = ItemCategory.TOT,
                    unitPrice = 150.00,
                    casePrice = 3000.00,
                    unitType = "Shot / Tot",
                    description = "30ml measure shot"
                )
            )

            val itemChrome = dao.insertItem(
                Item(
                    name = "Chrome Vodka 750ml",
                    category = ItemCategory.SPIRIT,
                    unitPrice = 1200.00,
                    casePrice = 13500.00,
                    unitType = "Bottle",
                    description = "Smooth Kenyan Vodka"
                )
            )

            val itemChromeTot = dao.insertItem(
                Item(
                    name = "Chrome Vodka Tot",
                    category = ItemCategory.TOT,
                    unitPrice = 100.00,
                    casePrice = 2200.00,
                    unitType = "Shot / Tot",
                    description = "30ml measure shot"
                )
            )

            val itemKenyaCane = dao.insertItem(
                Item(
                    name = "Kenya Cane 750ml",
                    category = ItemCategory.SPIRIT,
                    unitPrice = 1100.00,
                    casePrice = 12000.00,
                    unitType = "Bottle",
                    description = "The Spirit of Kenya"
                )
            )

            val itemCoke = dao.insertItem(
                Item(
                    name = "Coca-Cola 300ml",
                    category = ItemCategory.SOFT_DRINK,
                    unitPrice = 100.00,
                    casePrice = 2000.00,
                    unitType = "Bottle",
                    description = "Refreshing Soda"
                )
            )

            // Seed Counters
            val c1Id = dao.insertCounter(
                Counter(
                    name = "Main Counter – Terrace",
                    location = "Ground Floor Central"
                )
            )

            val c2Id = dao.insertCounter(
                Counter(
                    name = "Garden Bar",
                    location = "Outdoor Area"
                )
            )

            val c3Id = dao.insertCounter(
                Counter(
                    name = "Executive Lounge",
                    location = "1st Floor VIP"
                )
            )

            // Seed Stock for Counter 1 (Main Bar)
            val c1Stocks = listOf(
                CounterStock(counterId = c1Id, itemId = itemTusker, currentQuantity = 120),
                CounterStock(counterId = c1Id, itemId = itemTuskerMalt, currentQuantity = 60),
                CounterStock(counterId = c1Id, itemId = itemWhiteCap, currentQuantity = 48),
                CounterStock(counterId = c1Id, itemId = itemGuinness, currentQuantity = 48),
                CounterStock(counterId = c1Id, itemId = itemGilbeys, currentQuantity = 12),
                CounterStock(counterId = c1Id, itemId = itemGilbeysTot, currentQuantity = 100),
                CounterStock(counterId = c1Id, itemId = itemChrome, currentQuantity = 15),
                CounterStock(counterId = c1Id, itemId = itemChromeTot, currentQuantity = 120),
                CounterStock(counterId = c1Id, itemId = itemKenyaCane, currentQuantity = 20),
                CounterStock(counterId = c1Id, itemId = itemCoke, currentQuantity = 150)
            )
            dao.insertCounterStocks(c1Stocks)

            // Seed Stock for Counter 2 (Garden Bar)
            val c2Stocks = listOf(
                CounterStock(counterId = c2Id, itemId = itemTusker, currentQuantity = 48),
                CounterStock(counterId = c2Id, itemId = itemWhiteCap, currentQuantity = 36),
                CounterStock(counterId = c2Id, itemId = itemGuinness, currentQuantity = 24),
                CounterStock(counterId = c2Id, itemId = itemChromeTot, currentQuantity = 40),
                CounterStock(counterId = c2Id, itemId = itemCoke, currentQuantity = 50)
            )
            dao.insertCounterStocks(c2Stocks)

            // Seed Stock for Counter 3 (VIP Lounge)
            val c3Stocks = listOf(
                CounterStock(counterId = c3Id, itemId = itemTuskerMalt, currentQuantity = 36),
                CounterStock(counterId = c3Id, itemId = itemGilbeys, currentQuantity = 12),
                CounterStock(counterId = c3Id, itemId = itemGilbeysTot, currentQuantity = 90),
                CounterStock(counterId = c3Id, itemId = itemTusker, currentQuantity = 24),
                CounterStock(counterId = c3Id, itemId = itemGuinness, currentQuantity = 12)
            )
            dao.insertCounterStocks(c3Stocks)

            // Seed Sample Historical Shift & Reconciliation for John
            val prevShiftTime = System.currentTimeMillis() - 86400000L // Yesterday
            val prevShiftId = dao.insertShift(
                Shift(
                    counterId = c1Id,
                    counterName = "Main Counter – Terrace",
                    attendantId = attendant1Id,
                    attendantName = "John Kamau",
                    startTime = prevShiftTime - 28800000L,
                    endTime = prevShiftTime,
                    status = ShiftStatus.CLOSED,
                    totalExpectedSales = 24500.00,
                    totalSubmittedCash = 24400.00,
                    variance = -100.00,
                    reconciliationType = ReconciliationType.LOSS,
                    notes = "Evening shift closing. Small shortage of KSh 100.00."
                )
            )

            dao.insertReconciliation(
                Reconciliation(
                    shiftId = prevShiftId,
                    attendantId = attendant1Id,
                    attendantName = "John Kamau",
                    counterName = "Main Counter – Terrace",
                    expectedTotal = 24500.00,
                    submittedTotal = 24400.00,
                    variance = -100.00,
                    type = ReconciliationType.LOSS,
                    date = prevShiftTime,
                    monthYear = "2026-09",
                    notes = "Shift #$prevShiftId Shortage"
                )
            )

            // Seed Welcome Notifications
            dao.insertNotification(
                AppNotification(
                    targetRole = UserRole.ADMIN,
                    type = NotificationType.NEW_SIGNUP,
                    title = "New Attendant Sign-up",
                    message = "Marcus King has registered and is awaiting admin approval.",
                    relatedId = 4L
                )
            )

            dao.insertNotification(
                AppNotification(
                    targetUserId = attendant1Id,
                    targetRole = UserRole.ATTENDANT,
                    type = NotificationType.ACCOUNT_APPROVED,
                    title = "Account Approved",
                    message = "Welcome to Bar Stock Manager! Your attendant profile is active."
                )
            )
        }
    }
}
