package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CampusRepository(private val context: Context) {

    private var firestore: FirebaseFirestore

    init {
        try {
            FirebaseApp.getInstance()
            Log.d("CampusRepository", "Firebase default instance already configured.")
        } catch (e: IllegalStateException) {
            try {
                FirebaseApp.initializeApp(context)
                Log.d("CampusRepository", "Firebase auto-initialized from standard resource binding.")
            } catch (ex: Exception) {
                Log.e("CampusRepository", "Firebase auto-init failed: ${ex.message}")
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

    suspend fun insertEarning(earning: EarningEntity, customId: String? = null) {
        try {
            val docId = customId ?: earning.id.ifBlank { firestore.collection("earnings").document().id }
            val finalEarning = earning.copy(id = docId)
            firestore.collection("earnings").document(docId).set(finalEarning).await()
            Log.d("CampusRepository", "Earning inserted/merged to Firestore with ID: $docId")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error insertEarning: ${e.message}")
        }
    }

    suspend fun acceptOrderTransaction(orderId: String, partnerId: String, partnerName: String): Boolean {
        return try {
            val docRef = firestore.collection("orders").document(orderId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val status = snapshot.getString("status") ?: "PENDING"
                if (status == "PENDING") {
                    transaction.update(docRef, mapOf(
                        "status" to "ACCEPTED",
                        "deliveryPartnerId" to partnerId,
                        "deliveryPartnerName" to partnerName
                    ))
                    true
                } else {
                    false
                }
            }.await()
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error running acceptOrderTransaction: ${e.message}")
            false
        }
    }

    suspend fun verifyQRAndDeliverTransaction(orderId: String, scannedCode: String): Pair<Boolean, OrderEntity?> {
        return try {
            val docRef = firestore.collection("orders").document(orderId)
            var originalOrder: OrderEntity? = null
            val isSuccess = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val orderObj = snapshot.toObject(OrderEntity::class.java)?.copy(id = snapshot.id)
                originalOrder = orderObj
                if (orderObj != null && orderObj.status == "PICKED_UP" && orderObj.qrCodeToken == scannedCode.trim()) {
                    transaction.update(docRef, mapOf(
                        "status" to "DELIVERED",
                        "escrowStatus" to "RELEASED"
                    ))
                    true
                } else {
                    false
                }
            }.await()
            Pair(isSuccess, originalOrder)
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error in verifyQRAndDeliverTransaction: ${e.message}")
            Pair(false, null)
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

    suspend fun insertPayment(payment: PaymentEntity) {
        try {
            val docId = payment.id.ifBlank { firestore.collection("payments").document().id }
            val finalPayment = payment.copy(id = docId)
            firestore.collection("payments").document(docId).set(finalPayment).await()
            Log.d("CampusRepository", "Payment set in Firestore: $docId")
        } catch (e: Exception) {
            Log.e("CampusRepository", "Error insertPayment: ${e.message}")
        }
    }

    fun getPaymentsByCustomerFlow(phone: String): Flow<List<PaymentEntity>> = callbackFlow {
        val listenerReg = firestore.collection("payments")
            .whereEqualTo("customerPhone", phone)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        doc.toObject(PaymentEntity::class.java).copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(list)
                }
            }
        awaitClose { listenerReg.remove() }
    }
}
