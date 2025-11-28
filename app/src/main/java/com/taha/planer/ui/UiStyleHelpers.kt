package com.taha.planer.ui

import android.content.SharedPreferences
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private const val PREF_DESIGN_STYLE = "design_style"

/**
 * استایل فعلی رو از SharedPreferences می‌خوانیم.
 */
fun loadDesignStyle(prefs: SharedPreferences): DesignStyle {
    val saved = prefs.getString(PREF_DESIGN_STYLE, DesignStyle.SYSTEM.name)
    return saved
        ?.let { runCatching { DesignStyle.valueOf(it) }.getOrNull() }
        ?: DesignStyle.SYSTEM
}

/**
 * استایل انتخاب‌شده را در SharedPreferences ذخیره می‌کنیم.
 */
fun persistDesignStyle(prefs: SharedPreferences, style: DesignStyle) {
    prefs.edit().putString(PREF_DESIGN_STYLE, style.name).apply()
}

/**
 * لیبل قابل نمایش برای هر استایل (برای UI تنظیمات).
 */
fun designStyleLabel(style: DesignStyle): String = when (style) {
    DesignStyle.GLASS        -> "Glassmorphism"
    DesignStyle.NEOMORPH     -> "Neumorphism"
    DesignStyle.ILLUSTRATION -> "Illustration"
    DesignStyle.SYSTEM       -> "System default"
    DesignStyle.DEFAULT      -> "Planner default"
}

/**
 * رنگ کارت‌ها بسته به استایل انتخابی.
 * (بعداً هر وقت خواستی می‌تونیم دقیق‌ترش کنیم؛ فعلاً فقط برای کامپایل و ظاهر منطقیه.)
 */
@Composable
fun plannerCardColors(style: DesignStyle): CardColors = when (style) {
    DesignStyle.GLASS -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

    DesignStyle.NEOMORPH -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    )

    DesignStyle.ILLUSTRATION -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    DesignStyle.SYSTEM,
    DesignStyle.DEFAULT -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}
