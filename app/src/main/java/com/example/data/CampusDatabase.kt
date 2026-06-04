package com.example.data

// --- Standard Data Models for Firestore Serialization ---

data class OrderEntity(
    val id: String = "",
    val itemName: String = "",
    val pickupLocation: String = "",
    val dropLocation: String = "",
    val deliveryFee: Double = 0.0,
    val status: String = "PENDING", // PENDING, ACCEPTED, PICKED_UP, DELIVERED, CANCELLED
    val customerPhone: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val notes: String = "",
    val deliveryPartnerId: String? = null,
    val deliveryPartnerName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Float = 0f, 
    val partnerRating: Float = 0f, 
    val escrowStatus: String = "LOCKED", // LOCKED, RELEASED, REFUNDED
    val qrCodeToken: String = "",
    val isReported: Boolean = false,
    val reportReason: String? = null
)

data class UserProfileEntity(
    val registrationNumber: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val role: String = "CUSTOMER", // CUSTOMER, PARTNER, ADMIN
    val isEmailVerified: Boolean = false,
    val reliabilityScore: Int = 100,
    val strikes: Int = 0,
    val isSuspended: Boolean = false,
    val completedDeliveriesCount: Int = 0,
    val averageRating: Float = 5.0f
)

data class EarningEntity(
    val id: String = "",
    val partnerId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ReportEntity(
    val id: String = "",
    val orderId: String = "",
    val reporterName: String = "",
    val reportedRegNumber: String = "",
    val reason: String = "",
    val status: String = "PENDING", // PENDING, RESOLVED, DISMISSED
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationEntity(
    val id: String = "",
    val recipientId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
