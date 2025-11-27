package com.taha.planer.features.assistant

import android.content.Context
import com.taha.planer.features.alarms.AlarmRepeatType
import com.taha.planer.features.alarms.PlannerAlarm
import com.taha.planer.features.alarms.rescheduleAllAlarms
import com.taha.planer.features.tasks.PlannerTask
import com.taha.planer.features.tasks.loadTasks
import com.taha.planer.features.tasks.saveTasks
import com.taha.planer.features.habits.PlannerHabit
import com.taha.planer.features.habits.loadHabits
import com.taha.planer.features.habits.saveHabits
import org.json.JSONObject

// باید با فایل آلارم‌ها یکی باشد
private const val PREF_ALARMS = "planner_alarms"
private const val KEY_ALARMS = "alarms_v1"

/**
 * این تابع ACTION_JSON را می‌گیرد و اگر اکشن معتبر بود
 * آن را روی اپ اعمال می‌کند و یک متن تأیید برمی‌گرداند.
 *
 * اکشن‌های پشتیبانی‌شده:
 * - add_alarm
 * - add_task / update_task / delete_task
 * - add_habit / update_habit / delete_habit
 * - update_profile
 */
fun applyAssistantActionJson(context: Context, actionJson: String): String? {
    return try {
        val obj = JSONObject(actionJson)
        val type = obj.optString("type", "")

        when (type) {
            "add_alarm" -> handleAddAlarm(context, obj)
            "add_task" -> handleAddTask(context, obj)
            "update_task" -> handleUpdateTask(context, obj)
            "delete_task" -> handleDeleteTask(context, obj)
            "add_habit" -> handleAddHabit(context, obj)
            "update_habit" -> handleUpdateHabit(context, obj)
            "delete_habit" -> handleDeleteHabit(context, obj)
            "update_profile" -> handleUpdateProfile(context, obj)
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

// ---------------- آلارم ----------------

private fun handleAddAlarm(context: Context, obj: JSONObject): String {
    val title = obj.optString("title", "یادآور")
    val message = obj.optString("message", "")
    val hour = obj.optInt("hour", -1)
    val minute = obj.optInt("minute", -1)
    val repeatRaw = obj.optString("repeat", "ONCE")
    val section = obj.optString("section", "")

    val h = if (hour in 0..23) hour else 9
    val m = if (minute in 0..59) minute else 0
    val repeatType = when (repeatRaw.uppercase()) {
        "DAILY" -> AlarmRepeatType.DAILY
        else -> AlarmRepeatType.ONCE
    }

    val existing = loadAlarmsFromPrefs(context)
    val newAlarm = PlannerAlarm(
        id = System.currentTimeMillis(),
        title = title,
        message = message,
        hour = h,
        minute = m,
        repeatType = repeatType,
        enabled = true,
        sectionTag = section
    )
    val updated = existing + newAlarm
    saveAlarmsToPrefs(context, updated)
    rescheduleAllAlarms(context, updated)

    val sectionText = if (section.isNotBlank()) " برای بخش «$section»" else ""
    return "یک آلارم جدید$sectionText با عنوان «$title» روی ساعت %02d:%02d تنظیم شد."
        .format(h, m)
}

private fun loadAlarmsFromPrefs(context: Context): List<PlannerAlarm> {
    val prefs = context.getSharedPreferences(PREF_ALARMS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_ALARMS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 7) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val hour = parts[1].toIntOrNull() ?: return@mapNotNull null
            val minute = parts[2].toIntOrNull() ?: return@mapNotNull null
            val repeatCode = parts[3].toIntOrNull() ?: 0
            val enabled = parts[4] == "1"
            val title = parts[5]
            val message = parts[6]
            val sectionTag = if (parts.size >= 8) parts[7] else ""
            PlannerAlarm(
                id = id,
                title = title,
                message = message,
                hour = hour,
                minute = minute,
                repeatType = AlarmRepeatType.fromCode(repeatCode),
                enabled = enabled,
                sectionTag = sectionTag
            )
        }
}

private fun saveAlarmsToPrefs(context: Context, alarms: List<PlannerAlarm>) {
    val prefs = context.getSharedPreferences(PREF_ALARMS, Context.MODE_PRIVATE)
    val raw = alarms.joinToString("\n") { a ->
        val safeTitle = a.title.replace("\n", " ")
        val safeMsg = a.message.replace("\n", " ")
        val safeSection = a.sectionTag.replace("\n", " ")
        "${a.id}||${a.hour}||${a.minute}||${a.repeatType.code}||${if (a.enabled) "1" else "0"}||" +
                "$safeTitle||$safeMsg||$safeSection"
    }
    prefs.edit().putString(KEY_ALARMS, raw).apply()
}

// ---------------- کارها ----------------

private fun handleAddTask(context: Context, obj: JSONObject): String {
    val title = obj.optString("title", "کار جدید")
    val description = obj.optString("description", "")
    val date = obj.optString("date", "")

    val tasks = loadTasks(context)
    val newTask = PlannerTask(
        id = System.currentTimeMillis(),
        title = title,
        description = description,
        isDone = false,
        date = date
    )
    val updated = tasks + newTask
    saveTasks(context, updated)

    return "یک کار جدید با عنوان «$title» اضافه شد."
}

private fun handleUpdateTask(context: Context, obj: JSONObject): String? {
    val title = obj.optString("title", "").trim()
    if (title.isEmpty()) return null

    val tasks = loadTasks(context)
    val target = tasks.find { it.title.equals(title, ignoreCase = true) } ?: return "کاری با این عنوان پیدا نشد."

    val newTitle = obj.optString("new_title", target.title).ifBlank { target.title }
    val newDesc = obj.optString("new_description", target.description)
    val newDate = obj.optString("date", target.date)
    val doneFlag = if (obj.has("done")) obj.optBoolean("done") else target.isDone

    val updatedTask = target.copy(
        title = newTitle.trim(),
        description = newDesc.trim(),
        date = newDate.trim(),
        isDone = doneFlag
    )

    val updatedList = tasks.map { if (it.id == target.id) updatedTask else it }
    saveTasks(context, updatedList)

    return "کار «${target.title}» ویرایش شد."
}

private fun handleDeleteTask(context: Context, obj: JSONObject): String? {
    val title = obj.optString("title", "").trim()
    if (title.isEmpty()) return null

    val tasks = loadTasks(context)
    val target = tasks.find { it.title.equals(title, ignoreCase = true) }
        ?: return "کاری با این عنوان پیدا نشد."

    val updated = tasks.filterNot { it.id == target.id }
    saveTasks(context, updated)

    return "کار «${target.title}» حذف شد."
}

// ---------------- عادت‌ها ----------------

private fun handleAddHabit(context: Context, obj: JSONObject): String {
    val name = obj.optString("name", "عادت جدید")
    val description = obj.optString("description", "")
    val target = obj.optInt("target_per_day", 1).let { if (it <= 0) 1 else it }

    val habits = loadHabits(context)
    val newHabit = PlannerHabit(
        id = System.currentTimeMillis(),
        name = name,
        description = description,
        targetPerDay = target,
        enabled = true
    )
    val updated = habits + newHabit
    saveHabits(context, updated)

    return "یک عادت جدید با نام «$name» اضافه شد."
}

private fun handleUpdateHabit(context: Context, obj: JSONObject): String? {
    val name = obj.optString("name", "").trim()
    if (name.isEmpty()) return null

    val habits = loadHabits(context)
    val target = habits.find { it.name.equals(name, ignoreCase = true) }
        ?: return "عادتی با این نام پیدا نشد."

    val newName = obj.optString("new_name", target.name).ifBlank { target.name }
    val newDesc = obj.optString("new_description", target.description)
    val newTarget = obj.optInt("target_per_day", target.targetPerDay)
        .let { if (it <= 0) target.targetPerDay else it }
    val newEnabled = if (obj.has("enabled")) obj.optBoolean("enabled") else target.enabled

    val updatedHabit = target.copy(
        name = newName.trim(),
        description = newDesc.trim(),
        targetPerDay = newTarget,
        enabled = newEnabled
    )

    val updatedList = habits.map { if (it.id == target.id) updatedHabit else it }
    saveHabits(context, updatedList)

    return "عادت «${target.name}» ویرایش شد."
}

private fun handleDeleteHabit(context: Context, obj: JSONObject): String? {
    val name = obj.optString("name", "").trim()
    if (name.isEmpty()) return null

    val habits = loadHabits(context)
    val target = habits.find { it.name.equals(name, ignoreCase = true) }
        ?: return "عادتی با این نام پیدا نشد."

    val updated = habits.filterNot { it.id == target.id }
    saveHabits(context, updated)

    return "عادت «${target.name}» حذف شد."
}

// ---------------- پروفایل کاربر ----------------

private fun handleUpdateProfile(context: Context, obj: JSONObject): String {
    return updateAssistantProfileFromJson(context, obj)
}
