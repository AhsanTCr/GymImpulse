package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hooks.rememberThemeController
import com.example.navigation.ApexPulseAppNavHost
import com.example.ui.theme.ApexPulseTheme
import com.example.ui.theme.AppThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val (themeMode, setThemeMode) = rememberThemeController(AppThemeMode.DARK)
            ApexPulseTheme(themeMode = themeMode) {
                ApexPulseAppNavHost(
                    currentThemeMode = themeMode,
                    onChangeThemeMode = setThemeMode
                )
            }
        }
    }
}
