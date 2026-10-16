package com.gentlemanjoel.jot.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Sagan Slate palette. */
object Slate {
    val Background = Color(0xFF262B30)
    val Secondary = Color(0xFF1A1D20)
    val Accent = Color(0xFF4A90D9)
    val Text = Color(0xFFE4EAF0)
    val Muted = Color(0xFF8FA3B8)
    val Divider = Color(0xFF3A4249)
    val Danger = Color(0xFFD9674A)
}

private val JotColors = darkColorScheme(
    primary = Slate.Accent,
    onPrimary = Color(0xFF0D1217),
    secondary = Slate.Accent,
    onSecondary = Color(0xFF0D1217),
    background = Slate.Background,
    onBackground = Slate.Text,
    surface = Slate.Background,
    onSurface = Slate.Text,
    surfaceVariant = Slate.Secondary,
    onSurfaceVariant = Slate.Muted,
    outline = Slate.Muted,
    outlineVariant = Slate.Divider,
    error = Slate.Danger,
)

private val JotTypography = androidx.compose.material3.Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
    ),
)

/** Jot is always dark — the Sagan Slate scheme is applied regardless of system theme. */
@Composable
fun JotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JotColors,
        typography = JotTypography,
        content = content,
    )
}
