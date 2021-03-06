package com.example.tasknotifier

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasknotifier.android_services.TaskNotifierAndroidService
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.listadapters.ListAdapter
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.viewmodels.TaskViewModel
import org.json.JSONArray


class MainActivity : AppCompatActivity() {
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO Create 2 identical pending intents and check if they are equivalent.

        // TODO Debugging stuff
        /*
        val context = this
        val requestCode = 88
        val triggerAtMillis = Date().time + (1000 * 10)

        val mIntent = Intent(context, SendNotificationBroadcastReceiver::class.java)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(context, requestCode, mIntent, flags)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            else -> {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }

        val pendingIntentC3 = PendingIntent.getBroadcast(context, requestCode, mIntent, flags)
        val alarmManagerNew = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        Log.a("before alarm manager cancellation")
        alarmManagerNew.cancel(pendingIntentC3)
        Log.a("before pending intent cancellation")
        pendingIntentC3.cancel()
        */

//        let {
//            val intent = Intent()
//            intent.component =
//                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
//            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
//                startActivity(intent)
//            }
//        }

//        SPManager(this).let { preferenceManager ->
//            if (preferenceManager.isFirstLaunch()) {
//                Intent(this, TaskNotifierAndroidService::class.java).let { mIntent ->
//                    mIntent.putExtra(Constants.INTENT_EXTRA_NOTIFICATION_REVIVER_SERVICE, true)
//                    startService(mIntent)
//                }
//                preferenceManager.markFirstLaunchAsCompleted()
//            }
//        }

//        val receiver = ComponentName(applicationContext, ReScheduleTasks::class.java)
//        applicationContext.packageManager?.setComponentEnabledSetting(
//            receiver,
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP,
//        )

        // TODO: try again: setExactAndAllowWhileIdle.
        // TODO: export/import / Backup/Restore feature.
        // TODO: add app-settings feature for: 12/24 hour time format, dark/light theme, select notification sound, vibration on/off, sticky notification option at add-task.
        // TODO: FORCEFUL LIGHT THEME FOR NOW.
        // TODO: ask people that should there be title or not?
        // TODO: create notification view to also show notification count for a task, we might then skip title..
        // TODO: Start all the tasks [whose date/time are due (in future)].
        // TODO: Delete all the tasks.
        // TODO: Turn Off all the tasks.
        // TODO: Order By / Sort By: Date, On, Off, Expired.
        // TODO: Filter By On/Off/
        // TODO: use dismiss-able snack-bar (like that of LinkedIn) for toast like messages.
        // TODO: Hours, Days, Weeks algorithm like that of facebook.
        // TODO: try giving the option to delete the alarms by long press.
        // TODO: a scenario in which users will open the app after an update and then the tasks will not be scheduled.
        //  so in that case fix the bug which show that the tasks are not scheduled in the MainActivity list of tasks.
        // TODO: Show Hint on long-press, specially on icons.
        // TODO: trim description text while creating notification.
        // TODO: give checkbox with task delete dialog: "also remove notification"

        // RecyclerView
        val recyclerViewListAdapter = ListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAllTasks)
        recyclerView.adapter = recyclerViewListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ViewModel
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        taskViewModel.readAllData.observe(this) { tasks -> recyclerViewListAdapter.setData(tasks) }

        findViewById<Button>(R.id.buttonAddNewTask).setOnClickListener { onCliCkGoToAddUser() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.option_restart_service -> {
                onClickRestartService()
                true
            }
            R.id.option_export_data -> {
                Toast.makeText(this, "Not implemented yet!", Toast.LENGTH_SHORT).show()
                false
            }
            R.id.option_import_data -> {
                Toast.makeText(this, "Not implemented yet!", Toast.LENGTH_SHORT).show()
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun exportToFolder() {
//        val root: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val root = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

        // TODO: testing code to be removed when used properly:
//        var jsonString: String? = null
//        runBlocking {
//            launch {
//                jsonString = exportTasksAsJsonArrayAsync()
//            }
//        }
//        val fileOutputStream = openFileOutput("output.json", Context.MODE_WORLD_WRITEABLE)
//        fileOutputStream.write(jsonString?.toByteArray())
    }

    private suspend fun exportTasksAsJsonArrayAsync(): String {
        val tasks = TaskService(this).getAllAsync()
        val jsonArray = JSONArray()
        tasks.forEach { task -> jsonArray.put(task.toJsonObject()) }
        return jsonArray.toString()
    }

    private fun onCliCkGoToAddUser() {
        startActivity(Intent(this, ActivityAddTask::class.java))
    }

    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun onClickRestartService() {
        val mIntent = Intent(this, TaskNotifierAndroidService::class.java)
        mIntent.putExtra(Constants.INTENT_EXTRA_NOTIFICATION_REVIVER_SERVICE, true)
        mIntent.putExtra(Constants.INTENT_EXTRA_TASK_SCHEDULER_SERVICE, true)

        stopService(mIntent)
        startService(mIntent)
    }

    private fun onClickSettingsButton() {
        // source: https://stackoverflow.com/a/22655641/8075004
        val dialogViewSettings = this.layoutInflater.inflate(R.layout.settings_popup_alert_dialog_layout, null)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogViewSettings)

        val alertDialog = alertDialogBuilder.create()

        dialogViewSettings.findViewById<TextView>(R.id.textViewRestartService).setOnClickListener {
            onClickRestartService()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}