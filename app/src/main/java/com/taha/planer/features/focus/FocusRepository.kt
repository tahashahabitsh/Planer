package com.taha.planer.features.focus

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FocusSession(
    val id: Long,
    val title: String,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val date: String, // yyyy-MM-dd
    val successful: Boolean
)

private const val PREF_FOCUS = "planner_focus"
private const val KEY_FOCUS = "focus_sessions_v1"

private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

fun todayDate(): String = DATE_FORMATTER.format(Date())

fun loadFocusSessions(context: Context): List<FocusSession> {
    val prefs = context.getSharedPreferences(PREF_FOCUS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_FOCUS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 6) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val planned = parts[2].toIntOrNull() ?: 0
            val actual = parts[3].toIntOrNull() ?: 0
            val date = parts[4]
            val successful = parts[5] == "1"
            FocusSession(
                id = id,
                title = title,
                plannedMinutes = planned,
                actualMinutes = actual,
                date = date,
                successful = successful
            )
        }
}

fun saveFocusSessions(context: Context, sessions: List<FocusSession>) {
    val prefs = context.getSharedPreferences(PREF_FOCUS, Context.MODE_PRIVATE)
    val raw = sessions.joinToString("\n") { s ->
        val safeTitle = s.title.replace("\n", " ")
        "${s.id}||$safeTitle||${s.plannedMinutes}||${s.actualMinutes}||${s.date}||${if (s.successful) "1" else "0"}"
    }
    prefs.edit().putString(KEY_FOCUS, raw).apply()
}

/**
 * مجموع دقایق تمرکز هر روز در ۷ روز اخیر را برمی‌گرداند.
 * خروجی: لیست جفت (label فارسی کوتاه، مقدار دقیقه)
 */
fun last7DaysFocusSummary(sessions: List<FocusSession>): List<Pair<String, Int>> {
    val map = mutableMapOf<String, Int>()

    // ۷ روز اخیر
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        val key = DATE_FORMATTER.format(cal.time)
        map[key] = 0
    }

    sessions.forEach { s ->
        if (map.containsKey(s.date)) {
            val old = map[s.date] ?: 0
            map[s.date] = old + s.actualMinutes
        }
    }

    val days = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    return map
        .toSortedMap()
        .entries
        .mapIndexed { index, e ->
            val label = days[index % days.size]
            label to e.value
        }
}
