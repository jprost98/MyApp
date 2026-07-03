package com.example.myapp.utils

import java.text.SimpleDateFormat
import java.util.*

object TaskUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun calculateNextDueDate(lastDoneDate: String?, frequency: String?): String? {
        if (lastDoneDate.isNullOrBlank() || frequency.isNullOrBlank()) return null
        
        val date = try {
            isoFormat.parse(lastDoneDate)
        } catch (e: Exception) {
            null
        } ?: return null

        val calendar = Calendar.getInstance()
        calendar.time = date

        val parts = frequency.split(" ")
        if (parts.size < 2) return null

        val amount = parts[0].toIntOrNull() ?: return null
        val unit = parts[1].lowercase()

        when {
            unit.contains("day") -> calendar.add(Calendar.DAY_OF_YEAR, amount)
            unit.contains("week") -> calendar.add(Calendar.WEEK_OF_YEAR, amount)
            unit.contains("month") -> calendar.add(Calendar.MONTH, amount)
            unit.contains("year") -> calendar.add(Calendar.YEAR, amount)
            else -> return null
        }

        return isoFormat.format(calendar.time)
    }
}
