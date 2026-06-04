package com.example

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: CampusDeliveryViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                
                if (currentScreen == "admin") {
                    StaffAdminConsole(viewModel = viewModel)
                } else {
                    CampusDeliveryAppContent(viewModel = viewModel)
                }
            }
        }
    }
}
