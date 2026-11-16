package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateTimeUtils {

    private val shortDateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val monthDayFormatter = SimpleDateFormat("MMM d", Locale.US)

    /**
     * Computes a monthly membership cycle anchored to the actual payment date.
     * Example: Payment on Dec 4 -> cycle runs Dec 4 to Jan 4.
     */
    fun calculateCycleFromPaymentDate(year: Int, monthOneBased: Int, dayOfMonth: Int): Pair<String, String> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return shortDateFormatter.format(startCal.time) to shortDateFormatter.format(endCal.time)
    }

    fun formatCycleWindow(startYear: Int, startMonthOneBased: Int, startDay: Int): String {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, startYear)
            set(Calendar.MONTH, startMonthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, startDay)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return "${monthDayFormatter.format(startCal.time)} – ${shortDateFormatter.format(endCal.time)}"
    }

    fun greetingForHour(hourOfDay: Int): String = when (hourOfDay) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Late Night Grind"
    }
}
