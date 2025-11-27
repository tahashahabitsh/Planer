package com.taha.planer.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// حالت رنگ
enum class ColorMode { SYSTEM, LIGHT, DARK }

// استایل طراحی
enum class DesignStyle { GLASS, NEOMORPH, ILLUSTRATION }

// کنترلر تنظیمات UI
data class UiSettingsController(
    val colorMode: ColorMode,
    val designStyle: DesignStyle,
    val setColorMode: (ColorMode) -> Unit,
    val setDesignStyle: (DesignStyle) -> Unit
)

// CompositionLocal برای دسترسی از همه‌جا
val LocalUiSettings = staticCompositionLocalOf {
    UiSettingsController(
        colorMode = ColorMode.SYSTEM,
        designStyle = DesignStyle.GLASS,
        setColorMode = {},
        setDesignStyle = {}
    )
}

// SharedPreferences keys
private const val PREF_UI = "planner_ui"
private const val KEY_COLOR_MODE = "color_mode"
private const val KEY_DESIGN_STYLE = "design_style"

private fun readInitialColorMode(context: Context): ColorMode {
    val prefs = context.getSharedPreferences(PREF_UI, Context.MODE_PRIVATE)
    return when (prefs.getString(KEY_COLOR_MODE, "system")) {
        "light" -> ColorMode.LIGHT
        "dark" -> ColorMode.DARK
        else -> ColorMode.SYSTEM
    }
}

private fun readInitialDesignStyle(context: Context): DesignStyle {
    val prefs = context.getSharedPreferences(PREF_UI, Context.MODE_PRIVATE)
    return when (prefs.getString(KEY_DESIGN_STYLE, "glass")) {
        "neo" -> DesignStyle.NEOMORPH
        "illustration" -> DesignStyle.ILLUSTRATION
        else -> DesignStyle.GLASS
    }
}

private fun persistUiSettings(
    context: Context,
    colorMode: ColorMode,
    designStyle: DesignStyle
) {
    val prefs = context.getSharedPreferences(PREF_UI, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(
            KEY_COLOR_MODE,
            when (colorMode) {
                ColorMode.SYSTEM -> "system"
                ColorMode.LIGHT -> "light"
                ColorMode.DARK -> "dark"
            }
        )
        .putString(
            KEY_DESIGN_STYLE,
            when (designStyle) {
                DesignStyle.GLASS -> "glass"
                DesignStyle.NEOMORPH -> "neo"
                DesignStyle.ILLUSTRATION -> "illustration"
            }
        )
        .apply()
}

// ---------- پالت‌های رنگ بر اساس استایل ----------

// GLASS – شیشه‌ای، پس‌زمینه روشن‌تر و شفاف‌تر
private val LightGlassColors = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xCCE0E7FF.toInt()),
    onPrimaryContainer = Color(0xFF020617),
    secondary = Color(0xFF22C55E),
    onSecondary = Color.Black,
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF020617),
    surface = Color(0x80FFFFFF),
    onSurface = Color(0xFF020617),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val DarkGlassColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF020617),
    primaryContainer = Color(0x803F3F46),
    onPrimaryContainer = Color(0xFFE5E7EB),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color.Black,
    background = Color(0xFF020617),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0x8025252B),
    onSurface = Color(0xFFE5E7EB),
    error = Color(0xFFF97373),
    onError = Color.Black
)

// NEOMORPH – سطح نرم، اختلاف کم بین پس‌زمینه و کارت‌ها
private val LightNeoColors = lightColorScheme(
    primary = Color(0xFF0EA5E9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF020617),
    secondary = Color(0xFF22C55E),
    onSecondary = Color.Black,
    background = Color(0xFFE5E7EB),
    onBackground = Color(0xFF020617),
    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF020617),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val DarkNeoColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF020617),
    primaryContainer = Color(0xFF0F172A),
    onPrimaryContainer = Color(0xFFE5E7EB),
    secondary = Color(0xFF22C55E),
    onSecondary = Color.Black,
    background = Color(0xFF020617),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF0B1120),
    onSurface = Color(0xFFE5E7EB),
    error = Color(0xFFF97373),
    onError = Color.Black
)

// ILLUSTRATION – رنگی‌تر و شادتر
private val LightIllustrationColors = lightColorScheme(
    primary = Color(0xFFEC4899),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4F1),
    onPrimaryContainer = Color(0xFF500724),
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    background = Color(0xFFFDF2F8),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF111827),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val DarkIllustrationColors = darkColorScheme(
    primary = Color(0xFFF472B6),
    onPrimary = Color(0xFF020617),
    primaryContainer = Color(0xFF500724),
    onPrimaryContainer = Color(0xFFFCE7F3),
    secondary = Color(0xFFA5B4FC),
    onSecondary = Color(0xFF020617),
    background = Color(0xFF020617),
    onBackground = Color(0xFFF9A8D4),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF9A8D4),
    error = Color(0xFFF97373),
    onError = Color.Black
)

private fun colorSchemeFor(
    dark: Boolean,
    style: DesignStyle
) = when (style) {
    DesignStyle.GLASS ->
        if (dark) DarkGlassColors else LightGlassColors

    DesignStyle.NEOMORPH ->
        if (dark) DarkNeoColors else LightNeoColors

    DesignStyle.ILLUSTRATION ->
        if (dark) DarkIllustrationColors else LightIllustrationColors
}

/**
 * تم اصلی برنامه.
 *
 * این هم روشن/تاریک را کنترل می‌کند، هم استایل طراحی را از طریق LocalUiSettings
 * در اختیار صفحات قرار می‌دهد و رنگ‌ها را بر اساس آن عوض می‌کند.
 */
@Composable
fun PlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // فعلاً استفاده نمی‌کنیم ولی برای سازگاری گذاشتیم
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var colorMode by remember { mutableStateOf(readInitialColorMode(context)) }
    var designStyle by remember { mutableStateOf(readInitialDesignStyle(context)) }

    val effectiveDark = when (colorMode) {
        ColorMode.SYSTEM -> darkTheme
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }

    val controller = remember(colorMode, designStyle) {
        UiSettingsController(
            colorMode = colorMode,
            designStyle = designStyle,
            setColorMode = { mode ->
                colorMode = mode
                persistUiSettings(context, mode, designStyle)
            },
            setDesignStyle = { style ->
                designStyle = style
                persistUiSettings(context, colorMode, style)
            }
        )
    }

    val colors = colorSchemeFor(effectiveDark, designStyle)

    CompositionLocalProvider(LocalUiSettings provides controller) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * برچسب خوانا برای استایل فعلی
 */
fun designStyleLabel(style: DesignStyle): String =
    when (style) {
        DesignStyle.GLASS -> "Glassmorphism (شیشه‌ای)"
        DesignStyle.NEOMORPH -> "Neomorphism (نئومرفیک)"
        DesignStyle.ILLUSTRATION -> "Illustration (تصویرسازی)"
    }
