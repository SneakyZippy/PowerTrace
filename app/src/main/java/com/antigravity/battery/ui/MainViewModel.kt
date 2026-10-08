package com.antigravity.battery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.battery.core.dump.BinderDumpSource
import com.antigravity.battery.core.dump.MockDiagnosticDumpSource
import com.antigravity.battery.core.engine.SnapshotDeltaCalculator
import com.antigravity.battery.core.engine.SnapshotManager
import com.antigravity.battery.core.engine.TimeWindowFilter
import com.antigravity.battery.core.engine.TimelineAggregationEngine
import com.antigravity.battery.core.engine.TimelineViewData
import com.antigravity.battery.core.engine.WakeupCulpritDetector
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.core.model.ObservationSession
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.model.SessionDelta
import com.antigravity.battery.core.model.SessionStatus
import com.antigravity.battery.core.permission.PermissionManager
import com.antigravity.battery.core.permission.PermissionState
import com.antigravity.battery.core.power.EnergyAttributionModel
import com.antigravity.battery.core.power.PowerProfileResolver
import com.antigravity.battery.core.resolver.UidPackageResolver
import com.antigravity.battery.data.db.BatteryDatabase
import com.antigravity.battery.data.db.SessionEntity
import com.antigravity.battery.data.export.DiagnosticExporter
import com.antigravity.battery.data.repository.BatterySessionRepository
import com.antigravity.battery.service.BatterySessionService
import com.antigravity.battery.service.ContinuousMonitoringService
import com.antigravity.battery.service.SessionTriggerReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class AppNavTab {
    OVERVIEW,
    BENCHMARK,
    HISTORY,
    SETTINGS,
    SETUP
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val permissionManager = PermissionManager(application)
    private val uidResolver = UidPackageResolver(application)
    private val culpritDetector = WakeupCulpritDetector()
    private val powerResolver = PowerProfileResolver(application)

    private val database = BatteryDatabase.getInstance(application)
    private val continuousDao = database.continuousSnapshotDao()
    private val repository = BatterySessionRepository(database)

    private val binderSource = BinderDumpSource(application)
    private val mockSource = MockDiagnosticDumpSource()
    private val snapshotManager = SnapshotManager(binderSource, application)

    private val _useMockSource = MutableStateFlow(false)
    val useMockSource: StateFlow<Boolean> = _useMockSource.asStateFlow()

    private val _isContinuousTrackingEnabled = MutableStateFlow(false)
    val isContinuousTrackingEnabled: StateFlow<Boolean> = _isContinuousTrackingEnabled.asStateFlow()

    private val _powerProfile = MutableStateFlow(powerResolver.resolve())
    val powerProfile: StateFlow<PowerProfileData> = _powerProfile.asStateFlow()

    private val _permissionState = MutableStateFlow(permissionManager.checkPermissions())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _currentTab = MutableStateFlow(
        if (_permissionState.value.areAllRequiredGranted) AppNavTab.OVERVIEW else AppNavTab.SETUP
    )
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    // 24/7 Timeline state
    private val _selectedTimeFilter = MutableStateFlow(TimeWindowFilter.TODAY)
    val selectedTimeFilter: StateFlow<TimeWindowFilter> = _selectedTimeFilter.asStateFlow()

    private val _timelineData = MutableStateFlow<TimelineViewData?>(null)
    val timelineData: StateFlow<TimelineViewData?> = _timelineData.asStateFlow()

    // Manual session state
    private val _activeSession = MutableStateFlow<ObservationSession?>(null)
    val activeSession: StateFlow<ObservationSession?> = _activeSession.asStateFlow()

    private val _latestBenchmarkDelta = MutableStateFlow<SessionDelta?>(null)
    val latestBenchmarkDelta: StateFlow<SessionDelta?> = _latestBenchmarkDelta.asStateFlow()

    private val _selectedCulprit = MutableStateFlow<CulpritApp?>(null)
    val selectedCulprit: StateFlow<CulpritApp?> = _selectedCulprit.asStateFlow()

    private val _elapsedTimeFormatted = MutableStateFlow("00:00:00")
    val elapsedTimeFormatted: StateFlow<String> = _elapsedTimeFormatted.asStateFlow()

    private val _autoStopOnUnlock = MutableStateFlow(true)
    val autoStopOnUnlock: StateFlow<Boolean> = _autoStopOnUnlock.asStateFlow()

    private val _autoStopOnCharger = MutableStateFlow(true)
    val autoStopOnCharger: StateFlow<Boolean> = _autoStopOnCharger.asStateFlow()

    val historicalSessions: StateFlow<List<SessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var timerJob: Job? = null
    private var timelineRefreshJob: Job? = null

    init {
        refreshTimelineData()

        // Periodically refresh timeline data while app is open
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                refreshTimelineData()
            }
        }

        // Wire up receiver for automated triggers
        SessionTriggerReceiver.onTriggerEvent = { trigger ->
            if (activeSession.value?.status == SessionStatus.RECORDING) {
                if (trigger == "SCREEN_UNLOCKED" && _autoStopOnUnlock.value) {
                    stopBenchmarkSession()
                } else if (trigger == "CHARGER_CONNECTED" && _autoStopOnCharger.value) {
                    stopBenchmarkSession()
                }
            }
        }
    }

    fun setTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun selectCulprit(culprit: CulpritApp?) {
        _selectedCulprit.value = culprit
    }

    fun setTimeFilter(filter: TimeWindowFilter) {
        _selectedTimeFilter.value = filter
        refreshTimelineData()
    }

    fun refreshPermissions() {
        val updated = permissionManager.checkPermissions()
        _permissionState.value = updated
        if (updated.areAllRequiredGranted) {
            if (_currentTab.value == AppNavTab.SETUP) {
                _currentTab.value = AppNavTab.OVERVIEW
            }
        }
    }

    fun toggleContinuousTracking(enabled: Boolean) {
        _isContinuousTrackingEnabled.value = enabled
        if (enabled) {
            ContinuousMonitoringService.start(getApplication())
        } else {
            ContinuousMonitoringService.stop(getApplication())
        }
    }

    fun refreshTimelineData() {
        timelineRefreshJob?.cancel()
        timelineRefreshJob = viewModelScope.launch {
            val nowMs = System.currentTimeMillis()
            val (startMs, endMs) = computeTimeBounds(_selectedTimeFilter.value, nowMs)

            val snapshots = if (_selectedTimeFilter.value == TimeWindowFilter.SINCE_LAST_CHARGE) {
                val lastCharge = continuousDao.getLastFullChargeSnapshot()
                val since = lastCharge?.timestampMs ?: (nowMs - 24 * 3600 * 1000L)
                continuousDao.getSnapshotsSince(since)
            } else {
                continuousDao.getSnapshotsBetween(startMs, endMs)
            }

            val attributionModel = EnergyAttributionModel(_powerProfile.value)
            val timelineEngine = TimelineAggregationEngine(attributionModel, culpritDetector, uidResolver)
            _timelineData.value = timelineEngine.aggregate(_selectedTimeFilter.value, snapshots, nowMs)
        }
    }

    private suspend fun computeTimeBounds(filter: TimeWindowFilter, nowMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (filter) {
            TimeWindowFilter.TODAY -> {
                cal.timeInMillis = nowMs
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, nowMs)
            }
            TimeWindowFilter.YESTERDAY -> {
                cal.timeInMillis = nowMs
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Pair(start, cal.timeInMillis)
            }
            TimeWindowFilter.OVERNIGHT_SLEEP -> {
                cal.timeInMillis = nowMs
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                if (currentHour < 12) {
                    val endMs = if (currentHour >= 7) {
                        cal.set(Calendar.HOUR_OF_DAY, 7)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.timeInMillis
                    } else nowMs
                    cal.timeInMillis = nowMs
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    Pair(cal.timeInMillis, endMs)
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, 7)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val end = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 0)
                    Pair(cal.timeInMillis, end)
                }
            }
            TimeWindowFilter.PAST_7_DAYS -> {
                Pair(nowMs - (7 * 24 * 3600 * 1000L), nowMs)
            }
            TimeWindowFilter.SINCE_LAST_CHARGE -> {
                val lastCharge = continuousDao.getLastFullChargeSnapshot()
                val start = lastCharge?.timestampMs ?: (nowMs - 24 * 3600 * 1000L)
                Pair(start, nowMs)
            }
        }
    }

    fun setMockSource(useMock: Boolean) {
        _useMockSource.value = useMock
        snapshotManager.setDumpSource(if (useMock) mockSource else binderSource)
    }

    fun toggleAutoStopUnlock(enabled: Boolean) {
        _autoStopOnUnlock.value = enabled
    }

    fun toggleAutoStopCharger(enabled: Boolean) {
        _autoStopOnCharger.value = enabled
    }

    fun updateCpuAwakeMa(ma: Double) {
        _powerProfile.value = _powerProfile.value.copy(cpuAwakeMa = ma)
        refreshTimelineData()
    }

    fun updateRadioMa(ma: Double) {
        _powerProfile.value = _powerProfile.value.copy(radioActiveMa = ma)
        refreshTimelineData()
    }

    // Manual Benchmark Methods
    fun startBenchmarkSession(presetName: String) {
        viewModelScope.launch {
            if (_useMockSource.value) {
                mockSource.setSnapshotStage(endSnapshot = false)
            }

            val startSnapshot = snapshotManager.captureSnapshot()
            android.util.Log.d("MainViewModel", "startBenchmarkSession: startSnapshot uids=${startSnapshot.uidMetricsMap.size}, battery=${startSnapshot.deviceMetrics.batteryLevelPercent}%")
            val session = ObservationSession(
                id = UUID.randomUUID().toString(),
                title = presetName,
                startTimeMs = System.currentTimeMillis(),
                startSnapshot = startSnapshot,
                status = SessionStatus.RECORDING
            )

            _activeSession.value = session
            startSessionTimer(session.startTimeMs)

            BatterySessionService.start(
                getApplication(),
                startSnapshot.deviceMetrics.batteryLevelPercent
            )
        }
    }

    fun stopBenchmarkSession() {
        viewModelScope.launch {
            val session = _activeSession.value ?: return@launch
            timerJob?.cancel()
            BatterySessionService.stop(getApplication())

            if (_useMockSource.value) {
                mockSource.setSnapshotStage(endSnapshot = true)
            }

            val endSnapshot = snapshotManager.captureSnapshot()
            val attributionModel = EnergyAttributionModel(_powerProfile.value)
            val deltaCalculator = SnapshotDeltaCalculator(attributionModel, culpritDetector, uidResolver)
            val delta = deltaCalculator.computeDelta(session.startSnapshot, endSnapshot)
            android.util.Log.d("MainViewModel", "stopBenchmarkSession: endSnapshot uids=${endSnapshot.uidMetricsMap.size}, delta culprits=${delta.culprits.size}")

            val completedSession = session.copy(
                endTimeMs = System.currentTimeMillis(),
                endSnapshot = endSnapshot,
                status = SessionStatus.COMPLETED,
                delta = delta
            )

            _activeSession.value = completedSession
            _latestBenchmarkDelta.value = delta

            repository.saveSession(completedSession)
            refreshTimelineData()
        }
    }

    fun resetBenchmarkSession() {
        _activeSession.value = null
        _latestBenchmarkDelta.value = null
    }

    fun selectHistoricalSession(sessionEntity: SessionEntity) {
        val culprits = repository.deserializeCulprits(sessionEntity.culpritsJson)
        val session = ObservationSession(
            id = sessionEntity.id,
            title = sessionEntity.title,
            startTimeMs = sessionEntity.startTimeMs,
            endTimeMs = sessionEntity.endTimeMs,
            startSnapshot = com.antigravity.battery.core.model.BatterySnapshot(
                timestampMs = sessionEntity.startTimeMs,
                deviceMetrics = com.antigravity.battery.core.model.DeviceLevelMetrics(
                    timestampMs = sessionEntity.startTimeMs,
                    batteryLevelPercent = sessionEntity.batteryStartPercent,
                    voltageMv = 4000,
                    temperatureDeciCelsius = 280,
                    isScreenOn = false,
                    totalRealtimeMs = 0L,
                    totalUptimeMs = 0L,
                    screenOnDurationMs = 0L,
                    screenOffDurationMs = 0L
                ),
                uidMetricsMap = emptyMap(),
                packageAlarms = emptyMap()
            ),
            endSnapshot = null,
            status = SessionStatus.COMPLETED,
            delta = SessionDelta(
                durationMs = sessionEntity.durationMs,
                batteryLevelStart = sessionEntity.batteryStartPercent,
                batteryLevelEnd = sessionEntity.batteryEndPercent,
                batteryPercentDrop = sessionEntity.batteryPercentDrop,
                voltageDropMv = 0,
                tempDeltaDeciCelsius = 0,
                drainRatePercentPerHour = sessionEntity.drainRatePercentPerHour,
                realtimeMs = sessionEntity.durationMs,
                uptimeMs = sessionEntity.durationMs,
                screenOnDurationMs = sessionEntity.screenOnDurationMs,
                screenOffDurationMs = sessionEntity.durationMs - sessionEntity.screenOnDurationMs,
                deepSleepDurationMs = sessionEntity.deepSleepDurationMs,
                awakeScreenOffMs = sessionEntity.awakeScreenOffMs,
                deepSleepEfficiencyPercent = sessionEntity.deepSleepEfficiencyPercent,
                isInterrupted = sessionEntity.isInterrupted,
                interruptionReason = sessionEntity.interruptionReason,
                culprits = culprits
            )
        )
        _activeSession.value = session
        _latestBenchmarkDelta.value = session.delta
        _currentTab.value = AppNavTab.BENCHMARK
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun exportMarkdown(sessionEntity: SessionEntity): String {
        val culprits = repository.deserializeCulprits(sessionEntity.culpritsJson)
        val dummySession = ObservationSession(
            id = sessionEntity.id,
            title = sessionEntity.title,
            startTimeMs = sessionEntity.startTimeMs,
            endTimeMs = sessionEntity.endTimeMs,
            startSnapshot = com.antigravity.battery.core.model.BatterySnapshot(
                timestampMs = sessionEntity.startTimeMs,
                deviceMetrics = com.antigravity.battery.core.model.DeviceLevelMetrics(
                    timestampMs = sessionEntity.startTimeMs,
                    batteryLevelPercent = sessionEntity.batteryStartPercent,
                    voltageMv = 4000,
                    temperatureDeciCelsius = 280,
                    isScreenOn = false,
                    totalRealtimeMs = 0L,
                    totalUptimeMs = 0L,
                    screenOnDurationMs = 0L,
                    screenOffDurationMs = 0L
                ),
                uidMetricsMap = emptyMap(),
                packageAlarms = emptyMap()
            ),
            delta = SessionDelta(
                durationMs = sessionEntity.durationMs,
                batteryLevelStart = sessionEntity.batteryStartPercent,
                batteryLevelEnd = sessionEntity.batteryEndPercent,
                batteryPercentDrop = sessionEntity.batteryPercentDrop,
                voltageDropMv = 0,
                tempDeltaDeciCelsius = 0,
                drainRatePercentPerHour = sessionEntity.drainRatePercentPerHour,
                realtimeMs = sessionEntity.durationMs,
                uptimeMs = sessionEntity.durationMs,
                screenOnDurationMs = sessionEntity.screenOnDurationMs,
                screenOffDurationMs = sessionEntity.durationMs - sessionEntity.screenOnDurationMs,
                deepSleepDurationMs = sessionEntity.deepSleepDurationMs,
                awakeScreenOffMs = sessionEntity.awakeScreenOffMs,
                deepSleepEfficiencyPercent = sessionEntity.deepSleepEfficiencyPercent,
                isInterrupted = sessionEntity.isInterrupted,
                interruptionReason = sessionEntity.interruptionReason,
                culprits = culprits
            )
        )
        return DiagnosticExporter.exportToMarkdown(dummySession)
    }

    private fun startSessionTimer(startTimeMs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                val hours = (elapsed / 3_600_000)
                val minutes = (elapsed % 3_600_000) / 60_000
                val seconds = (elapsed % 60_000) / 1000
                _elapsedTimeFormatted.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                delay(1000L)
            }
        }
    }
}
