package com.antigravity.battery.core.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import com.antigravity.battery.core.model.ChargeTelemetrySample
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.LiveChargeTelemetry
import com.antigravity.battery.core.model.ReconnectPromptSession
import com.antigravity.battery.data.db.BatteryDatabase
import com.antigravity.battery.data.repository.AccessoryRepository
import com.antigravity.battery.service.ChargingNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manages charging telemetry sampling, real-time metrics calculation,
 * and completion criteria checking.
 */
class ChargingBenchmarkManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var samplingJob: Job? = null
    private var simulationJob: Job? = null

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repository = AccessoryRepository(BatteryDatabase.getInstance(context).accessoryDao())

    private val _liveTelemetry = MutableStateFlow(LiveChargeTelemetry())
    val liveTelemetry: StateFlow<LiveChargeTelemetry> = _liveTelemetry.asStateFlow()

    // State for the ongoing session
    private var activeSessionId: String = UUID.randomUUID().toString()
    private var sessionStartTimeMs: Long = 0L
    private var startBatteryLevel: Int = -1
    private var startTempDeciC: Int = 0
    private var peakWattage: Float = 0f
    private var peakCurrentMa: Int = 0
    private var minVoltageMv: Int = Int.MAX_VALUE
    private var maxVoltageMv: Int = 0
    private var maxTempDeciC: Int = 0
    private var wattageSum: Double = 0.0
    private var sampleCount: Int = 0
    private var isThrottled: Boolean = false
    private var lastKnownPlugged: Boolean = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
            }
        }
    }

    // Selected accessories during a live test
    var activeBrickId: String? = null
    var activeCableId: String? = null
    var activeBrickName: String = "Unspecified Charger"
    var activeCableName: String = "Unspecified Cable"

    private val samplesList = mutableListOf<ChargeTelemetrySample>()

    // Last completed session waiting to be tagged/saved
    private val _pendingCompletedSession = MutableStateFlow<Pair<ChargeTestSession, List<ChargeTelemetrySample>>?>(null)
    val pendingCompletedSession: StateFlow<Pair<ChargeTestSession, List<ChargeTelemetrySample>>?> = _pendingCompletedSession.asStateFlow()

    // Reconnect prompt for resuming shortly disconnected sessions
    private val _reconnectPrompt = MutableStateFlow<ReconnectPromptSession?>(null)
    val reconnectPrompt: StateFlow<ReconnectPromptSession?> = _reconnectPrompt.asStateFlow()

    private val _isGracePeriodActive = MutableStateFlow(false)
    val isGracePeriodActive: StateFlow<Boolean> = _isGracePeriodActive.asStateFlow()

    private data class PausedSessionData(
        val sessionId: String,
        val brickId: String?,
        val cableId: String?,
        val brickName: String,
        val cableName: String,
        val startTimeMs: Long,
        val disconnectTimeMs: Long,
        val startBatteryLevel: Int,
        val endBatteryLevel: Int,
        val startTempDeciC: Int,
        val maxTempDeciC: Int,
        val peakWattage: Float,
        val peakCurrentMa: Int,
        val minVoltageMv: Int,
        val maxVoltageMv: Int,
        val wattageSum: Double,
        val sampleCount: Int,
        val isThrottled: Boolean,
        val samples: List<ChargeTelemetrySample>
    )

    private var pausedSessionData: PausedSessionData? = null
    private var gracePeriodTimerJob: Job? = null
    private var promptAutoResumeJob: Job? = null

    companion object {
        const val PREFS_NAME = "powertrace_accessory_prefs"
        const val PREF_KEY_AUTO_PROMPT_ENABLED = "auto_prompt_enabled"
        const val PREF_KEY_MIN_DURATION_MINUTES = "min_duration_minutes"
        const val PREF_KEY_MIN_BATTERY_GAIN = "min_battery_gain"
        const val PREF_KEY_DISCONNECT_GRACE_ENABLED = "disconnect_grace_enabled"
        const val PREF_KEY_DISCONNECT_GRACE_SECONDS = "disconnect_grace_seconds"

        const val DEFAULT_MIN_DURATION_MINUTES = 5
        const val DEFAULT_MIN_BATTERY_GAIN = 5
        const val DEFAULT_DISCONNECT_GRACE_SECONDS = 60

        @Volatile
        private var INSTANCE: ChargingBenchmarkManager? = null

        fun getInstance(context: Context): ChargingBenchmarkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChargingBenchmarkManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        scope.launch {
            repository.ensureDefaultPresets()
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(powerReceiver, filter)

        // Initial sample & check plug status
        val batteryStatus = getBatteryIntent()
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        lastKnownPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ||
                (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && plugged == BatteryManager.BATTERY_PLUGGED_DOCK)

        if (lastKnownPlugged) {
            startSession()
        } else {
            sampleCurrentState()
        }
    }

    fun isAutoPromptEnabled(): Boolean = prefs.getBoolean(PREF_KEY_AUTO_PROMPT_ENABLED, true)
    fun setAutoPromptEnabled(enabled: Boolean) = prefs.edit().putBoolean(PREF_KEY_AUTO_PROMPT_ENABLED, enabled).apply()

    fun getMinDurationMinutes(): Int = prefs.getInt(PREF_KEY_MIN_DURATION_MINUTES, DEFAULT_MIN_DURATION_MINUTES)
    fun setMinDurationMinutes(minutes: Int) = prefs.edit().putInt(PREF_KEY_MIN_DURATION_MINUTES, minutes).apply()

    fun getMinBatteryGain(): Int = prefs.getInt(PREF_KEY_MIN_BATTERY_GAIN, DEFAULT_MIN_BATTERY_GAIN)
    fun setMinBatteryGain(gain: Int) = prefs.edit().putInt(PREF_KEY_MIN_BATTERY_GAIN, gain).apply()

    fun isDisconnectGraceEnabled(): Boolean = prefs.getBoolean(PREF_KEY_DISCONNECT_GRACE_ENABLED, true)
    fun setDisconnectGraceEnabled(enabled: Boolean) = prefs.edit().putBoolean(PREF_KEY_DISCONNECT_GRACE_ENABLED, enabled).apply()

    fun getDisconnectGraceSeconds(): Int = prefs.getInt(PREF_KEY_DISCONNECT_GRACE_SECONDS, DEFAULT_DISCONNECT_GRACE_SECONDS)
    fun setDisconnectGraceSeconds(seconds: Int) = prefs.edit().putInt(PREF_KEY_DISCONNECT_GRACE_SECONDS, seconds).apply()

    @Synchronized
    fun onPowerConnected() {
        if (lastKnownPlugged && sessionStartTimeMs > 0L && pausedSessionData == null) {
            // Already connected and session is actively recording
            return
        }
        lastKnownPlugged = true
        gracePeriodTimerJob?.cancel()
        gracePeriodTimerJob = null
        _isGracePeriodActive.value = false

        val paused = pausedSessionData
        val nowMs = System.currentTimeMillis()

        if (paused != null && isDisconnectGraceEnabled()) {
            val disconnectDurationMs = nowMs - paused.disconnectTimeMs
            val graceMs = getDisconnectGraceSeconds() * 1000L
            val wasMeaningfulSession = paused.samples.size >= 5 && (paused.disconnectTimeMs - paused.startTimeMs) >= 15_000L

            if (disconnectDurationMs <= graceMs && wasMeaningfulSession) {
                // Disconnected shortly! Prompt the user whether to go on or make a new one
                val avgWattage = if (paused.sampleCount > 0) (paused.wattageSum / paused.sampleCount).toFloat() else paused.peakWattage
                val prevSession = ChargeTestSession(
                    id = paused.sessionId,
                    brickId = paused.brickId,
                    cableId = paused.cableId,
                    brickName = paused.brickName,
                    cableName = paused.cableName,
                    startTimeMs = paused.startTimeMs,
                    endTimeMs = paused.disconnectTimeMs,
                    startBatteryLevel = paused.startBatteryLevel,
                    endBatteryLevel = paused.endBatteryLevel,
                    peakWattage = paused.peakWattage,
                    avgWattage = avgWattage,
                    minVoltageMv = if (paused.minVoltageMv == Int.MAX_VALUE) 0 else paused.minVoltageMv,
                    maxVoltageMv = paused.maxVoltageMv,
                    peakCurrentMa = paused.peakCurrentMa,
                    startTempDeciC = paused.startTempDeciC,
                    maxTempDeciC = paused.maxTempDeciC,
                    isThrottled = paused.isThrottled
                )

                _reconnectPrompt.value = ReconnectPromptSession(
                    previousSession = prevSession,
                    previousSamples = paused.samples,
                    disconnectedDurationMs = disconnectDurationMs
                )

                ChargingNotificationHelper.showReconnectPromptNotification(
                    context = context,
                    session = prevSession,
                    disconnectedDurationMs = disconnectDurationMs
                )

                // If user doesn't interact within 60s while charging, automatically continue previous session
                promptAutoResumeJob?.cancel()
                promptAutoResumeJob = scope.launch {
                    delay(60_000L)
                    if (isActive && _reconnectPrompt.value != null) {
                        resumePreviousSession()
                    }
                }

                // Start recording live immediately so charging telemetry is not lost while waiting
                startSession(
                    brickId = paused.brickId,
                    cableId = paused.cableId
                )
                return
            } else {
                finalizePausedSession()
            }
        }

        startSession()
    }

    @Synchronized
    fun onPowerDisconnected(): ChargeTestSession? {
        if (!lastKnownPlugged && pausedSessionData != null) {
            // Already handled disconnect
            return null
        }
        lastKnownPlugged = false
        promptAutoResumeJob?.cancel()
        promptAutoResumeJob = null
        ChargingNotificationHelper.cancelReconnectNotification(context)
        _reconnectPrompt.value = null

        val nowMs = System.currentTimeMillis()
        val durationMs = if (sessionStartTimeMs > 0L) nowMs - sessionStartTimeMs else 0L

        // If no active session or it was less than 15s with few samples, don't pause it for resume
        if (sessionStartTimeMs == 0L || sampleCount < 5 || durationMs < 15_000L) {
            sessionStartTimeMs = 0L
            peakWattage = 0f
            peakCurrentMa = 0
            wattageSum = 0.0
            sampleCount = 0
            isThrottled = false
            samplesList.clear()
            _isGracePeriodActive.value = false
            sampleCurrentState()
            return null
        }

        if (!isDisconnectGraceEnabled()) {
            return endSession()
        }

        val batteryStatus = getBatteryIntent()
        val endBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, startBatteryLevel) ?: startBatteryLevel

        val paused = PausedSessionData(
            sessionId = activeSessionId,
            brickId = activeBrickId,
            cableId = activeCableId,
            brickName = activeBrickName,
            cableName = activeCableName,
            startTimeMs = sessionStartTimeMs,
            disconnectTimeMs = nowMs,
            startBatteryLevel = startBatteryLevel,
            endBatteryLevel = endBatteryLevel,
            startTempDeciC = startTempDeciC,
            maxTempDeciC = maxTempDeciC,
            peakWattage = peakWattage,
            peakCurrentMa = peakCurrentMa,
            minVoltageMv = minVoltageMv,
            maxVoltageMv = maxVoltageMv,
            wattageSum = wattageSum,
            sampleCount = sampleCount,
            isThrottled = isThrottled,
            samples = samplesList.toList()
        )
        pausedSessionData = paused
        _isGracePeriodActive.value = true

        // Reset live ongoing session trackers so it's not marked as actively charging
        sessionStartTimeMs = 0L
        peakWattage = 0f
        peakCurrentMa = 0
        wattageSum = 0.0
        sampleCount = 0
        isThrottled = false
        samplesList.clear()

        // Start grace countdown timer
        gracePeriodTimerJob?.cancel()
        gracePeriodTimerJob = scope.launch {
            val totalSeconds = getDisconnectGraceSeconds()
            for (remaining in totalSeconds downTo 1) {
                if (!isActive) break
                sampleCurrentState()
                delay(1000L)
            }
            if (isActive) {
                // Grace window elapsed without reconnect: finalize previous session
                finalizePausedSession()
            }
        }

        sampleCurrentState()
        return null
    }

    @Synchronized
    fun finalizePausedSession(): ChargeTestSession? {
        gracePeriodTimerJob?.cancel()
        gracePeriodTimerJob = null
        _isGracePeriodActive.value = false

        val paused = pausedSessionData ?: return null
        pausedSessionData = null

        val avgWattage = if (paused.sampleCount > 0) (paused.wattageSum / paused.sampleCount).toFloat() else paused.peakWattage
        val session = ChargeTestSession(
            id = paused.sessionId,
            brickId = paused.brickId,
            cableId = paused.cableId,
            brickName = paused.brickName,
            cableName = paused.cableName,
            startTimeMs = paused.startTimeMs,
            endTimeMs = paused.disconnectTimeMs,
            startBatteryLevel = paused.startBatteryLevel,
            endBatteryLevel = paused.endBatteryLevel,
            peakWattage = paused.peakWattage,
            avgWattage = avgWattage,
            minVoltageMv = if (paused.minVoltageMv == Int.MAX_VALUE) 0 else paused.minVoltageMv,
            maxVoltageMv = paused.maxVoltageMv,
            peakCurrentMa = paused.peakCurrentMa,
            startTempDeciC = paused.startTempDeciC,
            maxTempDeciC = paused.maxTempDeciC,
            isThrottled = paused.isThrottled
        )

        val durationMinutes = session.durationMs / (60 * 1000L)
        val batteryGain = session.batteryGained

        val autoPrompt = isAutoPromptEnabled()
        val minMinutes = getMinDurationMinutes()
        val minGain = getMinBatteryGain()
        val qualifies = durationMinutes >= minMinutes && batteryGain >= minGain

        if (qualifies && autoPrompt) {
            _pendingCompletedSession.value = Pair(session, paused.samples)
            ChargingNotificationHelper.showSessionFinishedNotification(context, session)
        }

        sampleCurrentState()
        return session
    }

    fun finalizePausedSessionNow() {
        promptAutoResumeJob?.cancel()
        promptAutoResumeJob = null
        ChargingNotificationHelper.cancelReconnectNotification(context)
        _reconnectPrompt.value = null
        finalizePausedSession()
    }

    @Synchronized
    fun resumePreviousSession() {
        promptAutoResumeJob?.cancel()
        promptAutoResumeJob = null
        ChargingNotificationHelper.cancelReconnectNotification(context)
        _reconnectPrompt.value = null

        val paused = pausedSessionData ?: return
        pausedSessionData = null

        // Merge previous paused session with any samples collected since reconnect
        activeSessionId = paused.sessionId
        activeBrickId = paused.brickId
        activeCableId = paused.cableId
        activeBrickName = paused.brickName
        activeCableName = paused.cableName

        sessionStartTimeMs = paused.startTimeMs
        startBatteryLevel = paused.startBatteryLevel
        startTempDeciC = paused.startTempDeciC

        peakWattage = maxOf(peakWattage, paused.peakWattage)
        peakCurrentMa = maxOf(peakCurrentMa, paused.peakCurrentMa)
        minVoltageMv = minOf(minVoltageMv, paused.minVoltageMv)
        maxVoltageMv = maxOf(maxVoltageMv, paused.maxVoltageMv)
        maxTempDeciC = maxOf(maxTempDeciC, paused.maxTempDeciC)

        wattageSum += paused.wattageSum
        sampleCount += paused.sampleCount
        isThrottled = isThrottled || paused.isThrottled

        val combinedSamples = (paused.samples + samplesList).takeLast(600)
        samplesList.clear()
        samplesList.addAll(combinedSamples)

        sampleCurrentState()
    }

    @Synchronized
    fun startNewSessionFromPrompt() {
        promptAutoResumeJob?.cancel()
        promptAutoResumeJob = null
        ChargingNotificationHelper.cancelReconnectNotification(context)
        _reconnectPrompt.value = null

        finalizePausedSession()
        startSession()
        sampleCurrentState()
    }

    @Synchronized
    fun startSession(brickId: String? = activeBrickId, cableId: String? = activeCableId) {
        ChargingNotificationHelper.cancelNotification(context)
        ChargingNotificationHelper.cancelReconnectNotification(context)
        activeBrickId = brickId
        activeCableId = cableId
        activeSessionId = UUID.randomUUID().toString()
        sessionStartTimeMs = System.currentTimeMillis()
        wattageSum = 0.0
        sampleCount = 0
        peakWattage = 0f
        peakCurrentMa = 0
        minVoltageMv = Int.MAX_VALUE
        maxVoltageMv = 0
        maxTempDeciC = 0
        isThrottled = false
        samplesList.clear()

        val batteryStatus = getBatteryIntent()
        startBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        startTempDeciC = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0

        startPolling()
    }

    @Synchronized
    fun endSession(): ChargeTestSession? {
        if (sessionStartTimeMs == 0L) return null

        val endTimeMs = System.currentTimeMillis()
        val batteryStatus = getBatteryIntent()
        val endBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, startBatteryLevel) ?: startBatteryLevel
        val avgWattage = if (sampleCount > 0) (wattageSum / sampleCount).toFloat() else peakWattage

        val session = ChargeTestSession(
            id = activeSessionId,
            brickId = activeBrickId,
            cableId = activeCableId,
            brickName = activeBrickName,
            cableName = activeCableName,
            startTimeMs = sessionStartTimeMs,
            endTimeMs = endTimeMs,
            startBatteryLevel = startBatteryLevel,
            endBatteryLevel = endBatteryLevel,
            peakWattage = peakWattage,
            avgWattage = avgWattage,
            minVoltageMv = if (minVoltageMv == Int.MAX_VALUE) 0 else minVoltageMv,
            maxVoltageMv = maxVoltageMv,
            peakCurrentMa = peakCurrentMa,
            startTempDeciC = startTempDeciC,
            maxTempDeciC = maxTempDeciC,
            isThrottled = isThrottled
        )

        val durationMinutes = session.durationMs / (60 * 1000L)
        val batteryGain = session.batteryGained

        // Check if session qualifies for prompt
        val autoPrompt = isAutoPromptEnabled()
        val minMinutes = getMinDurationMinutes()
        val minGain = getMinBatteryGain()

        val qualifies = durationMinutes >= minMinutes && batteryGain >= minGain

        if (qualifies && autoPrompt) {
            _pendingCompletedSession.value = Pair(session, samplesList.toList())
            ChargingNotificationHelper.showSessionFinishedNotification(context, session)
        }

        sessionStartTimeMs = 0L
        peakWattage = 0f
        peakCurrentMa = 0
        wattageSum = 0.0
        sampleCount = 0
        isThrottled = false

        sampleCurrentState()
        return session
    }

    fun clearPendingSession() {
        _pendingCompletedSession.value = null
        ChargingNotificationHelper.cancelNotification(context)
    }

    fun setPendingSession(session: ChargeTestSession, samples: List<ChargeTelemetrySample>) {
        _pendingCompletedSession.value = Pair(session, samples)
    }

    fun saveCompletedSession(
        brickId: String?,
        cableId: String?,
        brickName: String,
        cableName: String,
        notes: String = ""
    ) {
        ChargingNotificationHelper.cancelNotification(context)
        val pending = _pendingCompletedSession.value ?: return
        val updatedSession = pending.first.copy(
            brickId = brickId,
            cableId = cableId,
            brickName = brickName,
            cableName = cableName,
            notes = notes
        )
        val samples = pending.second

        scope.launch {
            repository.saveSession(updatedSession, samples)
            clearPendingSession()
        }
    }

    fun startPolling() {
        if (samplingJob?.isActive == true) return
        samplingJob = scope.launch {
            while (isActive) {
                sampleCurrentState()
                delay(1000L) // 1 second sample rate
            }
        }
    }

    fun stopPolling() {
        samplingJob?.cancel()
        samplingJob = null
    }

    @Synchronized
    fun sampleCurrentState() {
        val batteryStatus = getBatteryIntent() ?: return

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        val isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ||
                (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && plugged == BatteryManager.BATTERY_PLUGGED_DOCK)

        val isCharging = isPlugged && (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL)

        if (!lastKnownPlugged && isPlugged) {
            onPowerConnected()
            return
        } else if (lastKnownPlugged && !isPlugged) {
            onPowerDisconnected()
            return
        }

        val plugType = when {
            plugged == BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            plugged == BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi Pad"
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && plugged == BatteryManager.BATTERY_PLUGGED_DOCK -> "Pixel Stand / Dock"
            else -> if (isCharging) "Charging" else "Unplugged"
        }

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempDeciC = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

        // Read instantaneous current
        // Standard Android: positive = charging into battery, negative = discharging out of battery
        val rawCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val rawMa = if (kotlin.math.abs(rawCurrent) > 50000) {
            rawCurrent / 1000
        } else {
            rawCurrent
        }

        // Net charging current into battery (mA). Must be strictly positive and only when charging.
        val chargingCurrentMa = if (isCharging && rawMa > 0) rawMa else 0

        // Wattage = V (mV) * I (mA) / 1,000,000
        val wattage = if (isCharging && voltageMv > 0 && chargingCurrentMa > 0) {
            (voltageMv.toFloat() * chargingCurrentMa.toFloat()) / 1_000_000f
        } else {
            0f
        }

        val nowMs = System.currentTimeMillis()

        if (isCharging) {
            if (sessionStartTimeMs == 0L) {
                sessionStartTimeMs = nowMs
                startBatteryLevel = level
                startTempDeciC = tempDeciC
            }

            if (wattage > peakWattage) peakWattage = wattage
            if (chargingCurrentMa > peakCurrentMa) peakCurrentMa = chargingCurrentMa
            if (voltageMv in 1 until minVoltageMv) minVoltageMv = voltageMv
            if (voltageMv > maxVoltageMv) maxVoltageMv = voltageMv
            if (tempDeciC > maxTempDeciC) maxTempDeciC = tempDeciC

            wattageSum += wattage
            sampleCount++

            // Throttling logic: high temp and wattage dropped below 60% of peak while battery not full
            if (tempDeciC >= 390 && level < 75 && peakWattage > 10f && wattage < (peakWattage * 0.6f)) {
                isThrottled = true
            }

            // Keep in-memory samples
            if (samplesList.size < 600) {
                samplesList.add(
                    ChargeTelemetrySample(
                        sessionId = activeSessionId,
                        timestampMs = nowMs,
                        batteryLevel = level,
                        voltageMv = voltageMv,
                        currentMa = chargingCurrentMa,
                        wattage = wattage,
                        tempDeciC = tempDeciC
                    )
                )
            }
        } else {
            // Not charging: reset ongoing live metrics if not in active session
            sessionStartTimeMs = 0L
            peakWattage = 0f
            peakCurrentMa = 0
            wattageSum = 0.0
            sampleCount = 0
            isThrottled = false
        }

        val avgWattage = if (isCharging && sampleCount > 0) (wattageSum / sampleCount).toFloat() else wattage
        val durationMs = if (isCharging && sessionStartTimeMs > 0L) (nowMs - sessionStartTimeMs) else 0L

        // Display current: when charging show +chargingCurrentMa, when unplugged show rawMa (e.g. -450 mA)
        val displayMa = if (isCharging) chargingCurrentMa else rawMa

        val remainingGraceSec = if (_isGracePeriodActive.value && pausedSessionData != null) {
            val elapsedSec = (nowMs - pausedSessionData!!.disconnectTimeMs) / 1000L
            (getDisconnectGraceSeconds() - elapsedSec).coerceAtLeast(0L).toInt()
        } else {
            0
        }

        _liveTelemetry.value = LiveChargeTelemetry(
            isCharging = isCharging,
            chargePlugType = plugType,
            currentWattage = wattage,
            currentMa = displayMa,
            voltageMv = voltageMv,
            batteryLevelPercent = level,
            tempDeciCelsius = tempDeciC,
            peakWattageSoFar = if (isCharging) peakWattage else 0f,
            avgWattageSoFar = if (isCharging) avgWattage else 0f,
            sessionDurationMs = durationMs,
            isThrottlingDetected = if (isCharging) isThrottled else false,
            isGracePeriodActive = _isGracePeriodActive.value,
            gracePeriodRemainingSeconds = remainingGraceSec
        )
    }

    /**
     * Simulates a fast-charging cycle for 5 seconds to test and demo the HUD & saving flow.
     */
    fun simulateMockChargeSession(onComplete: () -> Unit = {}) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val simSessionId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val simStartTime = now - (12 * 60 * 1000L) // 12 min simulated duration
            val simSamples = mutableListOf<ChargeTelemetrySample>()

            var simPeakWattage = 0f
            var simPeakCurrentMa = 0
            var simWattageSum = 0.0
            var simSampleCount = 0
            var simMaxTemp = 295

            val mockWatts = listOf(15.2f, 25.8f, 38.5f, 44.6f, 42.1f, 36.4f, 30.8f)
            for ((index, w) in mockWatts.withIndex()) {
                val voltageMv = 4200 + (index * 20)
                val currentMa = ((w * 1_000_000f) / voltageMv).toInt()
                val temp = 300 + (index * 10)

                simPeakWattage = maxOf(simPeakWattage, w)
                simPeakCurrentMa = maxOf(simPeakCurrentMa, currentMa)
                simMaxTemp = maxOf(simMaxTemp, temp)
                simWattageSum += w
                simSampleCount++

                val sample = ChargeTelemetrySample(
                    sessionId = simSessionId,
                    timestampMs = now - ((mockWatts.size - index) * 1000L),
                    batteryLevel = 22 + (index * 2),
                    voltageMv = voltageMv,
                    currentMa = currentMa,
                    wattage = w,
                    tempDeciC = temp
                )
                simSamples.add(sample)

                _liveTelemetry.value = LiveChargeTelemetry(
                    isCharging = true,
                    chargePlugType = "AC Wall Charger (Simulation)",
                    currentWattage = w,
                    currentMa = currentMa,
                    voltageMv = voltageMv,
                    batteryLevelPercent = 22 + (index * 2),
                    tempDeciCelsius = temp,
                    peakWattageSoFar = simPeakWattage,
                    avgWattageSoFar = (simWattageSum / simSampleCount).toFloat(),
                    sessionDurationMs = 12 * 60 * 1000L,
                    isThrottlingDetected = false
                )
                delay(600L)
            }

            // Conclude simulation session
            val simSession = ChargeTestSession(
                id = simSessionId,
                brickId = activeBrickId,
                cableId = activeCableId,
                brickName = activeBrickName.ifBlank { "Stock OEM Charger" },
                cableName = activeCableName.ifBlank { "Stock USB-C Cable" },
                startTimeMs = simStartTime,
                endTimeMs = System.currentTimeMillis(),
                startBatteryLevel = 22,
                endBatteryLevel = 36,
                peakWattage = simPeakWattage,
                avgWattage = (simWattageSum / simSampleCount).toFloat(),
                minVoltageMv = 4200,
                maxVoltageMv = 4340,
                peakCurrentMa = simPeakCurrentMa,
                startTempDeciC = 295,
                maxTempDeciC = simMaxTemp,
                isThrottled = false
            )

            _pendingCompletedSession.value = Pair(simSession, simSamples)
            ChargingNotificationHelper.showSessionFinishedNotification(context, simSession)

            // Re-sample real device state immediately
            sampleCurrentState()
            onComplete()
        }
    }

    private fun getBatteryIntent(): Intent? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        return context.registerReceiver(null, filter)
    }
}
