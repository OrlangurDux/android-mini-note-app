package app.mininote.mininote.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.R
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.repository.AuthRepository
import app.mininote.mininote.data.repository.LoginOutcome
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailNotFoundHint: Boolean = false,
)

/** Данные для перехода на экран ввода кода второго фактора — mfa-токен нужно передать в
 * [AuthRepository.verifyOtp] as-is, ожидающая сторона отвечает лишь за отображение обратного отсчёта. */
data class MfaChallenge(val mfaToken: String, val expiresInSeconds: Long)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginSuccess = Channel<Unit>(Channel.BUFFERED)
    val loginSuccess = _loginSuccess.receiveAsFlow()

    private val _mfaRequired = Channel<MfaChallenge>(Channel.BUFFERED)
    val mfaRequired = _mfaRequired.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, emailNotFoundHint = false) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    /** Проверка email на blur — если аккаунта нет, подсказываем перейти к регистрации. */
    fun onEmailFocusLost() {
        val email = _uiState.value.email
        if (!email.contains("@")) return
        viewModelScope.launch {
            when (val result = authRepository.checkEmailExists(email)) {
                is ApiResult.Success -> _uiState.update { it.copy(emailNotFoundHint = !result.data) }
                is ApiResult.Failure -> Unit // некритичная подсказка — молча игнорируем сетевые ошибки
            }
        }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (!state.email.contains("@")) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.error_invalid_email)) }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.error_empty_password)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(state.email, state.password)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    when (val outcome = result.data) {
                        LoginOutcome.Success -> _loginSuccess.send(Unit)
                        is LoginOutcome.MfaRequired ->
                            _mfaRequired.send(MfaChallenge(outcome.mfaToken, outcome.expiresInSeconds))
                    }
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
