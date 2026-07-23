package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val RetroColorScheme = darkColorScheme(
  primary = RetroWhite,
  onPrimary = RetroBlack,
  secondary = RetroLightGray,
  onSecondary = RetroBlack,
  background = RetroBlack,
  onBackground = RetroWhite,
  surface = RetroBlack,
  onSurface = RetroWhite,
  tertiary = RetroGray
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for retro screen vibe
  dynamicColor: Boolean = false, // Disable dynamic colors to keep it authentically retro black-and-white
  content: @Composable () -> Unit,
) {
  val colorScheme = RetroColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
