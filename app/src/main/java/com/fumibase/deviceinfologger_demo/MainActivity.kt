package com.fumibase.deviceinfologger_demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fumibase.deviceinfologger_demo.ui.AppNavHost
import com.fumibase.deviceinfologger_demo.ui.theme.DeviceInfologgerdemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as DeviceLogApp).repository
        setContent {
            DeviceInfologgerdemoTheme {
                AppNavHost(repository)
            }
        }
    }
}
