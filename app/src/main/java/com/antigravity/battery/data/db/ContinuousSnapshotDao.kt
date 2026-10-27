package com.antigravity.battery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContinuousSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PeriodicSnapshotEntity): Long

    @Query("SELECT * FROM periodic_snapshots WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs ASC")
    suspend fun getSnapshotsBetween(startMs: Long, endMs: Long): List<PeriodicSnapshotEntity>

    @Query("SELECT id, timestampMs, batteryLevel, voltageMv, tempDeciCelsius, isCharging, isScreenOn, totalRealtimeMs, totalUptimeMs, screenOnDurationMs, screenOffDurationMs FROM periodic_snapshots WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs ASC")
    suspend fun getLightweightSnapshotsBetween(startMs: Long, endMs: Long): List<LightweightSnapshot>

    @Query("SELECT id, timestampMs, batteryLevel, voltageMv, tempDeciCelsius, isCharging, isScreenOn, totalRealtimeMs, totalUptimeMs, screenOnDurationMs, screenOffDurationMs FROM periodic_snapshots WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    suspend fun getLightweightSnapshotsSince(sinceMs: Long): List<LightweightSnapshot>

    @Query("SELECT id, timestampMs, batteryLevel, voltageMv, tempDeciCelsius, isCharging, isScreenOn, totalRealtimeMs, totalUptimeMs, screenOnDurationMs, screenOffDurationMs FROM periodic_snapshots ORDER BY timestampMs ASC")
    suspend fun getAllLightweightSnapshots(): List<LightweightSnapshot>

    @Query("SELECT * FROM periodic_snapshots WHERE id IN (:ids)")
    suspend fun getSnapshotsByIds(ids: List<Long>): List<PeriodicSnapshotEntity>

    suspend fun getSnapshotsByIdsChunked(ids: List<Long>, chunkSize: Int = 25): List<PeriodicSnapshotEntity> {
        if (ids.isEmpty()) return emptyList()
        val uniqueIds = ids.distinct()
        val result = ArrayList<PeriodicSnapshotEntity>(uniqueIds.size)
        for (chunk in uniqueIds.chunked(chunkSize)) {
            try {
                result.addAll(getSnapshotsByIds(chunk))
            } catch (e: Exception) {
                android.util.Log.e("ContinuousDao", "Error fetching snapshot chunk of size ${chunk.size}: ${e.message}")
            }
        }
        return result
    }

    @Query("SELECT * FROM periodic_snapshots ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLatestSnapshot(): PeriodicSnapshotEntity?

    @Query("SELECT * FROM periodic_snapshots ORDER BY timestampMs DESC LIMIT 1")
    fun getLatestSnapshotFlow(): Flow<PeriodicSnapshotEntity?>

    @Query("SELECT * FROM periodic_snapshots WHERE isCharging = 1 AND batteryLevel >= 90 ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLastFullChargeSnapshot(): PeriodicSnapshotEntity?

    @Query("SELECT * FROM periodic_snapshots WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    suspend fun getSnapshotsSince(sinceMs: Long): List<PeriodicSnapshotEntity>

    @Query("SELECT * FROM periodic_snapshots WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    fun getSnapshotsSinceFlow(sinceMs: Long): Flow<List<PeriodicSnapshotEntity>>

    @Query("DELETE FROM periodic_snapshots WHERE timestampMs < :cutoffMs")
    suspend fun pruneSnapshotsOlderThan(cutoffMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRollup(rollup: DailyRollupEntity)

    @Query("SELECT * FROM daily_rollups ORDER BY dateString DESC")
    fun getAllDailyRollups(): Flow<List<DailyRollupEntity>>
}
