package com.friction.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand Colors ─────────────────────────────────────────────────────────────

object FrictionColors {
    val Background   = Color(0xFF0A0A0A)
    val Surface      = Color(0xFF111111)
    val Surface2     = Color(0xFF1A1A1A)
    val Border       = Color(0xFF222222)
    val Accent       = Color(0xFFB5FF47)  // neon green
    val AccentDim    = Color(0x1FB5FF47)
    val AccentGlow   = Color(0x4DB5FF47)
    val OnBackground = Color(0xFFF0F0F0)
    val Muted        = Color(0xFF666666)
    val Danger       = Color(0xFFFF4D4D)
}

private val FrictionColorScheme = darkColorScheme(
    primary         = FrictionColors.Accent,
    onPrimary       = Color.Black,
    background      = FrictionColors.Background,
    onBackground    = FrictionColors.OnBackground,
    surface         = FrictionColors.Surface,
    onSurface       = FrictionColors.OnBackground,
    surfaceVariant  = FrictionColors.Surface2,
    outline         = FrictionColors.Border
)

@Composable
fun FrictionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FrictionColorScheme,
        content = content
    )
}
