package com.fumibase.deviceinfologger_demo.data.source

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

data class BootTimes(
    val elapsedRealtimeMs: Long,
    val uptimeMs: Long,
) {
    val sleepMs: Long get() = elapsedRealtimeMs - uptimeMs
}

class BootSource(private val context: Context) {
    fun bootCount(): Int? = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrNull()

    fun times(): BootTimes = BootTimes(
        elapsedRealtimeMs = SystemClock.elapsedRealtime(),
        uptimeMs = SystemClock.uptimeMillis(),
    )
}
