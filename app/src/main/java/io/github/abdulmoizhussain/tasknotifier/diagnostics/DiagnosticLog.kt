package io.github.abdulmoizhussain.tasknotifier.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DiagnosticLog {
    private const val DIRECTORY_NAME = "diagnostics"
    private const val MAX_FILE_BYTES = 1024 * 1024L

    @Synchronized
    fun record(
        context: Context,
        event: String,
        taskId: Int? = null,
        attributes: Map<String, Any?> = emptyMap(),
        throwable: Throwable? = null,
    ) {
        try {
            val applicationContext = context.applicationContext
            val processName = processName(applicationContext)
            val logFile = currentLogFile(applicationContext, processName)
            rotateIfNecessary(logFile)

            val entry = JSONObject().apply {
                put("timeMillis", System.currentTimeMillis())
                put("elapsedRealtimeMillis", SystemClock.elapsedRealtime())
                put("time", utcTimestamp())
                put("event", event)
                put("process", processName)
                put("pid", Process.myPid())
                put("thread", Thread.currentThread().name)
                if (taskId != null) {
                    put("taskId", taskId)
                }
                attributes.forEach { (key, value) -> put(key, jsonValue(value)) }
                if (throwable != null) {
                    put("errorType", throwable.javaClass.name)
                    put("errorMessage", throwable.localizedMessage ?: throwable.toString())
                }
            }

            logFile.appendText(entry.toString() + "\n", Charsets.UTF_8)
        } catch (_: Exception) {
            // Diagnostics must never interfere with reminder delivery.
        }
    }

    fun files(context: Context): List<File> {
        return diagnosticsDirectory(context.applicationContext)
            .listFiles()
            ?.filter { it.isFile }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    private fun currentLogFile(context: Context, processName: String): File {
        val safeProcessName = processName
            .removePrefix(context.packageName)
            .ifBlank { "main" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(diagnosticsDirectory(context), "events-$safeProcessName.jsonl")
    }

    private fun diagnosticsDirectory(context: Context): File {
        return File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }
    }

    private fun rotateIfNecessary(logFile: File) {
        if (!logFile.exists() || logFile.length() < MAX_FILE_BYTES) {
            return
        }

        val previousFile = File(logFile.parentFile, "${logFile.nameWithoutExtension}-previous.jsonl")
        if (previousFile.exists()) {
            previousFile.delete()
        }
        logFile.renameTo(previousFile)
    }

    @Suppress("DEPRECATION")
    private fun processName(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return android.app.Application.getProcessName()
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.runningAppProcesses
            ?.firstOrNull { it.pid == Process.myPid() }
            ?.processName
            ?: context.packageName
    }

    private fun utcTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private fun jsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is String, is Number, is Boolean, is JSONObject -> value
            is Iterable<*> -> org.json.JSONArray().apply {
                value.forEach { put(jsonValue(it)) }
            }
            is Array<*> -> org.json.JSONArray().apply {
                value.forEach { put(jsonValue(it)) }
            }
            else -> value.toString()
        }
    }
}
