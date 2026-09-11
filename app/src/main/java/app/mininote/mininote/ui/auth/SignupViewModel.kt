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

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailTaken: Boolean = false,
)

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _signupSuccess = Channel<String>(Channel.BUFFERED) // передаёт email для SignupSentScreen
    val signupSuccess = _signupSuccess.receiveAsFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null, emailTaken = false) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    fun onEmailFocusLost() {
        val email = _uiState.value.email
        if (!email.contains("@")) return
        viewModelScope.launch {
            when (val result = authRepository.checkEmailExists(email)) {
                is ApiResult.Success -> if (result.data) _uiState.update {
                    it.copy(emailTaken = true, errorMessage = context.getString(R.string.signup_error_email_taken))
                }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.email.contains("@")) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.error_invalid_email)) }
            return
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.signup_password_hint)) }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.profile_password_error_mismatch)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signUp(state.name, state.email, state.password)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _signupSuccess.send(state.email)
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
