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
