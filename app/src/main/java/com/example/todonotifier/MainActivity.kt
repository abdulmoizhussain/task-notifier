package com.example.todonotifier

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDate: Int = 0
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0
    private var selectedRepeat: Int = 0
    private var selectedStopAfter: Int = 0
    private lateinit var repeatValues: Array<String>
    private lateinit var stopAfterValues: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repeatValues = resources.getStringArray(R.array.repeat_values)
        stopAfterValues = resources.getStringArray(R.array.stop_after_values)

        setSelectedDate()
        setSelectedTime()
        setSelectedRepeat()
        setSelectedStopAfter()
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
            currentDay
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
            true
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
            repeatValues,
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
            stopAfterValues,
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
        val calendarSelected = Calendar.getInstance()
        calendarSelected.set(selectedYear, selectedMonth, selectedDate)

        val textViewDate = findViewById<TextView>(R.id.textViewDate)
        textViewDate.text = SimpleDateFormat(
            "EEE, dd MMM, yyyy",
            Locale.getDefault(),
        ).format(calendarSelected.time)
    }

    private fun setSelectedTime() {
        val calendarSelected = Calendar.getInstance()
        calendarSelected.set(Calendar.HOUR_OF_DAY, selectedHour)
        calendarSelected.set(Calendar.MINUTE, selectedMinute)

        val textViewTime = findViewById<TextView>(R.id.textViewTime)
        textViewTime.text = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault(),
        ).format(calendarSelected.time)
    }

    private fun setSelectedRepeat() {
        val textViewRepeat = findViewById<TextView>(R.id.textViewRepeat)
        textViewRepeat.text = repeatValues[selectedRepeat]
        enableStopAfterControl(selectedRepeat != 0)
    }

    private fun setSelectedStopAfter() {
        val textViewStopAfter = findViewById<TextView>(R.id.textViewStopAfter)
        textViewStopAfter.text = stopAfterValues[selectedStopAfter]
    }
}
