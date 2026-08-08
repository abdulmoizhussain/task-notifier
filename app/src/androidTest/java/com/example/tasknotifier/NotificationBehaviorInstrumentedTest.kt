package com.example.tasknotifier

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.tasknotifier.android_services.TaskNotifierAndroidService
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.common.Globals
import com.example.tasknotifier.common.TaskStatusEnum
import com.example.tasknotifier.broadcast_receivers.SendNotificationBroadcastReceiver
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyAlarmManager
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        createdTaskIds.forEach {
            MyAlarmManager.cancel(context, it)
            MyNotificationManager.cancelById(context, it)
        }
        runBlocking {
            createdTaskIds.forEach { taskDao.deleteOneByIdAsync(it) }
        }

        resumedTaskActivity()?.let { activity ->
            instrumentation.runOnMainSync { activity.finish() }
        }
        resumedEditActivity()?.let { activity ->
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
        val firstGroup = activeNotification(firstTask.id).notification.group
        val secondGroup = activeNotification(secondTask.id).notification.group
        assertNotNull(firstContentIntent)
        assertNotNull(secondContentIntent)
        assertNotEquals(firstContentIntent, secondContentIntent)
        assertEquals("${Constants.NOTIFICATION_GROUP_TASK_PREFIX}.${firstTask.id}", firstGroup)
        assertEquals("${Constants.NOTIFICATION_GROUP_TASK_PREFIX}.${secondTask.id}", secondGroup)
        assertNotEquals(firstGroup, secondGroup)

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

    @Test
    fun longTaskDetailsCanScroll() {
        val longDescription = List(80) {
            "This is a long reminder description used to verify detail-screen scrolling."
        }.joinToString(" ")
        val task = createTask(longDescription)
        postSilently(task)

        activeNotification(task.id).notification.contentIntent.send()
        assertDisplayedTask(task)

        val activity = checkNotNull(resumedTaskActivity())
        var canScrollDown = false
        instrumentation.runOnMainSync {
            val scrollView = activity.findViewById<ScrollView>(R.id.scrollViewTaskDetails)
            canScrollDown = scrollView.canScrollVertically(1)
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
        instrumentation.waitForIdleSync()

        var scrollPosition = 0
        instrumentation.runOnMainSync {
            scrollPosition = activity.findViewById<ScrollView>(R.id.scrollViewTaskDetails).scrollY
        }

        assertTrue("Long task details did not create scrollable overflow", canScrollDown)
        assertTrue("Task details did not scroll downward", scrollPosition > 0)
    }

    @Test
    fun turningOffSchedulingCancelsAlarmButKeepsActiveNotification() {
        val task = createTask("Turn off scheduling test").apply {
            dateTime = System.currentTimeMillis() + 10 * 60_000
            status = TaskStatusEnum.On
            inProgress = true
        }
        runBlocking { taskDao.updateOneAsync(task) }
        TaskService.createIntentAndSetExactAlarm(context, task.id, task.dateTime)
        postSilently(task)

        context.startActivity(
            Intent(context, ActivityAddTask::class.java).apply {
                putExtra(Constants.INTENT_EXTRA_TASK_ID, task.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        assertTrue("Edit activity did not resume", waitUntil { resumedEditActivity() != null })

        val activity = checkNotNull(resumedEditActivity())
        instrumentation.runOnMainSync {
            activity.findViewById<Button>(R.id.buttonTurnOffTask).performClick()
        }

        val persistedTask = runBlocking { taskDao.getOneByIdAsync(task.id) }
        assertNotNull(persistedTask)
        assertEquals(TaskStatusEnum.Off, persistedTask?.status)
        assertTrue("Turning off scheduling changed in-progress state", persistedTask?.inProgress == true)
        assertTrue("The future alarm still exists", MyAlarmManager.isAlarmOff(context, task.id))
        assertTrue(
            "Turning off scheduling removed the active notification",
            notificationManager.activeNotifications.any { it.id == task.id }
        )
    }

    @Test
    fun staleAlarmBroadcastCannotReactivateAnOffTask() {
        val task = createTask("Stale alarm broadcast test").apply {
            status = TaskStatusEnum.Off
            inProgress = false
        }
        runBlocking { taskDao.updateOneAsync(task) }

        SendNotificationBroadcastReceiver().onReceive(
            context,
            Intent(context, SendNotificationBroadcastReceiver::class.java).apply {
                putExtra(Constants.INTENT_EXTRA_TASK_ID, task.id)
            }
        )

        val persistedTask = runBlocking { taskDao.getOneByIdAsync(task.id) }
        assertNotNull(persistedTask)
        assertEquals(TaskStatusEnum.Off, persistedTask?.status)
        assertFalse("Stale alarm changed in-progress state", persistedTask?.inProgress == true)
        assertFalse(
            "Stale alarm posted a notification for an off task",
            notificationManager.activeNotifications.any { it.id == task.id }
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun schedulerServiceStopsAfterWorkWhileSystemArtifactsRemain() {
        val task = createTask("One-shot scheduler service test").apply {
            dateTime = System.currentTimeMillis() + 10 * 60_000
            status = TaskStatusEnum.On
            inProgress = true
        }
        runBlocking { taskDao.updateOneAsync(task) }

        context.startService(
            Intent(context, TaskNotifierAndroidService::class.java).apply {
                putExtra(Constants.INTENT_EXTRA_NOTIFICATION_REVIVER_SERVICE, true)
                putExtra(Constants.INTENT_EXTRA_TASK_SCHEDULER_SERVICE, true)
            }
        )

        assertTrue(
            "The service did not schedule the future alarm",
            waitUntil { !MyAlarmManager.isAlarmOff(context, task.id) }
        )
        assertTrue(
            "The service did not post the active notification",
            waitUntil { notificationManager.activeNotifications.any { it.id == task.id } }
        )

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        assertTrue(
            "The one-shot scheduler service remained active after completing its work",
            waitUntil {
                activityManager.getRunningServices(Int.MAX_VALUE).none {
                    it.service.className == TaskNotifierAndroidService::class.java.name
                }
            }
        )

        assertFalse("The alarm disappeared when the service stopped", MyAlarmManager.isAlarmOff(context, task.id))
        assertTrue(
            "The notification disappeared when the service stopped",
            notificationManager.activeNotifications.any { it.id == task.id }
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
        assertTrue(
            "Notification ${task.id} was not posted",
            waitUntil { notificationManager.activeNotifications.any { it.id == task.id } }
        )
    }

    private fun activeNotification(taskId: Int) =
        notificationManager.activeNotifications.first { it.id == taskId }

    private fun assertDisplayedTask(task: Task) {
        assertTrue(
            "Task detail activity did not display task ${task.id}",
            waitUntil {
                val activity = resumedTaskActivity() ?: return@waitUntil false
                var displayedTaskId = -1
                var displayedDescription = ""
                instrumentation.runOnMainSync {
                    displayedTaskId = activity.intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, -1)
                    displayedDescription = activity.findViewById<TextView>(R.id.textViewTaskDescription).text.toString()
                }
                displayedTaskId == task.id && displayedDescription == task.description
            }
        )
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

    private fun resumedEditActivity(): ActivityAddTask? {
        var resumedActivity: ActivityAddTask? = null
        instrumentation.runOnMainSync {
            resumedActivity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<ActivityAddTask>()
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
