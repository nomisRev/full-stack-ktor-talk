package org.jetbrains.demo.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand Palette
val EnglishViolet = Color(0xFF413C58)
val EnglishViolet200 = Color(0xFF1A1823)
val EnglishViolet300 = Color(0xFF272435)
val EnglishViolet400 = Color(0xFF343046)
val EnglishViolet600 = Color(0xFF615983)

val AshGray = Color(0xFFA3C4BC)
val AshGray700 = Color(0xFFC7DBD6)
val AshGray800 = Color(0xFFDAE7E4)

val TeaGreen = Color(0xFFBFD7B5)
val TeaGreen100 = Color(0xFF23341C)

val Cream = Color(0xFFE7EFC5)
val Parchment = Color(0xFFF2E7C9)

private val LightColors: ColorScheme = lightColorScheme(
    primary = EnglishViolet,
    onPrimary = Color.White,
    primaryContainer = EnglishViolet600,
    onPrimaryContainer = Color.White,

    secondary = AshGray,
    onSecondary = Color(0xFF0D0C12),
    secondaryContainer = AshGray700,
    onSecondaryContainer = Color(0xFF0D0C12),

    tertiary = TeaGreen,
    onTertiary = Color(0xFF0D0C12),
    tertiaryContainer = Parchment,
    onTertiaryContainer = EnglishViolet300,

    background = Cream,
    onBackground = EnglishViolet400,

    surface = Parchment,
    onSurface = EnglishViolet400,
    surfaceVariant = AshGray700,
    onSurfaceVariant = EnglishViolet300,

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = EnglishViolet300,
    inverseOnSurface = Cream,
    inverseSurface = EnglishViolet400,
    inversePrimary = AshGray
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = TeaGreen,
    onPrimary = TeaGreen100,
    primaryContainer = EnglishViolet400,
    onPrimaryContainer = AshGray800,

    secondary = AshGray,
    onSecondary = EnglishViolet200,
    secondaryContainer = EnglishViolet300,
    onSecondaryContainer = AshGray800,

    tertiary = AshGray700,
    onTertiary = EnglishViolet200,

    background = EnglishViolet300,
    onBackground = Parchment,

    surface = EnglishViolet400,
    onSurface = Parchment,
    surfaceVariant = EnglishViolet300,
    onSurfaceVariant = AshGray800,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = AshGray,
    inverseOnSurface = EnglishViolet300,
    inverseSurface = Parchment,
    inversePrimary = TeaGreen
)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
