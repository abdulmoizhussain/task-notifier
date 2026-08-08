package com.example.tasknotifier.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationInstrumentedTest {
    companion object {
        private const val TEST_DATABASE = "migration-test"
    }

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Before
    fun removePreviousTestDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationFrom1To2PreservesTaskAndAddsUnknownTimestamps() {
        migrationHelper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO task_table
                    (description, id, dateTime, repeat, stopAfter, sentCount, status, inProgress)
                VALUES
                    ('Legacy reminder', 42, 123456789, 2, 3, 4, 'On', 1)
                """.trimIndent()
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            AppDatabase.MIGRATION_1_2,
        )

        migratedDatabase.query("SELECT * FROM task_table WHERE id = 42").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Legacy reminder", cursor.getString(cursor.getColumnIndexOrThrow("description")))
            assertEquals(123456789L, cursor.getLong(cursor.getColumnIndexOrThrow("dateTime")))
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("repeat")))
            assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("stopAfter")))
            assertEquals(4, cursor.getInt(cursor.getColumnIndexOrThrow("sentCount")))
            assertEquals("On", cursor.getString(cursor.getColumnIndexOrThrow("status")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("inProgress")))
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("dateCreated")))
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("dateModified")))
        }

        migratedDatabase.close()
    }
}
