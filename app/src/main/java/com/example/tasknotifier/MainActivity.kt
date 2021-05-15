package com.example.tasknotifier

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.listadapters.ListAdapter
import com.example.tasknotifier.viewmodels.TaskViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // RecyclerView
        val recyclerViewListAdapter = ListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAllTasks)
        recyclerView.adapter = recyclerViewListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ViewModel
        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
        taskViewModel.readAllData.observe(this, Observer { tasks ->
            recyclerViewListAdapter.setData(tasks)
        })


        // TODO testing in progress
        // testing

        for (i in 1..12) {
            val task = Task("first name", (2.5).toFloat(), 2, 2)
            taskViewModel.addTask(task)
        }


        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent: PendingIntent = Intent(this, SendNotificationBroadcastReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(this, 0, intent, 0)
        }

        // Set the alarm to start at 8:30 a.m.
        //        val calendar: Calendar = Calendar.getInstance().apply {
        //            timeInMillis = System.currentTimeMillis()
        //            set(Calendar.HOUR_OF_DAY, 8)
        //            set(Calendar.MINUTE, 30)
        //        }

        // setRepeating() lets you specify a precise custom interval--in this case,
        // 20 minutes.
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 10000,
            pendingIntent,
        )
        //        alarmManager.setRepeating(
        //            AlarmManager.RTC_WAKEUP,
        //            calendar.timeInMillis,
        //            1000 * 60 * 20,
        //            pendingIntent
        //        )

        // TODO testing in progress
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCliCkGoToAddUser(view: View) {
        startActivity(Intent(this, ActivityAddTask::class.java))
    }
}