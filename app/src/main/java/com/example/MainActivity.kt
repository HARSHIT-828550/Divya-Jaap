package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.DivyaJaapViewModelFactory
import com.example.navigation.DivyaJaapAppShell
import com.example.presentation.theme.DivyaJaapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as DivyaJaapApplication).container
        val viewModelFactory = DivyaJaapViewModelFactory(container)

        setContent {
            val settings by container.settingsRepository
                .observeSettings()
                .collectAsStateWithLifecycle(initialValue = com.example.database.UserSettings())

            LaunchedEffect(settings.screenAlwaysOn) {
                if (settings.screenAlwaysOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            DivyaJaapTheme {
                DivyaJaapAppShell(viewModelFactory = viewModelFactory)
            }
        }
    }
}
