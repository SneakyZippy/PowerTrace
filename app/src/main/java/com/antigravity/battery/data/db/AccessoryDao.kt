package com.antigravity.battery.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessoryDao {

    // --- Power Bricks ---
    @Query("SELECT * FROM power_bricks ORDER BY createdAt DESC")
    fun getAllBricks(): Flow<List<PowerBrickEntity>>

    @Query("SELECT * FROM power_bricks WHERE id = :brickId LIMIT 1")
    suspend fun getBrickById(brickId: String): PowerBrickEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrick(brick: PowerBrickEntity)

    @Update
    suspend fun updateBrick(brick: PowerBrickEntity)

    @Delete
    suspend fun deleteBrick(brick: PowerBrickEntity)

    // --- Cables ---
    @Query("SELECT * FROM cables ORDER BY createdAt DESC")
    fun getAllCables(): Flow<List<CableEntity>>

    @Query("SELECT * FROM cables WHERE id = :cableId LIMIT 1")
    suspend fun getCableById(cableId: String): CableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCable(cable: CableEntity)

    @Update
    suspend fun updateCable(cable: CableEntity)

    @Delete
    suspend fun deleteCable(cable: CableEntity)

    // --- Charge Test Sessions ---
    @Query("SELECT * FROM charge_test_sessions ORDER BY startTimeMs DESC")
    fun getAllSessions(): Flow<List<ChargeTestSessionEntity>>

    @Query("SELECT * FROM charge_test_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChargeTestSessionEntity?

    @Query("SELECT * FROM charge_test_sessions WHERE brickId = :brickId ORDER BY peakWattage DESC")
    fun getSessionsForBrick(brickId: String): Flow<List<ChargeTestSessionEntity>>

    @Query("SELECT * FROM charge_test_sessions WHERE cableId = :cableId ORDER BY peakWattage DESC")
    fun getSessionsForCable(cableId: String): Flow<List<ChargeTestSessionEntity>>

    @Query("UPDATE charge_test_sessions SET brickName = :newName WHERE brickId = :brickId")
    suspend fun updateSessionBrickName(brickId: String, newName: String)

    @Query("UPDATE charge_test_sessions SET cableName = :newName WHERE cableId = :cableId")
    suspend fun updateSessionCableName(cableId: String, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargeTestSessionEntity)

    @Delete
    suspend fun deleteSession(session: ChargeTestSessionEntity)

    // --- High-Resolution Samples ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<ChargeSampleEntity>)

    @Query("SELECT * FROM charge_samples WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getSamplesForSession(sessionId: String): Flow<List<ChargeSampleEntity>>

    @Query("DELETE FROM charge_samples WHERE sessionId = :sessionId")
    suspend fun deleteSamplesForSession(sessionId: String)
}
