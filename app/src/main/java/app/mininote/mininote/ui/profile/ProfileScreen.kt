@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.profile

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import app.mininote.mininote.R
import app.mininote.mininote.data.local.prefs.ThemeMode
import app.mininote.mininote.ui.components.ErrorBanner
import app.mininote.mininote.ui.components.HintBanner
import app.mininote.mininote.ui.components.PasswordTextField
import app.mininote.mininote.util.generateQrBitmap
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkChecked = when (uiState.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.loggedOut.collect { onLoggedOut() }
    }

    LaunchedEffect(uiState.passwordChangedMessage) {
        if (uiState.passwordChangedMessage != null) {
            delay(3000)
            viewModel.onDismissPasswordChangedMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 560.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.errorMessage?.let { ErrorBanner(it) }
            uiState.passwordChangedMessage?.let { HintBanner(it) }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                    if (uiState.avatarUrl.isNotBlank()) {
                        SubcomposeAsyncImage(
                            model = uiState.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { AvatarInitial(uiState.name, uiState.email) },
                            error = { AvatarInitial(uiState.name, uiState.email) },
                        )
                    } else {
                        AvatarInitial(uiState.name, uiState.email)
                    }
                }
                Column {
                    Text(text = uiState.name.ifBlank { stringResource(R.string.profile_no_name) }, style = MaterialTheme.typography.titleLarge)
                    Text(text = uiState.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column {
                SectionHeader(stringResource(R.string.profile_section_account))
                ProfileRow(
                    label = stringResource(R.string.profile_system_theme),
                    trailing = {
                        Switch(
                            checked = uiState.themeMode == ThemeMode.SYSTEM,
                            onCheckedChange = { enabled -> viewModel.onSetSystemTheme(enabled, systemDark) },
                        )
                    },
                )
                ProfileRow(
                    label = stringResource(R.string.profile_dark_theme),
                    trailing = {
                        Switch(
                            checked = darkChecked,
                            onCheckedChange = viewModel::onToggleDarkTheme,
                            enabled = uiState.themeMode != ThemeMode.SYSTEM,
                        )
                    },
                )
                ProfileRow(
                    label = stringResource(R.string.profile_language),
                    onClick = { showLanguageDialog = true },
                )
            }

            Column {
                SectionHeader(stringResource(R.string.profile_section_security))
                ProfileRow(label = stringResource(R.string.profile_password), onClick = viewModel::onRequestPasswordChange)
                ProfileRow(
                    label = stringResource(R.string.profile_tfa),
                    trailing = {
                        if (uiState.isTfaBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Switch(checked = uiState.isTfaEnabled, onCheckedChange = viewModel::onToggleTfa)
                        }
                    },
                )
            }

            Column {
                SectionHeader(stringResource(R.string.profile_section_other))
                ProfileRow(label = stringResource(R.string.profile_about), onClick = onNavigateToAbout)
            }

            OutlinedButton(onClick = viewModel::onLogout, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_sign_out))
            }
        }
    }

    if (uiState.showPasswordDialog) {
        PasswordChangeDialog(uiState = uiState, viewModel = viewModel)
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selected = uiState.languageOption,
            onSelect = { option ->
                viewModel.onLanguageSelected(option)
                showLanguageDialog = false
                activity?.recreate()
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    uiState.tfaSetupUrl?.let { url ->
        TfaSetupDialog(url = url, onDismiss = viewModel::onDismissTfaSetup)
    }
}

/**
 * Показывается один раз сразу после успешного включения 2FA. [url] — `otpauth://totp/...` из
 * ответа `PUT /users/tfa` (см. AuthRepository.enableTfa); QR — рендер этой же строки, ручной ввод —
 * параметр `secret` из неё же, сгруппированный по 4 символа (стандартная подача большинства
 * приложений-аутентификаторов, не выдумка клиента).
 */
@Composable
private fun TfaSetupDialog(url: String, onDismiss: () -> Unit) {
    val secret = remember(url) { runCatching { url.toUri().getQueryParameter("secret") }.getOrNull().orEmpty() }
    val qrBitmap = remember(url) { generateQrBitmap(url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_tfa_setup_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.profile_tfa_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.profile_tfa_setup_title),
                        modifier = Modifier.size(200.dp),
                    )
                }
                if (secret.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.profile_tfa_setup_manual_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = secret.chunked(4).joinToString(" "),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        },
    )
}

@Composable
private fun AvatarInitial(name: String, email: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = (name.firstOrNull() ?: email.firstOrNull())?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun ProfileRow(
    label: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = (if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth())
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            trailing?.invoke() ?: if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Unit
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun PasswordChangeDialog(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::onDismissPasswordChange,
        title = { Text(stringResource(R.string.profile_password_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.passwordError?.let { ErrorBanner(it) }
                PasswordTextField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = stringResource(R.string.profile_password_new),
                )
                PasswordTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = stringResource(R.string.profile_password_repeat),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::onSavePassword, enabled = !uiState.isSavingPassword) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onDismissPasswordChange) { Text(stringResource(R.string.profile_cancel)) }
        },
    )
}

@Composable
private fun LanguageDialog(
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        LanguageOption.SYSTEM to stringResource(R.string.language_system),
        LanguageOption.RUSSIAN to stringResource(R.string.language_russian),
        LanguageOption.ENGLISH to stringResource(R.string.language_english),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column {
                options.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, onClick = { onSelect(option) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Text(text = label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        },
    )
}
