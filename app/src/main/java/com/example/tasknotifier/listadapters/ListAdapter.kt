package com.example.tasknotifier.listadapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.tasknotifier.R
import com.example.tasknotifier.data.task.Task
import kotlinx.android.synthetic.main.row_recyclerview_all_tasks.view.*

class ListAdapter : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {

    private var taskList = emptyList<Task>()

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_recyclerview_all_tasks, parent, false))
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem: Task = taskList[position]
        val itemView: View = holder.itemView
        itemView.textViewDbId.text = currentItem.id.toString()
        itemView.textViewTaskDescription.text = currentItem.description

        val date = currentItem.dateTime
        itemView.textViewDateTime.text = date.toString()

        // source: https://stackoverflow.com/a/49712696
        itemView.setOnLongClickListener { onClickItemView ->
            Toast.makeText(onClickItemView.context, "asdf: long click listener", Toast.LENGTH_LONG).show()
            true
        }
    }

    fun setData(taskList: List<Task>) {
        this.taskList = taskList
        notifyDataSetChanged()
    }
}