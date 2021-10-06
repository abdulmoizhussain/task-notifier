package com.example.tasknotifier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasknotifier.android_services.NotificationReviverAndroidService
import com.example.tasknotifier.common.Console
import com.example.tasknotifier.listadapters.ListAdapter
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.SPManager
import com.example.tasknotifier.viewmodels.TaskViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        let {
//            val intent = Intent()
//            intent.component =
//                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
//            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
//                startActivity(intent)
//            }
//        }

        SPManager(this).let { preferenceManager ->
            if (preferenceManager.isFirstLaunch()) {
                startService(Intent(this, NotificationReviverAndroidService::class.java))
                preferenceManager.markFirstLaunchAsCompleted()
            }
        }

        runBlocking {
            launch {
                val tasks = TaskService(this@MainActivity).fetchAllTheInProgressAsync()
                Console.log(tasks.size)
            }
        }

//        val receiver = ComponentName(applicationContext, ReScheduleTasks::class.java)
//        applicationContext.packageManager?.setComponentEnabledSetting(
//            receiver,
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP,
//        )

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
        // TODO: a scenario in which users will open the app after an update and then the tasks will not be scheduled.
        //  so in that case fix the bug which show that the tasks are not scheduled in the MainActivity list of tasks.
        // TODO: EVERYTHING IS OKAY, JUST SHOW THE NOTIFICATIONS FROM A BACKGROUND SERVICE.
        // TODO: Show Hint on long-press, specially on icons.
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