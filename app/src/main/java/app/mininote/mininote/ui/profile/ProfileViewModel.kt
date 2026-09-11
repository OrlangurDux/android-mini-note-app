package app.mininote.mininote.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.R
import app.mininote.mininote.data.local.prefs.SettingsDataStore
import app.mininote.mininote.data.local.prefs.ThemeMode
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.repository.AuthRepository
import app.mininote.mininote.util.AppLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Языковой тег для [AppLocale]; null — "как в системе". */
enum class LanguageOption(val tag: String?) {
    SYSTEM(null),
    RUSSIAN("ru"),
    ENGLISH("en"),
}

private fun currentLanguageOption(context: Context): LanguageOption {
    val tag = AppLocale.getTag(context)
    return LanguageOption.entries.firstOrNull { it.tag == tag } ?: LanguageOption.SYSTEM
}

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageOption: LanguageOption = LanguageOption.SYSTEM,
    val showPasswordDialog: Boolean = false,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val isSavingPassword: Boolean = false,
    val passwordChangedMessage: String? = null,
    val isTfaEnabled: Boolean = false,
    val isTfaBusy: Boolean = false,
    /** Не-null сразу после успешного включения 2FA — otpauth:// URL для диалога с QR-кодом.
     * Источник истины для самого состояния переключателя — [isTfaEnabled] (из `GET /users/profile`,
     * см. `UserProfileDto.is_2fa`), это поле лишь одноразовая витрина для setup-диалога. */
    val tfaSetupUrl: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(languageOption = currentLanguageOption(context)))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOut = _loggedOut.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        loadProfile()
    }

    /**
     * AppCompatDelegate.setApplicationLocales() оказался no-op в этом приложении: он резолвит
     * LocaleManager через список активных AppCompatDelegate-инстансов, а MainActivity — сознательно
     * обычный ComponentActivity без единой AppCompatActivity в графе, поэтому список всегда пуст
     * (подтверждено эмпирически: `adb shell cmd locale get-app-locales` оставался пустым после
     * вызова). [AppLocale] — собственный механизм (SharedPreferences + Activity.attachBaseContext),
     * работающий одинаково на всех API 31+. Немедленный recreate() вызывающая сторона (ProfileScreen)
     * делает сама сразу после выбора.
     */
    fun onLanguageSelected(option: LanguageOption) {
        AppLocale.setTag(context, option.tag)
        _uiState.update { it.copy(languageOption = option) }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getProfile()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = result.data.name,
                        email = result.data.email,
                        avatarUrl = result.data.avatar,
                        isTfaEnabled = result.data.is_2fa,
                    )
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun onToggleDarkTheme(dark: Boolean) {
        viewModelScope.launch { settingsDataStore.setThemeMode(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) }
    }

    /** [systemDark] — снимок isSystemInDarkTheme() на момент клика с UI (ViewModel не Compose-aware);
     * при выключении системного режима фиксируем текущий видимый на экране режим как явный выбор,
     * а не молча откатываем на LIGHT. */
    fun onSetSystemTheme(enabled: Boolean, systemDark: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(
                if (enabled) ThemeMode.SYSTEM else if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT,
            )
        }
    }

    fun onRequestPasswordChange() {
        _uiState.update { it.copy(showPasswordDialog = true, newPassword = "", confirmPassword = "", passwordError = null) }
    }

    fun onDismissPasswordChange() = _uiState.update { it.copy(showPasswordDialog = false) }

    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value, passwordError = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, passwordError = null) }

    fun onSavePassword() {
        val state = _uiState.value
        if (state.newPassword.isBlank()) {
            _uiState.update { it.copy(passwordError = context.getString(R.string.profile_password_error_empty)) }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(passwordError = context.getString(R.string.profile_password_error_mismatch)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPassword = true, passwordError = null) }
            when (val result = authRepository.changePassword(state.newPassword)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isSavingPassword = false,
                        showPasswordDialog = false,
                        passwordChangedMessage = context.getString(R.string.profile_password_changed),
                    )
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isSavingPassword = false, passwordError = result.message) }
            }
        }
    }

    fun onDismissPasswordChangedMessage() = _uiState.update { it.copy(passwordChangedMessage = null) }

    /** Включение показывает setup-диалог с QR (см. [tfaSetupUrl]); выключение бэкенд принимает без
     * подтверждения кодом (проверено эмпирически — см. project memory), поэтому UI-подтверждение
     * здесь не требуется отдельным диалогом, сам Switch и есть подтверждение действия. */
    fun onToggleTfa(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTfaBusy = true, errorMessage = null) }
            if (enabled) {
                when (val result = authRepository.enableTfa()) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(isTfaBusy = false, isTfaEnabled = true, tfaSetupUrl = result.data)
                    }
                    is ApiResult.Failure -> _uiState.update { it.copy(isTfaBusy = false, errorMessage = result.message) }
                }
            } else {
                when (val result = authRepository.disableTfa()) {
                    is ApiResult.Success -> _uiState.update { it.copy(isTfaBusy = false, isTfaEnabled = false) }
                    is ApiResult.Failure -> _uiState.update { it.copy(isTfaBusy = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun onDismissTfaSetup() = _uiState.update { it.copy(tfaSetupUrl = null) }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _loggedOut.send(Unit)
        }
    }
}
