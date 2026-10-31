package ru.upestop.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Палитра веб-версии (tailwind.config.ts). */
object UpeColors {
    val Bg = Color(0xFF0A0D0F)
    val Surface = Color(0xFF141A1D)
    val Surface2 = Color(0xFF1A2125)
    val Border = Color(0xFF2A343A)
    val Text = Color(0xFFECE9E0)
    val TextDim = Color(0xFF98A3A8)
    val TextMute = Color(0xFF5D686D)
    val Cool = Color(0xFF60A5FA)
    val Amber = Color(0xFFFBBF24)
    val Warn = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
    val Safe = Color(0xFF10B981)
}

object UpeText {
    /** Моноширинная метка заглавными с разрядкой. */
    val label = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        lineHeight = 16.sp,
    )
    val mono = TextStyle(fontFamily = FontFamily.Monospace)
}

private val colorScheme = darkColorScheme(
    primary = UpeColors.Cool,
    onPrimary = UpeColors.Bg,
    primaryContainer = UpeColors.Surface2,
    onPrimaryContainer = UpeColors.Text,
    secondary = UpeColors.Amber,
    onSecondary = UpeColors.Bg,
    secondaryContainer = UpeColors.Surface2,
    onSecondaryContainer = UpeColors.Text,
    tertiary = UpeColors.Safe,
    onTertiary = UpeColors.Bg,
    background = UpeColors.Bg,
    onBackground = UpeColors.Text,
    surface = UpeColors.Bg,
    onSurface = UpeColors.Text,
    surfaceVariant = UpeColors.Surface2,
    onSurfaceVariant = UpeColors.TextDim,
    surfaceContainerLowest = UpeColors.Bg,
    surfaceContainerLow = UpeColors.Surface,
    surfaceContainer = UpeColors.Surface,
    surfaceContainerHigh = UpeColors.Surface2,
    surfaceContainerHighest = UpeColors.Surface2,
    outline = UpeColors.Border,
    outlineVariant = UpeColors.Border,
    error = UpeColors.Danger,
    onError = UpeColors.Text,
    inverseSurface = UpeColors.Text,
    inverseOnSurface = UpeColors.Bg,
    inversePrimary = UpeColors.Cool,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

@Composable
fun UpeStopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, shapes = shapes, content = content)
}
