package com.antigravity.battery.core.dump

import android.content.Context
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Safe diagnostic dump provider that reflects android.os.ServiceManager.
 * Uses a concrete temporary file on disk instead of an anonymous pipe to completely eliminate
 * pipe-buffer deadlocks and SIGPIPE (Broken Pipe) crashes in System Server.
 */
class BinderDumpSource(private val context: Context? = null) : DiagnosticDumpSource {

    companion object {
        private const val TAG = "BinderDumpSource"
        private val dumpMutex = Mutex()
    }

    override suspend fun getBatterystatsCheckin(): String = withContext(Dispatchers.IO) {
        var result = dumpServiceSafely("batterystats", arrayOf("--checkin"))
        if (result.isBlank()) {
            Log.w(TAG, "batterystats --checkin returned blank, trying -c...")
            result = dumpServiceSafely("batterystats", arrayOf("-c"))
        }
        result
    }

    override suspend fun getAlarmDump(): String = withContext(Dispatchers.IO) {
        dumpServiceSafely("alarm", arrayOf())
    }

    override suspend fun getPowerDump(): String = withContext(Dispatchers.IO) {
        dumpServiceSafely("power", arrayOf())
    }

    private suspend fun dumpServiceSafely(serviceName: String, args: Array<String>): String = dumpMutex.withLock {
        val targetDir = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        val dumpFile = File(targetDir, "dump_${serviceName}_temp.txt")

        var pfd: ParcelFileDescriptor? = null
        try {
            if (dumpFile.exists()) dumpFile.delete()
            dumpFile.createNewFile()

            pfd = ParcelFileDescriptor.open(
                dumpFile,
                ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE
            )

            val smClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = smClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, serviceName) as? IBinder
                ?: return@withLock ""

            // Dump to a concrete file descriptor.
            // Unlike pipes, file descriptors never send SIGPIPE (Broken Pipe) to system_server.
            binder.dump(pfd.fileDescriptor, args)

            pfd.close()
            pfd = null

            if (dumpFile.exists()) {
                val content = dumpFile.readText()
                dumpFile.delete()
                Log.d(TAG, "dumpServiceSafely($serviceName): read ${content.length} chars (file size was ${dumpFile.length()})")
                content
            } else {
                Log.w(TAG, "dumpServiceSafely($serviceName): dumpFile does not exist")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Safe dump of $serviceName failed: ${e.message}", e)
            ""
        } finally {
            try { pfd?.close() } catch (_: Exception) {}
            try { if (dumpFile.exists()) dumpFile.delete() } catch (_: Exception) {}
        }
    }
}
