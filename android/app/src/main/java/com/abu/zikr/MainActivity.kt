package com.abu.zikr

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidProviderSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.abu.zikr.ui.SettingsScreen
import com.abu.zikr.ui.theme.ZikrTheme
import com.abu.zikr.update.UpdateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZikrTheme {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    // Reminders can be delayed or silently dropped by
                    // battery optimization even with an exact alarm
                    // scheduled - this is the one-time system dialog
                    // ("Allow Zikr to ignore battery optimizations?")
                    // that actually asks for background-running
                    // permission, which is what a reminder app needs.
                    val powerManager = context.getSystemService(PowerManager::class.java)
                    if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false) {
                        context.startActivity(
                            Intent(AndroidProviderSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${context.packageName}")),
                        )
                    }

                    UpdateFlow.checkNow(context)
                }

                SettingsScreen()
            }
        }
    }
}
