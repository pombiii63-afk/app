package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CampusRepository(private val context: Context) {

    private var firestore: FirebaseFirestore

    init {
        // Safe check and programmatic initialization of default FirebaseApp
        var needsInit = true
        try {
            FirebaseApp.getInstance()
            needsInit = false
            Log.d("CampusRepository", "Firebase is already initialized.")
        } catch (e: IllegalStateException) {
            needsInit = true
        }

        if (needsInit) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyB_campus_deliv_fallback_key_2026")
                    .setApplicationId("1:123456789012:android:abcdef1234567890")
                    .setProjectId("campus-delivery-rxcpt")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d("CampusRepository", "Firebase initialized programmatically with options")
            } catch (ex: Exception) {
                Log.e("CampusRepository", "Programmatic Firebase initialization failed: ${ex.message}")
            }
        }
        firestore = FirebaseFirestore.getInstance()
    }

    // --- Real-time Streams using Firestore Snapshot Listeners ---

    val allOrdersFlow: Flow<List<OrderEntity>> = callbackFlow {
        val listenerReg = firestore.collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CampusRepository", "Error listen orders: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val ordersList = snapshot.mapNotNull { doc ->
                        doc.toObject(OrderEntity::class.java).copy(id = doc.id)
                    }
                    trySend(ordersList)
                }
            }
        awaitClose { listenerReg.remove() }
    }

    val allProfilesFlow: Flow<List<UserProfileEntity>> = callbackFlow {
        val listenerReg = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CampusRepository", "Error listen users: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val profilesList = snapshot.mapNotNull { doc ->
                        doc.toObject(UserProfileEntity::class.java).copy(registrationNumber = doc.id)
                    }
                    trySend(profilesList)
                }
            }
        awaitClose { listenerReg.remove() }
    }

    val allReportsFlow: Flow<List<ReportEntity>> = callbackFlow {
        val listenerReg = firestore.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CampusRepository", "Error listen reports: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reportsList = snapshot.mapNotNull { doc ->
                        doc.toObject(ReportEntity::class.java).copy(id = doc.id)
                    }
                    trySend(reportsList)
                }
            }
        awaitClose { listenerReg.remove() }
    }

    fun getOrdersByCustomerFlow(phone: String): Flow<List<OrderEntity>> = callbackFlow {
        val listenerReg = firestore.collection("orders")
            .whereEqualTo("customerPhone", phone)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        doc.toObject(OrderEntity::class.java).copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(list)
                }
            }
        awaitClose { listenerReg.remove() }
    }

    fun getOrdersByPartnerFlow(partnerId: String): Flow<List<OrderEntity>> = callbackFlow {
        val listenerReg = firestore.collection("orders")
            .whereEqualTo("deliveryPartnerId", partnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        doc.toObject(OrderEntity::class.java).copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(list)
                }
            }
        awaitClose { listenerReg.remove() }
    }

    fun getEarningsFlow(partnerId: String): Flow<List<EarningEntity>> = callbackFlow {
        val listenerReg = firestore.collection("earnings")
            .whereEqualTo("partnerId", partnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        doc.toObject(EarningEntity::class.java).copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(list)
                }
            }
        awaitClose { listenerReg.remove() }
    }

    // --- CRUD Actions ---

    suspend fun getProfileByRegNum(regNum: String): UserProfileEntity? {
        return try {
            val doc = firestore.collection("users").document(regNum.uppercase().trim()).get().await()
            if (doc.exists()) {
                doc.toObject(UserProfileEntity::class.java)?.copy(registrationNumber = doc.id)
            } else null
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error getProfileByRegNum: ${e.message}")
            null
        }
    }

    suspend fun getProfileByPhone(phone: String): UserProfileEntity? {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phone.trim())
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                doc.toObject(UserProfileEntity::class.java)?.copy(registrationNumber = doc.id)
            } else null
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error getProfileByPhone: ${e.message}")
            null
        }
    }

    suspend fun insertProfile(profile: UserProfileEntity) {
        try {
            val regNum = profile.registrationNumber.uppercase().trim()
            firestore.collection("users").document(regNum).set(profile).await()
            Log.d("CampusRepository", "Profile set in Firestore: $regNum")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error insertProfile: ${e.message}")
        }
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        insertProfile(profile)
    }

    suspend fun insertOrder(order: OrderEntity): String {
        return try {
            val ref = if (order.id.isNotBlank()) {
                firestore.collection("orders").document(order.id)
            } else {
                firestore.collection("orders").document()
            }
            val finalOrder = order.copy(id = ref.id)
            ref.set(finalOrder).await()
            Log.d("CampusRepository", "Order set in Firestore: ${ref.id}")
            ref.id
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error insertOrder: ${e.message}")
            ""
        }
    }

    suspend fun updateOrder(order: OrderEntity) {
        if (order.id.isNotBlank()) {
            try {
                firestore.collection("orders").document(order.id).set(order).await()
                Log.d("CampusRepository", "Order updated: ${order.id}")
            } catch (e: Exception) {
                Log.e("CampusRepository", "Error updateOrder: ${e.message}")
            }
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        try {
            firestore.collection("orders").document(orderId).update("status", status).await()
            Log.d("CampusRepository", "Order status updated: $orderId -> $status")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error updateOrderStatus: ${e.message}")
        }
    }

    suspend fun deleteOrderById(orderId: String) {
        try {
            firestore.collection("orders").document(orderId).delete().await()
            Log.d("CampusRepository", "Order deleted from Firestore: $orderId")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error deleteOrderById: ${e.message}")
        }
    }

    suspend fun insertEarning(earning: EarningEntity) {
        try {
            val ref = firestore.collection("earnings").document()
            val finalEarning = earning.copy(id = ref.id)
            ref.set(finalEarning).await()
            Log.d("CampusRepository", "Earning inserted to Firestore")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error insertEarning: ${e.message}")
        }
    }

    suspend fun insertReport(report: ReportEntity) {
        try {
            val ref = firestore.collection("reports").document()
            val finalReport = report.copy(id = ref.id)
            ref.set(finalReport).await()
            Log.d("CampusRepository", "Report inserted to Firestore")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error insertReport: ${e.message}")
        }
    }

    suspend fun updateReport(report: ReportEntity) {
        if (report.id.isNotBlank()) {
            try {
                firestore.collection("reports").document(report.id).set(report).await()
                Log.d("CampusRepository", "Report updated in Firestore: ${report.id}")
            } catch (e: Exception) {
                Log.e("CampusRepository", "Error updateReport: ${e.message}")
            }
        }
    }

    suspend fun updateSuspensionStatus(regNum: String, suspended: Boolean) {
        try {
            firestore.collection("users").document(regNum.uppercase().trim())
                .update("suspended", suspended).await()
            Log.d("CampusRepository", "Suspension updated for $regNum: $suspended")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error updateSuspension: ${e.message}")
        }
    }

    suspend fun updateTrustScore(regNum: String, strikesCount: Int, newScore: Int) {
        try {
            firestore.collection("users").document(regNum.uppercase().trim())
                .update(
                    "strikes", strikesCount,
                    "reliabilityScore", newScore
                ).await()
            Log.d("CampusRepository", "Trust score updated for $regNum")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error updateTrustScore: ${e.message}")
        }
    }
}
