package com.sneakyzippy.powerlab.core.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

/**
 * Optional helper for granting PowerLab's ADB-level permissions on-device via Shizuku
 * without requiring a PC connection.
 */
object ShizukuPermissionHelper {

    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val SHIZUKU_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE"
    const val SHIZUKU_GITHUB_URL = "https://github.com/RikkaApps/Shizuku/releases"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun requestShizukuPermission() {
        if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }

    fun openShizukuApp(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Binds a short-lived Shizuku UserService and executes `pm grant` + `appops set`
     * commands inside Shizuku's privileged shell process.
     */
    suspend fun grantAllPermissions(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!Shizuku.pingBinder()) {
                error("Shizuku service is not running.")
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                error("Shizuku permission has not been granted to PowerLab.")
            }

            val packageName = context.packageName
            val shellCommand = listOf(
                "pm grant $packageName android.permission.BATTERY_STATS",
                "pm grant $packageName android.permission.DUMP",
                "pm grant $packageName android.permission.PACKAGE_USAGE_STATS",
                "pm grant $packageName android.permission.INTERACT_ACROSS_USERS",
                "appops set $packageName GET_USAGE_STATS allow"
            ).joinToString(" && ")

            val args = Shizuku.UserServiceArgs(
                ComponentName(packageName, ShizukuShellService::class.java.name)
            )
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(false)
                .version(1)

            val output = withTimeout(15_000L) {
                suspendCancellableCoroutine { continuation ->
                    var connectionRef: ServiceConnection? = null
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            val result = try {
                                val shell = IShizukuShellService.Stub.asInterface(service)
                                shell.runCommand(shellCommand)
                            } catch (e: Exception) {
                                "ERR:-1:${e.message ?: "Binder call failed"}"
                            } finally {
                                runCatching { Shizuku.unbindUserService(args, this, true) }
                            }
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            if (continuation.isActive) {
                                continuation.resume("ERR:-1:Shizuku UserService disconnected")
                            }
                        }
                    }
                    connectionRef = connection

                    continuation.invokeOnCancellation {
                        connectionRef?.let { runCatching { Shizuku.unbindUserService(args, it, true) } }
                    }

                    Shizuku.bindUserService(args, connection)
                }
            }

            if (output != "OK") {
                error(output.removePrefix("ERR:").substringAfter(':', output))
            }
        }
    }
}
