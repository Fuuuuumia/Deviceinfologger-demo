package com.fumibase.deviceinfologger_demo.ui.live

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fumibase.deviceinfologger_demo.data.DeviceRepository
import com.fumibase.deviceinfologger_demo.data.db.DeviceProfile
import com.fumibase.deviceinfologger_demo.data.source.BatteryState
import com.fumibase.deviceinfologger_demo.data.source.BootTimes
import com.fumibase.deviceinfologger_demo.data.source.ScreenStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LiveUiState(
    val battery: BatteryState? = null,
    val currentNowRaw: Int? = null,
    val thermalStatus: Int? = null,
    val thermalHeadroom: Float? = null,
    val bootTimes: BootTimes? = null,
    val bootCount: Int? = null,
    val todayScreen: ScreenStats? = null,
    val hasUsagePermission: Boolean = false,
)

class LiveViewModel(private val repository: DeviceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveUiState(hasUsagePermission = repository.hasUsagePermission()))
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    val profile: StateFlow<DeviceProfile?> = repository.observeLatestProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        Log.d(TAG, "監視を開始します")

        monitoringJob = viewModelScope.launch {
            launch {
                repository.batteryFlow().collect { battery ->
                    _uiState.update { it.copy(battery = battery) }
                }
            }
            launch {
                repository.thermalStatusFlow().collect { status ->
                    _uiState.update { it.copy(thermalStatus = status) }
                }
            }
            launch {
                while (isActive) {
                    val current = repository.currentNowRaw()
                    _uiState.update { it.copy(currentNowRaw = current) }
                    delay(2_000)
                }
            }
            launch {
                while (isActive) {
                    val headroom = repository.thermalHeadroom()
                    _uiState.update { it.copy(thermalHeadroom = headroom) }
                    delay(10_000)
                }
            }
            launch {
                val bootCount = repository.bootCount()
                while (isActive) {
                    val times = repository.bootTimes()
                    _uiState.update { it.copy(bootTimes = times, bootCount = bootCount) }
                    delay(1_000)
                }
            }
            launch(Dispatchers.IO) {
                val hasPermission = repository.hasUsagePermission()
                val stats = if (hasPermission) repository.todayScreenStats() else null
                _uiState.update { it.copy(todayScreen = stats, hasUsagePermission = hasPermission) }
            }
        }
    }

    fun stopMonitoring() {
        if (monitoringJob == null) return
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "監視を停止しました")
    }

    private companion object {
        const val TAG = "LiveViewModel"
    }
}
