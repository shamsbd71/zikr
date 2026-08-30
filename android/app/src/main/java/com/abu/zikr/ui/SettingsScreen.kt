package com.abu.zikr.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidProviderSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu.zikr.data.ZikrData
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.scheduler.AlarmScheduler
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
    val scheduler = remember { AlarmScheduler(context) }
    val scope = rememberCoroutineScope()

    val enabled by settings.enabled.collectAsStateWithLifecycle(initialValue = true)
    val minMinutes by settings.minIntervalMinutes.collectAsStateWithLifecycle(initialValue = 20)
    val maxMinutes by settings.maxIntervalMinutes.collectAsStateWithLifecycle(initialValue = 45)
    val speakAloud by settings.speakAloud.collectAsStateWithLifecycle(initialValue = true)

    // Re-checked on every resume, since the user grants this in system
    // Settings and comes back - a plain remember{} would go stale.
    val lifecycleOwner = LocalLifecycleOwner.current
    var exactAlarmsAllowed by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsAllowed = scheduler.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmsAllowed) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Reminders may be delayed or skipped without \"Alarms & reminders\" permission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = {
                        context.startActivity(
                            Intent(AndroidProviderSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:${context.packageName}")),
                        )
                    }) {
                        Text("Allow exact alarms")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Timing", style = MaterialTheme.typography.titleSmall)
                Text("Every $minMinutes–$maxMinutes min", style = MaterialTheme.typography.bodyMedium)
            }

            MinuteStepper(
                label = "Min",
                value = minMinutes,
                onValueChange = { value ->
                    scope.launch {
                        settings.setMinInterval(value)
                        scheduler.onSettingsChanged()
                    }
                },
            )

            MinuteStepper(
                label = "Max",
                value = maxMinutes,
                onValueChange = { value ->
                    scope.launch {
                        settings.setMaxInterval(value)
                        scheduler.onSettingsChanged()
                    }
                },
            )

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

/**
 * A −/number/+ row for exact minute entry. Replaces a full-range
 * (1..180) Slider, which made it effectively impossible to land on a
 * precise low value like "1" or "2" for testing - a few pixels of drag
 * covered dozens of minutes.
 */
@Composable
private fun MinuteStepper(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(36.dp), style = MaterialTheme.typography.bodyMedium)

        IconButton(onClick = {
            val next = (value - 1).coerceIn(1, 180)
            text = next.toString()
            onValueChange(next)
        }) {
            Text("–", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                text = digitsOnly
                digitsOnly.toIntOrNull()?.let { parsed ->
                    if (parsed in 1..180) onValueChange(parsed)
                }
            },
            modifier = Modifier.width(72.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        IconButton(onClick = {
            val next = (value + 1).coerceIn(1, 180)
            text = next.toString()
            onValueChange(next)
        }) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }

        Text("min", style = MaterialTheme.typography.bodySmall)
    }
}
