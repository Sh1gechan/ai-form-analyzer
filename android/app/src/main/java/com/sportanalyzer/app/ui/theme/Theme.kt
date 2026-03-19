package com.sportanalyzer.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── iOS-inspired Dark Color System ─────────────────────────────
val iOSBlue        = Color(0xFF0A84FF)
val iOSGreen       = Color(0xFF30D158)
val iOSOrange      = Color(0xFFFF9F0A)
val iOSRed         = Color(0xFFFF453A)
val iOSIndigo      = Color(0xFF5E5CE6)

val SystemBlack    = Color(0xFF000000)   // Pure black background (like iPhone)
val SystemDark     = Color(0xFF1C1C1E)   // Card / grouped background
val SystemFill     = Color(0xFF2C2C2E)   // Secondary fill
val SystemFill2    = Color(0xFF3A3A3C)   // Tertiary fill (separator icons etc.)
val PrimaryLabel   = Color(0xFFFFFFFF)   // Primary text
val SecondaryLabel = Color(0xFF8E8E93)   // Secondary text
val Separator      = Color(0xFF38383A)   // Dividers

// ── Legacy aliases (used in SummaryScreen & other screens) ─────
val CyanPrimary    = iOSBlue
val OrangeAccent   = iOSOrange
val GreenSuccess   = iOSGreen
val NavyBackground = SystemBlack
val NavySurface    = SystemDark
val NavyCard       = SystemDark
val NavyVariant    = SystemFill
val SlateGray      = SecondaryLabel
val PureWhite      = PrimaryLabel

private val AppColorScheme = darkColorScheme(
    primary              = iOSBlue,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF1A2D4A),
    onPrimaryContainer   = iOSBlue,
    secondary            = iOSOrange,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFF2C1F10),
    onSecondaryContainer = iOSOrange,
    tertiary             = iOSGreen,
    onTertiary           = Color.White,
    background           = SystemBlack,
    onBackground         = PrimaryLabel,
    surface              = SystemDark,
    onSurface            = PrimaryLabel,
    surfaceVariant       = SystemFill,
    onSurfaceVariant     = SecondaryLabel,
    outline              = Separator,
    error                = iOSRed,
    onError              = Color.White
)

@Composable
fun SportAnalyzerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
