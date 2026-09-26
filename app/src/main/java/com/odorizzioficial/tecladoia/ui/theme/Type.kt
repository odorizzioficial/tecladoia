package com.odorizzioficial.tecladoia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * O design pede Roboto Flex. Nenhum arquivo de fonte e empacotado: o app usa a
 * familia padrao do sistema, que no Android moderno ja e Roboto/Roboto Flex.
 * Os tamanhos, pesos, entrelinhas e tracking vem do DESIGN.md.
 */
private val AppFont = FontFamily.Default

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = AppFont,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 34.sp,
        letterSpacing = (-0.015).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFont,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFont,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        letterSpacing = 0.14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFont,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        letterSpacing = 0.36.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFont,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        letterSpacing = 0.44.sp
    )
)
