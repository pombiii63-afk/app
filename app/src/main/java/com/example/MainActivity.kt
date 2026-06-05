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
        try {
            FirebaseApp.getInstance()
            Log.d("MainActivity", "FirebaseApp default instance already configured.")
        } catch (e: IllegalStateException) {
            try {
                FirebaseApp.initializeApp(this)
                Log.d("MainActivity", "Firebase initialized automatically in MainActivity.")
            } catch (ex: Exception) {
                Log.e("MainActivity", "Failed to initialize Firebase automatically: ${ex.message}")
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
