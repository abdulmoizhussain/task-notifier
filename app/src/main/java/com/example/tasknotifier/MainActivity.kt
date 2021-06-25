package com.example.tasknotifier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasknotifier.listadapters.ListAdapter
import com.example.tasknotifier.viewmodels.TaskViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: export/import / Backup/Restore feature.
        // TODO: add app-settings feature for: 12/24 hour time format, dark/light theme, select notification sound, vibration on/off, sticky notification option at add-task.
        // TODO: FORCEFUL LIGHT THEME FOR NOW.
        // TODO: ask people that should there be title or not?
        // TODO: create notification view to also show notification count for a task, we might then skip title..
        // TODO: navigation back from task view to MainActivity.
        // TODO: Start all the tasks [whose date/time are due (in future)].
        // TODO: Delete all the tasks.
        // TODO: Turn Off all the tasks.
        // TODO: Order By / Sort By: Date, On, Off, Expired.
        // TODO: Filter By On/Off/
        // TODO: use dismiss-able snack-bar (like that of LinkedIn) for toast like messages.
        // TODO: Hours, Days, Weeks algorithm like that of facebook.
        // TODO: Give Yes/No confirmation before deleting an alarm.
        // TODO: try giving the option to delete the alarms by long press.
        // TODO: a scenario in which users will open the app after an update and then the tasks will not be scheduled. so in that case fix the bug which show that the tasks are not scheduled in the mainactivity list of tasks.
        // TODO:

        // RecyclerView
        val recyclerViewListAdapter = ListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAllTasks)
        recyclerView.adapter = recyclerViewListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ViewModel
        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
        taskViewModel.readAllData.observe(this, { tasks -> recyclerViewListAdapter.setData(tasks) })

        findViewById<Button>(R.id.buttonAddNewTask).setOnClickListener { onCliCkGoToAddUser() }
    }

    private fun onCliCkGoToAddUser() {
        startActivity(Intent(this, ActivityAddTask::class.java))
    }
}