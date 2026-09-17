package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppNotification
import com.example.data.model.Counter
import com.example.data.model.CounterStock
import com.example.data.model.CounterStockWithItem
import com.example.data.model.Dispute
import com.example.data.model.DisputeStatus
import com.example.data.model.Item
import com.example.data.model.Reconciliation
import com.example.data.model.Shift
import com.example.data.model.ShiftClosing
import com.example.data.model.ShiftStatus
import com.example.data.model.StockAdjustment
import com.example.data.model.StockVerification
import com.example.data.model.StoreStock
import com.example.data.model.StoreStockWithItem
import com.example.data.model.PurchaseReceipt
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BarStockDao {

    // --- Bar Profile ---
    @Query("SELECT * FROM bar_profile WHERE id = 1 LIMIT 1")
    fun getBarProfile(): Flow<com.example.data.model.BarProfile?>

    @Query("SELECT * FROM bar_profile WHERE id = 1 LIMIT 1")
    suspend fun getBarProfileSync(): com.example.data.model.BarProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBarProfile(profile: com.example.data.model.BarProfile)

    // --- Users ---
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersSync(): List<User>

    @Query("SELECT * FROM users WHERE status = :status ORDER BY createdAt DESC")
    fun getUsersByStatus(status: UserStatus): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
    fun getUsersByRole(role: UserRole): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE TRIM(phone) = TRIM(:phone) LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE (LOWER(TRIM(email)) = LOWER(TRIM(:identifier)) OR TRIM(phone) = TRIM(:identifier)) LIMIT 1")
    suspend fun getUserByIdentifier(identifier: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: UserStatus)

    @Query("UPDATE users SET password = :password WHERE id = :userId")
    suspend fun updateUserPassword(userId: Long, password: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)

    // --- Items ---
    @Query("SELECT * FROM items ORDER BY category ASC, name ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items")
    suspend fun getAllItemsSync(): List<Item>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    // --- Counters ---
    @Query("SELECT * FROM counters ORDER BY name ASC")
    fun getAllCounters(): Flow<List<Counter>>

    @Query("SELECT * FROM counters")
    suspend fun getAllCountersSync(): List<Counter>

    @Query("SELECT * FROM counters WHERE id = :counterId LIMIT 1")
    suspend fun getCounterById(counterId: Long): Counter?

    @Query("SELECT * FROM counters WHERE id = :counterId LIMIT 1")
    fun getCounterByIdFlow(counterId: Long): Flow<Counter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounter(counter: Counter): Long

    @Update
    suspend fun updateCounter(counter: Counter)

    @Query("DELETE FROM counters WHERE id = :counterId")
    suspend fun deleteCounter(counterId: Long)

    @Query("UPDATE counters SET activeAttendantId = :attendantId, activeAttendantName = :attendantName, activeShiftId = :shiftId WHERE id = :counterId")
    suspend fun updateCounterActiveShift(counterId: Long, attendantId: Long?, attendantName: String?, shiftId: Long?)

    // --- Counter Stocks ---
    @Query("SELECT * FROM counter_stocks")
    suspend fun getAllCounterStocksSync(): List<CounterStock>
    
    @Query("""
        SELECT cs.id AS stockId, cs.counterId, cs.itemId, i.name AS itemName, 
               i.category, i.unitPrice, i.casePrice, i.unitType, cs.currentQuantity, cs.minThreshold 
        FROM counter_stocks cs 
        INNER JOIN items i ON cs.itemId = i.id 
        WHERE cs.counterId = :counterId 
        ORDER BY i.category ASC, i.name ASC
    """)
    fun getCounterStocksWithItems(counterId: Long): Flow<List<CounterStockWithItem>>

    @Query("""
        SELECT cs.id AS stockId, cs.counterId, cs.itemId, i.name AS itemName, 
               i.category, i.unitPrice, i.casePrice, i.unitType, cs.currentQuantity, cs.minThreshold 
        FROM counter_stocks cs 
        INNER JOIN items i ON cs.itemId = i.id 
        WHERE cs.counterId = :counterId 
        ORDER BY i.category ASC, i.name ASC
    """)
    suspend fun getCounterStocksWithItemsSync(counterId: Long): List<CounterStockWithItem>

    @Query("SELECT * FROM counter_stocks WHERE counterId = :counterId AND itemId = :itemId LIMIT 1")
    suspend fun getCounterStock(counterId: Long, itemId: Long): CounterStock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterStock(stock: CounterStock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterStocks(stocks: List<CounterStock>)

    @Query("UPDATE counter_stocks SET currentQuantity = :newQuantity WHERE counterId = :counterId AND itemId = :itemId")
    suspend fun updateStockQuantity(counterId: Long, itemId: Long, newQuantity: Int)

    @Query("DELETE FROM counter_stocks WHERE counterId = :counterId AND itemId = :itemId")
    suspend fun deleteCounterStock(counterId: Long, itemId: Long)

    // --- Store Stock ---
    @Query("""
        SELECT ss.id AS stockId, ss.itemId, i.name AS itemName,
               i.category, i.unitType, ss.currentQuantity, ss.minThreshold
        FROM store_stocks ss
        INNER JOIN items i ON ss.itemId = i.id
        ORDER BY i.category ASC, i.name ASC
    """)
    fun getAllStoreStockWithItems(): Flow<List<StoreStockWithItem>>

    @Query("SELECT * FROM store_stocks WHERE itemId = :itemId LIMIT 1")
    suspend fun getStoreStock(itemId: Long): StoreStock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreStock(stock: StoreStock): Long

    @Query("SELECT * FROM store_stocks")
    suspend fun getAllStoreStocksSync(): List<StoreStock>

    @Query("DELETE FROM store_stocks")
    suspend fun clearStoreStocks()

    // --- Generated Purchase Receipts ---
    @Query("SELECT * FROM purchase_receipts ORDER BY purchaseDate DESC, id DESC")
    fun getAllPurchaseReceipts(): Flow<List<PurchaseReceipt>>

    @Query("SELECT * FROM purchase_receipts")
    suspend fun getAllPurchaseReceiptsSync(): List<PurchaseReceipt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseReceipt(receipt: PurchaseReceipt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseReceipts(receipts: List<PurchaseReceipt>)

    @Query("DELETE FROM purchase_receipts")
    suspend fun clearPurchaseReceipts()

    // --- Daily Item Cost Snapshots (last-purchase-cost-in-effect, frozen per day) ---
    @Query("SELECT * FROM item_cost_snapshots WHERE itemId = :itemId AND dayStart <= :dayStart ORDER BY dayStart DESC LIMIT 1")
    suspend fun getCostSnapshotOnOrBefore(itemId: Long, dayStart: Long): com.example.data.model.ItemCostSnapshot?

    @Query("SELECT MAX(dayStart) FROM item_cost_snapshots WHERE itemId = :itemId")
    suspend fun getLatestSnapshotDay(itemId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCostSnapshot(snapshot: com.example.data.model.ItemCostSnapshot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCostSnapshots(snapshots: List<com.example.data.model.ItemCostSnapshot>)

    @Query("DELETE FROM item_cost_snapshots")
    suspend fun clearItemCostSnapshots()

    // --- Shifts ---
    @Query("SELECT * FROM shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts")
    suspend fun getAllShiftsSync(): List<Shift>

    @Query("SELECT * FROM shifts WHERE counterId = :counterId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveShiftForCounter(counterId: Long): Shift?

    @Query("SELECT * FROM shifts WHERE attendantId = :attendantId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveShiftForAttendant(attendantId: Long): Shift?

    @Query("SELECT * FROM shifts WHERE attendantId = :attendantId AND status = 'ACTIVE' LIMIT 1")
    fun getActiveShiftForAttendantFlow(attendantId: Long): Flow<Shift?>

    @Query("SELECT * FROM shifts WHERE counterId = :counterId AND status = 'CLOSED' ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastClosedShiftForCounter(counterId: Long): Shift?

    @Query("SELECT * FROM shifts WHERE id = :shiftId LIMIT 1")
    suspend fun getShiftById(shiftId: Long): Shift?

    @Query("SELECT * FROM shifts WHERE id = :shiftId LIMIT 1")
    fun getShiftByIdFlow(shiftId: Long): Flow<Shift?>

    @Query("SELECT * FROM shifts WHERE attendantId = :attendantId ORDER BY startTime DESC")
    fun getShiftsByAttendant(attendantId: Long): Flow<List<Shift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)

    // --- Stock Verifications ---
    @Query("SELECT * FROM stock_verifications WHERE shiftId = :shiftId ORDER BY category ASC, itemName ASC")
    fun getStockVerificationsForShift(shiftId: Long): Flow<List<StockVerification>>

    @Query("SELECT * FROM stock_verifications WHERE shiftId = :shiftId")
    suspend fun getStockVerificationsForShiftSync(shiftId: Long): List<StockVerification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockVerifications(verifications: List<StockVerification>)

    // --- Shift Closings ---
    @Query("SELECT * FROM shift_closings WHERE shiftId = :shiftId ORDER BY category ASC, itemName ASC")
    fun getShiftClosingsForShift(shiftId: Long): Flow<List<ShiftClosing>>

    @Query("SELECT * FROM shift_closings")
    fun getAllShiftClosings(): Flow<List<ShiftClosing>>

    @Query("SELECT * FROM shift_closings WHERE shiftId = :shiftId")
    suspend fun getShiftClosingsForShiftSync(shiftId: Long): List<ShiftClosing>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftClosings(closings: List<ShiftClosing>)

    // --- Disputes ---
    @Query("SELECT * FROM disputes ORDER BY createdAt DESC")
    fun getAllDisputes(): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes")
    suspend fun getAllDisputesSync(): List<Dispute>

    @Query("SELECT * FROM disputes WHERE status = :status ORDER BY createdAt DESC")
    fun getDisputesByStatus(status: DisputeStatus): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes WHERE raisedByAttendantId = :attendantId OR involvesPreviousAttendantId = :attendantId ORDER BY createdAt DESC")
    fun getDisputesForAttendant(attendantId: Long): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes WHERE id = :disputeId LIMIT 1")
    suspend fun getDisputeById(disputeId: Long): Dispute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispute(dispute: Dispute): Long

    @Update
    suspend fun updateDispute(dispute: Dispute)

    // --- Stock Adjustments ---
    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC")
    fun getAllStockAdjustments(): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments")
    suspend fun getAllStockAdjustmentsSync(): List<StockAdjustment>

    @Query("SELECT * FROM stock_adjustments WHERE counterId = :counterId ORDER BY timestamp DESC")
    fun getAdjustmentsForCounter(counterId: Long): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE shiftId = :shiftId ORDER BY timestamp DESC")
    fun getAdjustmentsForShift(shiftId: Long): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE shiftId = :shiftId AND attendantConfirmed = 1")
    suspend fun getConfirmedAdjustmentsForShiftSync(shiftId: Long): List<StockAdjustment>

    @Query("SELECT * FROM stock_adjustments WHERE shiftId = :shiftId AND attendantConfirmed = 0")
    fun getPendingAdjustmentsForShift(shiftId: Long): Flow<List<StockAdjustment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAdjustment(adjustment: StockAdjustment): Long

    @Update
    suspend fun updateStockAdjustment(adjustment: StockAdjustment)

    // --- Reconciliations (Ledger) ---
    @Query("SELECT * FROM reconciliations ORDER BY date DESC")
    fun getAllReconciliations(): Flow<List<Reconciliation>>

    @Query("SELECT * FROM reconciliations")
    suspend fun getAllReconciliationsSync(): List<Reconciliation>

    @Query("SELECT * FROM reconciliations WHERE attendantId = :attendantId ORDER BY date DESC")
    fun getReconciliationsByAttendant(attendantId: Long): Flow<List<Reconciliation>>

    @Query("SELECT * FROM reconciliations WHERE monthYear = :monthYear ORDER BY date DESC")
    fun getReconciliationsByMonth(monthYear: String): Flow<List<Reconciliation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(reconciliation: Reconciliation): Long

    // --- Notifications ---
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications")
    suspend fun getAllNotificationsSync(): List<AppNotification>

    @Query("""
        SELECT * FROM app_notifications 
        WHERE targetUserId = :userId 
           OR (targetUserId IS NULL AND targetRole = :userRole) 
           OR (targetUserId IS NULL AND targetRole IS NULL)
        ORDER BY timestamp DESC
    """)
    fun getNotificationsForUser(userId: Long, userRole: UserRole): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Query("UPDATE app_notifications SET read = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: Long)

    @Query("UPDATE app_notifications SET read = 1")
    suspend fun markAllNotificationsAsRead()

    @Query("SELECT * FROM stock_verifications")
    suspend fun getAllStockVerificationsSync(): List<StockVerification>

    @Query("SELECT * FROM shift_closings")
    suspend fun getAllShiftClosingsSync(): List<ShiftClosing>

    @Query("DELETE FROM users")
    suspend fun clearUsers()
    
    @Query("DELETE FROM items")
    suspend fun clearItems()
    
    @Query("DELETE FROM counters")
    suspend fun clearCounters()
    
    @Query("DELETE FROM counter_stocks")
    suspend fun clearCounterStocks()
    
    @Query("DELETE FROM shifts")
    suspend fun clearShifts()
    
    @Query("DELETE FROM stock_verifications")
    suspend fun clearStockVerifications()
    
    @Query("DELETE FROM shift_closings")
    suspend fun clearShiftClosings()
    
    @Query("DELETE FROM disputes")
    suspend fun clearDisputes()
    
    @Query("DELETE FROM stock_adjustments")
    suspend fun clearStockAdjustments()
    
    @Query("DELETE FROM reconciliations")
    suspend fun clearReconciliations()
    
    @Query("DELETE FROM bar_profile")
    suspend fun clearBarProfile()

    @Query("DELETE FROM app_notifications")
    suspend fun clearNotifications()

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<com.example.data.model.Expense>>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesSync(): List<com.example.data.model.Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: com.example.data.model.Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()
}
