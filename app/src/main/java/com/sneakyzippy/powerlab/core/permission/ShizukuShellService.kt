package com.sneakyzippy.powerlab.core.permission

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * Privileged Shizuku UserService that runs in Shizuku's shell (UID 2000) process
 * to execute one-time `pm grant` and `appops set` commands on-device.
 */
class ShizukuShellService : IShizukuShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun runCommand(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                "OK"
            } else {
                "ERR:$exitCode:${output.ifBlank { "Command failed" }}"
            }
        } catch (e: Exception) {
            "ERR:-1:${e.message ?: "Unknown error"}"
        }
    }
}
