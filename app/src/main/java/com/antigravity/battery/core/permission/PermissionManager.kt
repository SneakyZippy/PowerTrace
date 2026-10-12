package com.antigravity.battery.core.permission

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

data class PermissionStatus(
    val permission: String,
    val isGranted: Boolean,
    val description: String,
    val adbCommand: String,
    val settingsAction: String? = null
)

data class PermissionState(
    val isBatteryStatsGranted: Boolean,
    val isDumpGranted: Boolean,
    val isPackageUsageStatsGranted: Boolean,
    val areAllRequiredGranted: Boolean,
    val statuses: List<PermissionStatus>,
    val fullAdbScript: String,
    val singleLineAdbCommand: String
)

/**
 * Manages checking ADB-elevated system permissions and formatting setup commands.
 */
class PermissionManager(private val context: Context) {

    private val packageName: String = context.packageName

    fun checkPermissions(): PermissionState {
        val batteryStatsGranted = context.checkSelfPermission(Manifest.permission.BATTERY_STATS) ==
                PackageManager.PERMISSION_GRANTED

        val dumpGranted = context.checkSelfPermission(Manifest.permission.DUMP) ==
                PackageManager.PERMISSION_GRANTED

        val usageStatsGranted = checkUsageStatsPermission()

        val statuses = listOf(
            PermissionStatus(
                permission = "android.permission.BATTERY_STATS",
                isGranted = batteryStatsGranted,
                description = "Required to query per-app wake locks, CPU clusters, and network radio states.",
                adbCommand = "adb shell pm grant $packageName android.permission.BATTERY_STATS"
            ),
            PermissionStatus(
                permission = "android.permission.DUMP",
                isGranted = dumpGranted,
                description = "Required to stream dumpsys alarm and dumpsys power snapshots.",
                adbCommand = "adb shell pm grant $packageName android.permission.DUMP"
            ),
            PermissionStatus(
                permission = "android.permission.PACKAGE_USAGE_STATS",
                isGranted = usageStatsGranted,
                description = "Required to correlate background app states and screen-off intervals. Can be granted via Settings or ADB.",
                adbCommand = "adb shell pm grant $packageName android.permission.PACKAGE_USAGE_STATS\nadb shell appops set $packageName GET_USAGE_STATS allow",
                settingsAction = android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS
            )
        )

        val fullScript = """
            adb shell pm grant $packageName android.permission.BATTERY_STATS
            adb shell pm grant $packageName android.permission.DUMP
            adb shell pm grant $packageName android.permission.PACKAGE_USAGE_STATS
            adb shell appops set $packageName GET_USAGE_STATS allow
        """.trimIndent()

        val singleLineCmd = "adb shell \"pm grant $packageName android.permission.BATTERY_STATS; pm grant $packageName android.permission.DUMP; pm grant $packageName android.permission.PACKAGE_USAGE_STATS; appops set $packageName GET_USAGE_STATS allow\""

        return PermissionState(
            isBatteryStatsGranted = batteryStatsGranted,
            isDumpGranted = dumpGranted,
            isPackageUsageStatsGranted = usageStatsGranted,
            areAllRequiredGranted = batteryStatsGranted && dumpGranted && usageStatsGranted,
            statuses = statuses,
            fullAdbScript = fullScript,
            singleLineAdbCommand = singleLineCmd
        )
    }

    private fun checkUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }
}
