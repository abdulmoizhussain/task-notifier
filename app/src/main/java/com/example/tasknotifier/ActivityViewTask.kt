package com.example.tasknotifier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyDateFormat
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class ActivityViewTask : AppCompatActivity() {
    private var taskDbId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_task)

        findViewById<Button>(R.id.buttonEditThisTask).setOnClickListener { onClickEditThisTask() }
        findViewById<Button>(R.id.buttonRemoveThisNotification).setOnClickListener { onClickRemoveThisNotification() }

        taskDbId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)

        if (taskDbId < 1) {
            return
        }

        val taskService = TaskService(this)
        runBlocking {
            launch {
                val task = taskService.getOneByIdAsync(taskDbId) ?: return@launch

                findViewById<TextView>(R.id.textViewTaskDescription).text = task.description

                val date = MyDateFormat.EEE_dd_MMM_yyyy.format(task.dateTime)
                findViewById<TextView>(R.id.textViewDate).text = getString(R.string.show_date_with_label, date)

                val time = MyDateFormat.HH_mm.format(task.dateTime)
                findViewById<TextView>(R.id.textViewTime).text = getString(R.string.show_time_with_label, time)

                val repeat = resources.getStringArray(R.array.repeat_values)[task.repeat]
                findViewById<TextView>(R.id.textViewRepeat).text = getString(R.string.show_repeat_with_label, repeat)

                val stopAfter = resources.getStringArray(R.array.stop_after_values)[task.stopAfter]
                findViewById<TextView>(R.id.textViewStopAfter).text = getString(R.string.show_stop_after_with_label, stopAfter)
            }
        }
    }

    private fun onClickRemoveThisNotification() {
        MyNotificationManager.cancelById(this, taskDbId)
    }

    private fun onClickEditThisTask() {
        val intent = Intent(this, ActivityAddTask::class.java)

        intent.putExtra(Constants.INTENT_EXTRA_TASK_ID, taskDbId)

        startActivity(intent)
    }
}