package ru.sakhabooks.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BirchBrown,
    onPrimary = WarmCream,
    primaryContainer = BirchBrownLight,
    onPrimaryContainer = DeepInk,
    secondary = Amber,
    onSecondary = DeepInk,
    secondaryContainer = AmberLight,
    onSecondaryContainer = DeepInk,
    tertiary = NorthBlue,
    tertiaryContainer = NorthBlueLight,
    background = WarmCream,
    onBackground = DeepInk,
    surface = WarmCream,
    onSurface = DeepInk,
    surfaceContainer = WarmSurface,
    error = Ochre,
)

private val DarkColors = darkColorScheme(
    primary = BirchBrownLight,
    onPrimary = DeepInk,
    primaryContainer = BirchBrown,
    onPrimaryContainer = NightText,
    secondary = AmberLight,
    onSecondary = DeepInk,
    secondaryContainer = Amber,
    tertiary = NorthBlueLight,
    background = NightBg,
    onBackground = NightText,
    surface = NightBg,
    onSurface = NightText,
    surfaceContainer = NightSurface,
)

@Composable
fun SakhaBooksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Цвета обоев (dynamic color) намеренно не используем — бережём фирменную палитру.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
