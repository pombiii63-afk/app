package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.razorpay.Checkout
import java.util.concurrent.TimeUnit

sealed interface AuthState {
    object Idle : AuthState
    data class CodeSent(val phone: String, val verificationId: String, val smsLogs: List<String>) : AuthState
    data class EmailVerificationPending(val profile: UserProfileEntity, val emailLogs: List<String>) : AuthState
    data class NeedsRegistration(val phone: String) : AuthState
    data class Authenticated(val profile: UserProfileEntity) : AuthState
}

class CampusDeliveryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CampusRepository = CampusRepository(application)
    
    init {
        // No startup mock seeding - keeping Firestore database clean for production use
    }

    // Observing Firestore Collections reactively via Flows
    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrdersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProfiles: StateFlow<List<UserProfileEntity>> = repository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ReportEntity>> = repository.allReportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State Managers ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentRole = MutableStateFlow("CUSTOMER") // CUSTOMER, PARTNER, ADMIN
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterLocation = MutableStateFlow("All")
    val filterLocation: StateFlow<String> = _filterLocation.asStateFlow()

    private val _selectedOrderForDetail = MutableStateFlow<OrderEntity?>(null)
    val selectedOrderForDetail: StateFlow<OrderEntity?> = _selectedOrderForDetail.asStateFlow()

    // Screen navigation state inside the App
    private val _currentScreen = MutableStateFlow("auth") // auth, main, admin, order_detail
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _activeTab = MutableStateFlow("feed") // Tab indices for main screens: feed, create, earnings, profile
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    // --- Real Firebase Authentication Logic ---

    fun verifyPhoneForOTP(phone: String, activity: Activity) {
        val cleaned = phone.trim().replace("\\s".toRegex(), "")
        // India format enforcement: Must be "+91" followed by exactly 10 digits
        if (cleaned.startsWith("+91") && cleaned.length == 13 && cleaned.substring(3).all { it.isDigit() }) {
            
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("CampusDeliveryAuth", "Phone Verification failed: ${e.message}")
                    val errorLogs = listOf(
                        "❌ [Firebase Auth] Verification Failed!",
                        "❌ Error: ${e.message}",
                        "⚠️ Please register this phone number as a Test Phone in the Firebase Auth console."
                    )
                    _authState.value = AuthState.CodeSent(cleaned, "", errorLogs)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    val logs = listOf(
                        "🔥 [Firebase Auth] Connected to Google Mobile Telecom Gateway...",
                        "🔥 [Firebase Auth] Dispatched real SMS verification code to (+91) device...",
                        "🔥 [Secure Signature] Verification ID: $verificationId"
                    )
                    _authState.value = AuthState.CodeSent(cleaned, verificationId, logs)
                }
            }

            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(cleaned)
                .setTimeout(30L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            
            try {
                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                Log.e("CampusDeliveryAuth", "verifyPhoneNumber call failure: ${e.message}")
                val logs = listOf(
                    "❌ [Firebase Auth] Provider Execution Failure: ${e.message}"
                )
                _authState.value = AuthState.CodeSent(cleaned, "", logs)
            }
        } else {
            val logs = listOf(
                "❌ Incorrect Format: Mobile number must be a real Indian (+91) device with 10 digits."
            )
            _authState.value = AuthState.CodeSent(cleaned, "", logs)
        }
    }

    fun submitOTP(otpCode: String) {
        val state = authState.value as? AuthState.CodeSent ?: return
        if (state.verificationId.isNotBlank()) {
            val credential = PhoneAuthProvider.getCredential(state.verificationId, otpCode)
            signInWithPhoneCredential(credential)
        }
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val phone = firebaseUser?.phoneNumber ?: ""
                    completeLocalPhoneSignIn(phone)
                } else {
                    Log.e("CampusDeliveryAuth", "SignIn failed: ${task.exception?.message}")
                    val state = authState.value as? AuthState.CodeSent
                    if (state != null) {
                        val updatedLogs = state.smsLogs + "❌ [Credential Check] Standard verification failed: ${task.exception?.message}"
                        _authState.value = state.copy(smsLogs = updatedLogs)
                    }
                }
            }
    }

    private fun completeLocalPhoneSignIn(phone: String) {
        viewModelScope.launch {
            val profile = repository.getProfileByPhone(phone)
            if (profile != null) {
                if (profile.isSuspended) {
                    _authState.value = AuthState.Authenticated(profile)
                } else if (!profile.isEmailVerified) {
                    sendCollegeEmailVerification(profile)
                } else {
                    _authState.value = AuthState.Authenticated(profile)
                    _currentRole.value = profile.role
                    _currentScreen.value = "main"
                }
            } else {
                _authState.value = AuthState.NeedsRegistration(phone)
            }
        }
    }

    fun registerNewStudent(
        phone: String,
        name: String,
        email: String,
        regNumber: String,
        initialRole: String
    ) {
        viewModelScope.launch {
            val emailClean = email.trim().lowercase()
            val isCollegeDomain = emailClean.endsWith(".edu") || emailClean.endsWith(".in") || emailClean.contains("@") && emailClean.split("@").last().contains("univ")
            
            if (name.isNotBlank() && isCollegeDomain && regNumber.isNotBlank()) {
                val newProfile = UserProfileEntity(
                    registrationNumber = regNumber.trim().uppercase(),
                    name = name.trim(),
                    phoneNumber = phone.trim(),
                    email = emailClean,
                    role = initialRole,
                    isEmailVerified = false,
                    reliabilityScore = 100,
                    strikes = 0,
                    isSuspended = false,
                    completedDeliveriesCount = 0
                )
                repository.insertProfile(newProfile)
                
                // Set Up Firebase Email Sign Up & Email Verification Link
                val auth = FirebaseAuth.getInstance()
                val tempPassword = regNumber.trim().uppercase() + "!" + phone.takeLast(4)
                
                auth.createUserWithEmailAndPassword(emailClean, tempPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful || task.exception?.message?.contains("already in use") == true) {
                            auth.signInWithEmailAndPassword(emailClean, tempPassword)
                                .addOnCompleteListener {
                                    sendCollegeEmailVerification(newProfile)
                                }
                        } else {
                            val logs = listOf(
                                "📧 [College Mail] Setting up identity registration...",
                                "❌ [Firebase Auth] Error registering credentials: ${task.exception?.message}"
                            )
                            _authState.value = AuthState.EmailVerificationPending(newProfile, logs)
                        }
                    }
            }
        }
    }

    private fun sendCollegeEmailVerification(profile: UserProfileEntity) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener { task ->
                val logs = mutableListOf<String>()
                if (task.isSuccessful) {
                    logs.add("📩 [College Mail] Verification dispatch succeeded!")
                    logs.add("📩 Real Firebase verification link delivered to: ${profile.email}")
                    logs.add("📩 status: Awaiting student verification action in browser/inbox...")
                } else {
                    logs.add("❌ [Firebase Auth] Verification failed to dispatch: ${task.exception?.message}")
                }
                _authState.value = AuthState.EmailVerificationPending(profile, logs)
            }
        } else {
            val logs = listOf(
                "❌ [Session Error] No active Firebase Auth account session details found."
            )
            _authState.value = AuthState.EmailVerificationPending(profile, logs)
        }
    }

    fun verifyFirebaseEmailStatus(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val state = authState.value as? AuthState.EmailVerificationPending ?: return
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { task ->
                if (user.isEmailVerified) {
                    viewModelScope.launch {
                        val verifiedProfile = state.profile.copy(isEmailVerified = true)
                        repository.insertProfile(verifiedProfile)
                        _authState.value = AuthState.Authenticated(verifiedProfile)
                        _currentRole.value = verifiedProfile.role
                        _currentScreen.value = "main"
                        onResult(true, "Official college email verified successfully!")
                    }
                } else {
                    onResult(false, "Verification link not clicked yet. Please check your educational inbox.")
                }
            }
        } else {
            onResult(false, "No active credential session found.")
        }
    }

    // --- Geolocation Coordinates Matchers for Campus Map ---
    fun getCoordinatesForLocation(locationName: String): Pair<Double, Double> {
        val clean = locationName.trim().lowercase()
        return when {
            clean.contains("gate") || clean.contains("visitor") -> Pair(12.9680, 79.1550)
            clean.contains("sjt") || clean.contains("academic") || clean.contains("block-1") -> Pair(12.9710, 79.1560)
            clean.contains("library") || clean.contains("reading") || clean.contains("annex") -> Pair(12.9702, 79.1585)
            clean.contains("fc2") || clean.contains("food court") || clean.contains("cafe") -> Pair(12.9716, 79.1594)
            clean.contains("boys hostel") || clean.contains("block d") -> Pair(12.9735, 79.1620)
            clean.contains("girls hostel") || clean.contains("block a") -> Pair(12.9691, 79.1633)
            else -> Pair(12.9716, 79.1594) // Central Campus Food Court
        }
    }

    // --- Razorpay Pre-Payment Escrow Handler ---
    var pendingOrderData: OrderCreationData? = null

    fun initiateOrderPayment(
        activity: Activity,
        itemName: String,
        pickup: String,
        drop: String,
        fee: Double,
        notes: String,
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ) {
        val authStateVal = authState.value as? AuthState.Authenticated ?: return
        val profile = authStateVal.profile
        
        pendingOrderData = OrderCreationData(
            itemName = itemName,
            pickup = pickup,
            drop = drop,
            fee = fee,
            notes = notes,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropLat = dropLat,
            dropLng = dropLng
        )

        val checkout = Checkout()
        val rzpKey = try {
            com.example.BuildConfig.RAZORPAY_KEY_ID.ifBlank { "rzp_test_campus_deliv_token_2026" }
        } catch (_: Throwable) {
            "rzp_test_campus_deliv_token_2026"
        }
        checkout.setKeyID(rzpKey)

        try {
            val options = org.json.JSONObject()
            options.put("name", "Campus Escrow Hub")
            options.put("description", "Secure pre-payment lock for: $itemName")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#6366F1")
            options.put("currency", "INR")
            options.put("amount", (fee * 80).toInt()) // roughly 1 USD = 80 INR paise equivalent for demo purposes

            val prefill = org.json.JSONObject()
            prefill.put("email", profile.email)
            prefill.put("contact", profile.phoneNumber)
            options.put("prefill", prefill)

            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e("CampusDeliveryAuth", "Razorpay initiation exception: ${e.message}")
        }
    }

    fun onRazorpayPaymentSuccess(paymentId: String) {
        val data = pendingOrderData ?: return
        viewModelScope.launch {
            val randToken = "CD-${(10000..99999).random()}-${(10..99).random()}"
            val customerPhoneVal = (authState.value as? AuthState.Authenticated)?.profile?.phoneNumber ?: ""
            val newOrder = OrderEntity(
                itemName = data.itemName,
                pickupLocation = data.pickup,
                dropLocation = data.drop,
                pickupLat = data.pickupLat,
                pickupLng = data.pickupLng,
                dropLat = data.dropLat,
                dropLng = data.dropLng,
                deliveryFee = data.fee,
                status = "PENDING",
                customerPhone = customerPhoneVal,
                customerName = (authState.value as? AuthState.Authenticated)?.profile?.name ?: "Student",
                customerEmail = (authState.value as? AuthState.Authenticated)?.profile?.email ?: "",
                notes = data.notes,
                rating = 0f,
                partnerRating = 0f,
                escrowStatus = "LOCKED", // Secure escrow locked state
                qrCodeToken = randToken
            )
            val insertedId = repository.insertOrder(newOrder)
            
            // Log this transaction inside payments Firestore collection
            val newPayment = PaymentEntity(
                id = paymentId,
                orderId = insertedId,
                amount = data.fee,
                status = "CAPTURED",
                customerPhone = customerPhoneVal
            )
            repository.insertPayment(newPayment)

            pendingOrderData = null
            _activeTab.value = "feed"
        }
    }

    fun onRazorpayPaymentFailure(errorMessage: String) {
        Log.e("CampusDeliveryAuth", "Razorpay escrow deposit failed: $errorMessage")
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        _authState.value = AuthState.Idle
        _currentScreen.value = "auth"
        _activeTab.value = "feed"
    }

    fun switchRole(newRole: String) {
        _currentRole.value = newRole
        val currentProfile = (authState.value as? AuthState.Authenticated)?.profile
        if (currentProfile != null) {
            viewModelScope.launch {
                val updated = currentProfile.copy(role = newRole)
                repository.insertProfile(updated)
                _authState.value = AuthState.Authenticated(updated)
            }
        }
    }

    // --- Order / Delivery Logic ---

    fun createOrder(
        itemName: String,
        pickup: String,
        drop: String,
        fee: Double,
        notes: String
    ) {
        val currentProfile = (authState.value as? AuthState.Authenticated)?.profile ?: return
        viewModelScope.launch {
            val randToken = "CD-${(10000..99999).random()}-${(10..99).random()}"
            val newOrder = OrderEntity(
                itemName = itemName,
                pickupLocation = pickup,
                dropLocation = drop,
                deliveryFee = fee,
                status = "PENDING",
                customerPhone = currentProfile.phoneNumber,
                customerName = currentProfile.name,
                customerEmail = currentProfile.email,
                notes = notes,
                rating = 0f,
                partnerRating = 0f,
                escrowStatus = "LOCKED",
                qrCodeToken = randToken
            )
            repository.insertOrder(newOrder)
            _activeTab.value = "feed"
        }
    }

    fun acceptOrder(orderId: String) {
        val currentProfile = (authState.value as? AuthState.Authenticated)?.profile ?: return
        if (currentProfile.role != "PARTNER") return
        
        viewModelScope.launch {
            repository.acceptOrderTransaction(
                orderId = orderId,
                partnerId = currentProfile.registrationNumber,
                partnerName = currentProfile.name
            )
        }
    }

    fun updateStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val orders = allOrders.value
            val order = orders.find { it.id == orderId }
            if (order != null) {
                val updated = order.copy(status = newStatus)
                repository.updateOrder(updated)
            }
        }
    }

    fun verifyQRAndDeliver(orderId: String, scannedCode: String): Boolean {
        val orders = allOrders.value
        val order = orders.find { it.id == orderId }
        
        if (order != null && order.status == "PICKED_UP" && order.qrCodeToken == scannedCode.trim()) {
            viewModelScope.launch {
                val (verified, orderObj) = repository.verifyQRAndDeliverTransaction(orderId, scannedCode)
                if (verified && orderObj != null) {
                    val partnerId = orderObj.deliveryPartnerId ?: ""
                    if (partnerId.isNotBlank()) {
                        val earning = EarningEntity(
                            id = orderId, // prevents duplicates during ledger merges
                            partnerId = partnerId,
                            amount = orderObj.deliveryFee,
                            description = "Delivery Completed: ${orderObj.itemName}"
                        )
                        repository.insertEarning(earning, customId = orderId)
                        
                        val profiles = allProfiles.value
                        val partnerProfile = profiles.find { it.registrationNumber == partnerId }
                        if (partnerProfile != null) {
                            val updatedPartner = partnerProfile.copy(
                                completedDeliveriesCount = partnerProfile.completedDeliveriesCount + 1,
                                reliabilityScore = minOf(100, partnerProfile.reliabilityScore + 2)
                            )
                            repository.insertProfile(updatedPartner)
                        }
                    }
                }
            }
            return true
        }
        return false
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            val orders = allOrders.value
            val order = orders.find { it.id == orderId }
            if (order != null) {
                if (order.status == "PENDING") {
                    val updated = order.copy(status = "CANCELLED", escrowStatus = "REFUNDED")
                    repository.updateOrder(updated)
                } else if (order.status == "ACCEPTED" || order.status == "PICKED_UP") {
                    val partnerId = order.deliveryPartnerId
                    if (partnerId != null) {
                        val profiles = allProfiles.value
                        val partnerProfile = profiles.find { it.registrationNumber == partnerId }
                        if (partnerProfile != null) {
                            val newStrikes = partnerProfile.strikes + 1
                            val newScore = maxOf(0, partnerProfile.reliabilityScore - 15)
                            val suspended = newStrikes >= 3
                            
                            val updatedPartner = partnerProfile.copy(
                                strikes = newStrikes,
                                reliabilityScore = newScore,
                                isSuspended = suspended
                            )
                            repository.insertProfile(updatedPartner)
                            
                            val systemReport = ReportEntity(
                                orderId = orderId,
                                reporterName = "Razorpay Escrow Guard",
                                reportedRegNumber = partnerId,
                                reason = "Delivery job cancellation after driver accepted or picked up goods."
                            )
                            repository.insertReport(systemReport)
                        }
                    }
                    val updated = order.copy(
                        status = "PENDING",
                        deliveryPartnerId = null,
                        deliveryPartnerName = null
                    )
                    repository.updateOrder(updated)
                }
            }
        }
    }

    fun submitRating(orderId: String, customerRating: Float, isPartnerRating: Boolean) {
        viewModelScope.launch {
            val orders = allOrders.value
            val order = orders.find { it.id == orderId }
            if (order != null) {
                val updated = if (isPartnerRating) {
                    order.copy(partnerRating = customerRating)
                } else {
                    order.copy(rating = customerRating)
                }
                repository.updateOrder(updated)
                
                if (isPartnerRating) {
                    val customerPhone = order.customerPhone
                    val guestProfile = allProfiles.value.find { it.phoneNumber == customerPhone }
                    if (guestProfile != null) {
                        val customerOrders = allOrders.value.filter { it.customerPhone == customerPhone && it.partnerRating > 0 }
                        val guestTotal = customerOrders.sumOf { it.partnerRating.toDouble() } + customerRating
                        val newCustomerAvg = (guestTotal / (customerOrders.size + 1)).toFloat()
                        
                        repository.insertProfile(guestProfile.copy(averageRating = newCustomerAvg))
                    }
                } else {
                    val partnerId = order.deliveryPartnerId
                    if (partnerId != null) {
                        val partnerOrders = allOrders.value.filter { it.deliveryPartnerId == partnerId && it.rating > 0 }
                        val partnerTotal = partnerOrders.sumOf { it.rating.toDouble() } + customerRating
                        val newPartnerAvg = (partnerTotal / (partnerOrders.size + 1)).toFloat()
                        
                        val partnerProfile = allProfiles.value.find { it.registrationNumber == partnerId }
                        if (partnerProfile != null) {
                            repository.insertProfile(partnerProfile.copy(averageRating = newPartnerAvg))
                        }
                    }
                }
            }
        }
    }

    // --- Search & Filtering ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterLocation(location: String) {
        _filterLocation.value = location
    }

    fun selectOrderDetail(order: OrderEntity?) {
        _selectedOrderForDetail.value = order
        if (order != null) {
            _currentScreen.value = "order_detail"
        } else {
            _currentScreen.value = "main"
        }
    }

    // --- Earnings Analytics Helper ---

    fun getFilteredEarnings(partnerId: String): Flow<Map<String, Double>> {
        return repository.getEarningsFlow(partnerId).map { list ->
            var daily = 0.0
            var weekly = 0.0
            var monthly = 0.0
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val weekMs = 7 * dayMs
            val monthMs = 30 * dayMs

            for (earns in list) {
                val diff = now - earns.timestamp
                if (diff <= dayMs) daily += earns.amount
                if (diff <= weekMs) weekly += earns.amount
                if (diff <= monthMs) monthly += earns.amount
            }

            mapOf(
                "Daily" to daily,
                "Weekly" to weekly,
                "Monthly" to monthly,
                "All-time" to list.sumOf { it.amount }
            )
        }
    }

    // --- Admin / Moderation Logic ---

    fun toggleSuspension(regNum: String) {
        viewModelScope.launch {
            val profile = allProfiles.value.find { it.registrationNumber == regNum }
            if (profile != null) {
                val currentSuspension = profile.isSuspended
                repository.updateSuspensionStatus(regNum, !currentSuspension)
                if (currentSuspension) {
                    repository.updateTrustScore(regNum, 0, 100)
                }
            }
        }
    }

    fun fileDisputeReport(orderId: String, reportedReg: String, reason: String) {
        val currentProfile = (authState.value as? AuthState.Authenticated)?.profile ?: return
        viewModelScope.launch {
            val report = ReportEntity(
                orderId = orderId,
                reporterName = currentProfile.name,
                reportedRegNumber = reportedReg,
                reason = reason
            )
            repository.insertReport(report)
        }
    }

    fun resolveDispute(reportId: String, markStrikes: Boolean) {
        viewModelScope.launch {
            val reportsList = allReports.value
            val report = reportsList.find { it.id == reportId }
            if (report != null) {
                repository.updateReport(report.copy(status = "RESOLVED"))
                
                if (markStrikes) {
                    val profile = allProfiles.value.find { it.registrationNumber == report.reportedRegNumber }
                    if (profile != null) {
                        val strikes = profile.strikes + 1
                        val newScore = maxOf(0, profile.reliabilityScore - 20)
                        repository.updateTrustScore(
                            report.reportedRegNumber,
                            strikes,
                            newScore
                        )
                        if (strikes >= 3) {
                            repository.updateSuspensionStatus(report.reportedRegNumber, true)
                        }
                    }
                }
            }
        }
    }

}
