package com.fumibase.deviceinfologger_demo.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_profile")
data class DeviceProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,

    val deviceId: String?,
    val installId: String,

    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val deviceName: String?,
    val socManufacturer: String?,
    val socModel: String?,
    val hardware: String,
    val board: String,
    val abis: String,
    val cpuCores: Int,
    val totalRamBytes: Long?,
    val totalStorageBytes: Long?,
    val screenWidthPx: Int?,
    val screenHeightPx: Int?,
    val densityDpi: Int?,
    val refreshRates: String?,
    val cameraCount: Int?,
    val sensorCount: Int?,
    val batteryTechnology: String?,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String?,
    val buildFingerprint: String,
)
