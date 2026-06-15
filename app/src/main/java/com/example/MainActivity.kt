package com.example

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.service.ArcAccessibilityService
import com.example.service.OverlayCoordinator
import com.example.ui.DashboardScreen
import com.example.ui.MainViewModel
import com.example.ui.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val onboardingFinished by viewModel.isOnboardingCompleted.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (!onboardingFinished) {
                            OnboardingScreen(onOnboardingFinished = {
                                viewModel.completeOnboarding()
                            })
                        } else {
                            DashboardScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Proactive sync checking: auto-detect permission updates if returned from system settings
        OverlayCoordinator.isAccessibilityActive.value = (ArcAccessibilityService.instance != null)
        OverlayCoordinator.isOverlayServiceActive.value = Settings.canDrawOverlays(this)
        viewModel.refreshBlocklist()
    }
}
