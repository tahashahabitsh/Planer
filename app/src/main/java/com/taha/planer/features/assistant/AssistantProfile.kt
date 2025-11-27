package com.taha.planer.features.assistant

import android.content.Context
import org.json.JSONObject

data class AssistantUserProfile(
    val summary: String,
    val goals: String,
    val preferences: String,
    val workStyle: String,
    val energyPattern: String
)

private const val PREF_PROFILE = "assistant_profile"
private const val KEY_SUMMARY = "summary"
private const val KEY_GOALS = "goals"
private const val KEY_PREFERENCES = "preferences"
private const val KEY_WORK_STYLE = "work_style"
private const val KEY_ENERGY_PATTERN = "energy_pattern"

fun loadAssistantProfile(context: Context): AssistantUserProfile? {
    val prefs = context.getSharedPreferences(PREF_PROFILE, Context.MODE_PRIVATE)
    val summary = prefs.getString(KEY_SUMMARY, "") ?: ""
    val goals = prefs.getString(KEY_GOALS, "") ?: ""
    val preferences = prefs.getString(KEY_PREFERENCES, "") ?: ""
    val workStyle = prefs.getString(KEY_WORK_STYLE, "") ?: ""
    val energyPattern = prefs.getString(KEY_ENERGY_PATTERN, "") ?: ""

    if (summary.isBlank() && goals.isBlank() && preferences.isBlank() && workStyle.isBlank() && energyPattern.isBlank()) {
        return null
    }

    return AssistantUserProfile(
        summary = summary,
        goals = goals,
        preferences = preferences,
        workStyle = workStyle,
        energyPattern = energyPattern
    )
}

fun saveAssistantProfile(context: Context, profile: AssistantUserProfile) {
    val prefs = context.getSharedPreferences(PREF_PROFILE, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(KEY_SUMMARY, profile.summary)
        .putString(KEY_GOALS, profile.goals)
        .putString(KEY_PREFERENCES, profile.preferences)
        .putString(KEY_WORK_STYLE, profile.workStyle)
        .putString(KEY_ENERGY_PATTERN, profile.energyPattern)
        .apply()
}

/**
 * این متن از پروفایل ساخته می‌شود و به پرامپت سیستم مدل اضافه می‌شود.
 */
fun buildProfileText(profile: AssistantUserProfile?): String {
    if (profile == null) return ""
    val parts = mutableListOf<String>()
    if (profile.summary.isNotBlank()) {
        parts.add("خلاصه‌ی شخصیت و وضعیت کاربر: ${profile.summary}")
    }
    if (profile.goals.isNotBlank()) {
        parts.add("هدف‌های اصلی کاربر در ۳ تا ۱۲ ماه آینده: ${profile.goals}")
    }
    if (profile.preferences.isNotBlank()) {
        parts.add("ترجیحات کاربر در کار، یادگیری و استراحت: ${profile.preferences}")
    }
    if (profile.workStyle.isNotBlank()) {
        parts.add("سبک کار و تمرکز کاربر: ${profile.workStyle}")
    }
    if (profile.energyPattern.isNotBlank()) {
        parts.add("الگوی انرژی روزانه کاربر: ${profile.energyPattern}")
    }
    return parts.joinToString("\n")
}

/**
 * این تابع از ACTION_JSON با type = update_profile
 * پروفایل کاربر را به‌روزرسانی می‌کند و یک پیام تأیید برمی‌گرداند.
 *
 * فیلدهای قابل‌استفاده در JSON:
 * - summary
 * - goals
 * - preferences
 * - work_style
 * - energy_pattern
 */
fun updateAssistantProfileFromJson(context: Context, obj: JSONObject): String {
    val current = loadAssistantProfile(context) ?: AssistantUserProfile(
        summary = "",
        goals = "",
        preferences = "",
        workStyle = "",
        energyPattern = ""
    )

    fun pick(key: String, old: String): String {
        return if (obj.has(key)) obj.optString(key, old) else old
    }

    val updated = current.copy(
        summary = pick("summary", current.summary),
        goals = pick("goals", current.goals),
        preferences = pick("preferences", current.preferences),
        workStyle = pick("work_style", current.workStyle),
        energyPattern = pick("energy_pattern", current.energyPattern)
    )

    saveAssistantProfile(context, updated)

    return "پروفایل تو (خلاصه، هدف‌ها و ترجیح‌ها) به‌روز شد تا بهتر باهات هماهنگ بشم."
}
