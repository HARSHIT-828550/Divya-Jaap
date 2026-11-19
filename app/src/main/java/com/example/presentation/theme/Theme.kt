package com.example.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DivyaDarkColorScheme = darkColorScheme(
    primary = SacredGold,
    onPrimary = NavyVoid,
    primaryContainer = NavyElevated,
    onPrimaryContainer = SacredGoldBright,
    secondary = SaffronAmber,
    onSecondary = NavyVoid,
    secondaryContainer = NavyCardHighlight,
    onSecondaryContainer = IvoryText,
    tertiary = RudrakshaLight,
    onTertiary = IvoryText,
    background = NavyVoid,
    onBackground = IvoryText,
    surface = NavySanctum,
    onSurface = IvoryText,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = SandText,
    outline = NavyBorder,
    outlineVariant = NavyBorder.copy(alpha = 0.5f),
    error = CoralMistake,
    onError = NavyVoid
)

val DivyaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DivyaJaapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DivyaDarkColorScheme,
        typography = DivyaTypography,
        shapes = DivyaShapes,
        content = content
    )
}
