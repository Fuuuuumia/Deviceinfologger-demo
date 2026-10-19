package com.fumibase.deviceinfologger_demo.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fumibase.deviceinfologger_demo.data.DeviceRepository
import com.fumibase.deviceinfologger_demo.data.db.DailySnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    repository: DeviceRepository,
    private val requestRecordNow: () -> Unit,
) : ViewModel() {
    val snapshots: StateFlow<List<DailySnapshot>> = repository.observeSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun recordNow() = requestRecordNow()
}
