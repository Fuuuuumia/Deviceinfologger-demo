package com.fumibase.deviceinfologger_demo.data.source

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ThermalSource(private val context: Context) {
    private val powerManager = context.getSystemService(PowerManager::class.java)

    fun currentStatus(): Int? = runCatching { powerManager.currentThermalStatus }.getOrNull()

    fun thermalStatusFlow(): Flow<Int> = callbackFlow {
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            trySend(status)
        }
        powerManager.addThermalStatusListener(ContextCompat.getMainExecutor(context), listener)
        Log.d(TAG, "熱状態リスナーを登録しました")

        awaitClose {
            powerManager.removeThermalStatusListener(listener)
            Log.d(TAG, "熱状態リスナーを解除しました")
        }
    }

    fun headroom(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching { powerManager.getThermalHeadroom(10) }.getOrNull()?.takeIf { !it.isNaN() }
    }

    private companion object {
        const val TAG = "ThermalSource"
    }
}
