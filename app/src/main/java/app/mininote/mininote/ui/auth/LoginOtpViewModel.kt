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

private const val OTP_CODE_LENGTH = 6

data class LoginOtpUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** mfa-токен приходит от [LoginScreen] как обычный параметр композабла (не через Hilt/route
 * injection — это чисто транзитное значение для одного вызова [AuthRepository.verifyOtp],
 * держать его в состоянии ViewModel незачем), поэтому у [onSubmit] его нет в конструкторе. */
@HiltViewModel
class LoginOtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginOtpUiState())
    val uiState: StateFlow<LoginOtpUiState> = _uiState.asStateFlow()

    private val _loginSuccess = Channel<Unit>(Channel.BUFFERED)
    val loginSuccess = _loginSuccess.receiveAsFlow()

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(OTP_CODE_LENGTH)
        _uiState.update { it.copy(code = digits, errorMessage = null) }
    }

    fun onSubmit(mfaToken: String) {
        val code = _uiState.value.code
        if (code.length != OTP_CODE_LENGTH) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.login_otp_error_invalid_code)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.verifyOtp(mfaToken, code)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _loginSuccess.send(Unit)
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
