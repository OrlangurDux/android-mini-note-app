package app.mininote.mininote.ui.theme

import androidx.compose.ui.graphics.Color

// Портировано 1:1 из mini-note-design/android/m3.jsx (M3_LIGHT / M3_DARK).
// seed: brand blue #1976d2

object MiniNoteLightColors {
    val primary = Color(0xFF1B61B4)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFD6E3FF)
    val onPrimaryContainer = Color(0xFF001C3B)
    val secondary = Color(0xFF565F71)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFDAE2F9)
    val onSecondaryContainer = Color(0xFF131C2B)
    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF410002)
    val success = Color(0xFF1E8E3E)
    val background = Color(0xFFF8F9FF)
    val onBackground = Color(0xFF191C20)
    val surface = Color(0xFFF8F9FF)
    val onSurface = Color(0xFF191C20)
    val surfaceVariant = Color(0xFFE1E2EC)
    val onSurfaceVariant = Color(0xFF44474F)
    val outline = Color(0xFF74777F)
    val outlineVariant = Color(0xFFC4C6D0)
    val inverseSurface = Color(0xFF2E3036)
    val inverseOnSurface = Color(0xFFF0F0F7)
    val inversePrimary = Color(0xFFA9C7FF)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFF2F3FA)
    val surfaceContainer = Color(0xFFECEDF4)
    val surfaceContainerHigh = Color(0xFFE6E7EE)
    val surfaceContainerHighest = Color(0xFFE1E2E9)
    val scrim = Color(0x66000000) // rgba(0,0,0,.4)
}

object MiniNoteDarkColors {
    val primary = Color(0xFFA9C7FF)
    val onPrimary = Color(0xFF00315E)
    val primaryContainer = Color(0xFF14477F)
    val onPrimaryContainer = Color(0xFFD6E3FF)
    val secondary = Color(0xFFBEC6DC)
    val onSecondary = Color(0xFF283141)
    val secondaryContainer = Color(0xFF3E4759)
    val onSecondaryContainer = Color(0xFFDAE2F9)
    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
    val success = Color(0xFF8FDB9C)
    val background = Color(0xFF111318)
    val onBackground = Color(0xFFE2E2E9)
    val surface = Color(0xFF111318)
    val onSurface = Color(0xFFE2E2E9)
    val surfaceVariant = Color(0xFF44474F)
    val onSurfaceVariant = Color(0xFFC4C6D0)
    val outline = Color(0xFF8E9099)
    val outlineVariant = Color(0xFF44474F)
    val inverseSurface = Color(0xFFE2E2E9)
    val inverseOnSurface = Color(0xFF2E3036)
    val inversePrimary = Color(0xFF1976D2)
    val surfaceContainerLowest = Color(0xFF0C0E13)
    val surfaceContainerLow = Color(0xFF191C20)
    val surfaceContainer = Color(0xFF1D2024)
    val surfaceContainerHigh = Color(0xFF282A2F)
    val surfaceContainerHighest = Color(0xFF33353A)
    val scrim = Color(0x80000000) // rgba(0,0,0,.5)
}

// Не входит в M3 ColorScheme — доменный цвет "успеха" (M3_LIGHT/DARK.success), используем
// как отдельный токен через LocalMiniNoteExtraColors (см. Theme.kt).
val StarredGold = Color(0xFFF5A623)
