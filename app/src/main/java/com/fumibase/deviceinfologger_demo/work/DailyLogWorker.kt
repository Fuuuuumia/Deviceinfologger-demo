package com.fumibase.deviceinfologger_demo.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fumibase.deviceinfologger_demo.DeviceLogApp
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class DailyLogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as DeviceLogApp).repository
        return try {
            repository.recordDailySnapshot()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "記録に失敗しました(試行 ${runAttemptCount + 1} 回目)", e)
            if (runAttemptCount < MAX_RETRY) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "DailyLogWorker"
        private const val UNIQUE_NAME = "daily_log"
        private const val MAX_RETRY = 5

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyLogWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DailyLogWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
