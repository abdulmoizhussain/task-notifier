package com.example.tasknotifier.utils

import java.text.SimpleDateFormat
import java.util.*

object MyDateFormat {
    val fullDateTime = SimpleDateFormat("dd/MMM/yyyy HH:mm:ss", Locale.ENGLISH)
    val HH_mm_ss = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    val HH_mm = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    val EEE_dd_MMM_yyyy = SimpleDateFormat("EEE, dd MMM, yyyy", Locale.ENGLISH)
    val EEE_dd_MMM_yyyy_HH_mm_ss = SimpleDateFormat("EEE, dd/MMM/yyyy  HH:mm:ss", Locale.ENGLISH)
}