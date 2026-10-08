package com.antigravity.battery.core.parser

import com.antigravity.battery.core.model.PackageAlarmMetrics
import java.util.regex.Pattern

/**
 * Parser for `dumpsys alarm` output to extract wakeup alarm counts and intent triggers per package.
 */
class AlarmDumpsysParser {

    private val packageHeaderRegex = Pattern.compile(
        """(?:u\d+a\d+|UID\s+\d+):([a-zA-Z0-9._]+).*?(\d+)\s+wakeups:?"""
    )

    private val alarmActionRegex = Pattern.compile(
        """\+(\S+)\s+(\d+)\s+alarms?:.*?(?:act=([a-zA-Z0-9._]+)|cmp=\{([^}]+)\})"""
    )

    fun parse(rawAlarmDump: String): Map<String, PackageAlarmMetrics> {
        val result = mutableMapOf<String, PackageAlarmMetrics>()

        var currentPackage: String? = null
        var currentWakeups = 0
        var currentActions = mutableMapOf<String, Int>()

        rawAlarmDump.lineSequence().forEach { line ->
            val trimmed = line.trim()

            // Check if line declares a package header with wakeups
            val pkgMatcher = packageHeaderRegex.matcher(trimmed)
            if (pkgMatcher.find()) {
                // Commit previous package
                currentPackage?.let { pkg ->
                    result[pkg] = PackageAlarmMetrics(
                        packageName = pkg,
                        wakeupCount = currentWakeups,
                        topAlarmActions = currentActions
                    )
                }

                currentPackage = pkgMatcher.group(1)
                currentWakeups = pkgMatcher.group(2)?.toIntOrNull() ?: 0
                currentActions = mutableMapOf()
                return@forEach
            }

            // Check if line declares specific alarm trigger action/component under current package
            if (currentPackage != null) {
                val actionMatcher = alarmActionRegex.matcher(trimmed)
                if (actionMatcher.find()) {
                    val count = actionMatcher.group(2)?.toIntOrNull() ?: 1
                    val actionName = actionMatcher.group(3)
                        ?: actionMatcher.group(4)?.substringAfterLast('/')
                        ?: "Unknown"

                    currentActions[actionName] = (currentActions[actionName] ?: 0) + count
                }
            }
        }

        // Commit final package if present
        currentPackage?.let { pkg ->
            result[pkg] = PackageAlarmMetrics(
                packageName = pkg,
                wakeupCount = currentWakeups,
                topAlarmActions = currentActions
            )
        }

        return result
    }
}
