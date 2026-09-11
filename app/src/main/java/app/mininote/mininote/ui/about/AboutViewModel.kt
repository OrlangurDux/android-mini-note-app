package app.mininote.mininote.ui.about

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.R
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.repository.SupportRepository
import app.mininote.mininote.data.repository.VersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AboutUiState(
    val version: String? = null,
    val author: String? = null,
    val contact: String? = null,
    val feedbackName: String = "",
    val feedbackContact: String = "",
    val feedbackMessage: String = "",
    val isSending: Boolean = false,
    val feedbackError: String? = null,
    val feedbackSent: Boolean = false,
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val versionRepository: VersionRepository,
    private val supportRepository: SupportRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Версия — необязательное украшение экрана, при офлайне/ошибке просто не показываем её.
            when (val result = versionRepository.getVersion()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(version = result.data.version, author = result.data.author, contact = result.data.contact)
                }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(feedbackName = value, feedbackError = null) }
    fun onContactChange(value: String) = _uiState.update { it.copy(feedbackContact = value, feedbackError = null) }
    fun onMessageChange(value: String) = _uiState.update { it.copy(feedbackMessage = value, feedbackError = null) }

    fun onSendFeedback() {
        val state = _uiState.value
        if (state.feedbackName.isBlank() || state.feedbackContact.isBlank()) {
            _uiState.update { it.copy(feedbackError = context.getString(R.string.about_feedback_error_fill_fields)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, feedbackError = null) }
            when (
                val result = supportRepository.sendFeedback(
                    name = state.feedbackName.trim(),
                    contact = state.feedbackContact.trim(),
                    message = state.feedbackMessage.trim(),
                )
            ) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isSending = false, feedbackSent = true, feedbackName = "", feedbackContact = "", feedbackMessage = "")
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isSending = false, feedbackError = result.message) }
            }
        }
    }

    fun onDismissFeedbackSent() = _uiState.update { it.copy(feedbackSent = false) }
}
