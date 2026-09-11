package app.mininote.mininote

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.mininote.mininote.data.local.prefs.SettingsDataStore
import app.mininote.mininote.data.local.prefs.ThemeMode
import app.mininote.mininote.data.repository.AuthRepository
import app.mininote.mininote.data.repository.ServerRepository
import app.mininote.mininote.ui.navigation.MiniNoteNavHost
import app.mininote.mininote.ui.navigation.Route
import app.mininote.mininote.ui.theme.MiniNoteTheme
import app.mininote.mininote.util.AppLocale
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var serverRepository: ServerRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MiniNoteTheme(darkTheme = darkTheme) {
                var startDestination by remember { mutableStateOf<Route?>(null) }

                LaunchedEffect(Unit) {
                    serverRepository.ensureInitialized()
                    startDestination = if (authRepository.isLoggedIn()) Route.Home else Route.Login
                }

                val destination = startDestination
                if (destination == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    MiniNoteNavHost(startDestination = destination)
                }
            }
        }
    }
}
