package com.taha.planer.ui



import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * سه سبک کلی طراحی برای کل اپ.
 */


/**
 * تبدیل یک رشته‌ی ذخیره‌شده در SharedPreferences به DesignStyle.
 * اگر مقدار ناشناس باشد، حالت Glass برمی‌گردد.
 */
fun parseDesignStyle(raw: String?): DesignStyle =
    when (raw) {
        "Neomorph" -> DesignStyle.Neomorph
        "Illustration" -> DesignStyle.Illustration
        "Glass" -> DesignStyle.Glass
        else -> DesignStyle.Glass
    }

/**
 * رنگ کارت‌ها بر اساس استایل.
 */
@Composable
fun plannerCardColors(style: DesignStyle) = when (style) {
    DesignStyle.Glass -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    DesignStyle.Neomorph -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    DesignStyle.Illustration -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

/**
 * شکل‌ها (گوشه‌ی کارت‌ها و …) بر اساس استایل.
 * این تابع ساده است ولی برای کامپایل کامل کافی است.
 */
fun plannerShapes(style: DesignStyle): Shapes = when (style) {
    DesignStyle.Glass -> Shapes(
        extraSmall = ShapeDefaults.ExtraSmall,
        small = ShapeDefaults.Small,
        medium = ShapeDefaults.Medium,
        large = ShapeDefaults.Large,
        extraLarge = ShapeDefaults.ExtraLarge
    )

    DesignStyle.Neomorph -> Shapes(
        extraSmall = ShapeDefaults.ExtraSmall,
        small = ShapeDefaults.Small,
        medium = ShapeDefaults.Medium,
        large = ShapeDefaults.Large,
        extraLarge = ShapeDefaults.ExtraLarge
    )

    DesignStyle.Illustration -> Shapes(
        extraSmall = ShapeDefaults.ExtraSmall,
        small = ShapeDefaults.Small,
        medium = ShapeDefaults.Medium,
        large = ShapeDefaults.Large,
        extraLarge = ShapeDefaults.ExtraLarge
    )
}

/**
 * تایپوگرافی واحد برای همه‌ی استایل‌ها.
 * اگر بعداً خواستیم، می‌توانیم برای هر استایل جدا تنظیم کنیم.
 */
fun plannerTypography(style: DesignStyle): Typography = Typography()

/**
 * برگردوندن لیبل فارسی برای استایل طراحی فعلی.
 * اگه بعداً enum عوض شد، فقط متن‌ها رو اینجا آپدیت می‌کنیم.
 */
fun designStyleLabel(style: DesignStyle): String {
    return when (style) {
        DesignStyle.SYSTEM        -> "پیش‌فرض سیستم"
        DesignStyle.GLASSMORPHISM -> "تم شیشه‌ای (Glassmorphism)"
        DesignStyle.DesignStyle.NEOMORPHISM   -> "تم نرم (Neumorphism)"
        DesignStyle.DesignStyle.ILLUSTRATION  -> "تم تصویرمحور (Illustration)"
    }
}

/**
 * برچسب فارسی برای استایل طراحی فعلی.
 * فقط برای نمایش در تنظیمات استفاده می‌شود.
 */
fun designStyleLabel(style: DesignStyle): String =
    when (style) {
        DesignStyle.SYSTEM        -> "پیش‌فرض سیستم"
        DesignStyle.GLASSMORPHISM -> "تم شیشه‌ای (Glassmorphism)"
        DesignStyle.DesignStyle.NEOMORPHISM   -> "تم نرم (Neumorphism)"
        DesignStyle.DesignStyle.ILLUSTRATION  -> "تم تصویرمحور (Illustration)"
    }
