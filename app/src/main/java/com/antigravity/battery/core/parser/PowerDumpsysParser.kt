package com.antigravity.battery.core.parser

import java.util.regex.Pattern

data class ActiveWakelockInfo(
    val type: String,
    val tag: String,
    val uid: Int,
    val pid: Int
)

/**
 * Parser for `dumpsys power` to identify instantaneous active wake locks held at snapshot time.
 */
class PowerDumpsysParser {

    private val wakelockPattern = Pattern.compile(
        """(PARTIAL_WAKE_LOCK|SCREEN_BRIGHT_WAKE_LOCK|SCREEN_DIM_WAKE_LOCK)\s+'([^']+)'\s+.*?\(uid=(\d+)(?:\s+pid=(\d+))?\)"""
    )

    fun parseActiveLocks(rawPowerDump: String): List<ActiveWakelockInfo> {
        val locks = mutableListOf<ActiveWakelockInfo>()
        rawPowerDump.lineSequence().forEach { line ->
            val matcher = wakelockPattern.matcher(line.trim())
            if (matcher.find()) {
                val type = matcher.group(1) ?: "PARTIAL_WAKE_LOCK"
                val tag = matcher.group(2) ?: "Unknown"
                val uid = matcher.group(3)?.toIntOrNull() ?: -1
                val pid = matcher.group(4)?.toIntOrNull() ?: -1
                locks.add(ActiveWakelockInfo(type, tag, uid, pid))
            }
        }
        return locks
    }
}
