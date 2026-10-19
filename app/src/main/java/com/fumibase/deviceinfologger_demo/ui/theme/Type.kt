package com.fumibase.deviceinfologger_demo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp

private val Japanese = TextStyle(localeList = LocaleList("ja-JP"))

private fun Typography.withJapaneseLocale() = Typography(
    displayLarge = displayLarge.merge(Japanese),
    displayMedium = displayMedium.merge(Japanese),
    displaySmall = displaySmall.merge(Japanese),
    headlineLarge = headlineLarge.merge(Japanese),
    headlineMedium = headlineMedium.merge(Japanese),
    headlineSmall = headlineSmall.merge(Japanese),
    titleLarge = titleLarge.merge(Japanese),
    titleMedium = titleMedium.merge(Japanese),
    titleSmall = titleSmall.merge(Japanese),
    bodyLarge = bodyLarge.merge(Japanese),
    bodyMedium = bodyMedium.merge(Japanese),
    bodySmall = bodySmall.merge(Japanese),
    labelLarge = labelLarge.merge(Japanese),
    labelMedium = labelMedium.merge(Japanese),
    labelSmall = labelSmall.merge(Japanese),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
).withJapaneseLocale()
