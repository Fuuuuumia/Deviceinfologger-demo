package com.fumibase.deviceinfologger_demo.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_snapshot",
    foreignKeys = [
        ForeignKey(
            entity = DeviceProfile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
        ),
    ],
    indices = [Index("profileId"), Index("targetDate")],
)
data class DailySnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val recordedAt: Long,
    val targetDate: String,
    val isBackfill: Boolean = false,

    val batteryLevel: Int? = null,
    val batteryTempC: Float? = null,
    val batteryVoltageMv: Int? = null,
    val batteryHealth: Int? = null,
    val cycleCount: Int? = null,
    val chargeCounterUah: Int? = null,
    val estimatedFullUah: Int? = null,
    val isCharging: Boolean? = null,

    val bootCount: Int? = null,
    val rebootsSincePrev: Int? = null,
    val elapsedRealtimeMs: Long? = null,
    val uptimeMs: Long? = null,

    val screenOnMs: Long? = null,
    val screenOnCount: Int? = null,
    val unlockCount: Int? = null,

    val thermalStatus: Int? = null,
)
