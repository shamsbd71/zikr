package com.abu.zikr.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abu.zikr.update.UpdateFlow
import com.abu.zikr.update.UpdateState
import com.abu.zikr.update.UpdateUiState

/**
 * The in-app update notice, shown at the top of the screen whenever a
 * newer release exists. "Update now" downloads inside the app and opens
 * the system installer - no browser, no hunting for the file.
 */
@Composable
fun UpdateBanner() {
    val state by UpdateState.state.collectAsState()
    val activity = LocalContext.current as? Activity ?: return

    val (title, info) = when (val s = state) {
        UpdateUiState.None -> return
        is UpdateUiState.Available -> "Zikr ${s.info.version} is available" to s.info
        is UpdateUiState.Downloading -> "Downloading Zikr ${s.info.version}…" to s.info
        is UpdateUiState.ReadyToInstall -> "Zikr ${s.info.version} is ready to install" to s.info
        is UpdateUiState.Failed -> "Zikr ${s.info.version} is available" to s.info
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)

            when (val s = state) {
                is UpdateUiState.Available -> {
                    Button(onClick = { UpdateFlow.downloadAndInstall(activity, info) }) { Text("Update now") }
                }
                is UpdateUiState.Downloading -> {
                    if (s.percent >= 0) {
                        LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                is UpdateUiState.ReadyToInstall -> {
                    Text(
                        "Android will ask you to confirm. The first time, it also asks you to allow Zikr to install updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(onClick = { UpdateFlow.install(activity, info) }) { Text("Install") }
                }
                is UpdateUiState.Failed -> {
                    Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { UpdateFlow.downloadAndInstall(activity, info) }) { Text("Try again") }
                }
                UpdateUiState.None -> Unit
            }
        }
    }
}
