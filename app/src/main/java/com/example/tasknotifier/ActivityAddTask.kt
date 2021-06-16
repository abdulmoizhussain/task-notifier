package com.example.tasknotifier

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.common.TaskStatusEnum
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyAlarmManager
import com.example.tasknotifier.utils.MyDateFormat
import com.example.tasknotifier.utils.MyNotificationManager
import com.example.tasknotifier.viewmodels.TaskViewModel
import kotlinx.coroutines.*
import java.util.*

class ActivityAddTask : AppCompatActivity() {
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDayOfMonth: Int = 0
    private var selectedHourOfDay: Int = 0
    private var selectedMinute: Int = 0
    private var selectedRepeat: Int = 0
    private var selectedStopAfter: Int = 0
    private var taskDbId: Int = 0
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        findViewById<LinearLayout>(R.id.linearLayoutDate).setOnClickListener { onClickSelectDate() }
        findViewById<LinearLayout>(R.id.linearLayoutTime).setOnClickListener { onClickSelectTime() }
        findViewById<LinearLayout>(R.id.linearLayoutRepeat).setOnClickListener { onClickSelectRepeat() }
        findViewById<LinearLayout>(R.id.linearLayoutStopAfter).setOnClickListener { onClickSelectStopAfter() }
        findViewById<Button>(R.id.buttonNotifyNow).setOnClickListener { onClickNotifyNow() }

        val buttonTurnOnOrUpdateTask = findViewById<Button>(R.id.buttonTurnOnOrUpdateTask)
        val buttonDeleteTask = findViewById<Button>(R.id.buttonDeleteTask)
        val buttonTurnOffTask = findViewById<Button>(R.id.buttonTurnOffTask)

        buttonTurnOnOrUpdateTask.setOnClickListener { onClickAddOrUpdateTask() }
        buttonDeleteTask.setOnClickListener { onClickDeleteTask() }
        buttonTurnOffTask.setOnClickListener { onClickTurnOffTask() }

        run {
            // set Today's date
            val dateToday = MyDateFormat.EEE_MMM_dd_yyyy.format(System.currentTimeMillis())
            findViewById<TextView>(R.id.textViewDateToday).text = resources.getString(R.string.text_date_today, dateToday)
        }

        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        applySoftKeyboardVirtualKeyboardListener()

        taskDbId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)

        if (taskDbId > 0) {
            runBlocking {
                // GlobalScope.launch will prevent us to make UI changes
                launch {
                    val task = taskViewModel.getOneByIdAsync(taskDbId)

                    if (task == null) {
                        Toast.makeText(this@ActivityAddTask, "Task with id: $taskDbId not found.", Toast.LENGTH_LONG).show()
                        setOneHourLaterDateTime()
                    } else {
                        findViewById<EditText>(R.id.editTextDescription).setText(task.description)

                        val calendar = Calendar.getInstance().apply { timeInMillis = task.dateTime }

                        selectedRepeat = task.repeat
                        selectedStopAfter = task.stopAfter
                        selectedYear = calendar.get(Calendar.YEAR)
                        selectedMonth = calendar.get(Calendar.MONTH)
                        selectedDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                        selectedHourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                        selectedMinute = calendar.get(Calendar.MINUTE)

                        if (task.status == TaskStatusEnum.On) {
                            buttonTurnOffTask.isEnabled = true
                        }
                    }
                    restOfTheWorkOfOnCreate()

                    buttonDeleteTask.isEnabled = true
                    buttonTurnOnOrUpdateTask.text = resources.getString(R.string.label_button_update)
                }
            }
        } else {
            setOneHourLaterDateTime()
            restOfTheWorkOfOnCreate()
        }
    }

    private fun restOfTheWorkOfOnCreate() {
        setSelectedDate()
        setSelectedTime()
        setSelectedRepeat()
        setSelectedStopAfter()
    }

    private fun setOneHourLaterDateTime() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 1)

        selectedYear = calendar.get(Calendar.YEAR)
        selectedMonth = calendar.get(Calendar.MONTH)
        selectedDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        selectedHourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = calendar.get(Calendar.MINUTE)
    }

    private fun onClickAddOrUpdateTask() {
        val task: Task = let {
            val calendar: Calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
                set(Calendar.HOUR_OF_DAY, selectedHourOfDay)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // When date/time is in the past, don't proceed.
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setMessage("Oops!\nThe scheduled time is in the past. Please choose a time in the future.")
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "OK") { dialog, _ ->
                    dialog.dismiss()
                }
                alertDialog.show()

                return
            }

            val triggerAtMillis = calendar.timeInMillis
            // TODO incomplete
            val description = findViewById<EditText>(R.id.editTextDescription).text.toString()

            Task(description, triggerAtMillis, selectedRepeat, selectedStopAfter)
        }

        // Do not create a new task. Update and reschedule existing one.
        if (taskDbId > 0) {

            task.id = taskDbId
            task.sentCount = 0

            taskViewModel.updateOne(task)

            TaskService.createIntentAndSetExactAlarm(this, taskDbId, task.dateTime)

            finish()
            return
        }

        // Add and schedule new Task
        runBlocking {
            GlobalScope.launch {
                val taskIdInt = taskViewModel.addOneAsync(task).toInt()

                TaskService.createIntentAndSetExactAlarm(this@ActivityAddTask, taskIdInt, task.dateTime)

                finish()
            }
        }
    }

    private fun onClickSelectDate() {
        val datePickerDialog = DatePickerDialog(
            this,
//            { view, year, monthOfYear, dayOfMonth ->
            { _, year, monthOfYear, dayOfMonth ->
                selectedYear = year
                selectedMonth = monthOfYear
                selectedDayOfMonth = dayOfMonth

                setSelectedDate()
            },
            selectedYear,
            selectedMonth,
            selectedDayOfMonth,
        )

        // source: https://stackoverflow.com/a/33996958
        // How to disable past dates in Android date picker?
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

        datePickerDialog.show()
    }

    private fun onClickSelectTime() {
        val timePickerDialog = TimePickerDialog(
            this,
//            { view, hourOfDay, minute ->
            { _, hourOfDay, minute ->
                selectedHourOfDay = hourOfDay
                selectedMinute = minute

                setSelectedTime()
            },
            selectedHourOfDay,
            selectedMinute,
            true,
        )
        timePickerDialog.show()
    }

    private fun onClickSelectRepeat() {
        // source: https://www.youtube.com/watch?v=Em7LJddHAbQ
        val listView = ListView(this)
        val builder = AlertDialog.Builder(this)
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_selectable_list_item,
            getRepeatValues(),
        )

        listView.adapter = arrayAdapter

        builder.setCancelable(true)
        builder.setView(listView)
        builder.setTitle(resources.getString(R.string.label_select_repeat_duration))

        val alertDialog = builder.create()

//        listView.setOnItemClickListener { parent, view, position, id ->
        listView.setOnItemClickListener { _, _, position, _ ->
            selectedRepeat = position
            setSelectedRepeat()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun onClickSelectStopAfter() {
        val listView = ListView(this)
        val builder = AlertDialog.Builder(this)
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_selectable_list_item,
            getStopAfterValues(),
        )

        listView.adapter = arrayAdapter

        builder.setCancelable(true)
        builder.setView(listView)
        builder.setTitle(resources.getString(R.string.label_stop_after))

        val alertDialog = builder.create()

//        listView.setOnItemClickListener { parent, view, position, id ->
        listView.setOnItemClickListener { _, _, position, _ ->
            selectedStopAfter = position
            setSelectedStopAfter()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun enableStopAfterControl(enable: Boolean = true) {
        findViewById<TextView>(R.id.textView8).isEnabled = enable
        findViewById<TextView>(R.id.textViewStopAfter).isEnabled = enable
        findViewById<View>(R.id.linearLayoutStopAfter).isClickable = enable
    }

    private fun setSelectedDate() {
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)

        findViewById<TextView>(R.id.textViewDate).text = MyDateFormat.EEE_MMM_dd_yyyy.format(calendar.time)
    }

    private fun setSelectedTime() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, selectedHourOfDay)
        calendar.set(Calendar.MINUTE, selectedMinute)

        findViewById<TextView>(R.id.textViewTime).text = MyDateFormat.HH_mm.format(calendar.time)
    }

    private fun setSelectedRepeat() {
        findViewById<TextView>(R.id.textViewRepeat).text = getRepeatValues()[selectedRepeat]
        enableStopAfterControl(selectedRepeat != 0)
    }

    private fun setSelectedStopAfter() {
        findViewById<TextView>(R.id.textViewStopAfter).text =
            getStopAfterValues()[selectedStopAfter]
    }

    private fun getRepeatValues(): Array<String> {
        return resources.getStringArray(R.array.repeat_values)
    }

    private fun getStopAfterValues(): Array<String> {
        return resources.getStringArray(R.array.stop_after_values)
    }

    private fun applySoftKeyboardVirtualKeyboardListener() {
        // source:
        // https://stackoverflow.com/a/25681196
        // https://www.tutorialspoint.com/how-to-write-a-softkeyboard-open-and-close-listener-in-an-activity-in-android
        val rootLayout = findViewById<View>(R.id.rootLayout)
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootLayout.getWindowVisibleDisplayFrame(rect)
            val screenHeight: Int = rootLayout.rootView.height
            val keypadHeight: Int = screenHeight - rect.bottom

            findViewById<LinearLayout>(R.id.linearLayoutBottomBar).visibility =
                if (keypadHeight > screenHeight * 0.15) View.GONE else View.VISIBLE
        }
    }

    private fun onClickDeleteTask() {
        MyAlarmManager.cancelByRequestCode(this, taskDbId)
        taskViewModel.deleteOneById(taskDbId)
        finish()
    }

    private fun onClickTurnOffTask() {
        runBlocking {
            launch {
                val task = taskViewModel.getOneByIdAsync(taskDbId)

                if (task == null) {
                    Toast.makeText(this@ActivityAddTask, "Task with id: $taskDbId not found.", Toast.LENGTH_LONG).show()
                } else {
                    task.status = TaskStatusEnum.Off

                    taskViewModel.updateOne(task)

                    MyAlarmManager.cancelByRequestCode(this@ActivityAddTask, taskDbId)

                    finish()
                }
            }
        }
    }

    private fun onClickNotifyNow() {
        // TODO incomplete
        val editTextDescriptionText = findViewById<EditText>(R.id.editTextDescription).text
        if (editTextDescriptionText.isNullOrBlank()) {
            Toast.makeText(this, "Task description is empty.", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO maybe temporary :P
        val currentTimeMillis = System.currentTimeMillis()
        val hourNow = MyDateFormat.HH_mm_ss.format(currentTimeMillis)
        val contentTitle = "(1) $hourNow"

        MyNotificationManager.notify(
            this.applicationContext,
            taskDbId,
            contentTitle,
            editTextDescriptionText.toString(),
            currentTimeMillis,
            true
        )
    }
}