package app.mininote.mininote.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "mininote_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SELECTED_SERVER_ID = longPreferencesKey("selected_server_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val selectedServerId: Flow<Long?> = context.dataStore.data.map { it[Keys.SELECTED_SERVER_ID] }

    suspend fun setSelectedServerId(id: Long) {
        context.dataStore.edit { it[Keys.SELECTED_SERVER_ID] = id }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}
