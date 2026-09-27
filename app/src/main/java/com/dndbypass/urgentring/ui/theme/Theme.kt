package com.dndbypass.urgentring.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UrgentRed = Color(0xFFD32F2F)
private val UrgentRedDark = Color(0xFFEF5350)

private val LightColors = lightColorScheme(primary = UrgentRed)
private val DarkColors = darkColorScheme(primary = UrgentRedDark)

@Composable
fun UrgentRingTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
