package app.mininote.mininote.ui.servers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.R
import app.mininote.mininote.data.local.db.entity.ServerEntity
import app.mininote.mininote.data.remote.ActiveServerHolder
import app.mininote.mininote.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServersUiState(
    val servers: List<ServerEntity> = emptyList(),
    val activeBaseUrl: String = "",
    val newServerName: String = "",
    val newServerAddress: String = "",
    val addError: String? = null,
)

private data class ServerFormState(
    val name: String = "",
    val address: String = "",
    val error: String? = null,
)

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    activeServerHolder: ActiveServerHolder,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val formState = MutableStateFlow(ServerFormState())

    val uiState: StateFlow<ServersUiState> = combine(
        serverRepository.observeServers(),
        activeServerHolder.flow,
        formState,
    ) { servers, active, form ->
        ServersUiState(
            servers = servers,
            activeBaseUrl = active.baseUrl,
            newServerName = form.name,
            newServerAddress = form.address,
            addError = form.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServersUiState())

    fun onNewNameChange(value: String) = formState.update { it.copy(name = value, error = null) }
    fun onNewAddressChange(value: String) = formState.update { it.copy(address = value, error = null) }

    fun selectServer(server: ServerEntity) {
        viewModelScope.launch { serverRepository.selectServer(server) }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch { serverRepository.deleteServer(server) }
    }

    fun saveNewServer() {
        val form = formState.value
        if (form.name.isBlank() || form.address.isBlank()) {
            formState.update { it.copy(error = context.getString(R.string.servers_error_fill_fields)) }
            return
        }
        viewModelScope.launch {
            serverRepository.addServer(form.name.trim(), form.address.trim())
            formState.value = ServerFormState()
        }
    }
}
