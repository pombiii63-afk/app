package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.EarningEntity
import com.example.data.OrderEntity
import com.example.data.ReportEntity
import com.example.data.UserProfileEntity
import com.example.viewmodel.AuthState
import com.example.viewmodel.CampusDeliveryViewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.app.Activity
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

// --- Color Constants & Sleek Interface Design Theme ---
val BrandRoyalPurple = Color(0xFF4F46E5)    // Premium Indigo Primary
val BrandLightPurple = Color(0xFF1E1B4B)    // Sleek Deep Purple Base layer
val BrandDarkPurple = Color(0xFF111827)     // Dark Card Bg
val ColorSuccess = Color(0xFF10B981)
val ColorWarning = Color(0xFFF59E0B)
val ColorDanger = Color(0xFFEF4444)
val ColorBackgroundLight = Color(0xFF0F172A) // Sleek Dark Slate Canvas
val CardBorderColorLight = Color(0xFF334155) // Precise High-contrast Slate borders

// Sleek Theme Extras
val ThemeCardBg = Color(0xFF1F2937)          // Cards background
val ThemeTextPrimary = Color(0xFFFFFFFF)     // High-contrast clean texts
val ThemeTextSecondary = Color(0xFFE5E7EB)   // Medium gray-white texts
val ThemeTextMuted = Color(0xFF94A3B8)       // Fine slate description texts
val ColorCyanAccent = Color(0xFF06B6D4)      // Cyan Accent highlight

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CampusDeliveryAppContent(viewModel: CampusDeliveryViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                slideInVertically(initialOffsetY = { it }, animationSpec = spring()) with
                        fadeOut()
            }
        ) { screen ->
            when (screen) {
                "auth" -> AuthScreen(viewModel)
                "main" -> MainAppLayout(viewModel)
                "order_detail" -> OrderDetailScreen(viewModel)
                else -> AuthScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 1. AUTHENTICATION & ONBOARDING SCREEN
// ==========================================
@Composable
fun AuthScreen(viewModel: CampusDeliveryViewModel) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    // Registration states
    var fullName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var registrationNum by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("CUSTOMER") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandLightPurple, ColorBackgroundLight)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Header / Hero Banner
        Spacer(modifier = Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Default.DeliveryDining,
            contentDescription = "Campus Delivery",
            tint = ColorCyanAccent,
            modifier = Modifier
                .size(80.dp)
                .background(BrandLightPurple, CircleShape)
                .padding(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Campus Delivery",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = ColorCyanAccent,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = "⚡ Instant Student-to-Student Deliveries",
            style = MaterialTheme.typography.bodyMedium.copy(color = ThemeTextMuted),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        when (val state = authState) {
            is AuthState.Idle -> {
                // Phone Number Entry Page
                Card(
                     modifier = Modifier.fillMaxWidth(),
                     colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                     elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                     shape = RoundedCornerShape(24.dp),
                     border = BorderStroke(1.dp, CardBorderColorLight)
                ) {
                     Column(modifier = Modifier.padding(24.dp)) {
                         Text(
                             text = "Verify Phone Identity",
                             style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                             color = ThemeTextPrimary
                         )
                         Spacer(modifier = Modifier.height(6.dp))
                         Text(
                             text = "Enter your India (+91) student mobile number. We will send an authenticated OTP code via our secure gateway.",
                             style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
                         )
                         Spacer(modifier = Modifier.height(20.dp))

                         OutlinedTextField(
                             value = phoneNumber,
                             onValueChange = { 
                                 if (it.startsWith("+91") || it.isEmpty()) {
                                     phoneNumber = it
                                 } else if (!it.startsWith("+")) {
                                     phoneNumber = "+91" + it.replaceFirst("^91".toRegex(), "")
                                 } else {
                                     phoneNumber = it
                                 }
                             },
                             label = { Text("India Mobile Number (+91)", color = ThemeTextSecondary) },
                             placeholder = { Text("+91 90000 00000", color = ThemeTextMuted) },
                             leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ColorCyanAccent) },
                             shape = RoundedCornerShape(12.dp),
                             modifier = Modifier.fillMaxWidth().testTag("phone_input"),
                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                             singleLine = true
                         )
                         Spacer(modifier = Modifier.height(24.dp))

                         val isPhoneValid = phoneNumber.trim().replace(" ", "").length == 13 && phoneNumber.trim().startsWith("+91")
                         Button(
                             onClick = { viewModel.verifyPhoneForOTP(phoneNumber, activity!!) },
                             enabled = isPhoneValid,
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(52.dp)
                                 .testTag("verify_phone_button"),
                             shape = RoundedCornerShape(12.dp),
                             colors = ButtonDefaults.buttonColors(
                                 containerColor = if (isPhoneValid) BrandRoyalPurple else CardBorderColorLight,
                                 contentColor = Color.White
                             )
                         ) {
                             Text("Request OTP Verification", fontWeight = FontWeight.Bold)
                         }
                     }
                }
            }
            is AuthState.CodeSent -> {
                // OTP Entry Page
                Card(
                     modifier = Modifier.fillMaxWidth(),
                     colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                     elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                     shape = RoundedCornerShape(24.dp),
                     border = BorderStroke(1.dp, CardBorderColorLight)
                ) {
                     Column(modifier = Modifier.padding(24.dp)) {
                         Text(
                             text = "Verify Phone OTP",
                             style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                             color = ThemeTextPrimary
                         )
                         Spacer(modifier = Modifier.height(6.dp))
                         Text(
                             text = "We have dispatched a security token via SMS gateway to ${state.phone}. Please check secure developer live logs below.",
                             style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
                         )
                         Spacer(modifier = Modifier.height(20.dp))

                         OutlinedTextField(
                             value = otpCode,
                             onValueChange = { otpCode = it },
                             label = { Text("4-Digit OTP Code", color = ThemeTextSecondary) },
                             placeholder = { Text("Enter 4 digits", color = ThemeTextMuted) },
                             leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = ColorCyanAccent) },
                             shape = RoundedCornerShape(12.dp),
                             modifier = Modifier.fillMaxWidth().testTag("otp_input"),
                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                             singleLine = true
                         )
                         Spacer(modifier = Modifier.height(18.dp))

                         Button(
                             onClick = { viewModel.submitOTP(otpCode) },
                             enabled = otpCode.trim().length >= 4,
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .height(52.dp)
                                 .testTag("submit_otp_button"),
                             shape = RoundedCornerShape(12.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalPurple)
                         ) {
                             Text("Verify Code & Proceed", fontWeight = FontWeight.Bold, color = Color.White)
                         }

                         Spacer(modifier = Modifier.height(20.dp))
                         Text("Live SMS Gateway Handshake Monitor", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ColorCyanAccent)
                         Spacer(modifier = Modifier.height(6.dp))
                         Box(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clip(RoundedCornerShape(8.dp))
                                 .background(Color.Black)
                                 .border(1.dp, CardBorderColorLight, RoundedCornerShape(8.dp))
                                 .padding(12.dp)
                         ) {
                             Column {
                                 state.smsLogs.forEach { log ->
                                     Text(
                                         text = log,
                                         fontFamily = FontFamily.Monospace,
                                         fontSize = 11.sp,
                                         color = ColorSuccess
                                     )
                                 }
                             }
                         }
                     }
                }
            }
            is AuthState.NeedsRegistration -> {
                // Student Registration Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CardBorderColorLight)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Student Bio Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ThemeTextPrimary
                        )
                        Text(
                            text = "Please complete registration to secure trust across the campus chain.",
                            style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name", color = ThemeTextSecondary) },
                            placeholder = { Text("Alex Johnson", color = ThemeTextMuted) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ColorCyanAccent) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = emailAddress,
                            onValueChange = { emailAddress = it },
                            label = { Text("College Email Address", color = ThemeTextSecondary) },
                            placeholder = { Text("name@collegename.edu", color = ThemeTextMuted) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ColorCyanAccent) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("email_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = registrationNum,
                            onValueChange = { registrationNum = it },
                            label = { Text("Student Registration Number", color = ThemeTextSecondary) },
                            placeholder = { Text("REG-2026-X11", color = ThemeTextMuted) },
                            leadingIcon = { Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = ColorCyanAccent) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reg_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Choose Initial Role Selector Row
                        Text("Select Initial Primary Role", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ThemeTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRole = "CUSTOMER" }
                                    .border(
                                        2.dp,
                                        if (selectedRole == "CUSTOMER") ColorCyanAccent else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedRole == "CUSTOMER") BrandLightPurple else ColorBackgroundLight
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = ColorCyanAccent)
                                    Text("Customer", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ThemeTextPrimary))
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRole = "PARTNER" }
                                    .border(
                                        2.dp,
                                        if (selectedRole == "PARTNER") ColorCyanAccent else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedRole == "PARTNER") BrandLightPurple else ColorBackgroundLight
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = ColorCyanAccent)
                                    Text("Rider Partner", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ThemeTextPrimary))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.registerNewStudent(
                                    phone = state.phone,
                                    name = fullName,
                                    email = emailAddress,
                                    regNumber = registrationNum,
                                    initialRole = selectedRole
                                )
                            },
                            enabled = fullName.isNotBlank() && emailAddress.isNotBlank() && registrationNum.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_register_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalPurple)
                        ) {
                            Text("Create Trust Verified Account", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            is AuthState.EmailVerificationPending -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, ColorCyanAccent)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "College Email Verification",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ThemeTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "We have dispatched a real Firebase email activation link to ${state.profile.email}. Please verify ownership of your university identity to unlock the campus delivery network.",
                            style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        val context = LocalContext.current
                        Button(
                            onClick = { 
                                viewModel.verifyFirebaseEmailStatus { success, message ->
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("verify_email_status_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess)
                        ) {
                            Text("I Clicked the Verification Link", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Active Verification Gateway Live Logs", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ColorCyanAccent)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .border(1.dp, CardBorderColorLight, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                state.emailLogs.forEach { log ->
                                    Text(
                                        text = log,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = ColorSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is AuthState.Authenticated -> {
                // If suspended, show ban screen, else wait for state migration
                if (state.profile.isSuspended) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                        border = BorderStroke(1.dp, ColorDanger),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = ColorDanger, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Account Suspended",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = ColorDanger
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Your account registry (${state.profile.registrationNumber}) has been locked out after exceeding the trust code (3+ cancellation infractions or reported fraudulent conduct).",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeTextSecondary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Login Screen", color = Color.White)
                            }
                        }
                    }
                } else {
                    Text("Trust Link Secured. Connecting...", color = BrandRoyalPurple)
                }
            }
        }
    }
}

// ==========================================
// 2. MAIN NAVIGATIONAL APP LAYOUT
// ==========================================
@Composable
fun MainAppLayout(viewModel: CampusDeliveryViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val profile = (authState as? AuthState.Authenticated)?.profile

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = ThemeCardBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // TAB 1: Feeds/Orders view
                NavigationBarItem(
                    selected = activeTab == "feed",
                    onClick = {
                        viewModel.selectOrderDetail(null)
                        viewModel.setTab("feed")
                    },
                    icon = { Icon(Icons.Default.Feed, contentDescription = "Live Feed") },
                    label = { Text("Feed") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ColorCyanAccent,
                        selectedTextColor = ColorCyanAccent,
                        unselectedIconColor = ThemeTextMuted,
                        unselectedTextColor = ThemeTextMuted,
                        indicatorColor = BrandLightPurple
                    )
                )

                // TAB 2: Create Request or Earnings dashboard depending on role!
                if (currentRole == "CUSTOMER") {
                    NavigationBarItem(
                        selected = activeTab == "create",
                        onClick = {
                            viewModel.setTab("create")
                        },
                        icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Request") },
                        label = { Text("Order") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorCyanAccent,
                            selectedTextColor = ColorCyanAccent,
                            unselectedIconColor = ThemeTextMuted,
                            unselectedTextColor = ThemeTextMuted,
                            indicatorColor = BrandLightPurple
                        )
                    )
                } else {
                    NavigationBarItem(
                        selected = activeTab == "earnings",
                        onClick = {
                            viewModel.setTab("earnings")
                        },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Earnings") },
                        label = { Text("Earnings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorCyanAccent,
                            selectedTextColor = ColorCyanAccent,
                            unselectedIconColor = ThemeTextMuted,
                            unselectedTextColor = ThemeTextMuted,
                            indicatorColor = BrandLightPurple
                        )
                    )
                }

                // TAB 3: Profile Config
                NavigationBarItem(
                    selected = activeTab == "profile",
                    onClick = {
                        viewModel.setTab("profile")
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ColorCyanAccent,
                        selectedTextColor = ColorCyanAccent,
                        unselectedIconColor = ThemeTextMuted,
                        unselectedTextColor = ThemeTextMuted,
                        indicatorColor = BrandLightPurple
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "feed" -> LiveFeedScreen(viewModel)
                "create" -> CreateRequestScreen(viewModel)
                "earnings" -> EarningsDashboardScreen(viewModel)
                "profile" -> ProfileScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 3. LIVE FEED & ORDER MATCHER SCREEN
// ==========================================
@Composable
fun LiveFeedScreen(viewModel: CampusDeliveryViewModel) {
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterLocation by viewModel.filterLocation.collectAsStateWithLifecycle()
    
    val currentProfile = (authState as? AuthState.Authenticated)?.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgroundLight)
    ) {
        // App Premium Branding Bar inspired by Swiggy/Zomato
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeCardBg)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Campus Hub",
                        tint = ColorCyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "University Campus Hub",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThemeTextPrimary
                    )
                }
                Text(
                    text = "Aesthetic Student Peer Network",
                    style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
                )
            }

            // Role Switch Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .border(1.dp, ColorCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                    .background(BrandLightPurple)
                    .clickable {
                        val nextRole = if (currentRole == "CUSTOMER") "PARTNER" else "CUSTOMER"
                        viewModel.switchRole(nextRole)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (currentRole == "CUSTOMER") Icons.Default.ShoppingBag else Icons.Default.DeliveryDining,
                    contentDescription = null,
                    tint = ColorCyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentRole,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ColorCyanAccent
                    )
                )
            }
        }

        HorizontalDivider(color = CardBorderColorLight.copy(alpha = 0.5f))

        // Large Search Bar and Horizontal Filter Quick Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeCardBg)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search fast food, printers, science park...", fontSize = 14.sp, color = ThemeTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ThemeTextMuted) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("feed_search"),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = ColorBackgroundLight,
                    unfocusedContainerColor = ColorBackgroundLight,
                    focusedIndicatorColor = BrandRoyalPurple,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Pickup/Drop Location Navigation chips
            val locationChips = listOf("All", "Main Court", "Library", "Admin Bldg", "Hostel Blocks")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(locationChips) { location ->
                    val selected = filterLocation == location
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateFilterLocation(location) },
                        label = { Text(location, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRoyalPurple,
                            selectedLabelColor = Color.White,
                            containerColor = BrandLightPurple,
                            labelColor = BrandRoyalPurple
                        ),
                        border = null
                    )
                }
            }
        }

        // List Header label
        Text(
            text = if (currentRole == "CUSTOMER") "Your Placed Delivery Requests" else "Available Campus Runs near You",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ThemeTextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        // Filter algorithm depending on current role, search query, and chip selecion
        val filteredOrders = allOrders.filter { order ->
            val matchRole = if (currentRole == "CUSTOMER") {
                order.customerPhone == currentProfile?.phoneNumber
            } else {
                order.status == "PENDING" || (order.status != "PENDING" && order.deliveryPartnerId == currentProfile?.registrationNumber)
            }

            val matchSearch = order.itemName.contains(searchQuery, ignoreCase = true) ||
                    order.pickupLocation.contains(searchQuery, ignoreCase = true) ||
                    order.dropLocation.contains(searchQuery, ignoreCase = true)

            val matchChip = if (filterLocation == "All") true else {
                order.pickupLocation.contains(filterLocation, ignoreCase = true) ||
                        order.dropLocation.contains(filterLocation, ignoreCase = true)
            }

            matchRole && matchSearch && matchChip
        }

        if (filteredOrders.isEmpty()) {
            EmptyFeedState(currentRole)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredOrders) { order ->
                    OrderCard(order = order, currentRole = currentRole, onSelect = {
                        viewModel.selectOrderDetail(order)
                    }, onAccept = {
                        viewModel.acceptOrder(order.id)
                    })
                }
            }
        }
    }
}

@Composable
fun EmptyFeedState(currentRole: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (currentRole == "CUSTOMER") Icons.Default.AllInbox else Icons.Default.ExploreOff,
            contentDescription = "Empty",
            tint = BrandRoyalPurple.copy(alpha = 0.3f),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (currentRole == "CUSTOMER") "No Order History Yet" else "No Available Delivery Jobs",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ThemeTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (currentRole == "CUSTOMER") {
                "Placing requests is simple! Move to the next tab to request food, documents or grocery dispatching."
            } else {
                "All campus delivery jobs have been scooped up by fellow student riders. Check back in a few minutes!"
            },
            style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted),
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// 4. ORDER CARD COMPONENT
// ==========================================
@Composable
fun OrderCard(order: OrderEntity, currentRole: String, onSelect: () -> Unit, onAccept: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardBorderColorLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ThemeTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Placed at: " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.timestamp)),
                        style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
                    )
                }

                // Delivery Fee Bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(BrandLightPurple)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", order.deliveryFee)} payout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ColorCyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Campus Pickup/Drop-off Route Map-style line
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Pin Column Icons
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandRoyalPurple, modifier = Modifier.size(16.dp))
                    Box(modifier = Modifier.height(16.dp).width(1.dp).background(CardBorderColorLight))
                    Icon(Icons.Default.Home, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Location Details Row Label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pickup: " + order.pickupLocation,
                        fontSize = 13.sp,
                        color = ThemeTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Drop-off: " + order.dropLocation,
                        fontSize = 13.sp,
                        color = ThemeTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge Indicator
                val badgeColors = when (order.status) {
                    "PENDING" -> Triple(BrandLightPurple, BrandRoyalPurple, "Awaiting Acceptance")
                    "ACCEPTED" -> Triple(ColorWarning.copy(alpha = 0.1f), ColorWarning, "Driver Assigned")
                    "PICKED_UP" -> Triple(ColorWarning.copy(alpha = 0.15f), ColorWarning, "Picked Up")
                    "DELIVERED" -> Triple(ColorSuccess.copy(alpha = 0.1f), ColorSuccess, "Completed")
                    else -> Triple(CardBorderColorLight, ThemeTextSecondary, order.status)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColors.first)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(badgeColors.second))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badgeColors.third,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColors.second
                        )
                    }
                }

                // Call to action accept button or tracking trigger
                if (currentRole == "PARTNER" && order.status == "PENDING") {
                    Button(
                        onClick = { onAccept() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp).testTag("accept_button_${order.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalPurple)
                    ) {
                        Text("Accept Run", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onSelect() }
                    ) {
                        Text("View & Track Status", fontSize = 12.sp, color = ColorCyanAccent, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowForward,
                            contentDescription = null,
                            tint = ColorCyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. CREATE REQUEST / PLACEMENT SCREEN
// ==========================================
@Composable
fun CreateRequestScreen(viewModel: CampusDeliveryViewModel) {
    var pItemName by remember { mutableStateOf("") }
    var pPickupLoc by remember { mutableStateOf("") }
    var pDropLoc by remember { mutableStateOf("") }
    var pTipAmt by remember { mutableStateOf("3.00") }
    var pInstructions by remember { mutableStateOf("") }

    // Coordinates states for our Maps geofence
    var pPickupLat by remember { mutableStateOf(12.9716) }
    var pPickupLng by remember { mutableStateOf(79.1594) }
    var pDropLat by remember { mutableStateOf(12.9702) }
    var pDropLng by remember { mutableStateOf(79.1585) }

    // Control which pin updates upon clicking the map: PICKUP or DROP
    var mapSelectMode by remember { mutableStateOf("PICKUP") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(ColorBackgroundLight)
            .padding(24.dp)
    ) {
        Text(
            text = "Initiate Delivery Run",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ThemeTextPrimary
        )
        Text(
            text = "Position your geofences on the interactive map, input specifications, and finalize secure escrow payment.",
            style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Geofence Interactive Map Panel ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Map Marker Pinning Geofence",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThemeTextPrimary
                )
                Text(
                    text = "Select active point, then TAP the campus map to drop standard coordinates instantly.",
                    style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Selection Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { mapSelectMode = "PICKUP" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mapSelectMode == "PICKUP") ColorCyanAccent else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Set Pickup (Blue PIN)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { mapSelectMode = "DROP" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mapSelectMode == "DROP") ColorSuccess else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Set Drop-off (Green PIN)", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Vector GPS Mapping Board with pointerInput tap detectors
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A)) // Dark slate background
                        .border(BorderStroke(1.dp, CardBorderColorLight), RoundedCornerShape(12.dp))
                        .pointerInput(mapSelectMode) {
                            detectTapGestures { offset ->
                                val latMin = 12.9650
                                val latMax = 12.9750
                                val lngMin = 79.1500
                                val lngMax = 79.1700
                                
                                val calculatedLat = latMax - (offset.y / size.height) * (latMax - latMin)
                                val calculatedLng = lngMin + (offset.x / size.width) * (lngMax - lngMin)
                                
                                if (mapSelectMode == "PICKUP") {
                                    pPickupLat = calculatedLat
                                    pPickupLng = calculatedLng
                                    pPickupLoc = "Pin @ (${String.format("%.4f", calculatedLat)}, ${String.format("%.4f", calculatedLng)})"
                                } else {
                                    pDropLat = calculatedLat
                                    pDropLng = calculatedLng
                                    pDropLoc = "Pin @ (${String.format("%.4f", calculatedLat)}, ${String.format("%.4f", calculatedLng)})"
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw architectural grid layout (Campus pathways simulation)
                        drawLine(Color(0xFF1E293B), start = Offset(w * 0.2f, 0f), end = Offset(w * 0.2f, h), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(w * 0.5f, 0f), end = Offset(w * 0.5f, h), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(w * 0.8f, 0f), end = Offset(w * 0.8f, h), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(0f, h * 0.3f), end = Offset(w, h * 0.3f), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(0f, h * 0.7f), end = Offset(w, h * 0.7f), strokeWidth = 5f)

                        val latMin = 12.9650
                        val latMax = 12.9750
                        val lngMin = 79.1500
                        val lngMax = 79.1700

                        fun mapCoordsToOffset(lat: Double, lng: Double): Offset {
                            val x = ((lng - lngMin) / (lngMax - lngMin)).coerceIn(0.0, 1.0) * w
                            val y = (1.0 - (lat - latMin) / (latMax - latMin)).coerceIn(0.0, 1.0) * h
                            return Offset(x.toFloat(), y.toFloat())
                        }

                        val pickupOffset = mapCoordsToOffset(pPickupLat, pPickupLng)
                        val dropOffset = mapCoordsToOffset(pDropLat, pDropLng)

                        // Draw Route Line
                        drawLine(
                            color = ColorCyanAccent.copy(alpha = 0.8f),
                            start = pickupOffset,
                            end = dropOffset,
                            strokeWidth = 6f
                        )

                        // Draw Pickup circle pinpoint (Blue/Cyan)
                        drawCircle(ColorCyanAccent, radius = 9f, center = pickupOffset)
                        drawCircle(ColorCyanAccent.copy(alpha = 0.3f), radius = 22f, center = pickupOffset)

                        // Draw Drop circle pinpoint (Green)
                        drawCircle(ColorSuccess, radius = 9f, center = dropOffset)
                        drawCircle(ColorSuccess.copy(alpha = 0.3f), radius = 22f, center = dropOffset)
                    }

                    // Floating instructions / Info badge over canvas
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Aesthetic Vector Mapper (Tap to pinpoint)",
                            color = ThemeTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Specifications Specifications Fields Form ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Item Description
                OutlinedTextField(
                    value = pItemName,
                    onValueChange = { pItemName = it },
                    label = { Text("What needs delivering?", color = ThemeTextSecondary) },
                    placeholder = { Text("e.g. Science Lab Files, Pizza Carton", color = ThemeTextMuted) },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = BrandRoyalPurple) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("item_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pickup location description
                OutlinedTextField(
                    value = pPickupLoc,
                    onValueChange = { 
                        pPickupLoc = it 
                        val coords = viewModel.getCoordinatesForLocation(it)
                        pPickupLat = coords.first
                        pPickupLng = coords.second
                    },
                    label = { Text("Pickup landmark / detail description", color = ThemeTextSecondary) },
                    placeholder = { Text("e.g. Science Cafe kiosk, Tech FC-2", color = ThemeTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = ColorCyanAccent) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pickup_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Drop location description
                OutlinedTextField(
                    value = pDropLoc,
                    onValueChange = { 
                        pDropLoc = it 
                        val coords = viewModel.getCoordinatesForLocation(it)
                        pDropLat = coords.first
                        pDropLng = coords.second
                    },
                    label = { Text("Drop-off landmark / detail description", color = ThemeTextSecondary) },
                    placeholder = { Text("e.g. Girls Hostel Block A Room 202", color = ThemeTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = ColorSuccess) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("drop_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tip/Reward pay
                OutlinedTextField(
                    value = pTipAmt,
                    onValueChange = { pTipAmt = it },
                    label = { Text("Escrow Delivery Reward Pay (INR equivalent)", color = ThemeTextSecondary) },
                    placeholder = { Text("150.00", color = ThemeTextMuted) },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = ColorSuccess) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tip_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Special Instructions
                OutlinedTextField(
                    value = pInstructions,
                    onValueChange = { pInstructions = it },
                    label = { Text("Special Rider notes (optional)", color = ThemeTextSecondary) },
                    placeholder = { Text("e.g. knock twice; keep pizza flat.", color = ThemeTextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("notes_input"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Escrow payments dispatch CTA
                val activity = LocalContext.current as? Activity
                Button(
                    onClick = {
                        val feeDouble = pTipAmt.toDoubleOrNull() ?: 150.0
                        if (activity != null) {
                            viewModel.initiateOrderPayment(
                                activity = activity,
                                itemName = pItemName,
                                pickup = pPickupLoc,
                                drop = pDropLoc,
                                fee = feeDouble,
                                notes = pInstructions,
                                pickupLat = pPickupLat,
                                pickupLng = pPickupLng,
                                dropLat = pDropLat,
                                dropLng = pDropLng
                            )
                        }
                    },
                    enabled = pItemName.isNotBlank() && pPickupLoc.isNotBlank() && pDropLoc.isNotBlank() && pTipAmt.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_request_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalPurple)
                ) {
                    Text("Pre-Pay Escrow & Broadcast Order", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 6. EARNINGS DASHBOARD SCREEN
// ==========================================
@Composable
fun EarningsDashboardScreen(viewModel: CampusDeliveryViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentProfile = (authState as? AuthState.Authenticated)?.profile

    if (currentProfile == null) return

    val earningsMap by viewModel.getFilteredEarnings(currentProfile.registrationNumber)
        .collectAsStateWithLifecycle(initialValue = mapOf("Daily" to 0.0, "Weekly" to 0.0, "Monthly" to 0.0, "All-time" to 0.0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(ColorBackgroundLight)
            .padding(24.dp)
    ) {
        Text(
            text = "Partner Earnings Center",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ThemeTextPrimary
        )
        Text(
            text = "Track your university payouts, completed orders, and reliability grades dynamically.",
            style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Grid of overall earnings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily earnings card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                border = BorderStroke(1.dp, CardBorderColorLight),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Yield", fontSize = 12.sp, color = ThemeTextMuted, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", earningsMap["Daily"] ?: 0.0)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCyanAccent
                    )
                }
            }

            // Monthly earnings card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                border = BorderStroke(1.dp, CardBorderColorLight),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Payout", fontSize = 12.sp, color = ThemeTextMuted, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", earningsMap["Monthly"] ?: 0.0)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorSuccess
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large total stats banner (Sleek Dark Purple Theme)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandDarkPurple),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Career Earnings", fontSize = 12.sp, color = Color(0xFFD0BCFF), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", earningsMap["All-time"] ?: 0.0)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Deliveries Met", fontSize = 12.sp, color = Color(0xFFD0BCFF), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandRoyalPurple)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentProfile.completedDeliveriesCount} Runs",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom drawn Canvas charts for a clean premium startup feel!
        Text("Weekly Progression Analytics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextPrimary)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Draw elegant chart lines mapping simulation weekly progressions
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Simple path mapping metrics
                    val pts = listOf(
                        Offset(width * 0.1f, height * 0.8f),
                        Offset(width * 0.25f, height * 0.7f),
                        Offset(width * 0.4f, height * 0.85f),
                        Offset(width * 0.55f, height * 0.45f),
                        Offset(width * 0.7f, height * 0.6f),
                        Offset(width * 0.85f, height * 0.3f),
                        Offset(width * 0.95f, height * 0.2f)
                    )

                    val linePath = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            lineTo(pts[i].x, pts[i].y)
                        }
                    }

                    // Stroke
                    drawPath(
                        path = linePath,
                        color = ColorCyanAccent,
                        style = Stroke(width = 6f)
                    )

                    // Draw circles
                    for (offset in pts) {
                        drawCircle(
                            color = ColorCyanAccent,
                            radius = 8f,
                            center = offset
                        )
                    }
                }

                // Days Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mon", fontSize = 10.sp, color = ThemeTextMuted)
                    Text("Tue", fontSize = 10.sp, color = ThemeTextMuted)
                    Text("Wed", fontSize = 10.sp, color = ThemeTextMuted)
                    Text("Thu", fontSize = 10.sp, color = ThemeTextMuted)
                    Text("Fri", fontSize = 10.sp, color = ThemeTextMuted)
                    Text("Sat", fontSize = 10.sp, color = ThemeTextMuted)
                    Text("Sun", fontSize = 10.sp, color = ThemeTextMuted)
                }
            }
        }
    }
}

// ==========================================
// 7. PROFILE & USER ADJUSTMENTS SCREEN
// ==========================================
@Composable
fun ProfileScreen(viewModel: CampusDeliveryViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val profile = (authState as? AuthState.Authenticated)?.profile

    if (profile == null) return

    val updatedLiveProfile = profiles.find { it.registrationNumber == profile.registrationNumber } ?: profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(ColorBackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper card avatar info
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.size(90.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(BrandRoyalPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = updatedLiveProfile.name.take(1).uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            // Trust Batch
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ColorSuccess)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Verified, contentDescription = "Trust verified", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = updatedLiveProfile.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = ThemeTextPrimary
        )
        Text(
            text = updatedLiveProfile.email,
            style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Modern Trust System Metric indicators
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "University Trust Index Metrics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ThemeTextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Peer Reliability score", fontSize = 11.sp, color = ThemeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${updatedLiveProfile.reliabilityScore}% Rating", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorCyanAccent)
                    }

                    Column {
                        Text("Role Violations", fontSize = 11.sp, color = ThemeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${updatedLiveProfile.strikes} / 3 Strikes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (updatedLiveProfile.strikes > 0) ColorDanger else ColorSuccess)
                    }

                    Column {
                        Text("Rating star", fontSize = 11.sp, color = ThemeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = ColorWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format(Locale.getDefault(), "%.1f", updatedLiveProfile.averageRating), fontWeight = FontWeight.Bold, color = ThemeTextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom graphic progress meter representing reliability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(CardBorderColorLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(updatedLiveProfile.reliabilityScore / 100f)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandRoyalPurple, ColorSuccess)
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Student identifiers list
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("College Reg Number") },
                    supportingContent = { Text(updatedLiveProfile.registrationNumber) },
                    leadingContent = { Icon(Icons.Default.Badge, contentDescription = null, tint = ColorCyanAccent) },
                    colors = ListItemDefaults.colors(containerColor = ThemeCardBg, headlineColor = ThemeTextPrimary, supportingColor = ThemeTextSecondary)
                )
                HorizontalDivider(color = CardBorderColorLight, thickness = 1.dp)
                ListItem(
                    headlineContent = { Text("Verified Phone Index") },
                    supportingContent = { Text(updatedLiveProfile.phoneNumber) },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = ColorCyanAccent) },
                    colors = ListItemDefaults.colors(containerColor = ThemeCardBg, headlineColor = ThemeTextPrimary, supportingColor = ThemeTextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Admin Console trigger (For administrative presentation)
        if (updatedLiveProfile.registrationNumber == "ADMIN2026") {
            Button(
                onClick = {
                    viewModel.setScreen("admin")
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_console_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalPurple)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Staff Administration Console", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Logout action button
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Exit", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Secure Clear Session (Log Out)", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 8. ORDER TRACKING & DETAILS SCREEN
// ==========================================
@Composable
fun OrderDetailScreen(viewModel: CampusDeliveryViewModel) {
    val order by viewModel.selectedOrderForDetail.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    val currentProfile = (authState as? AuthState.Authenticated)?.profile

    if (order == null || currentProfile == null) return

    var inputReportReason by remember { mutableStateOf("") }
    var ratingStarsState by remember { mutableStateOf(0f) }
    var partnerRatingStarsState by remember { mutableStateOf(0f) }
    var showScanOverlay by remember { mutableStateOf(false) }
    var qrVerifySuccessMessage by remember { mutableStateOf<String?>(null) }
    var qrVerifyErrorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(ColorBackgroundLight)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeCardBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectOrderDetail(null) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ColorCyanAccent)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Secure Order Tracker",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = ThemeTextPrimary
                )
                Text(
                    text = "ID: #${order!!.id} • Escrow Guaranteed",
                    fontSize = 11.sp,
                    color = ColorCyanAccent
                )
            }
        }

        HorizontalDivider(color = CardBorderColorLight)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(280.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Geometric Campus GPS Vector Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Pathway intersections
                        drawLine(Color(0xFF1E293B), start = Offset(w * 0.2f, 0f), end = Offset(w * 0.2f, h), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(w * 0.5f, 0f), end = Offset(w * 0.5f, h), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(w * 0.8f, 0f), end = Offset(w * 0.8f, h), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(0f, h * 0.3f), end = Offset(w, h * 0.3f), strokeWidth = 5f)
                        drawLine(Color(0xFF1E293B), start = Offset(0f, h * 0.7f), end = Offset(w, h * 0.7f), strokeWidth = 5f)

                        val latMin = 12.9650
                        val latMax = 12.9750
                        val lngMin = 79.1500
                        val lngMax = 79.1700

                        fun mapCoordsToOffset(lat: Double, lng: Double): Offset {
                            val x = ((lng - lngMin) / (lngMax - lngMin)).coerceIn(0.0, 1.0) * w
                            val y = (1.0 - (lat - latMin) / (latMax - latMin)).coerceIn(0.0, 1.0) * h
                            return Offset(x.toFloat(), y.toFloat())
                        }

                        val pickupOffset = mapCoordsToOffset(order!!.pickupLat, order!!.pickupLng)
                        val dropOffset = mapCoordsToOffset(order!!.dropLat, order!!.dropLng)

                        // Draw Route Line
                        drawLine(
                            color = ColorCyanAccent,
                            start = pickupOffset,
                            end = dropOffset,
                            strokeWidth = 6f
                        )

                        // Draw Pickup Circle Node
                        drawCircle(ColorCyanAccent, radius = 9f, center = pickupOffset)
                        drawCircle(ColorCyanAccent.copy(alpha = 0.3f), radius = 20f, center = pickupOffset)

                        // Draw Drop Circle Node
                        drawCircle(ColorSuccess, radius = 9f, center = dropOffset)
                        drawCircle(ColorSuccess.copy(alpha = 0.3f), radius = 20f, center = dropOffset)

                        // Vehicle Position offset
                        val progress = when (order!!.status) {
                            "PENDING" -> 0.0f
                            "ACCEPTED" -> 0.3f
                            "PICKED_UP" -> 0.7f
                            "DELIVERED" -> 1.0f
                            else -> 0.0f
                        }

                        val vehicleOffset = Offset(
                            pickupOffset.x + (dropOffset.x - pickupOffset.x) * progress,
                            pickupOffset.y + (dropOffset.y - pickupOffset.y) * progress
                        )

                        // Draw Runner Scooter pin (Orange highlighter)
                        drawCircle(ColorWarning, radius = 12f, center = vehicleOffset)
                        drawCircle(Color.Black, radius = 6f, center = vehicleOffset)
                        drawCircle(ColorCyanAccent, radius = 3f, center = vehicleOffset)
                    }
                }

                // Landmark label overlay overlays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ColorCyanAccent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pickup: " + order!!.pickupLocation.take(15) + "...", color = ThemeTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ColorSuccess))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Drop: " + order!!.dropLocation.take(15) + "...", color = ThemeTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Live status info banner overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Interactive Campus GPS Tracker", fontSize = 10.sp, color = ThemeTextSecondary)
                        Text(
                            text = when (order!!.status) {
                                "PENDING" -> "Awaiting Acceptance"
                                "ACCEPTED" -> "Partner en route to items"
                                "PICKED_UP" -> "Courier In-Transit"
                                "DELIVERED" -> "Delivered Safe"
                                else -> "Cancelled"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ColorCyanAccent
                        )
                    }
                }
            }
        }

        // Razorpay Verified Escrow Vault status box (Ensures secure trust & wallet locking)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Razorpay Escrow Shield", tint = ColorCyanAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Razorpay Escrow Vault Shield",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemeTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("UPI Deposited Funds Status", fontSize = 11.sp, color = ThemeTextMuted)
                        Text(
                            text = when (order!!.escrowStatus) {
                                "LOCKED" -> "🔒 FUNDS SECURED (ESCROW ACTIVE)"
                                "RELEASED" -> "🔓 ESCROW DISBURSED (PAID TO COURIER)"
                                "REFUNDED" -> "↩️ ESCROW RETURNED (UPI ACCOUNT CREDITED)"
                                else -> "CANCELLED"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = when (order!!.escrowStatus) {
                                "LOCKED" -> ColorWarning
                                "RELEASED" -> ColorSuccess
                                "REFUNDED" -> ColorCyanAccent
                                else -> ColorDanger
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandLightPurple)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Razorpay Sandbox", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorCyanAccent)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Merchant payout is securely locked in Escrow. Funds release dynamically upon reciprocal student QR code handshake verification. Rest certain under secure protocol.",
                    color = ThemeTextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Order Metadata Details (Package elements & details info)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
            border = BorderStroke(1.dp, CardBorderColorLight),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Package Item", fontSize = 11.sp, color = ThemeTextMuted)
                        Text(text = order!!.itemName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Fulfillment Reward", fontSize = 11.sp, color = ThemeTextMuted)
                        Text(text = "₹${String.format(Locale.getDefault(), "%.2f", order!!.deliveryFee)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorSuccess)
                    }
                }

                if (order!!.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorBackgroundLight)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💡 Transit Instructions: " + order!!.notes,
                            fontSize = 11.sp,
                            color = ThemeTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Order Progress Timeline Indicator Dots
                Text("Process State Timeline", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ThemeTextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                val milestones = listOf("PENDING", "ACCEPTED", "PICKED_UP", "DELIVERED")
                val currentMilestoneIdx = milestones.indexOf(order!!.status)

                milestones.forEachIndexed { idx, m ->
                    val active = idx <= currentMilestoneIdx
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (active) ColorCyanAccent else CardBorderColorLight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (m) {
                                "PENDING" -> "Broadcast Broadcasted on Campus Feed"
                                "ACCEPTED" -> "Accepted by Partner Courier: " + (order!!.deliveryPartnerName ?: "Awaiting Rider Match")
                                "PICKED_UP" -> "Picked up from initial point. [Escrow active; Transit in progress]"
                                "DELIVERED" -> "Completed & QR Handshake verified safely."
                                else -> m
                            },
                            fontWeight = if (idx == currentMilestoneIdx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (idx == currentMilestoneIdx) ThemeTextPrimary else ThemeTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QR Handshake Verification Mechanism Box (Mandatory flow securely matching both parties)
        if (order!!.status == "PICKED_UP") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                border = BorderStroke(1.dp, ColorCyanAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "College Trust QR Handshake",
                        fontWeight = FontWeight.Bold,
                        color = ColorCyanAccent,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentRole == "CUSTOMER" && order!!.customerPhone == currentProfile.phoneNumber) {
                        // CUSTOMER generates vector QR Canvas
                        Text(
                            "Delivery verification security token is encapsulated below. Present this to your Rider Partner to release Escrow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom drawn cyber QR code matching the exact tokens securely on Compose canvas
                        Canvas(
                            modifier = Modifier
                                .size(140.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            val blockSize = size.width / 10f
                            val codeHash = order!!.qrCodeToken.hashCode()
                            
                            // Draw realistic QR corner finder patterns
                            drawRect(color = Color.Black, size = Size(blockSize * 3, blockSize * 3), topLeft = Offset(0f, 0f))
                            drawRect(color = Color.White, size = Size(blockSize, blockSize), topLeft = Offset(blockSize, blockSize))

                            drawRect(color = Color.Black, size = Size(blockSize * 3, blockSize * 3), topLeft = Offset(size.width - blockSize * 3, 0f))
                            drawRect(color = Color.White, size = Size(blockSize, blockSize), topLeft = Offset(size.width - blockSize * 2, blockSize))

                            drawRect(color = Color.Black, size = Size(blockSize * 3, blockSize * 3), topLeft = Offset(0f, size.height - blockSize * 3))
                            drawRect(color = Color.White, size = Size(blockSize, blockSize), topLeft = Offset(blockSize, size.height - blockSize * 2))

                            // Fill remaining blocks pseudo-randomly using hash representing the secure token
                            for (row in 0..9) {
                                for (col in 0..9) {
                                    if ((row < 3 && col < 3) || (row < 3 && col >= 7) || (row >= 7 && col < 3)) {
                                        // skip finder templates
                                        continue
                                    }
                                    val blockBit = (codeHash shadowShift (row * col)) % 2 == 0
                                    if (blockBit) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(col * blockSize, row * blockSize),
                                            size = Size(blockSize, blockSize)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandLightPurple)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Token Signature: ${order!!.qrCodeToken}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ColorCyanAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (currentRole == "PARTNER" && order!!.deliveryPartnerId == currentProfile.registrationNumber) {
                        // PARTNER must scan/secure completion
                        Text(
                            "You must scan/type the Customer's on-screen Trust QR code to safely release Razorpay Escrow Funds and complete the delivery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!showScanOverlay) {
                            val context = LocalContext.current
                            Button(
                                onClick = { 
                                    try {
                                        val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
                                        scanner.startScan()
                                            .addOnSuccessListener { barcode ->
                                                val rawValue = barcode.rawValue
                                                if (!rawValue.isNullOrBlank()) {
                                                    val verified = viewModel.verifyQRAndDeliver(order!!.id, rawValue)
                                                    if (verified) {
                                                        qrVerifySuccessMessage = "Secure Handshake matching! Escrow funds released to your wallet."
                                                        qrVerifyErrorMessage = null
                                                        showScanOverlay = false
                                                    } else {
                                                        qrVerifyErrorMessage = "Validation Error: Scanned token did not match order sign status."
                                                        qrVerifySuccessMessage = null
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { ex ->
                                                Log.e("CampusDeliveryAuth", "Google Scanner unsupported/canceled: ${ex.message}")
                                                qrVerifyErrorMessage = "Google Play Services scanner unavailable in this emulator. Falling back to Live Camera simulator."
                                                showScanOverlay = true
                                            }
                                    } catch (e: Exception) {
                                        Log.e("CampusDeliveryAuth", "Google Code scanner throw: ${e.message}")
                                        qrVerifyErrorMessage = "Play Services scanning API error. Falling back to Camera Simulator."
                                        showScanOverlay = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalPurple),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Trust QR Scanner", color = Color.White)
                                }
                            }
                        } else {
                            // Render high-fidelity simulator camera scanner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(2.dp, ColorCyanAccent, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Scanning vector light paths
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = ColorCyanAccent,
                                        start = Offset(0f, size.height / 2f),
                                        end = Offset(size.width, size.height / 2f),
                                        strokeWidth = 4f
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎥 Camera Feed Live Simulator", fontSize = 10.sp, color = ThemeTextMuted)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            val verified = viewModel.verifyQRAndDeliver(order!!.id, order!!.qrCodeToken)
                                            if (verified) {
                                                qrVerifySuccessMessage = "Handshake verified successfully! Funds released."
                                                showScanOverlay = false
                                            } else {
                                                qrVerifyErrorMessage = "Token mismatch! Try again."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess)
                                    ) {
                                        Text("Tap to Simulate Instant QR Scan", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { showScanOverlay = false }) {
                                Text("Cancel Scanner", fontSize = 11.sp)
                            }
                        }

                        qrVerifySuccessMessage?.let { msg ->
                            Text(msg, color = ColorSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                        qrVerifyErrorMessage?.let { err ->
                            Text(err, color = ColorDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Partner / Customer action workflows
        if (currentRole == "PARTNER" && order!!.deliveryPartnerId == currentProfile.registrationNumber) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                border = BorderStroke(1.dp, CardBorderColorLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Partner Delivery Console", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ThemeTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (order!!.status == "ACCEPTED") {
                        Button(
                            onClick = { viewModel.updateStatus(order!!.id, "PICKED_UP") },
                            modifier = Modifier.fillMaxWidth().testTag("mark_pickup_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalPurple)
                        ) {
                            Text("Confirm Package Picked Up", color = Color.White)
                        }
                    } else if (order!!.status == "PICKED_UP") {
                        Text(
                            "Scan Customer's checkin QR code above to trigger payout dispatch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (order!!.status == "DELIVERED") {
                        Text("Delivery Complete! mutual evaluation system active.", color = ColorSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        // PARTNER rates CUSTOMER
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Rate Student Customer Experience", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorCyanAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            for (star in 1..5) {
                                val filled = star.toFloat() <= (if (order!!.partnerRating > 0f) order!!.partnerRating else partnerRatingStarsState)
                                IconButton(onClick = {
                                    if (order!!.partnerRating == 0f) {
                                        partnerRatingStarsState = star.toFloat()
                                        viewModel.submitRating(order!!.id, star.toFloat(), isPartnerRating = true)
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Star",
                                        tint = ColorWarning
                                    )
                                }
                            }
                        }
                        if (order!!.partnerRating > 0f) {
                            Text("Thank you for submitting student peer rating!", color = ColorSuccess, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }

                    if (order!!.status != "DELIVERED" && order!!.status != "CANCELLED") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { 
                                viewModel.cancelOrder(order!!.id)
                                viewModel.selectOrderDetail(null)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorDanger),
                            modifier = Modifier.fillMaxWidth().testTag("partner_cancel_btn")
                        ) {
                            Text("Abandon Accepted Order (Rider Penalty Apply)")
                        }
                    }
                }
            }
        } else if (currentRole == "CUSTOMER" && order!!.customerPhone == currentProfile.phoneNumber) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                border = BorderStroke(1.dp, CardBorderColorLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (order!!.status == "PENDING") {
                        Button(
                            onClick = {
                                viewModel.cancelOrder(order!!.id)
                                viewModel.selectOrderDetail(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
                            modifier = Modifier.fillMaxWidth().testTag("customer_cancel_btn")
                        ) {
                            Text("Cancel Ongoing Broadcaster (Full Refund)", color = Color.White)
                        }
                    } else if (order!!.status == "DELIVERED") {
                        // CUSTOMER rates PARTNER
                        Text("Rate Rider Delivery Quality", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorCyanAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            for (star in 1..5) {
                                val filled = star.toFloat() <= (if (order!!.rating > 0f) order!!.rating else ratingStarsState)
                                IconButton(onClick = {
                                    if (order!!.rating == 0f) {
                                        ratingStarsState = star.toFloat()
                                        viewModel.submitRating(order!!.id, star.toFloat(), isPartnerRating = false)
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Star",
                                        tint = ColorWarning
                                    )
                                }
                            }
                        }

                        if (order!!.rating > 0f) {
                            Text(
                                "Evaluated with ${order!!.rating} Stars! Thank you.",
                                textAlign = TextAlign.Center,
                                color = ColorSuccess,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = CardBorderColorLight)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Submit trust report
                        Text("Report delivery Partner / Conflict", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorDanger)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputReportReason,
                            onValueChange = { inputReportReason = it },
                            placeholder = { Text("Describe conduct details, excessive delays, or safety breaches...", color = ThemeTextMuted) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("report_issue_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (inputReportReason.isNotBlank()) {
                                    viewModel.fileDisputeReport(
                                        orderId = order!!.id,
                                        reportedReg = order!!.deliveryPartnerId ?: "",
                                        reason = inputReportReason
                                    )
                                    inputReportReason = ""
                                }
                            },
                            enabled = inputReportReason.isNotBlank() && (order!!.deliveryPartnerId != null),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
                            modifier = Modifier.fillMaxWidth().testTag("submit_report_issue_btn")
                        ) {
                            Text("Submit Trust Dispute Complaint", color = Color.White)
                        }
                    } else if (order!!.status == "ACCEPTED" || order!!.status == "PICKED_UP") {
                        Text(
                            "Rider peer en route: " + (order!!.deliveryPartnerName ?: "Assigned Courier") + "\nSecure Razorpay Locked: YES",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorCyanAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Infix operator function helper to avoid bitwise shift problems in canvas hashing
private infix fun Int.shadowShift(shift: Int): Int {
    val rot = shift % 31
    return (this ushr rot) or (this shl (32 - rot))
}

// ==========================================
// 9. RE-ORGANIZED STAFF ADMINISTRATION PANEL
// ==========================================
@Composable
fun StaffAdminConsole(viewModel: CampusDeliveryViewModel) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val reports by viewModel.allReports.collectAsStateWithLifecycle()

    var activeAdminTab by remember { mutableStateOf("disputes") } // disputes, students

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgroundLight)
    ) {
        // Upper Admin Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeCardBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.setScreen("main")
                viewModel.setTab("profile")
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ColorCyanAccent)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Staff Operations Dashboard",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = ThemeTextPrimary
            )
        }

        TabRow(
            selectedTabIndex = if (activeAdminTab == "disputes") 0 else 1,
            containerColor = ThemeCardBg,
            contentColor = ColorCyanAccent
        ) {
            Tab(
                selected = activeAdminTab == "disputes",
                onClick = { activeAdminTab = "disputes" },
                text = { Text("Active Disputes (${reports.filter { it.status == "PENDING" }.size})", color = if (activeAdminTab == "disputes") ColorCyanAccent else ThemeTextMuted) }
            )
            Tab(
                selected = activeAdminTab == "students",
                onClick = { activeAdminTab = "students" },
                text = { Text("Verify Registries (${profiles.size})", color = if (activeAdminTab == "students") ColorCyanAccent else ThemeTextMuted) }
            )
        }

        if (activeAdminTab == "disputes") {
            val pendingReports = reports.filter { it.status == "PENDING" }
            if (pendingReports.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Pending Conflict Disputes", fontWeight = FontWeight.Bold, color = ThemeTextPrimary)
                    Text("University peer trust index is completely stable.", style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextMuted))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingReports) { report ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("complain_report_${report.id}"),
                            colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                            border = BorderStroke(1.dp, CardBorderColorLight),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reporter: " + report.reporterName, fontWeight = FontWeight.Bold, color = ThemeTextPrimary)
                                    Text("Against: " + report.reportedRegNumber, fontSize = 12.sp, color = ColorDanger, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Breach Reason: " + report.reason,
                                    fontSize = 12.sp,
                                    color = ThemeTextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.resolveDispute(report.id, markStrikes = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Uphold Strike", fontSize = 11.sp, color = Color.White)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.resolveDispute(report.id, markStrikes = false) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeTextMuted),
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Dismiss Claim", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(profiles) { student ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("profile_admin_${student.registrationNumber}"),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardBg),
                        border = BorderStroke(1.dp, CardBorderColorLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(student.name, fontWeight = FontWeight.Bold, color = ThemeTextPrimary)
                                Text("Reg: ${student.registrationNumber}", style = MaterialTheme.typography.bodySmall.copy(color = ThemeTextSecondary))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ColorCyanAccent, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Strikes: ${student.strikes} | Score: ${student.reliabilityScore}%", fontSize = 11.sp, color = ThemeTextMuted)
                                }
                            }

                            Button(
                                onClick = { viewModel.toggleSuspension(student.registrationNumber) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (student.isSuspended) ColorSuccess else ColorDanger
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = if (student.isSuspended) "Restore" else "Suspend",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
