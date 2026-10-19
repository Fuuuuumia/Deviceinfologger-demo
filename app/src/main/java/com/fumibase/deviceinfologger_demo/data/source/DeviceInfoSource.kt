package com.fumibase.deviceinfologger_demo.data.source

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.fumibase.deviceinfologger_demo.data.db.DeviceProfile
import java.util.UUID
import kotlin.math.roundToInt

class DeviceInfoSource(private val context: Context) {
    fun collectProfile(batteryTechnology: String?): DeviceProfile =
        collectProfile(batteryTechnology, maxScreenSize())

    private fun collectProfile(batteryTechnology: String?, screenSize: Pair<Int, Int>?) = DeviceProfile(
        createdAt = System.currentTimeMillis(),
        deviceId = androidId(),
        installId = getOrCreateInstallId(),
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        deviceName = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull(),
        socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null,
        socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null,
        hardware = Build.HARDWARE,
        board = Build.BOARD,
        abis = Build.SUPPORTED_ABIS.joinToString(),
        cpuCores = Runtime.getRuntime().availableProcessors(),
        totalRamBytes = totalRamBytes(),
        totalStorageBytes = totalStorageBytes(),
        screenWidthPx = screenSize?.first,
        screenHeightPx = screenSize?.second,
        densityDpi = runCatching { context.resources.displayMetrics.densityDpi }.getOrNull(),
        refreshRates = refreshRates(),
        cameraCount = runCatching {
            context.getSystemService(CameraManager::class.java).cameraIdList.size
        }.getOrNull(),
        sensorCount = runCatching {
            context.getSystemService(SensorManager::class.java).getSensorList(Sensor.TYPE_ALL).size
        }.getOrNull(),
        batteryTechnology = batteryTechnology,
        androidVersion = Build.VERSION.RELEASE,
        sdkInt = Build.VERSION.SDK_INT,
        securityPatch = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() },
        buildFingerprint = Build.FINGERPRINT,
    )

    @SuppressLint("HardwareIds")
    private fun androidId(): String? = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull()

    fun getOrCreateInstallId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_INSTALL_ID, null)?.let { return it }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_ID, newId).apply()
        return newId
    }

    private fun totalRamBytes(): Long? = runCatching {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.totalMem
    }.getOrNull()

    private fun totalStorageBytes(): Long? = runCatching {
        context.getSystemService(StorageStatsManager::class.java).getTotalBytes(StorageManager.UUID_DEFAULT)
    }.getOrNull()

    private fun maxScreenSize(): Pair<Int, Int>? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            defaultDisplay()?.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }.getOrNull()

    private fun refreshRates(): String? = runCatching {
        defaultDisplay()?.supportedModes
            ?.map { it.refreshRate.roundToInt() }
            ?.distinct()
            ?.sorted()
            ?.joinToString(",")
    }.getOrNull()

    private fun defaultDisplay(): Display? =
        context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)

    private companion object {
        const val PREFS_NAME = "device_log_prefs"
        const val KEY_INSTALL_ID = "install_id"
    }
}
