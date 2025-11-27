package com.taha.planer.features.habits

import android.content.Context

data class PlannerHabit(
    val id: Long,
    val name: String,
    val description: String,
    val targetPerDay: Int,
    val enabled: Boolean
)

private const val PREF_HABITS = "planner_habits"
private const val KEY_HABITS = "habits_v1"

fun loadHabits(context: Context): List<PlannerHabit> {
    val prefs = context.getSharedPreferences(PREF_HABITS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_HABITS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 5) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val name = parts[1]
            val description = parts[2]
            val target = parts[3].toIntOrNull() ?: 1
            val enabled = parts[4] == "1"
            PlannerHabit(
                id = id,
                name = name,
                description = description,
                targetPerDay = target,
                enabled = enabled
            )
        }
}

fun saveHabits(context: Context, habits: List<PlannerHabit>) {
    val prefs = context.getSharedPreferences(PREF_HABITS, Context.MODE_PRIVATE)
    val raw = habits.joinToString("\n") { h ->
        val safeName = h.name.replace("\n", " ")
        val safeDesc = h.description.replace("\n", " ")
        "${h.id}||$safeName||$safeDesc||${h.targetPerDay}||${if (h.enabled) "1" else "0"}"
    }
    prefs.edit().putString(KEY_HABITS, raw).apply()
}
