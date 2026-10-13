package com.arango.base.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CalmLightColorScheme =
  lightColorScheme(
    primary = CalmPrimary,
    onPrimary = CalmOnPrimary,
    primaryContainer = CalmPrimaryContainer,
    onPrimaryContainer = CalmOnPrimaryContainer,
    inversePrimary = CalmInversePrimary,
    secondary = CalmSecondary,
    onSecondary = CalmOnSecondary,
    secondaryContainer = CalmSecondaryContainer,
    onSecondaryContainer = CalmOnSecondaryContainer,
    tertiary = CalmTertiary,
    onTertiary = CalmOnTertiary,
    tertiaryContainer = CalmTertiaryContainer,
    onTertiaryContainer = CalmOnTertiaryContainer,
    error = CalmError,
    onError = CalmOnError,
    errorContainer = CalmErrorContainer,
    onErrorContainer = CalmOnErrorContainer,
    background = CalmBackground,
    onBackground = CalmOnBackground,
    surface = CalmSurface,
    onSurface = CalmOnSurface,
    surfaceVariant = CalmSurfaceContainerHighest,
    onSurfaceVariant = CalmOnSurfaceVariant,
    surfaceContainerLowest = CalmSurfaceContainerLowest,
    surfaceContainerLow = CalmSurfaceContainerLow,
    surfaceContainer = CalmSurfaceContainer,
    surfaceContainerHigh = CalmSurfaceContainerHigh,
    surfaceContainerHighest = CalmSurfaceContainerHighest,
    surfaceDim = CalmSurfaceDim,
    surfaceBright = CalmSurfaceBright,
    outline = CalmOutline,
    outlineVariant = CalmOutlineVariant,
    inverseSurface = CalmInverseSurface,
    inverseOnSurface = CalmInverseOnSurface,
    surfaceTint = CalmSurfaceTint,
  )

private val CalmDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFC3E9C5),
    onPrimary = Color(0xFF0F3816),
    primaryContainer = Color(0xFF282D2B),
    onPrimaryContainer = Color(0xFFE2E6E4),
    inversePrimary = CalmPrimary,
    secondary = Color(0xFF98D49E),
    onSecondary = Color(0xFF0D3815),
    secondaryContainer = Color(0xFF234428),
    onSecondaryContainer = Color(0xFFC3E9C5),
    tertiary = Color(0xFFF0B8AE),
    onTertiary = Color(0xFF522119),
    tertiaryContainer = Color(0xFF522119),
    onTertiaryContainer = Color(0xFFF0B8AE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141716),
    onBackground = Color(0xFFE2E6E4),
    surface = Color(0xFF181C1B),
    onSurface = Color(0xFFE2E6E4),
    surfaceVariant = Color(0xFF262A29),
    onSurfaceVariant = Color(0xFFB8BCBA),
    surfaceContainerLowest = Color(0xFF121514),
    surfaceContainerLow = Color(0xFF1A1D1C),
    surfaceContainer = Color(0xFF222624),
    surfaceContainerHigh = Color(0xFF2B2F2D),
    surfaceContainerHighest = Color(0xFF353A38),
    surfaceDim = Color(0xFF121514),
    surfaceBright = Color(0xFF3C413F),
    outline = Color(0xFF8A908D),
    outlineVariant = Color(0xFF434846),
    inverseSurface = CalmSurface,
    inverseOnSurface = CalmOnSurface,
    surfaceTint = Color(0xFF98D49E)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // App is light mode only per user design specification
  val colorScheme = CalmLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
