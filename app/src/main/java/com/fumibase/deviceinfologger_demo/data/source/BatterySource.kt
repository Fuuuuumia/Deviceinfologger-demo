package com.fumibase.deviceinfologger_demo.data.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryState(
    val level: Int?,
    val tempC: Float?,
    val voltageMv: Int?,
    val health: Int?,
    val status: Int?,
    val plugged: Int?,
    val isCharging: Boolean?,
    val cycleCount: Int?,
    val chargeCounterUah: Int?,
    val estimatedFullUah: Int?,
    val technology: String?,
)

class BatterySource(private val context: Context) {
    private val batteryManager = context.getSystemService(BatteryManager::class.java)

    fun snapshot(): BatteryState? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        return parse(intent).copy(isCharging = runCatching { batteryManager.isCharging }.getOrNull())
    }

    fun batteryFlow(): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(parse(intent))
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        Log.d(TAG, "ACTION_BATTERY_CHANGED の Receiver を登録しました")

        awaitClose {
            context.unregisterReceiver(receiver)
            Log.d(TAG, "ACTION_BATTERY_CHANGED の Receiver を解除しました")
        }
    }

    fun currentNowRaw(): Int? = runCatching {
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    }.getOrNull()?.takeIf { it != Int.MIN_VALUE }

    private fun chargeCounterUah(): Int? = runCatching {
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    }.getOrNull()?.takeIf { it > 0 && it != Int.MIN_VALUE }

    private fun parse(intent: Intent): BatteryState {
        val level = runCatching {
            val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (raw >= 0 && scale > 0) raw * 100 / scale else null
        }.getOrNull()
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1).takeIf { it >= 0 }
        val chargeCounter = chargeCounterUah()

        return BatteryState(
            level = level,
            tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }?.let { it / 10f },
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it > 0 },
            health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1).takeIf { it >= 0 },
            status = status,
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1).takeIf { it >= 0 },
            isCharging = status?.let {
                it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
            },
            cycleCount = cycleCount(intent),
            chargeCounterUah = chargeCounter,
            estimatedFullUah = estimateFullUah(chargeCounter, level),
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
        )
    }

    private fun cycleCount(intent: Intent): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it >= 0 }
    }

    private fun estimateFullUah(chargeCounterUah: Int?, level: Int?): Int? {
        if (chargeCounterUah == null || level == null || level < 50) return null
        return (chargeCounterUah / (level / 100.0)).toInt()
    }

    private companion object {
        const val TAG = "BatterySource"
    }
}
