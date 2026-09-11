package app.mininote.mininote.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.R
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.repository.AuthRepository
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

enum class ForgotNavEvent { ToCode, ToNew, ToDone }

data class ForgotUiState(
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val emailStepError: String? = null,
    val newPasswordStepError: String? = null,
)

/**
 * Общее состояние для всех 4 экранов "Забыли пароль" (scoped на вложенный nav-граф).
 * Бэкенд не даёт отдельного эндпоинта проверки кода (см. project memory) — код реально
 * проверяется только на финальном вызове confirmPasswordReset, поэтому шаг ForgotCode
 * валидирует только формат (6 цифр), а ошибка неверного кода всплывает на ForgotNew.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotUiState())
    val uiState: StateFlow<ForgotUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<ForgotNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailStepError = null) }
    fun onCodeChange(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(code = value) }
        }
    }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, newPasswordStepError = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, newPasswordStepError = null) }

    fun submitEmail() {
        val email = _uiState.value.email
        if (!email.contains("@")) {
            _uiState.update { it.copy(emailStepError = context.getString(R.string.error_invalid_email)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emailStepError = null) }
            // Сначала проверяем существование email через /users/check — без этого /users/forgot
            // на незарегистрированный email отдаёт сырую ошибку драйвера бэкенда ("mongo: no
            // documents in result"), а не человекочитаемое сообщение. Предугадываем этот случай
            // тем же дешёвым методом, что уже используется в Login/Signup для email на blur.
            when (val checkResult = authRepository.checkEmailExists(email)) {
                is ApiResult.Success -> {
                    if (!checkResult.data) {
                        _uiState.update {
                            it.copy(isLoading = false, emailStepError = context.getString(R.string.forgot_email_not_found))
                        }
                        return@launch
                    }
                }
                is ApiResult.Failure -> Unit // проверка недоступна (например, офлайн) — не блокируем, пробуем основной вызов
            }
            when (val result = authRepository.requestPasswordReset(email)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navEvents.send(ForgotNavEvent.ToCode)
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, emailStepError = result.message) }
            }
        }
    }

    fun submitCode() {
        if (_uiState.value.code.length == 6) {
            viewModelScope.launch { _navEvents.send(ForgotNavEvent.ToNew) }
        }
    }

    fun submitNewPassword() {
        val state = _uiState.value
        if (state.password.length < 8) {
            _uiState.update { it.copy(newPasswordStepError = context.getString(R.string.signup_password_hint)) }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(newPasswordStepError = context.getString(R.string.profile_password_error_mismatch)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, newPasswordStepError = null) }
            when (
                val result = authRepository.confirmPasswordReset(state.email, state.code, state.password)
            ) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navEvents.send(ForgotNavEvent.ToDone)
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, newPasswordStepError = result.message) }
            }
        }
    }
}
