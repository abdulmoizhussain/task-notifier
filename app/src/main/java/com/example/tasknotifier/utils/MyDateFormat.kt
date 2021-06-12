package com.example.tasknotifier.utils

import java.text.SimpleDateFormat
import java.util.*

object MyDateFormat {
    val fullDateTime = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH)
    val HH_mm_ss = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    val HH_mm = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    val EEE_MMM_dd_yyyy = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.ENGLISH)
    val EEE_MMM_dd_yyyy_HH_mm_ss = SimpleDateFormat("EEE, MMM dd, yyyy  HH:mm:ss", Locale.ENGLISH)
}