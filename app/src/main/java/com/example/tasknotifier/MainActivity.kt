package com.example.tasknotifier

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasknotifier.data.ListAdapter
import com.example.tasknotifier.data.UserViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // RecyclerView
        val recyclerViewListAdapter = ListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAllTasks)
        recyclerView.adapter = recyclerViewListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ViewModel
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel.readAllData.observe(this, Observer { tasks ->
            recyclerViewListAdapter.setData(tasks)
        })

    }

    @Suppress("UNUSED_PARAMETER")
    fun onCliCkGoToAddUser(view: View) {
        startActivity(Intent(this, ActivityAddTask::class.java))
    }
}