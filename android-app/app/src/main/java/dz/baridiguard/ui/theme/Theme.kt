package dz.baridiguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    background = GuardBgDark,
    surface = GuardSurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White,
    error = DangerRed
)

@Composable
fun BaridiGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
