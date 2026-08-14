package io.github.abdulmoizhussain.tasknotifier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.services.TaskService
import io.github.abdulmoizhussain.tasknotifier.utils.MyDateFormat
import io.github.abdulmoizhussain.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ActivityViewTask : AppCompatActivity() {
    private var taskDbId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_task)
        title = getString(R.string.title_reminder_details)

        findViewById<Button>(R.id.buttonEditThisTask).setOnClickListener { onClickEditThisTask() }
        findViewById<Button>(R.id.buttonRemoveThisNotification).setOnClickListener { onClickRemoveThisNotification() }

        displayTaskFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        displayTaskFromIntent(intent)
    }

    private fun displayTaskFromIntent(sourceIntent: Intent) {
        taskDbId = sourceIntent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)

        if (taskDbId < 1) {
            displayMissingTask()
            return
        }

        findViewById<Button>(R.id.buttonEditThisTask).isEnabled = true
        findViewById<Button>(R.id.buttonRemoveThisNotification).isEnabled = true

        val taskService = TaskService(this)
        runBlocking {
            launch {
                val task = taskService.getOneByIdAsync(taskDbId)
                val textViewDate = findViewById<TextView>(R.id.textViewDate)

                if (task == null) {
                    displayMissingTask()
                    return@launch
                }

                findViewById<TextView>(R.id.textViewTaskDescription).text = task.description

                val date = MyDateFormat.EEE_MMM_dd_yyyy.format(task.dateTime)
                textViewDate.text = getString(R.string.show_date_with_label, date)

                val time = MyDateFormat.HH_mm.format(task.dateTime)
                findViewById<TextView>(R.id.textViewTime).text = getString(R.string.show_time_with_label, time)

                val repeat = resources.getStringArray(R.array.repeat_values)[task.repeat]
                findViewById<TextView>(R.id.textViewRepeat).text = getString(R.string.show_repeat_with_label, repeat)

                val stopAfter = resources.getStringArray(R.array.stop_after_values)[task.stopAfter]
                findViewById<TextView>(R.id.textViewStopAfter).text = getString(R.string.show_stop_after_with_label, stopAfter)
            }
        }
    }

    private fun displayMissingTask() {
        findViewById<TextView>(R.id.textViewTaskDescription).text = ""
        findViewById<TextView>(R.id.textViewDate).text = getString(R.string.msg_task_deleted)
        findViewById<TextView>(R.id.textViewTime).text = ""
        findViewById<TextView>(R.id.textViewRepeat).text = ""
        findViewById<TextView>(R.id.textViewStopAfter).text = ""
        findViewById<Button>(R.id.buttonEditThisTask).isEnabled = false
        findViewById<Button>(R.id.buttonRemoveThisNotification).isEnabled = false
    }

    private fun onClickRemoveThisNotification() {
        DiagnosticLog.record(
            this,
            "REMOVE_NOTIFICATION_CLICKED",
            taskDbId,
            mapOf("inProgressTarget" to false),
        )
        TaskService(this).turnOffInProgressByTaskId(taskDbId)
        MyNotificationManager.cancelById(this, taskDbId)
    }

    private fun onClickEditThisTask() {
        val intent = Intent(this, ActivityAddTask::class.java)

        intent.putExtra(Constants.INTENT_EXTRA_TASK_ID, taskDbId)

        startActivity(intent)
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
