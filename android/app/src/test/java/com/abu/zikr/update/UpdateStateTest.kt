package com.abu.zikr.update

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStateTest {
    private val v2 = UpdateInfo("2.0.0", "https://example.invalid/Zikr-2.0.0.apk")
    private val v3 = UpdateInfo("3.0.0", "https://example.invalid/Zikr-3.0.0.apk")

    @After
    fun reset() = UpdateState.clear()

    @Test
    fun offerShowsAvailable() {
        UpdateState.offer(v2)
        assertEquals(UpdateUiState.Available(v2), UpdateState.state.value)
    }

    @Test
    fun offerDoesNotInterruptDownloadOfSameVersion() {
        UpdateState.set(UpdateUiState.Downloading(v2, 40))
        UpdateState.offer(v2)
        assertEquals(UpdateUiState.Downloading(v2, 40), UpdateState.state.value)
    }

    @Test
    fun offerDoesNotHideReadyInstallOfSameVersion() {
        UpdateState.set(UpdateUiState.ReadyToInstall(v2))
        UpdateState.offer(v2)
        assertEquals(UpdateUiState.ReadyToInstall(v2), UpdateState.state.value)
    }

    @Test
    fun newerVersionReplacesStaleDownload() {
        UpdateState.set(UpdateUiState.ReadyToInstall(v2))
        UpdateState.offer(v3)
        assertEquals(UpdateUiState.Available(v3), UpdateState.state.value)
    }

    @Test
    fun failedCanBeRetriedByOffer() {
        UpdateState.set(UpdateUiState.Failed(v2, "x"))
        UpdateState.offer(v2)
        assertEquals(UpdateUiState.Available(v2), UpdateState.state.value)
    }
}
