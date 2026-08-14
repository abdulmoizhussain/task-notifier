package io.github.abdulmoizhussain.tasknotifier.diagnostics

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.github.abdulmoizhussain.tasknotifier.BuildConfig
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.data.AppDatabase
import io.github.abdulmoizhussain.tasknotifier.data.task.Task
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.TimeZone
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DiagnosticsExporter(context: Context) {
    private val applicationContext = context.applicationContext

    suspend fun exportTo(destination: Uri) {
        val outputStream = requireNotNull(
            applicationContext.contentResolver.openOutputStream(destination)
        ) { "Could not open the selected diagnostics file." }
        outputStream.use { exportTo(it) }
    }

    suspend fun exportTo(outputStream: OutputStream) {
        DiagnosticLog.record(applicationContext, "DIAGNOSTICS_EXPORT_STARTED")
        val tasks = AppDatabase.getDatabase(applicationContext).taskDao().readAllAsync()
        val temporaryDirectory = File(
            applicationContext.cacheDir,
            "diagnostics-export-${UUID.randomUUID()}",
        ).apply { mkdirs() }

        try {
            val logicalSnapshot = File(temporaryDirectory, "task_notifier_snapshot.db")
            createLogicalDatabaseSnapshot(logicalSnapshot, tasks)
            checkpointDatabase()

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                zip.addString("app-state.json", createAppState(tasks).toString(2))
                zip.addString("tasks.json", createTasksJson(tasks).toString(2))
                zip.addString("README.txt", readme())
                zip.addFile("database/task_notifier_snapshot.db", logicalSnapshot)
                addRawDatabaseFiles(zip)
                DiagnosticLog.files(applicationContext).forEach { logFile ->
                    zip.addFile("events/${logFile.name}", logFile)
                }
            }

            DiagnosticLog.record(
                applicationContext,
                "DIAGNOSTICS_EXPORT_COMPLETED",
                attributes = mapOf("taskCount" to tasks.size),
            )
        } catch (exception: Exception) {
            DiagnosticLog.record(
                applicationContext,
                "DIAGNOSTICS_EXPORT_FAILED",
                throwable = exception,
            )
            throw exception
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun createLogicalDatabaseSnapshot(snapshotFile: File, tasks: List<Task>) {
        SQLiteDatabase.openOrCreateDatabase(snapshotFile, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE task_table (
                    description TEXT NOT NULL,
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    dateTime INTEGER NOT NULL,
                    repeat INTEGER NOT NULL,
                    stopAfter INTEGER NOT NULL,
                    sentCount INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    inProgress INTEGER NOT NULL,
                    dateCreated INTEGER NOT NULL DEFAULT 0,
                    dateModified INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            database.version = 2
            database.beginTransaction()
            try {
                tasks.forEach { task ->
                    val values = ContentValues().apply {
                        put("description", task.description)
                        put("id", task.id)
                        put("dateTime", task.dateTime)
                        put("repeat", task.repeat)
                        put("stopAfter", task.stopAfter)
                        put("sentCount", task.sentCount)
                        put("status", task.status.name)
                        put("inProgress", if (task.inProgress) 1 else 0)
                        put("dateCreated", task.dateCreated)
                        put("dateModified", task.dateModified)
                    }
                    check(database.insertOrThrow("task_table", null, values) != -1L)
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
    }

    private fun checkpointDatabase() {
        try {
            AppDatabase.getDatabase(applicationContext)
                .openHelper
                .writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { cursor -> while (cursor.moveToNext()) Unit }
        } catch (exception: Exception) {
            DiagnosticLog.record(
                applicationContext,
                "DATABASE_CHECKPOINT_FAILED",
                throwable = exception,
            )
        }
    }

    private fun addRawDatabaseFiles(zip: ZipOutputStream) {
        val databaseFile = applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME)
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            val file = File(databaseFile.path + suffix)
            if (file.exists()) {
                zip.addFile("database/raw/${file.name}", file)
            }
        }
    }

    private fun createTasksJson(tasks: List<Task>): JSONArray {
        return JSONArray().apply {
            tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id)
                    put("description", task.description)
                    put("dateTime", task.dateTime)
                    put("repeat", task.repeat)
                    put("stopAfter", task.stopAfter)
                    put("sentCount", task.sentCount)
                    put("status", task.status.name)
                    put("inProgress", task.inProgress)
                    put("dateCreated", task.dateCreated)
                    put("dateModified", task.dateModified)
                })
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createAppState(tasks: List<Task>): JSONObject {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val packageInfo = applicationContext.packageManager
            .getPackageInfo(applicationContext.packageName, 0)
        val activeNotificationIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.map { it.id }
        } else {
            emptyList()
        }

        return JSONObject().apply {
            put("capturedAtMillis", System.currentTimeMillis())
            put("packageName", applicationContext.packageName)
            put("versionName", packageInfo.versionName)
            put(
                "versionCode",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }
            )
            put("debugBuild", BuildConfig.DEBUG)
            put("databaseVersion", 2)
            put("taskCount", tasks.size)
            put("timezone", TimeZone.getDefault().id)
            put("deviceSdk", Build.VERSION.SDK_INT)
            put("deviceManufacturer", Build.MANUFACTURER)
            put("deviceModel", Build.MODEL)
            put(
                "notificationsEnabled",
                NotificationManagerCompat.from(applicationContext).areNotificationsEnabled(),
            )
            put("activeNotificationIds", JSONArray(activeNotificationIds))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                put(
                    "defaultChannelImportance",
                    notificationManager
                        .getNotificationChannel(Constants.NOTIFICATION_CHANNEL_DEFAULT)
                        ?.importance ?: JSONObject.NULL,
                )
                put(
                    "silentChannelImportance",
                    notificationManager
                        .getNotificationChannel(Constants.NOTIFICATION_CHANNEL_SILENT)
                        ?.importance ?: JSONObject.NULL,
                )
            }
        }
    }

    private fun readme(): String {
        return """
            Task Notifier diagnostics

            task_notifier_snapshot.db is a standalone SQLite copy of every task captured by one
            Room query. Open it directly in DB Browser for SQLite.

            database/raw contains the original database and any WAL/SHM sidecar files found at
            export time. Keep those files together when inspecting the raw database.

            Event logs intentionally omit reminder descriptions. tasks.json and the database
            contain reminder text because they are full user-requested data snapshots.
        """.trimIndent()
    }

    private fun ZipOutputStream.addString(path: String, value: String) {
        putNextEntry(ZipEntry(path))
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.addFile(path: String, file: File) {
        putNextEntry(ZipEntry(path))
        file.inputStream().buffered().use { input -> input.copyTo(this) }
        closeEntry()
    }
}
