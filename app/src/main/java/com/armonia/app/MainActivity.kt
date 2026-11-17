package com.armonia.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armonia.app.ui.AppState
import com.armonia.app.ui.AppViewModel
import com.armonia.app.ui.components.LoadingScreen
import com.armonia.app.ui.navigation.ArmoniaNavHost
import com.armonia.app.ui.onboarding.OnboardingScreen
import com.armonia.app.ui.theme.ArmoniaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            ArmoniaTheme {
                val appViewModel: AppViewModel = viewModel()
                val state by appViewModel.state.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val current = state) {
                        AppState.Loading -> LoadingScreen()

                        AppState.NeedsOnboarding -> OnboardingScreen(
                            onComplete = appViewModel::completeOnboarding
                        )

                        is AppState.Ready -> ArmoniaNavHost(
                            profile = current.profile,
                            onProfileChange = appViewModel::updateProfile
                        )
                    }
                }
            }
        }
    }
}
