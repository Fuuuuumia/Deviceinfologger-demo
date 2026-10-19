package com.fumibase.deviceinfologger_demo

import android.app.Application
import android.util.Log
import com.fumibase.deviceinfologger_demo.data.DeviceRepository
import com.fumibase.deviceinfologger_demo.data.db.AppDatabase
import com.fumibase.deviceinfologger_demo.data.source.BatterySource
import com.fumibase.deviceinfologger_demo.data.source.BootSource
import com.fumibase.deviceinfologger_demo.data.source.DeviceInfoSource
import com.fumibase.deviceinfologger_demo.data.source.ThermalSource
import com.fumibase.deviceinfologger_demo.data.source.UsageSource
import com.fumibase.deviceinfologger_demo.work.DailyLogWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DeviceLogApp : Application() {
    val repository: DeviceRepository by lazy {
        val context = applicationContext
        DeviceRepository(
            dao = AppDatabase.create(context).snapshotDao(),
            deviceInfoSource = DeviceInfoSource(context),
            batterySource = BatterySource(context),
            thermalSource = ThermalSource(context),
            bootSource = BootSource(context),
            usageSource = UsageSource(context),
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        DailyLogWorker.schedule(this)

        appScope.launch {
            runCatching { repository.ensureLatestProfile() }
                .onFailure { Log.e("DeviceLogApp", "DeviceProfile の作成に失敗しました", it) }
        }
    }
}
