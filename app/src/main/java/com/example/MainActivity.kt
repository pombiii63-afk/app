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
        
        // Initialize Firebase programmatically on start to ensure safety
        try {
            FirebaseApp.getInstance()
            Log.d("MainActivity", "FirebaseApp default instance already exists.")
        } catch (e: IllegalStateException) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyB_campus_deliv_fallback_key_2026")
                    .setApplicationId("1:123456789012:android:abcdef1234567890")
                    .setProjectId("campus-delivery-rxcpt")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MainActivity", "Firebase initialized programmatically in MainActivity.")
            } catch (ex: Exception) {
                Log.e("MainActivity", "Failed to initialize Firebase programmatically: ${ex.message}")
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
