package com.abu.zikr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu.zikr.data.ZikrData
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.scheduler.ReminderScheduler
import com.abu.zikr.settings.Settings
import com.abu.zikr.speech.Speech
import kotlinx.coroutines.launch

/** Same handful of options as SettingsView.swift / settings_window.py /
 * SettingsForm.cs - nothing more. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val scheduler = remember { ReminderScheduler(context) }
    val scope = rememberCoroutineScope()

    val enabled by settings.enabled.collectAsStateWithLifecycle(initialValue = true)
    val minMinutes by settings.minIntervalMinutes.collectAsStateWithLifecycle(initialValue = 20)
    val maxMinutes by settings.maxIntervalMinutes.collectAsStateWithLifecycle(initialValue = 45)
    val speakAloud by settings.speakAloud.collectAsStateWithLifecycle(initialValue = true)

    Scaffold(topBar = { TopAppBar(title = { Text("Zikr") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enable reminders")
                Switch(
                    checked = enabled,
                    onCheckedChange = { value ->
                        scope.launch {
                            settings.setEnabled(value)
                            scheduler.onSettingsChanged()
                        }
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Timing", style = MaterialTheme.typography.titleSmall)
                Text("Every $minMinutes–$maxMinutes min", style = MaterialTheme.typography.bodyMedium)
            }

            Column {
                Text("Min: $minMinutes min", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = minMinutes.toFloat(),
                    valueRange = 1f..180f,
                    onValueChange = { value ->
                        scope.launch {
                            settings.setMinInterval(value.toInt())
                            scheduler.onSettingsChanged()
                        }
                    },
                )
            }

            Column {
                Text("Max: $maxMinutes min", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = maxMinutes.toFloat(),
                    valueRange = 1f..180f,
                    onValueChange = { value ->
                        scope.launch {
                            settings.setMaxInterval(value.toInt())
                            scheduler.onSettingsChanged()
                        }
                    },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Speak zikr aloud")
                Switch(
                    checked = speakAloud,
                    onCheckedChange = { value -> scope.launch { settings.setSpeakAloud(value) } },
                )
            }

            Button(onClick = {
                scope.launch {
                    val zikr = ZikrData.random(context)
                    NotificationHelper.show(context, zikr)
                    if (speakAloud) Speech.speak(context, zikr)
                }
            }) {
                Text("Test Zikr (Speak + Notify)")
            }

            Text(
                "Test always speaks the zikr aloud and shows a notification, regardless of the toggle above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
