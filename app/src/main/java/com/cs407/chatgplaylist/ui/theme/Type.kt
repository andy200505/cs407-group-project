package com.cs407.chatgplaylist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

private val AppTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
)

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = AppTextStyle.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    displayMedium = AppTextStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    displaySmall = AppTextStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),

    headlineLarge = AppTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = AppTextStyle.copy(fontSize = 20.sp),
    headlineSmall = AppTextStyle.copy(fontSize = 18.sp),

    titleLarge = AppTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = AppTextStyle.copy(fontSize = 16.sp),
    titleSmall = AppTextStyle.copy(fontSize = 14.sp),

    bodyLarge = AppTextStyle.copy(fontSize = 16.sp),
    bodyMedium = AppTextStyle.copy(fontSize = 14.sp),
    bodySmall = AppTextStyle.copy(fontSize = 12.sp),

    labelLarge = AppTextStyle.copy(fontSize = 14.sp),
    labelMedium = AppTextStyle.copy(fontSize = 12.sp),
    labelSmall = AppTextStyle.copy(fontSize = 10.sp)
)