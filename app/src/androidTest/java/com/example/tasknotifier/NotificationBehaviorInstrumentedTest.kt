package com.example.tasknotifier

import android.app.NotificationManager
import android.content.Context
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.common.Globals
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationBehaviorInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val taskDao = AppDatabase.getDatabase(context).taskDao()
    private val createdTaskIds = mutableListOf<Int>()

    @After
    fun cleanUp() {
        createdTaskIds.forEach { MyNotificationManager.cancelById(context, it) }
        runBlocking {
            createdTaskIds.forEach { taskDao.deleteOneByIdAsync(it) }
        }

        resumedTaskActivity()?.let { activity ->
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test
    fun eachNotificationOpensItsOwnTask() {
        val firstTask = createTask("Notification routing test A")
        val secondTask = createTask("Notification routing test B")
        postSilently(firstTask)
        postSilently(secondTask)

        val firstContentIntent = activeNotification(firstTask.id).notification.contentIntent
        val secondContentIntent = activeNotification(secondTask.id).notification.contentIntent
        assertNotNull(firstContentIntent)
        assertNotNull(secondContentIntent)
        assertNotEquals(firstContentIntent, secondContentIntent)

        firstContentIntent.send()
        assertDisplayedTask(firstTask)

        secondContentIntent.send()
        assertDisplayedTask(secondTask)
    }

    @Test
    fun swipingAnActiveTaskNotificationRestoresItSilently() {
        val task = createTask("Persistent notification test")
        postSilently(task)

        val dismissIntent = activeNotification(task.id).notification.deleteIntent
        assertNotNull(dismissIntent)

        notificationManager.cancel(task.id)
        dismissIntent.send()

        assertTrue(
            "The active task notification was not restored after dismissal",
            waitUntil { notificationManager.activeNotifications.any { it.id == task.id } }
        )
    }

    private fun createTask(description: String): Task {
        val task = Task(description).apply {
            dateTime = System.currentTimeMillis()
            inProgress = true
        }
        task.id = runBlocking { taskDao.addOneAsync(task).toInt() }
        createdTaskIds.add(task.id)
        return task
    }

    private fun postSilently(task: Task) {
        MyNotificationManager.notifySilently(
            context,
            task.id,
            Globals.createTitleForTask(task.dateTime, task.sentCount),
            task.description,
            task.dateTime,
            true,
        )
    }

    private fun activeNotification(taskId: Int) =
        notificationManager.activeNotifications.first { it.id == taskId }

    private fun assertDisplayedTask(task: Task) {
        assertTrue("Task detail activity did not resume", waitUntil { resumedTaskActivity() != null })
        instrumentation.waitForIdleSync()

        val activity = checkNotNull(resumedTaskActivity())
        var displayedTaskId = -1
        var displayedDescription = ""
        instrumentation.runOnMainSync {
            displayedTaskId = activity.intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, -1)
            displayedDescription = activity.findViewById<TextView>(R.id.textViewTaskDescription).text.toString()
        }

        assertEquals(task.id, displayedTaskId)
        assertEquals(task.description, displayedDescription)
    }

    private fun resumedTaskActivity(): ActivityViewTask? {
        var resumedActivity: ActivityViewTask? = null
        instrumentation.runOnMainSync {
            resumedActivity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<ActivityViewTask>()
                .firstOrNull()
        }
        return resumedActivity
    }

    private fun waitUntil(timeoutMillis: Long = 3000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) {
                return true
            }
            Thread.sleep(50)
        }
        return condition()
    }
}
