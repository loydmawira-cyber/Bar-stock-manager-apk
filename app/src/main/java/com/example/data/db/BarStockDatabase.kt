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
import com.example.data.model.ItemCostSnapshot
import com.example.data.model.NotificationType
import com.example.data.model.Reconciliation
import com.example.data.model.ReconciliationType
import com.example.data.model.Shift
import com.example.data.model.ShiftClosing
import com.example.data.model.ShiftStatus
import com.example.data.model.StockAdjustment
import com.example.data.model.StockVerification
import com.example.data.model.StoreStock
import com.example.data.model.PurchaseReceipt
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.Expense

class UserRoleConverter {
    @TypeConverter
    fun toUserRole(value: String?): UserRole {
        if (value == null) return UserRole.ATTENDANT
        return when (value.uppercase()) {
            "ADMIN" -> UserRole.OWNER
            "OWNER" -> UserRole.OWNER
            "MANAGER" -> UserRole.MANAGER
            "ATTENDANT" -> UserRole.ATTENDANT
            else -> UserRole.ATTENDANT
        }
    }

    @TypeConverter
    fun fromUserRole(role: UserRole?): String {
        return (role ?: UserRole.ATTENDANT).name
    }
}

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
        AppNotification::class,
        StoreStock::class,
        PurchaseReceipt::class,
        Expense::class,
        ItemCostSnapshot::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(UserRoleConverter::class)
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
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .addCallback(BarStockDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE shift_closings ADD COLUMN bottleVolumeMl INTEGER NOT NULL DEFAULT 0"
                )
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
                        if (dao.getUserByEmail("admin@savannahbar.co.ke") != null) {
                            clearLegacyDemoData(dao)
                            populateInitialData(dao)
                        }
                    }
                }
            }
        }

        private suspend fun clearLegacyDemoData(dao: BarStockDao) {
            dao.clearNotifications()
            dao.clearReconciliations()
            dao.clearStockAdjustments()
            dao.clearDisputes()
            dao.clearShiftClosings()
            dao.clearStockVerifications()
            dao.clearShifts()
            dao.clearCounterStocks()
            dao.clearStoreStocks()
            dao.clearPurchaseReceipts()
            dao.clearItemCostSnapshots()
            dao.clearCounters()
            dao.clearItems()
            dao.clearUsers()
            dao.clearBarProfile()
        }

        suspend fun populateInitialData(dao: BarStockDao) {
            // New installations start with only a starter profile, one counter,
            // and beer products. No demo users, sales, shifts, or notifications.
            dao.insertOrUpdateBarProfile(
                BarProfile(
                    id = 1L,
                    barName = "Your Bar",
                    location = "",
                    contactPhone = "",
                    currencyCode = "USD",
                    currencySymbol = "$",
                    managerName = "",
                    openingHours = "",
                    isRegistered = false
                )
            )

            val items = listOf(
                Item(name = "Lager Beer 500ml", category = ItemCategory.BEER, unitPrice = 0.0, casePrice = 0.0, unitType = "Bottle"),
                Item(name = "Premium Beer 500ml", category = ItemCategory.BEER, unitPrice = 0.0, casePrice = 0.0, unitType = "Bottle"),
                Item(name = "Stout Beer 500ml", category = ItemCategory.BEER, unitPrice = 0.0, casePrice = 0.0, unitType = "Bottle"),
                Item(name = "Cider 330ml", category = ItemCategory.BEER, unitPrice = 0.0, casePrice = 0.0, unitType = "Bottle")
            )
            val itemIds = items.map { dao.insertItem(it) }
            val counterId = dao.insertCounter(
                Counter(name = "Main Counter", location = "Main Bar Area")
            )
            dao.insertCounterStocks(
                itemIds.map { itemId ->
                    CounterStock(
                        counterId = counterId,
                        itemId = itemId,
                        currentQuantity = 0,
                        minThreshold = 0
                    )
                }
            )
        }
    }
}
