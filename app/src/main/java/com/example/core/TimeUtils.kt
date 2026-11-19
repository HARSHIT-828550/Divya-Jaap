package com.example.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private const val DATE_PATTERN = "yyyy-MM-dd"
    private const val DISPLAY_DATE_PATTERN = "dd MMM"

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)
        return formatter.format(Date(nowMillis))
    }

    fun dateKeyOffset(daysAgo: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return SimpleDateFormat(DATE_PATTERN, Locale.US).format(cal.time)
    }

    fun shortLabelForDateKey(dateKey: String): String {
        return try {
            val parsed = SimpleDateFormat(DATE_PATTERN, Locale.US).parse(dateKey)
            if (parsed != null) {
                SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US).format(parsed)
            } else {
                dateKey
            }
        } catch (_: Exception) {
            dateKey
        }
    }

    fun calculateStreak(activeDateKeys: Set<String>, nowMillis: Long = System.currentTimeMillis()): Int {
        if (activeDateKeys.isEmpty()) return 0
        var streak = 0
        var offset = 0
        // If today has no count yet, check starting from yesterday so active daily streak isn't prematurely zeroed mid-day
        if (!activeDateKeys.contains(dateKeyOffset(0, nowMillis))) {
            offset = 1
        }
        while (activeDateKeys.contains(dateKeyOffset(offset, nowMillis))) {
            streak++
            offset++
        }
        return streak
    }

    fun formatDuration(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0L)
        val hours = safeSeconds / 3600
        val minutes = (safeSeconds % 3600) / 60
        val secs = safeSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%dh %02dm", hours, minutes)
        } else if (minutes > 0) {
            String.format(Locale.US, "%dm %02ds", minutes, secs)
        } else {
            String.format(Locale.US, "%ds", secs)
        }
    }
}
