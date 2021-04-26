package com.example.todonotifier

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        println("")
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectDate(view: View) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
//            { view, year, monthOfYear, dayOfMonth ->
            { _, year, monthOfYear, dayOfMonth ->
                // Display Selected date in textbox
//                lblDate.setText("" + dayOfMonth + " " + ", " + year)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectTime(view: View) {
        println("aaaaaa just clicked")

        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
//            { view, hourOfDay, minute ->
            { _, hourOfDay, minute ->

            }, hour, minute, true
        )

        timePickerDialog.show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectRepeat(view: View) {
        // source: https://www.youtube.com/watch?v=Em7LJddHAbQ

        val data = resources.getStringArray(R.array.repeat_values)
        val listView = ListView(this)
        val builder = AlertDialog.Builder(this)
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_selectable_list_item,
            data,
        )

        listView.adapter = arrayAdapter

        builder.setCancelable(true)
        builder.setView(listView)
        builder.setTitle("Select Repeat Duration")

        val alertDialog = builder.create()


//        listView.setOnItemClickListener { parent, view, position, id ->
        listView.setOnItemClickListener { _, _, position, _ ->
            Toast.makeText(this, position.toString(), Toast.LENGTH_LONG).show()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSelectStopAfter(view: View) {
        val data = resources.getStringArray(R.array.stop_after_values)
        val listView = ListView(this)
        val builder = AlertDialog.Builder(this)
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_selectable_list_item,
            data,
        )

        listView.adapter = arrayAdapter

        builder.setCancelable(true)
        builder.setView(listView)
        builder.setTitle("Stop After")

        val alertDialog = builder.create()


//        listView.setOnItemClickListener { parent, view, position, id ->
        listView.setOnItemClickListener { _, _, position, _ ->
            Toast.makeText(this, position.toString(), Toast.LENGTH_LONG).show()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
