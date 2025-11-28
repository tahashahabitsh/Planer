package com.taha.planer.features.settings

import com.taha.planer.ui.DesignStyle
import com.taha.planer.ui.ColorMode

data class LocalUiSettings(
    val designStyle: DesignStyle = DesignStyle.SYSTEM,
    val colorMode: ColorMode = ColorMode.SYSTEM,
)
