package io.github.abdulmoizhussain.tasknotifier

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.abdulmoizhussain.tasknotifier.data.AppDatabase
import io.github.abdulmoizhussain.tasknotifier.data.task.Task
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticsExporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class DiagnosticsExportInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val taskDao = AppDatabase.getDatabase(context).taskDao()
    private var createdTaskId = 0

    @After
    fun cleanUp() {
        if (createdTaskId > 0) {
            runBlocking { taskDao.deleteOneByIdAsync(createdTaskId) }
        }
    }

    @Test
    fun exportContainsQueryableDatabaseSnapshotAndEventLog() {
        val task = Task("Diagnostics export test").apply {
            dateTime = 123456789L
            repeat = 2
            stopAfter = 3
            sentCount = 4
            inProgress = true
            dateCreated = 100L
            dateModified = 200L
        }
        createdTaskId = runBlocking { taskDao.addOneAsync(task).toInt() }
        DiagnosticLog.record(context, "DIAGNOSTICS_TEST_MARKER", createdTaskId)

        val bytes = ByteArrayOutputStream().also { output ->
            runBlocking { DiagnosticsExporter(context).exportTo(output) }
        }.toByteArray()
        val entries = unzip(bytes)

        assertTrue(entries.containsKey("app-state.json"))
        assertTrue(entries.containsKey("tasks.json"))
        assertTrue(entries.keys.any { it.startsWith("events/events-") })

        val snapshotBytes = checkNotNull(entries["database/task_notifier_snapshot.db"])
        val snapshotFile = File(context.cacheDir, "diagnostics-export-test.db")
        snapshotFile.writeBytes(snapshotBytes)
        try {
            SQLiteDatabase.openDatabase(
                snapshotFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { database ->
                database.query(
                    "task_table",
                    arrayOf("inProgress", "sentCount", "dateModified"),
                    "id = ?",
                    arrayOf(createdTaskId.toString()),
                    null,
                    null,
                    null,
                ).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(1, cursor.getInt(0))
                    assertEquals(4, cursor.getInt(1))
                    assertEquals(200L, cursor.getLong(2))
                }
            }
        } finally {
            snapshotFile.delete()
        }
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }
}
