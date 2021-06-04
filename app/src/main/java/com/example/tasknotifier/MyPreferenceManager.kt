package com.example.tasknotifier

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class MyPreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val keyTaskId = "KEY_TASK_ID"

    private fun getCurrentTaskId(): Int {
        return sharedPreferences.getInt(keyTaskId, 0)
    }

    fun getNextTaskId(): Int {
        val nextTaskId = this.getCurrentTaskId() + 1
        val sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()
        sharedPreferencesEditor.putInt(keyTaskId, nextTaskId)
        sharedPreferencesEditor.apply()
        return nextTaskId
    }
}