package com.taha.planer.features.tasks

import android.content.Context

data class PlannerTask(
    val id: Long,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val date: String // فرمت پیشنهادی: YYYY-MM-DD یا خالی
)

private const val PREF_TASKS = "planner_tasks"
private const val KEY_TASKS = "tasks_v1"

fun loadTasks(context: Context): List<PlannerTask> {
    val prefs = context.getSharedPreferences(PREF_TASKS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_TASKS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 5) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val description = parts[2]
            val isDone = parts[3] == "1"
            val date = parts[4]
            PlannerTask(
                id = id,
                title = title,
                description = description,
                isDone = isDone,
                date = date
            )
        }
}

fun saveTasks(context: Context, tasks: List<PlannerTask>) {
    val prefs = context.getSharedPreferences(PREF_TASKS, Context.MODE_PRIVATE)
    val raw = tasks.joinToString("\n") { t ->
        val safeTitle = t.title.replace("\n", " ")
        val safeDesc = t.description.replace("\n", " ")
        val safeDate = t.date.replace("\n", " ")
        "${t.id}||$safeTitle||$safeDesc||${if (t.isDone) "1" else "0"}||$safeDate"
    }
    prefs.edit().putString(KEY_TASKS, raw).apply()
}
