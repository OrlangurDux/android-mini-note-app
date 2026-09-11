@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.servers

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.mininote.mininote.R
import app.mininote.mininote.data.local.db.entity.ServerEntity
import app.mininote.mininote.ui.components.ErrorBanner

@Composable
fun ServersScreen(
    onBack: () -> Unit,
    viewModel: ServersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        // Клавиатура иначе перекрывает форму добавления сервера снизу экрана (см. тот же
        // паттерн в NoteDetailScreen).
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.servers_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Тап по пустому месту (не по самим полям — они перехватывают событие раньше)
                // закрывает клавиатуру и снимает фокус.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    })
                },
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.servers, key = { it.id }) { server ->
                    ServerRow(
                        server = server,
                        selected = server.baseUrl == uiState.activeBaseUrl,
                        onSelect = { viewModel.selectServer(server) },
                        onDelete = { viewModel.deleteServer(server) },
                    )
                    HorizontalDivider()
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(R.string.servers_add_title), style = MaterialTheme.typography.titleMedium)
                uiState.addError?.let { ErrorBanner(it) }
                OutlinedTextField(
                    value = uiState.newServerName,
                    onValueChange = viewModel::onNewNameChange,
                    label = { Text(stringResource(R.string.servers_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.newServerAddress,
                    onValueChange = viewModel::onNewAddressChange,
                    label = { Text(stringResource(R.string.servers_address_label)) },
                    placeholder = { Text(stringResource(R.string.servers_address_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = viewModel::saveNewServer, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.profile_save))
                }
            }
        }
    }
}

@Composable
private fun ServerRow(
    server: ServerEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = if (server.isDefault) stringResource(R.string.servers_default_suffix, server.name) else server.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(text = server.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!server.isDefault) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
