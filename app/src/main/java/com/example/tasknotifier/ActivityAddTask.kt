package com.example.tasknotifier

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.viewmodels.TaskViewModel
import kotlinx.android.synthetic.main.activity_add_task.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ActivityAddTask : AppCompatActivity() {
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDayOfMonth: Int = 0
    private var selectedHourOfDay: Int = 0
    private var selectedMinute: Int = 0
    private var selectedRepeat: Int = 0
    private var selectedStopAfter: Int = 0
    private var checkboxSetExact: Boolean = false
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        applySoftKeyboardVirtualKeyboardListener()

        run {
            // set Today's date
            val dateToday = SimpleDateFormat(
                "EEE, dd MMM, yyyy",
                Locale.getDefault(),
            ).format(Date())
            val text = "Today is $dateToday"
            findViewById<TextView>(R.id.textViewDateToday).text = text
        }

        run {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, 1)

            selectedYear = calendar.get(Calendar.YEAR)
            selectedMonth = calendar.get(Calendar.MONTH)
            selectedDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            selectedHourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            selectedMinute = calendar.get(Calendar.MINUTE)
        }

        setSelectedDate()
        setSelectedTime()
        setSelectedRepeat()
        setSelectedStopAfter()

        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onAddTask(view: View) {

        val calendar: Calendar = Calendar.getInstance().apply {
//            timeInMillis = System.currentTimeMillis()

            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
            set(Calendar.HOUR_OF_DAY, selectedHourOfDay)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // TODO testing in progress

        val triggerAtMillis = calendar.timeInMillis
        val description = editTextDescription.text.toString()
        val taskIdInt = MyPreferenceManager(this).getNextTaskId()
        taskViewModel.addTask(Task(taskIdInt, description, triggerAtMillis, selectedRepeat, selectedStopAfter))

        val intent = Intent(applicationContext, SendNotificationBroadcastReceiver::class.java)
        intent.putExtra(Constants.INTENT_EXTRA_TASK_ID, taskIdInt)
        intent.putExtra(Constants.INTENT_EXTRA_TASK_DESCRIPTION, description)
        intent.putExtra(Constants.INTENT_EXTRA_SET_WHEN, triggerAtMillis)

        if (checkboxSetExact) {
            MyAlarmManager.setExact(this, taskIdInt, intent, triggerAtMillis)
        } else {
            MyAlarmManager.setInexact(this, taskIdInt, intent, triggerAtMillis)
        }

        finish()
        // TODO testing in progress
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectDate(view: View) {
        var calendar: Calendar

        try {
            calendar = Calendar.getInstance().apply {

                val date: Date? = SimpleDateFormat("EEE, dd MMM, yyyy", Locale.getDefault())
                    .parse(view.findViewById<TextView>(R.id.textViewDate).text.toString())

                if (date == null) {
                    throw ParseException("ParseException", 0)
                }
                timeInMillis = date.time
            }
        } catch (_: ParseException) {
            calendar = Calendar.getInstance()
        }


        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
//            { view, year, monthOfYear, dayOfMonth ->
            { _, year, monthOfYear, dayOfMonth ->
                selectedYear = year
                selectedMonth = monthOfYear
                selectedDayOfMonth = dayOfMonth

                setSelectedDate()
            },
            currentYear,
            currentMonth,
            currentDay,
        )

        // source: https://stackoverflow.com/a/33996958
        // How to disable past dates in Android date picker?
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

        datePickerDialog.show()
    }

    fun onClickSelectTime(view: View) {
        var calendar: Calendar

        try {
            calendar = Calendar.getInstance().apply {
                val date: Date? = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .parse(view.findViewById<TextView>(R.id.textViewTime).text.toString())

                if (date == null) {
                    throw ParseException("ParseException", 0)
                }
                timeInMillis = date.time
            }
        } catch (_: ParseException) {
            calendar = Calendar.getInstance()
        }

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
//            { view, hourOfDay, minute ->
            { _, hourOfDay, minute ->
                selectedHourOfDay = hourOfDay
                selectedMinute = minute

                setSelectedTime()
            },
            currentHour,
            currentMinute,
            true,
        )
        timePickerDialog.show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectRepeat(view: View) {
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
        builder.setTitle("Select Repeat Duration")

        val alertDialog = builder.create()

//        listView.setOnItemClickListener { parent, view, position, id ->
        listView.setOnItemClickListener { _, _, position, _ ->
            selectedRepeat = position
            setSelectedRepeat()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectStopAfter(view: View) {
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
        builder.setTitle("Stop After")

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
        findViewById<View>(R.id.linearLayout4).isClickable = enable
    }

    private fun setSelectedDate() {
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)

        findViewById<TextView>(R.id.textViewDate).text = SimpleDateFormat(
            "EEE, dd MMM, yyyy",
            Locale.getDefault(),
        ).format(calendar.time)
    }

    private fun setSelectedTime() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, selectedHourOfDay)
        calendar.set(Calendar.MINUTE, selectedMinute)

        findViewById<TextView>(R.id.textViewTime).text = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault(),
        ).format(calendar.time)
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

    fun onClickCheckboxSetExact(view: View) {
        checkboxSetExact = (view as CheckBox).isChecked
    }
}