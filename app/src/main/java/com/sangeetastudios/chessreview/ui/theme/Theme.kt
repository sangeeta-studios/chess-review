package com.sangeetastudios.chessreview.ui.theme

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
import com.sangeetastudios.chessreview.analysis.MoveClass

object BoardColors {
    val light = Color(0xFFEBECD0)
    val dark = Color(0xFF739552)
    val lastMoveLight = Color(0xFFF5F682)
    val lastMoveDark = Color(0xFFB9CA43)
    val check = Color(0xCCE53935)
    val bestArrow = Color(0xCC81B64C)
    val playedArrow = Color(0x99F7C631)
}

fun MoveClass.color(): Color = when (this) {
    MoveClass.BEST -> Color(0xFF81B64C)
    MoveClass.EXCELLENT -> Color(0xFF5FA046)
    MoveClass.GOOD -> Color(0xFF77915F)
    MoveClass.INACCURACY -> Color(0xFFF7C631)
    MoveClass.MISTAKE -> Color(0xFFFFA459)
    MoveClass.BLUNDER -> Color(0xFFFA412D)
    MoveClass.FORCED -> Color(0xFF97AF8B)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF4E7837),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEDB4),
    onPrimaryContainer = Color(0xFF0E2000),
    secondary = Color(0xFF55624C),
    background = Color(0xFFF8FAF0),
    surface = Color(0xFFF8FAF0),
    surfaceVariant = Color(0xFFE0E4D6),
    onSurfaceVariant = Color(0xFF44483E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5EA),
    surfaceContainer = Color(0xFFECEFE4),
    surfaceContainerHigh = Color(0xFFE6E9DE),
    surfaceContainerHighest = Color(0xFFE1E4D9),
    secondaryContainer = Color(0xFFD8E7CB),
    onSecondaryContainer = Color(0xFF131F0D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81B64C),
    onPrimary = Color(0xFF1A3700),
    primaryContainer = Color(0xFF365E22),
    onPrimaryContainer = Color(0xFFCDEDB4),
    secondary = Color(0xFFBCCBB0),
    background = Color(0xFF262421),
    surface = Color(0xFF262421),
    surfaceContainer = Color(0xFF302E2B),
    surfaceContainerHigh = Color(0xFF3A3835),
    surfaceContainerLow = Color(0xFF2B2926),
    surfaceContainerHighest = Color(0xFF45433F),
    secondaryContainer = Color(0xFF3E4A36),
    onSecondaryContainer = Color(0xFFD8E7CB),
)

@Composable
fun ChessReviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
