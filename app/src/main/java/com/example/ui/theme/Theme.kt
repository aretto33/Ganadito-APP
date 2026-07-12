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

private val DarkColorScheme =
  darkColorScheme(
    primary = GanaditoPrimaryBrown,
    secondary = GanaditoMediumBrown,
    tertiary = GanaditoDarkBrown,
    background = Color(0xFF1B120F),
    surface = Color(0xFF2B1D19),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GanaditoBackground,
    onSurface = GanaditoBackground
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GanaditoPrimaryBrown,
    secondary = GanaditoMediumBrown,
    tertiary = GanaditoDarkBrown,
    background = GanaditoBackground,
    surface = GanaditoSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GanaditoDarkBrown,
    onSurface = GanaditoDarkBrown
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  // Setting to false by default to prefer the Ganadito brown palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
