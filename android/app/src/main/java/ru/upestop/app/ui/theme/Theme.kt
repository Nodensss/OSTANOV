package ru.upestop.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Bg = Color(0xFF0A0D0F)
val Surface = Color(0xFF141A1D)
val Surface2 = Color(0xFF1A2125)
val Border = Color(0xFF2A343A)
val Ink = Color(0xFFECE9E0)
val Dim = Color(0xFF98A3A8)
val Mute = Color(0xFF5D686D)
val Cool = Color(0xFF60A5FA)
val Amber = Color(0xFFFBBF24)
val Warn = Color(0xFFF59E0B)
val Danger = Color(0xFFEF4444)
val Safe = Color(0xFF10B981)

@Composable
fun UpeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Cool, onPrimary = Bg, background = Bg,
            onBackground = Ink, surface = Surface, onSurface = Ink, surfaceVariant = Surface2,
            onSurfaceVariant = Dim, outline = Border, error = Danger),
        shapes = Shapes(extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(4.dp), large = RoundedCornerShape(4.dp), extraLarge = RoundedCornerShape(4.dp)),
        content = content
    )
}
