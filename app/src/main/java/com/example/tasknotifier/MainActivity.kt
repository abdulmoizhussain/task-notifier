package com.example.tasknotifier

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
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
//        for (i in 1..12) {
//            val task = Task("first name", (2.5).toFloat(), 2, 2)
//            taskViewModel.addTask(task)
//        }
        // TODO testing in progress
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCliCkGoToAddUser(view: View) {
        startActivity(Intent(this, ActivityAddTask::class.java))
    }
}