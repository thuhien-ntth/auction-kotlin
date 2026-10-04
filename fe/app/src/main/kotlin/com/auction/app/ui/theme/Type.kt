package com.auction.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

val AppTypography = Typography(
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.Default, fontSize = 15.sp),
    labelLarge = base.labelLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium)
)
