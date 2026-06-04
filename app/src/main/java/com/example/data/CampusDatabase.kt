package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Support Entities ---

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val pickupLocation: String,
    val dropLocation: String,
    val deliveryFee: Double,
    val status: String, // PENDING, ACCEPTED, PICKED_UP, DELIVERED, CANCELLED
    val customerPhone: String,
    val customerName: String,
    val customerEmail: String,
    val notes: String = "",
    val deliveryPartnerId: String? = null,
    val deliveryPartnerName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Float = 0f, // Customer's rating of Partner
    val partnerRating: Float = 0f, // Partner's rating of Customer
    val escrowStatus: String = "LOCKED", // LOCKED, RELEASED, REFUNDED
    val qrCodeToken: String = "",
    val isReported: Boolean = false,
    val reportReason: String? = null
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val registrationNumber: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val role: String, // CUSTOMER, PARTNER, ADMIN
    val isEmailVerified: Boolean = false, // verification gate
    val reliabilityScore: Int = 100, // Starts at 100, drops on cancellation/strikes
    val strikes: Int = 0,
    val isSuspended: Boolean = false,
    val completedDeliveriesCount: Int = 0,
    val averageRating: Float = 5.0f
)

@Entity(tableName = "earnings")
data class EarningEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partnerId: String,
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val reporterName: String,
    val reportedRegNumber: String,
    val reason: String,
    val status: String = "PENDING", // PENDING, RESOLVED, DISMISSED
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAOs ---

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, status: String)

    @Query("SELECT * FROM orders WHERE customerPhone = :phone ORDER BY timestamp DESC")
    fun getOrdersByCustomerFlow(phone: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE deliveryPartnerId = :partnerId ORDER BY timestamp DESC")
    fun getOrdersByPartnerFlow(partnerId: String): Flow<List<OrderEntity>>

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Int)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfilesFlow(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE registrationNumber = :regNum LIMIT 1")
    suspend fun getProfileByRegNum(regNum: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getProfileByPhone(phone: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isSuspended = :suspended WHERE registrationNumber = :regNum")
    suspend fun updateSuspensionStatus(regNum: String, suspended: Boolean)

    @Query("UPDATE user_profiles SET strikes = :strikesCount, reliabilityScore = :newScore WHERE registrationNumber = :regNum")
    suspend fun updateTrustScore(regNum: String, strikesCount: Int, newScore: Int)
}

@Dao
interface EarningDao {
    @Query("SELECT * FROM earnings WHERE partnerId = :partnerId ORDER BY timestamp DESC")
    fun getEarningsByPartnerFlow(partnerId: String): Flow<List<EarningEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEarning(earning: EarningEntity)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)
}

// --- AppDatabase ---

@Database(
    entities = [OrderEntity::class, UserProfileEntity::class, EarningEntity::class, ReportEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun earningDao(): EarningDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: CampusDatabase? = null

        fun getDatabase(context: Context): CampusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampusDatabase::class.java,
                    "campus_delivery_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
