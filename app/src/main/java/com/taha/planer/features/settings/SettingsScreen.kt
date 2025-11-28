package com.taha.planer.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taha.planer.ui.ColorMode
import com.taha.planer.ui.DesignStyle
import com.taha.planer.ui.LocalUiSettings
import com.taha.planer.ui.designStyleLabel

@Composable
fun SettingsScreen() {
    val ui = LocalUiSettings.current

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "تنظیمات برنامه",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            ThemeSettingsCard(ui.colorMode, onModeChange = ui.setColorMode)

            DesignStyleSettingsCard(ui.designStyle, onStyleChange = ui.setDesignStyle)

            AppearanceInfoCard()
        }
    }
}

@Composable
private fun ThemeSettingsCard(
    selectedMode: ColorMode,
    onModeChange: (ColorMode) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "تم رنگی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "انتخاب کن که برنامه همیشه روشن باشد، همیشه تاریک، یا با تم سیستم هماهنگ شود.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeModeButton(
                    label = "سیستم",
                    isSelected = selectedMode == ColorMode.SYSTEM,
                    onClick = { onModeChange(ColorMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeButton(
                    label = "روشن",
                    isSelected = selectedMode == ColorMode.LIGHT,
                    onClick = { onModeChange(ColorMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeButton(
                    label = "تاریک",
                    isSelected = selectedMode == ColorMode.DARK,
                    onClick = { onModeChange(ColorMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DesignStyleSettingsCard(
    selectedStyle: DesignStyle,
    onStyleChange: (DesignStyle) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "استایل طراحی (UI)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "سه سبک اصلی طراحی که بعداً می‌تونیم روی کارت‌ها، دکمه‌ها و پس‌زمینه‌ها اعمالشان کنیم.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyleButton(
                    label = "Glass",
                    description = "شیشه‌ای و شفاف",
                    isSelected = selectedStyle == DesignStyle.GLASS,
                    onClick = { onStyleChange(DesignStyle.GLASS) },
                    modifier = Modifier.weight(1f)
                )
                StyleButton(
                    label = "Neo",
                    description = "سایه نرم و برجسته",
                    isSelected = selectedStyle == DesignStyle.NEOMORPH,
                    onClick = { onStyleChange(DesignStyle.NEOMORPH) },
                    modifier = Modifier.weight(1f)
                )
                StyleButton(
                    label = "Illustration",
                    description = "رنگی و تصویرسازی",
                    isSelected = selectedStyle == DesignStyle.ILLUSTRATION,
                    onClick = { onStyleChange(DesignStyle.ILLUSTRATION) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "استایل فعلی: ${designStyleLabel(selectedStyle)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StyleButton(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AppearanceInfoCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "چطور از تم و استایل استفاده می‌شود؟",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• تم روشن/تاریک روی رنگ پس‌زمینه و اجزای اصلی تاثیر می‌گذارد.\n" +
                        "• استایل طراحی (Glass / Neomorph / Illustration) در کارت‌ها و بخش‌های مختلف استفاده می‌شود.\n" +
                        "• تنظیمات این صفحه در حافظه ذخیره می‌شود و بعد از باز و بسته کردن برنامه هم حفظ می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
        }
    }
}

// Local UI settings that we keep in memory / prefs
data class LocalUiSettings(
    val designStyle: DesignStyle = DesignStyle.SYSTEM,
    val colorMode: ColorMode = ColorMode.SYSTEM,
)

// Local UI settings that we keep in memory / prefs
data class LocalUiSettings(
    val designStyle: DesignStyle = DesignStyle.SYSTEM,
    val colorMode: ColorMode = ColorMode.SYSTEM,
)
