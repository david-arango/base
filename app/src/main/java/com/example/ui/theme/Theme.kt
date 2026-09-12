package com.example.ui.theme

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
    primary = CalmInversePrimary,
    onPrimary = CalmPrimary,
    primaryContainer = CalmPrimaryContainer,
    onPrimaryContainer = CalmOnPrimaryContainer,
    secondary = CalmSecondaryContainer,
    onSecondary = CalmOnSecondaryContainer,
    secondaryContainer = CalmSecondary,
    onSecondaryContainer = CalmOnSecondary,
    background = CalmInverseSurface,
    onBackground = CalmInverseOnSurface,
    surface = CalmInverseSurface,
    onSurface = CalmInverseOnSurface,
    surfaceVariant = CalmPrimaryContainer,
    onSurfaceVariant = CalmOutlineVariant,
    surfaceContainerLowest = Color(0xFF1E201F),
    surfaceContainerLow = Color(0xFF252827),
    surfaceContainer = Color(0xFF2C302E),
    surfaceContainerHigh = Color(0xFF353A37),
    surfaceContainerHighest = Color(0xFF3F4441),
    outline = CalmOutline,
    outlineVariant = CalmOutlineVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For Base, we preserve the distinctive Calm Organic palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> CalmDarkColorScheme
      else -> CalmLightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
