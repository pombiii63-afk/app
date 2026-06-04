package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CampusDeliveryAppContent
import com.example.ui.StaffAdminConsole
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CampusDeliveryViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {
    private var viewModelRef: CampusDeliveryViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Preload Razorpay to optimize transaction initiation speed
        try {
            Checkout.preload(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "Razorpay preload err: ${e.message}")
        }
        
        // Initialize Firebase on app launch
        var isFirebaseInit = false
        try {
            FirebaseApp.getInstance()
            isFirebaseInit = true
            Log.d("MainActivity", "FirebaseApp default instance already configured.")
        } catch (e: IllegalStateException) {
            try {
                val app = FirebaseApp.initializeApp(this)
                if (app != null) {
                    isFirebaseInit = true
                    Log.d("MainActivity", "Firebase initialized automatically in MainActivity.")
                } else {
                    Log.w("MainActivity", "Firebase automatically initialized returned null (missing google-services.json/keys strings).")
                }
            } catch (ex: Exception) {
                Log.e("MainActivity", "Failed to initialize Firebase automatically: ${ex.message}")
            }
        }

        if (!isFirebaseInit) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyB-campus-delivery-placeholderKey2")
                    .setApplicationId("1:123456789012:android:abcdef1234567890")
                    .setProjectId("campus-delivery-placeholder1")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.w("MainActivity", "Firebase initialized with safe placeholder fallback options in MainActivity.")
            } catch (failedEx: Exception) {
                Log.e("MainActivity", "Failed to initialize placeholder Firebase: ${failedEx.message}")
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: CampusDeliveryViewModel = viewModel()
                viewModelRef = viewModel
                val currentScreen by viewModel.currentScreen.collectAsState()
                
                if (currentScreen == "admin") {
                    StaffAdminConsole(viewModel = viewModel)
                } else {
                    CampusDeliveryAppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Log.d("MainActivity", "Razorpay Transaction Success: $razorpayPaymentId")
        viewModelRef?.onRazorpayPaymentSuccess(razorpayPaymentId ?: "TXN_OK")
    }

    override fun onPaymentError(errorCode: Int, errorMessage: String?) {
        Log.e("MainActivity", "Razorpay Transaction Failed: code=$errorCode, msg=$errorMessage")
        viewModelRef?.onRazorpayPaymentFailure(errorMessage ?: "Escrow transaction rejected by node.")
    }
}
