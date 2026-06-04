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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF4F46E5),     // Indigo
    secondary = Color(0xFF7C3AED),   // Violet
    tertiary = Color(0xFF06B6D4),    // Cyan
    background = Color(0xFF0F172A),  // Deep Slate
    surface = Color(0xFF1F2937),     // Dark Card Gray
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFE5E7EB)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
