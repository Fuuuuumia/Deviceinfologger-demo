package com.fumibase.deviceinfologger_demo.data.source

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

data class ScreenStats(
    val screenOnMs: Long,
    val screenOnCount: Int,
    val unlockCount: Int,
    val eventCount: Int,
)

class UsageSource(private val context: Context) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)

    @Suppress("DEPRECATION")
    fun hasPermission(): Boolean = runCatching {
        appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun screenStats(startMs: Long, endMs: Long): ScreenStats? {
        if (!hasPermission()) return null
        return runCatching {
            val events = usageStatsManager.queryEvents(startMs, endMs) ?: return null
            val event = UsageEvents.Event()

            var onSince: Long? = null
            var seenScreenEvent = false
            var totalOnMs = 0L
            var onCount = 0
            var unlockCount = 0
            var eventCount = 0

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                eventCount++
                when (event.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        if (onSince == null) onSince = event.timeStamp
                        onCount++
                        seenScreenEvent = true
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        val since = onSince ?: if (!seenScreenEvent) startMs else null
                        if (since != null) totalOnMs += event.timeStamp - since
                        onSince = null
                        seenScreenEvent = true
                    }
                    UsageEvents.Event.KEYGUARD_HIDDEN -> unlockCount++
                }
            }
            onSince?.let { totalOnMs += endMs - it }

            ScreenStats(totalOnMs, onCount, unlockCount, eventCount)
        }.getOrNull()
    }
}
