package app.mininote.mininote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.mininote.mininote.R

/**
 * Показывается вместо обычного стека приложения, когда токен истёк офлайн — переавторизоваться
 * без сети всё равно нельзя (у бэкенда нет refresh-эндпоинта, см. project memory), поэтому
 * "истёк+офлайн" и "истёк+онлайн" сводятся к одному и тому же экрану.
 */
@Composable
fun SessionExpiredScreen(onNavigateToLogin: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.session_expired_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.session_expired_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Button(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_login))
            }
        }
    }
}
