package com.example.tasknotifier.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.tasknotifier.common.Constants

// SharedPreferencesManager
class SPManager(context: Context) {
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getCurrentTaskId(): Int {
        return sharedPreferences.getInt(Constants.KEY_TASK_ID, 0)
    }

    fun getNextTaskId(): Int {
        val nextTaskId = this.getCurrentTaskId() + 1
        val sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()
        sharedPreferencesEditor.putInt(Constants.KEY_TASK_ID, nextTaskId)
        sharedPreferencesEditor.apply()
        return nextTaskId
    }

    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_FIRST_TIME_LAUNCH, true)
    }

    fun markFirstLaunchAsCompleted() {
        sharedPreferences.edit().putBoolean(Constants.KEY_FIRST_TIME_LAUNCH, false).apply()
    }
}