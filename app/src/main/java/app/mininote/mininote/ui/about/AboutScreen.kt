@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.mininote.mininote.R
import app.mininote.mininote.ui.components.ErrorBanner
import app.mininote.mininote.ui.components.HintBanner

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            uiState.version?.let {
                Text(text = stringResource(R.string.about_version, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            uiState.author?.let {
                Text(text = stringResource(R.string.about_author, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            uiState.contact?.let {
                Text(text = stringResource(R.string.about_contact, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(text = stringResource(R.string.about_feedback_title), style = MaterialTheme.typography.titleMedium)

            if (uiState.feedbackSent) {
                HintBanner(stringResource(R.string.about_feedback_sent))
            } else {
                uiState.feedbackError?.let { ErrorBanner(it) }
                OutlinedTextField(
                    value = uiState.feedbackName,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.signup_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.feedbackContact,
                    onValueChange = viewModel::onContactChange,
                    label = { Text(stringResource(R.string.about_feedback_contact_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.feedbackMessage,
                    onValueChange = viewModel::onMessageChange,
                    label = { Text(stringResource(R.string.about_feedback_message_label)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = viewModel::onSendFeedback,
                    enabled = !uiState.isSending,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.about_feedback_send))
                }
            }
        }
    }
}
