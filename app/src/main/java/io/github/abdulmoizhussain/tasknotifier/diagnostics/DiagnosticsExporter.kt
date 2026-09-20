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
import java.security.MessageDigest
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

    /**
     * Reminder text is deliberately not exported. Everything needed to debug the
     * notification pipeline is structural, so the description is reduced to a shape
     * and a stable fingerprint: length and line count keep the notification layout
     * debuggable, and the hash shows whether the text changed between two exports
     * without revealing what it says.
     */
    private fun redactDescription(description: String): JSONObject {
        return JSONObject().apply {
            put("redacted", true)
            put("length", description.length)
            put("lineCount", description.lines().size)
            put("isBlank", description.isBlank())
            put("sha256Prefix", sha256Prefix(description))
        }
    }

    private fun sha256Prefix(value: String): String {
        return try {
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .take(6)
                .joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "unavailable"
        }
    }

    private fun createTasksJson(tasks: List<Task>): JSONArray {
        return JSONArray().apply {
            tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id)
                    put("description", redactDescription(task.description))
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
        val activeSnapshot = NotificationTelemetry.activeSnapshot(notificationManager)
        val activeNotificationIds = NotificationTelemetry.activeIds(activeSnapshot)

        // The check that matters most: tasks the app believes are showing a
        // notification, but which are not actually in the shade.
        val inProgressTaskIds = tasks.filter { it.inProgress }.map { it.id }
        val missingNotificationIds = inProgressTaskIds.filterNot { activeNotificationIds.contains(it) }

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
            put("activeNotificationDetail", JSONArray(NotificationTelemetry.describeActive(activeSnapshot)))
            put("inProgressTaskIds", JSONArray(inProgressTaskIds))
            put("missingNotificationIds", JSONArray(missingNotificationIds))
            put("missingNotificationCount", missingNotificationIds.size)

            put("buildDisplay", Build.DISPLAY)
            put("buildFingerprint", Build.FINGERPRINT)
            put("deviceDevice", Build.DEVICE)
            put("timezoneOffsetMillis", TimeZone.getDefault().getOffset(System.currentTimeMillis()))
            put("targetSdk", applicationContext.applicationInfo.targetSdkVersion)

            NotificationTelemetry.environment(applicationContext).forEach { (key, value) ->
                put(key, value ?: JSONObject.NULL)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                put(
                    "canScheduleExactAlarms",
                    (applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager)
                        .canScheduleExactAlarms(),
                )
            }

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

            PRIVACY
            app-state.json and tasks.json do NOT contain reminder text. Each description is
            replaced by its length, line count and a short SHA-256 prefix, which is enough to
            tell whether the text changed between exports without revealing what it says.

            WARNING: database/task_notifier_snapshot.db and database/raw/ still contain the
            full reminder text, because they are verbatim database snapshots. Delete those
            files before sharing this bundle if the reminder text is sensitive.

            Event logs have never contained reminder descriptions.

            WHAT TO LOOK AT FIRST
            app-state.json -> missingNotificationIds. Non-empty means the app believes a task
            is showing a notification that is not actually in the shade.

            events/*.jsonl, per notification post:
              NOTIFICATION_POST_REQUESTED  burst context (millisSincePreviousPost,
                                           postsInLastSecond, postsOfThisIdInProcess) plus
                                           device power and channel state at post time
              NOTIFICATION_POST_RESULT     immediate read-back, from a single snapshot
              NOTIFICATION_POST_VERIFIED   the post was still alive a few seconds later
              NOTIFICATION_POST_LOST       it was not - the post was silently dropped
              NOTIFICATION_RECONCILE       expected vs actual notification ids on wake-up
              NOTIFICATION_DISMISSED_RECEIVED  carries ageHours, so a system reap is
                                           distinguishable from a user swipe

            NOTIFICATION_POST_LOST is the event to search for first.

            database/raw contains the original database and any WAL/SHM sidecar files found at
            export time. Keep those files together when inspecting the raw database.
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
