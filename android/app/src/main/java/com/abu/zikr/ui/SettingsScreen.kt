package com.abu.zikr.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidProviderSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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

private const val AUTOMATIC_VOICE_LABEL = "Automatic (system default)"

/** Same handful of options as SettingsView.swift / settings_window.py /
 * SettingsForm.cs, grouped into cards with a line of guidance under
 * each control so the screen explains itself rather than assuming
 * familiarity with the app. */
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
    val bismillahOnUnlock by settings.bismillahOnUnlock.collectAsStateWithLifecycle(initialValue = true)
    val selectedVoiceName by settings.selectedVoiceName.collectAsStateWithLifecycle(initialValue = null)

    // Re-checked on every resume, since both permissions below are
    // granted in system Settings and the user comes back here - a
    // plain remember{} would go stale the moment they return.
    val lifecycleOwner = LocalLifecycleOwner.current
    var exactAlarmsAllowed by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    var batteryUnrestricted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsAllowed = scheduler.canScheduleExactAlarms()
                batteryUnrestricted = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Zikr") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Zikr reminds you on its own. Nothing here needs attention except how often, " +
                    "and which voice, you'd like to hear.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!exactAlarmsAllowed || !batteryUnrestricted) {
                PermissionWarnings(
                    exactAlarmsAllowed = exactAlarmsAllowed,
                    batteryUnrestricted = batteryUnrestricted,
                    context = context,
                )
            }

            SettingsSection(title = "General") {
                ToggleRow(
                    title = "Enable reminders",
                    subtitle = "Turn all reminders on or off.",
                    checked = enabled,
                    onCheckedChange = { value ->
                        scope.launch {
                            settings.setEnabled(value)
                            scheduler.onSettingsChanged()
                        }
                    },
                )
            }

            SettingsSection(title = "Timing") {
                Text(
                    "A reminder fires at a random moment somewhere in this range, then picks a new " +
                        "random moment for next time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Every $minMinutes–$maxMinutes min",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
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
            }

            SettingsSection(title = "Voice") {
                ToggleRow(
                    title = "Speak zikr aloud",
                    subtitle = "Read each reminder out loud, not just show it as a notification.",
                    checked = speakAloud,
                    onCheckedChange = { value -> scope.launch { settings.setSpeakAloud(value) } },
                )
                VoicePicker(
                    context = context,
                    selectedVoiceName = selectedVoiceName,
                    onVoiceSelected = { name -> scope.launch { settings.setSelectedVoiceName(name) } },
                )
            }

            SettingsSection(title = "System") {
                ToggleRow(
                    title = "Bismillah on unlock",
                    subtitle = "Say Bismillah once each time you unlock your phone. Only works " +
                        "while Zikr's background process is alive, which reminders keep fresh.",
                    checked = bismillahOnUnlock,
                    onCheckedChange = { value -> scope.launch { settings.setBismillahOnUnlock(value) } },
                )
            }

            SettingsSection(title = "Test") {
                Button(onClick = {
                    scope.launch {
                        val zikr = ZikrData.random(context)
                        NotificationHelper.show(context, zikr)
                        if (speakAloud) Speech.speak(context, zikr, selectedVoiceName)
                    }
                }) {
                    Text("Test Zikr (Speak + Notify)")
                }
                Text(
                    "Always speaks and shows a notification immediately, regardless of the toggle above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: android.content.Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

@Composable
private fun PermissionWarnings(
    exactAlarmsAllowed: Boolean,
    batteryUnrestricted: Boolean,
    context: android.content.Context,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Reminders may be delayed or skipped",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            if (!batteryUnrestricted) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Battery optimization can stop reminders from firing on time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(AndroidProviderSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${context.packageName}")),
                        )
                    }) {
                        Text("Allow background running")
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmsAllowed) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Without \"Alarms & reminders\", timing is approximate rather than exact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(AndroidProviderSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:${context.packageName}")),
                        )
                    }) {
                        Text("Allow exact alarms")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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

/**
 * Lists installed voices for whichever language Speech would actually
 * use (Arabic if available, else English) and lets the user pick one -
 * the system otherwise silently defaults to whatever voice a device
 * ships with (often female, with no in-app way to change it before this
 * existed). Selecting an entry previews it immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoicePicker(
    context: android.content.Context,
    selectedVoiceName: String?,
    onVoiceSelected: (String?) -> Unit,
) {
    var voices by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        voices = Speech.listVoicesForCurrentLanguage(context)
    }

    val displayLabel = selectedVoiceName ?: AUTOMATIC_VOICE_LABEL

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Voice", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Choose which installed voice reads the zikr aloud.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = displayLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(AUTOMATIC_VOICE_LABEL) },
                    onClick = {
                        expanded = false
                        onVoiceSelected(null)
                    },
                )
                voices.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onVoiceSelected(name)
                            scope.launch { Speech.preview(context, name, "SubhanAllah") }
                        },
                    )
                }
            }
        }

        if (voices.isEmpty()) {
            Text(
                "No installed voices found for this language yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
