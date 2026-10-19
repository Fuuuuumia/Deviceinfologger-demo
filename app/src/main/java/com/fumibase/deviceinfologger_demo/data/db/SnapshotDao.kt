package com.fumibase.deviceinfologger_demo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert
    suspend fun insertProfile(profile: DeviceProfile): Long

    @Query("SELECT * FROM device_profile ORDER BY id DESC LIMIT 1")
    suspend fun latestProfile(): DeviceProfile?

    @Query("SELECT * FROM device_profile ORDER BY id DESC LIMIT 1")
    fun observeLatestProfile(): Flow<DeviceProfile?>

    @Insert
    suspend fun insertSnapshot(snapshot: DailySnapshot): Long

    @Query(
        "SELECT * FROM daily_snapshot WHERE isBackfill = 0 AND bootCount IS NOT NULL " +
            "ORDER BY recordedAt DESC LIMIT 1"
    )
    suspend fun latestSnapshotWithBootCount(): DailySnapshot?

    @Query("SELECT COUNT(*) FROM daily_snapshot WHERE targetDate = :date")
    suspend fun countByTargetDate(date: String): Int

    @Query("SELECT * FROM daily_snapshot ORDER BY targetDate DESC, recordedAt DESC")
    fun observeSnapshots(): Flow<List<DailySnapshot>>
}
