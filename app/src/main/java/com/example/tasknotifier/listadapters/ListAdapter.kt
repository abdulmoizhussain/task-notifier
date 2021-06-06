package com.example.tasknotifier.listadapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.tasknotifier.ActivityAddTask
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.MyAlarmManager
import com.example.tasknotifier.R
import com.example.tasknotifier.data.task.Task
import kotlinx.android.synthetic.main.row_recyclerview_all_tasks.view.*
import java.text.SimpleDateFormat
import java.util.*

class ListAdapter : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {

    private var taskList = emptyList<Task>()

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
//    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_recyclerview_all_tasks, parent, false))
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentTaskItem: Task = taskList[position]
        val taskId = currentTaskItem.id
        val itemView: View = holder.itemView
        itemView.textViewTaskDescription.text = currentTaskItem.description

        // TODO:
        // remove :ss when all the dev and testing is complete
        // recheck all the date formats whether they are valid or not.
        itemView.textViewDateTime.text = SimpleDateFormat("EEE, dd/MMM/yyyy  HH:mm:ss", Locale.getDefault())
            .format(currentTaskItem.dateTime)

        itemView.textViewStatus.text = currentTaskItem.status.toString()

        itemView.setOnClickListener { onClickItemView ->
            val context = onClickItemView.context
            val intent = Intent(context, ActivityAddTask::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_TASK_ID, taskId)
            context.startActivity(intent)
        }

        // source: https://stackoverflow.com/a/49712696
        itemView.setOnLongClickListener { onClickItemView ->
            val context = onClickItemView.context
            MyAlarmManager.cancelByRequestCode(context, taskId)
            Toast.makeText(context, "Cancelled the alarm with request code: $taskId", Toast.LENGTH_SHORT).show()
            true
        }
    }

    fun setData(taskList: List<Task>) {
        this.taskList = taskList
        notifyDataSetChanged()
    }
}