package com.taha.planer.ui
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * کارت مشترک که ظاهرش بر اساس DesignStyle عوض می‌شود:
 * - GLASS: شیشه‌ای، کمی شفاف با بوردر
 * - NEOMORPH: سطح نرم با سایه‌ی ملایم
 * - ILLUSTRATION: رنگی و زنده‌تر
 */
@Composable
fun PlannerCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val ui = LocalUiSettings.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    val (bgColor, border, elevation) = when (ui.designStyle) {
        DesignStyle.GLASS -> {
            Triple(
                colors.primary.copy(alpha = 0.08f),
                BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f)),
                0.dp
            )
        }

        DesignStyle.NEOMORPH -> {
            Triple(
                colors.surface,
                null,
                8.dp
            )
        }

        DesignStyle.ILLUSTRATION -> {
            Triple(
                colors.secondaryContainer,
                null,
                4.dp
            )
        }
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        contentColor = colors.onSurface,
        shape = shape,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = border
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}
