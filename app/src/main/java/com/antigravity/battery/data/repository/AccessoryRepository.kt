package com.antigravity.battery.data.repository

import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTelemetrySample
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.data.db.AccessoryDao
import com.antigravity.battery.data.db.CableEntity
import com.antigravity.battery.data.db.PowerBrickEntity
import com.antigravity.battery.data.db.ChargeSampleEntity as DbChargeSample
import com.antigravity.battery.data.db.ChargeTestSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AccessoryRepository(private val accessoryDao: AccessoryDao) {

    val powerBricks: Flow<List<PowerBrick>> = accessoryDao.getAllBricks().map { list ->
        list.map { it.toDomain() }
    }

    val cables: Flow<List<Cable>> = accessoryDao.getAllCables().map { list ->
        list.map { it.toDomain() }
    }

    val sessions: Flow<List<ChargeTestSession>> = accessoryDao.getAllSessions().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getBrickById(id: String): PowerBrick? =
        accessoryDao.getBrickById(id)?.toDomain()

    suspend fun getCableById(id: String): Cable? =
        accessoryDao.getCableById(id)?.toDomain()

    suspend fun saveBrick(brick: PowerBrick) {
        accessoryDao.insertBrick(brick.toEntity())
    }

    suspend fun deleteBrick(brick: PowerBrick) {
        accessoryDao.deleteBrick(brick.toEntity())
    }

    suspend fun saveCable(cable: Cable) {
        accessoryDao.insertCable(cable.toEntity())
    }

    suspend fun deleteCable(cable: Cable) {
        accessoryDao.deleteCable(cable.toEntity())
    }

    suspend fun saveSession(session: ChargeTestSession, samples: List<ChargeTelemetrySample> = emptyList()) {
        accessoryDao.insertSession(session.toEntity())
        if (samples.isNotEmpty()) {
            accessoryDao.insertSamples(samples.map { it.toEntity() })
        }
    }

    suspend fun deleteSession(session: ChargeTestSession) {
        accessoryDao.deleteSamplesForSession(session.id)
        accessoryDao.deleteSession(session.toEntity())
    }

    fun getSamplesForSession(sessionId: String): Flow<List<ChargeTelemetrySample>> {
        return accessoryDao.getSamplesForSession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun ensureDefaultPresets() {
        val existingBricks = accessoryDao.getAllBricks().first()
        if (existingBricks.isEmpty()) {
            accessoryDao.insertBrick(
                PowerBrickEntity(
                    id = "preset_oem",
                    name = "Stock OEM Charger",
                    manufacturer = "OEM",
                    ratedWattage = 25f,
                    portType = "USB-C",
                    notes = "Original charger included with device"
                )
            )
            accessoryDao.insertBrick(
                PowerBrickEntity(
                    id = "preset_gan65",
                    name = "65W GaN Fast Charger",
                    manufacturer = "Generic",
                    ratedWattage = 65f,
                    portType = "USB-C PD",
                    notes = "Multi-port USB-PD GaN brick"
                )
            )
        }

        val existingCables = accessoryDao.getAllCables().first()
        if (existingCables.isEmpty()) {
            accessoryDao.insertCable(
                CableEntity(
                    id = "preset_stock_cable",
                    name = "Stock USB-C Cable",
                    manufacturer = "OEM",
                    ratedWattage = 60f,
                    lengthMeters = 1.0f,
                    cableType = "Type-C to Type-C",
                    notes = "Standard bundled cable"
                )
            )
            accessoryDao.insertCable(
                CableEntity(
                    id = "preset_braided_cable",
                    name = "Braided 100W Cable (2m)",
                    manufacturer = "Generic",
                    ratedWattage = 100f,
                    lengthMeters = 2.0f,
                    cableType = "Type-C to Type-C E-Marker",
                    notes = "Heavy-duty 2m cable"
                )
            )
        }
    }

    // --- Mappers ---
    private fun PowerBrickEntity.toDomain() = PowerBrick(
        id = id,
        name = name,
        manufacturer = manufacturer,
        ratedWattage = ratedWattage,
        portType = portType,
        notes = notes,
        createdAt = createdAt
    )

    private fun PowerBrick.toEntity() = PowerBrickEntity(
        id = id,
        name = name,
        manufacturer = manufacturer,
        ratedWattage = ratedWattage,
        portType = portType,
        notes = notes,
        createdAt = createdAt
    )

    private fun CableEntity.toDomain() = Cable(
        id = id,
        name = name,
        manufacturer = manufacturer,
        ratedWattage = ratedWattage,
        lengthMeters = lengthMeters,
        cableType = cableType,
        notes = notes,
        createdAt = createdAt
    )

    private fun Cable.toEntity() = CableEntity(
        id = id,
        name = name,
        manufacturer = manufacturer,
        ratedWattage = ratedWattage,
        lengthMeters = lengthMeters,
        cableType = cableType,
        notes = notes,
        createdAt = createdAt
    )

    private fun ChargeTestSessionEntity.toDomain() = ChargeTestSession(
        id = id,
        brickId = brickId,
        cableId = cableId,
        brickName = brickName,
        cableName = cableName,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        startBatteryLevel = startBatteryLevel,
        endBatteryLevel = endBatteryLevel,
        peakWattage = peakWattage,
        avgWattage = avgWattage,
        minVoltageMv = minVoltageMv,
        maxVoltageMv = maxVoltageMv,
        peakCurrentMa = peakCurrentMa,
        startTempDeciC = startTempDeciC,
        maxTempDeciC = maxTempDeciC,
        isThrottled = isThrottled,
        notes = notes
    )

    private fun ChargeTestSession.toEntity() = ChargeTestSessionEntity(
        id = id,
        brickId = brickId,
        cableId = cableId,
        brickName = brickName,
        cableName = cableName,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        startBatteryLevel = startBatteryLevel,
        endBatteryLevel = endBatteryLevel,
        peakWattage = peakWattage,
        avgWattage = avgWattage,
        minVoltageMv = minVoltageMv,
        maxVoltageMv = maxVoltageMv,
        peakCurrentMa = peakCurrentMa,
        startTempDeciC = startTempDeciC,
        maxTempDeciC = maxTempDeciC,
        isThrottled = isThrottled,
        notes = notes
    )

    private fun DbChargeSample.toDomain() = ChargeTelemetrySample(
        id = id,
        sessionId = sessionId,
        timestampMs = timestampMs,
        batteryLevel = batteryLevel,
        voltageMv = voltageMv,
        currentMa = currentMa,
        wattage = wattage,
        tempDeciC = tempDeciC
    )

    private fun ChargeTelemetrySample.toEntity() = DbChargeSample(
        id = id,
        sessionId = sessionId,
        timestampMs = timestampMs,
        batteryLevel = batteryLevel,
        voltageMv = voltageMv,
        currentMa = currentMa,
        wattage = wattage,
        tempDeciC = tempDeciC
    )
}
