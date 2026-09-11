package app.mininote.mininote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Цвета, которых нет в стандартной M3 ColorScheme, но которые определены в дизайн-токенах
 * (mini-note-design/android/m3.jsx): `success` и золотой цвет "избранного".
 */
data class MiniNoteExtraColors(
    val success: Color,
    val starred: Color,
)

private val LocalMiniNoteExtraColors = staticCompositionLocalOf {
    MiniNoteExtraColors(success = MiniNoteLightColors.success, starred = StarredGold)
}

val MaterialTheme.extraColors: MiniNoteExtraColors
    @Composable get() = LocalMiniNoteExtraColors.current

private val LightColorScheme = lightColorScheme(
    primary = MiniNoteLightColors.primary,
    onPrimary = MiniNoteLightColors.onPrimary,
    primaryContainer = MiniNoteLightColors.primaryContainer,
    onPrimaryContainer = MiniNoteLightColors.onPrimaryContainer,
    secondary = MiniNoteLightColors.secondary,
    onSecondary = MiniNoteLightColors.onSecondary,
    secondaryContainer = MiniNoteLightColors.secondaryContainer,
    onSecondaryContainer = MiniNoteLightColors.onSecondaryContainer,
    error = MiniNoteLightColors.error,
    onError = MiniNoteLightColors.onError,
    errorContainer = MiniNoteLightColors.errorContainer,
    onErrorContainer = MiniNoteLightColors.onErrorContainer,
    background = MiniNoteLightColors.background,
    onBackground = MiniNoteLightColors.onBackground,
    surface = MiniNoteLightColors.surface,
    onSurface = MiniNoteLightColors.onSurface,
    surfaceVariant = MiniNoteLightColors.surfaceVariant,
    onSurfaceVariant = MiniNoteLightColors.onSurfaceVariant,
    outline = MiniNoteLightColors.outline,
    outlineVariant = MiniNoteLightColors.outlineVariant,
    inverseSurface = MiniNoteLightColors.inverseSurface,
    inverseOnSurface = MiniNoteLightColors.inverseOnSurface,
    inversePrimary = MiniNoteLightColors.inversePrimary,
    surfaceContainerLowest = MiniNoteLightColors.surfaceContainerLowest,
    surfaceContainerLow = MiniNoteLightColors.surfaceContainerLow,
    surfaceContainer = MiniNoteLightColors.surfaceContainer,
    surfaceContainerHigh = MiniNoteLightColors.surfaceContainerHigh,
    surfaceContainerHighest = MiniNoteLightColors.surfaceContainerHighest,
    scrim = MiniNoteLightColors.scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = MiniNoteDarkColors.primary,
    onPrimary = MiniNoteDarkColors.onPrimary,
    primaryContainer = MiniNoteDarkColors.primaryContainer,
    onPrimaryContainer = MiniNoteDarkColors.onPrimaryContainer,
    secondary = MiniNoteDarkColors.secondary,
    onSecondary = MiniNoteDarkColors.onSecondary,
    secondaryContainer = MiniNoteDarkColors.secondaryContainer,
    onSecondaryContainer = MiniNoteDarkColors.onSecondaryContainer,
    error = MiniNoteDarkColors.error,
    onError = MiniNoteDarkColors.onError,
    errorContainer = MiniNoteDarkColors.errorContainer,
    onErrorContainer = MiniNoteDarkColors.onErrorContainer,
    background = MiniNoteDarkColors.background,
    onBackground = MiniNoteDarkColors.onBackground,
    surface = MiniNoteDarkColors.surface,
    onSurface = MiniNoteDarkColors.onSurface,
    surfaceVariant = MiniNoteDarkColors.surfaceVariant,
    onSurfaceVariant = MiniNoteDarkColors.onSurfaceVariant,
    outline = MiniNoteDarkColors.outline,
    outlineVariant = MiniNoteDarkColors.outlineVariant,
    inverseSurface = MiniNoteDarkColors.inverseSurface,
    inverseOnSurface = MiniNoteDarkColors.inverseOnSurface,
    inversePrimary = MiniNoteDarkColors.inversePrimary,
    surfaceContainerLowest = MiniNoteDarkColors.surfaceContainerLowest,
    surfaceContainerLow = MiniNoteDarkColors.surfaceContainerLow,
    surfaceContainer = MiniNoteDarkColors.surfaceContainer,
    surfaceContainerHigh = MiniNoteDarkColors.surfaceContainerHigh,
    surfaceContainerHighest = MiniNoteDarkColors.surfaceContainerHighest,
    scrim = MiniNoteDarkColors.scrim,
)

/**
 * @param darkTheme источник истины — системная тема по умолчанию, но экран "Профиль" (Фаза 4)
 * переопределяет её persisted-настройкой пользователя ("Тёмная тема").
 */
@Composable
fun MiniNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) {
        MiniNoteExtraColors(success = MiniNoteDarkColors.success, starred = StarredGold)
    } else {
        MiniNoteExtraColors(success = MiniNoteLightColors.success, starred = StarredGold)
    }

    CompositionLocalProvider(LocalMiniNoteExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MiniNoteTypography,
            content = content,
        )
    }
}
