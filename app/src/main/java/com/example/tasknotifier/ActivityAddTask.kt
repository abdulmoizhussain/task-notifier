package com.example.tasknotifier

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.tasknotifier.data.Task
import com.example.tasknotifier.data.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

class ActivityAddTask : AppCompatActivity() {
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDate: Int = 0
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0
    private var selectedRepeat: Int = 0
    private var selectedStopAfter: Int = 0
    private lateinit var userViewModel: UserViewModel

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
            selectedDate = calendar.get(Calendar.DAY_OF_MONTH)
            selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
            selectedMinute = calendar.get(Calendar.MINUTE)
        }

        setSelectedDate()
        setSelectedTime()
        setSelectedRepeat()
        setSelectedStopAfter()

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onAddUser(view: View) {
        val task = Task(0, "first name", "last name", 2)
        userViewModel.addUser(task)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectDate(view: View) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
//            { view, year, monthOfYear, dayOfMonth ->
            { _, year, monthOfYear, dayOfMonth ->
                selectedYear = year
                selectedMonth = monthOfYear
                selectedDate = dayOfMonth

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

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectTime(view: View) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
//            { view, hourOfDay, minute ->
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
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
        calendar.set(selectedYear, selectedMonth, selectedDate)

        findViewById<TextView>(R.id.textViewDate).text = SimpleDateFormat(
            "EEE, dd MMM, yyyy",
            Locale.getDefault(),
        ).format(calendar.time)
    }

    private fun setSelectedTime() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
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
}