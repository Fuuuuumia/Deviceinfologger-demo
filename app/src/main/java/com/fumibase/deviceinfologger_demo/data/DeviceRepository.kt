package com.fumibase.deviceinfologger_demo.data

import android.os.Build
import android.util.Log
import com.fumibase.deviceinfologger_demo.data.db.DailySnapshot
import com.fumibase.deviceinfologger_demo.data.db.DeviceProfile
import com.fumibase.deviceinfologger_demo.data.db.SnapshotDao
import com.fumibase.deviceinfologger_demo.data.source.BatterySource
import com.fumibase.deviceinfologger_demo.data.source.BatteryState
import com.fumibase.deviceinfologger_demo.data.source.BootSource
import com.fumibase.deviceinfologger_demo.data.source.BootTimes
import com.fumibase.deviceinfologger_demo.data.source.DeviceInfoSource
import com.fumibase.deviceinfologger_demo.data.source.ScreenStats
import com.fumibase.deviceinfologger_demo.data.source.ThermalSource
import com.fumibase.deviceinfologger_demo.data.source.UsageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class DeviceRepository(
    private val dao: SnapshotDao,
    private val deviceInfoSource: DeviceInfoSource,
    private val batterySource: BatterySource,
    private val thermalSource: ThermalSource,
    private val bootSource: BootSource,
    private val usageSource: UsageSource,
) {
    private val profileMutex = Mutex()

    fun observeLatestProfile(): Flow<DeviceProfile?> = dao.observeLatestProfile()

    suspend fun ensureLatestProfile(): DeviceProfile = withContext(Dispatchers.IO) {
        profileMutex.withLock {
            val latest = dao.latestProfile()
            val currentFingerprint = DEBUG_FINGERPRINT_OVERRIDE ?: Build.FINGERPRINT
            if (latest != null &&
                latest.buildFingerprint == currentFingerprint &&
                latest.sdkInt == Build.VERSION.SDK_INT
            ) {
                return@withLock latest
            }

            val technology = batterySource.snapshot()?.technology
            val profile = deviceInfoSource.collectProfile(technology)
                .copy(buildFingerprint = currentFingerprint)
            val id = dao.insertProfile(profile)
            Log.i(TAG, "DeviceProfile を追加しました id=$id (以前の fingerprint=${latest?.buildFingerprint})")
            profile.copy(id = id)
        }
    }

    fun observeSnapshots(): Flow<List<DailySnapshot>> = dao.observeSnapshots()

    suspend fun recordDailySnapshot() = withContext(Dispatchers.IO) {
        val profile = ensureLatestProfile()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val battery = batterySource.snapshot()
        val times = bootSource.times()
        val bootCount = bootSource.bootCount()
        val prevBootCount = dao.latestSnapshotWithBootCount()?.bootCount
        val reboots = if (bootCount != null && prevBootCount != null) {
            (bootCount - prevBootCount).takeIf { it >= 0 }
        } else {
            null
        }
        val screen = screenStatsOf(yesterday)

        val snapshot = DailySnapshot(
            profileId = profile.id,
            recordedAt = System.currentTimeMillis(),
            targetDate = yesterday.toString(),
            batteryLevel = battery?.level,
            batteryTempC = battery?.tempC,
            batteryVoltageMv = battery?.voltageMv,
            batteryHealth = battery?.health,
            cycleCount = battery?.cycleCount,
            chargeCounterUah = battery?.chargeCounterUah,
            estimatedFullUah = battery?.estimatedFullUah,
            isCharging = battery?.isCharging,
            bootCount = bootCount,
            rebootsSincePrev = reboots,
            elapsedRealtimeMs = times.elapsedRealtimeMs,
            uptimeMs = times.uptimeMs,
            screenOnMs = screen?.screenOnMs,
            screenOnCount = screen?.screenOnCount,
            unlockCount = screen?.unlockCount,
            thermalStatus = thermalSource.currentStatus(),
        )
        val id = dao.insertSnapshot(snapshot)
        Log.i(TAG, "DailySnapshot を記録しました id=$id targetDate=${snapshot.targetDate}")

        backfillMissingDays(profile.id, today)
    }

    private suspend fun backfillMissingDays(profileId: Long, today: LocalDate) {
        if (!usageSource.hasPermission()) return
        for (daysAgo in 2..BACKFILL_DAYS) {
            val date = today.minusDays(daysAgo.toLong())
            if (dao.countByTargetDate(date.toString()) > 0) continue
            val stats = screenStatsOf(date) ?: continue
            if (stats.eventCount == 0) continue
            dao.insertSnapshot(
                DailySnapshot(
                    profileId = profileId,
                    recordedAt = System.currentTimeMillis(),
                    targetDate = date.toString(),
                    isBackfill = true,
                    screenOnMs = stats.screenOnMs,
                    screenOnCount = stats.screenOnCount,
                    unlockCount = stats.unlockCount,
                )
            )
            Log.i(TAG, "補完記録を追加しました targetDate=$date")
        }
    }

    private fun screenStatsOf(date: LocalDate): ScreenStats? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return usageSource.screenStats(start, end)
    }

    fun batteryFlow(): Flow<BatteryState> = batterySource.batteryFlow()
    fun currentNowRaw(): Int? = batterySource.currentNowRaw()
    fun thermalStatusFlow(): Flow<Int> = thermalSource.thermalStatusFlow()
    fun thermalHeadroom(): Float? = thermalSource.headroom()
    fun bootTimes(): BootTimes = bootSource.times()
    fun bootCount(): Int? = bootSource.bootCount()
    fun hasUsagePermission(): Boolean = usageSource.hasPermission()

    suspend fun todayScreenStats(): ScreenStats? = withContext(Dispatchers.IO) {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        usageSource.screenStats(start, System.currentTimeMillis())
    }

    private companion object {
        const val TAG = "DeviceRepository"
        const val BACKFILL_DAYS = 7

        val DEBUG_FINGERPRINT_OVERRIDE: String? = null
    }
}
