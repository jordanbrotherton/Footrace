package com.example.footrace.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import com.example.footrace.R

val Stencil = FontFamily(
    Font(
        R.font.stencil_font,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.stencil_font,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.stencil_font,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        R.font.stencil_font,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    ),
)

private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Stencil, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = Stencil, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Stencil, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Stencil, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Stencil, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    bodyMedium = base.bodyMedium.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    bodySmall = base.bodySmall.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = Stencil, fontWeight = FontWeight.SemiBold),
    )