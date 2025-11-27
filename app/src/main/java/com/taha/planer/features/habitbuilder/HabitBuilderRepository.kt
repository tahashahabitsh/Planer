package com.taha.planer.features.habitbuilder

import android.content.Context

data class HabitPlan(
    val id: Long,
    val name: String,
    val why: String,
    val cue: String,
    val action: String,
    val reward: String,
    val frequencyPerWeek: Int,
    val minVersion: String,
    val obstacles: String,
    val antiObstacles: String,
    val startDate: String, // yyyy-MM-dd
    val status: HabitPlanStatus
)

enum class HabitPlanStatus {
    ACTIVE,
    PAUSED,
    COMPLETED
}

private const val PREF_HABIT_PLAN = "planner_habit_builder"
private const val KEY_HABIT_PLAN = "habit_plans_v1"

fun loadHabitPlans(context: Context): List<HabitPlan> {
    val prefs = context.getSharedPreferences(PREF_HABIT_PLAN, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_HABIT_PLAN, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw.split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 12) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val name = parts[1]
            val why = parts[2]
            val cue = parts[3]
            val action = parts[4]
            val reward = parts[5]
            val freq = parts[6].toIntOrNull() ?: 3
            val minVersion = parts[7]
            val obstacles = parts[8]
            val antiObstacles = parts[9]
            val startDate = parts[10]
            val status = when (parts[11]) {
                "PAUSED" -> HabitPlanStatus.PAUSED
                "COMPLETED" -> HabitPlanStatus.COMPLETED
                else -> HabitPlanStatus.ACTIVE
            }
            HabitPlan(
                id = id,
                name = name,
                why = why,
                cue = cue,
                action = action,
                reward = reward,
                frequencyPerWeek = freq,
                minVersion = minVersion,
                obstacles = obstacles,
                antiObstacles = antiObstacles,
                startDate = startDate,
                status = status
            )
        }
}

fun saveHabitPlans(context: Context, plans: List<HabitPlan>) {
    val prefs = context.getSharedPreferences(PREF_HABIT_PLAN, Context.MODE_PRIVATE)
    val raw = plans.joinToString("\n") { p ->
        val safeName = p.name.replace("\n", " ")
        val safeWhy = p.why.replace("\n", " ")
        val safeCue = p.cue.replace("\n", " ")
        val safeAction = p.action.replace("\n", " ")
        val safeReward = p.reward.replace("\n", " ")
        val safeMin = p.minVersion.replace("\n", " ")
        val safeObs = p.obstacles.replace("\n", " ")
        val safeAnti = p.antiObstacles.replace("\n", " ")
        val statusStr = when (p.status) {
            HabitPlanStatus.ACTIVE -> "ACTIVE"
            HabitPlanStatus.PAUSED -> "PAUSED"
            HabitPlanStatus.COMPLETED -> "COMPLETED"
        }
        "${p.id}||$safeName||$safeWhy||$safeCue||$safeAction||$safeReward||${p.frequencyPerWeek}||" +
                "$safeMin||$safeObs||$safeAnti||${p.startDate}||$statusStr"
    }
    prefs.edit().putString(KEY_HABIT_PLAN, raw).apply()
}
