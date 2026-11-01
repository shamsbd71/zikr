package com.abu.zikr.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the in-app update banner shows. */
sealed interface UpdateUiState {
    data object None : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateUiState
    /** Downloaded and verified; waiting on the system installer (or on
     * the user allowing Zikr to install apps). */
    data class ReadyToInstall(val info: UpdateInfo) : UpdateUiState
    data class Failed(val info: UpdateInfo, val message: String) : UpdateUiState
}

/** Process-wide holder so a check from anywhere (launch, the Settings
 * button, the once-a-day check piggybacked on reminders) lights up the
 * same banner. */
object UpdateState {
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.None)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun set(value: UpdateUiState) {
        _state.value = value
    }

    /** A fresh "available" result only replaces the banner if nothing
     * is already in progress for that same version. */
    fun offer(info: UpdateInfo) {
        val current = _state.value
        val busy = (current is UpdateUiState.Downloading && current.info == info) ||
            (current is UpdateUiState.ReadyToInstall && current.info == info)
        if (!busy) _state.value = UpdateUiState.Available(info)
    }

    fun clear() {
        _state.value = UpdateUiState.None
    }
}
