@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.mininote.mininote.R
import app.mininote.mininote.ui.components.ErrorBanner
import kotlinx.coroutines.delay

/**
 * Второй шаг логина при включённой 2FA (см. AuthRepository.LoginOutcome.MfaRequired). [mfaToken]
 * приходит из навигационного маршрута как обычный параметр — короткоживущее одноразовое значение,
 * которому не место в состоянии ViewModel/SavedStateHandle сверх времени жизни этого экрана.
 */
@Composable
fun LoginOtpScreen(
    mfaToken: String,
    expiresInSeconds: Long,
    onLoggedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginOtpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loginSuccess.collect { onLoggedIn() }
    }

    var remainingSeconds by remember { mutableIntStateOf(expiresInSeconds.coerceAtLeast(0).toInt()) }
    LaunchedEffect(mfaToken) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
    }
    val expired = remainingSeconds <= 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_otp_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.login_otp_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )

            uiState.errorMessage?.let { ErrorBanner(it) }

            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text(stringResource(R.string.login_otp_code_label)) },
                singleLine = true,
                enabled = !expired,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = if (expired) {
                    stringResource(R.string.login_otp_expired)
                } else {
                    stringResource(R.string.login_otp_expires_in, remainingSeconds)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { viewModel.onSubmit(mfaToken) },
                enabled = !uiState.isLoading && !expired && uiState.code.length == OTP_CODE_LENGTH_UI,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_confirm))
                }
            }

            if (expired) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.login_otp_back_to_login))
                }
            }
        }
    }
}

private const val OTP_CODE_LENGTH_UI = 6
