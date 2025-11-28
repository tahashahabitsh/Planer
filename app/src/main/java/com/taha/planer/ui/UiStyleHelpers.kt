package com.taha.planer.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Helperهای عمومی برای استایل‌های مختلف UI اپ.
 *
 * DesignStyle الان این مقادیر رو دارد:
 *  - GLASSMORPHISM
 *  - NEOMORPHISM
 *  - ILLUSTRATION
 *  - SYSTEM
 *  - DEFAULT
 */

/** برچسب نمایشی هر استایل (برای Settings و ...) */
fun designStyleLabel(style: DesignStyle): String =
    when (style) {
        DesignStyle.GLASSMORPHISM -> "گلس‌مورفیسم"
        DesignStyle.NEOMORPHISM   -> "نئومورفیسم"
        DesignStyle.ILLUSTRATION  -> "ایلاستریشن"
        DesignStyle.SYSTEM        -> "بر اساس تم سیستم"
        DesignStyle.DEFAULT       -> "تم پیش‌فرض برنامه"
    }

/** رنگ‌های کارت‌ها بر اساس استایل انتخاب‌شده */
@Composable
fun plannerCardColors(style: DesignStyle): CardColors =
    when (style) {
        DesignStyle.GLASSMORPHISM -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        DesignStyle.NEOMORPHISM -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        DesignStyle.ILLUSTRATION -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        DesignStyle.SYSTEM,
        DesignStyle.DEFAULT -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

/** شکل‌ها (گوشه‌های کارت و ...) بر اساس استایل */
fun plannerShapes(style: DesignStyle): Shapes =
    when (style) {
        DesignStyle.GLASSMORPHISM -> Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small      = RoundedCornerShape(16.dp),
            medium     = RoundedCornerShape(24.dp),
            large      = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(40.dp)
        )
        DesignStyle.NEOMORPHISM -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small      = RoundedCornerShape(12.dp),
            medium     = RoundedCornerShape(20.dp),
            large      = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        )
        DesignStyle.ILLUSTRATION -> Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small      = RoundedCornerShape(18.dp),
            medium     = RoundedCornerShape(26.dp),
            large      = RoundedCornerShape(34.dp),
            extraLarge = RoundedCornerShape(44.dp)
        )
        DesignStyle.SYSTEM,
        DesignStyle.DEFAULT -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small      = RoundedCornerShape(12.dp),
            medium     = RoundedCornerShape(20.dp),
            large      = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        )
    }
