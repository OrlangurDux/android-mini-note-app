@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.components.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mininote.mininote.R
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

/**
 * Настоящий визивиг-редактор markdown на базе стороннего rich-text-движка
 * (com.mohamedrejeb.richeditor:richeditor-compose) — самодельная реализация через
 * VisualTransformation (см. историю в plan-файле, "Фаза 6.1") только приглушала маркеры разметки
 * цветом, но не убирала их из поля ввода, из-за чего `**`/`#` оставались на виду прямо поверх
 * форматированного текста. Здесь маркеры не рисуются вовсе: [RichTextState] хранит богатую модель
 * параграфов, тулбар вызывает её toggle-методы напрямую (жирный/курсив/код/списки/заголовки), а
 * markdown как plain-текст используется только на границе с [value]/[onValueChange] — тем же
 * контрактом, что был у ViewModel раньше, поэтому выше по стеку ничего менять не пришлось.
 */
@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 10,
) {
    val richTextState = rememberRichTextState()
    var lastKnownMarkdown by remember { mutableStateOf<String?>(null) }

    // Внешнее изменение value (например, отмена правки откатывает draft к сохранённому content) —
    // перезагружаем модель. Пропускаем, если value — это эхо нашего же последнего toMarkdown().
    LaunchedEffect(value) {
        if (value != lastKnownMarkdown) {
            richTextState.setMarkdown(value)
            lastKnownMarkdown = value
        }
    }

    // Любое редактирование пользователем меняет richTextState.annotatedString — прокидываем
    // актуальный markdown наверх, снова запоминая его, чтобы эффект выше не перезагрузил модель.
    LaunchedEffect(richTextState.annotatedString) {
        val markdown = richTextState.toMarkdown()
        if (markdown != lastKnownMarkdown) {
            lastKnownMarkdown = markdown
            onValueChange(markdown)
        }
    }

    val currentSpanStyle = richTextState.currentSpanStyle
    val isBold = currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = currentSpanStyle.fontStyle == FontStyle.Italic

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarIconButton(
                active = isBold,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            ) {
                Icon(Icons.Filled.FormatBold, contentDescription = stringResource(R.string.markdown_bold))
            }
            ToolbarIconButton(
                active = isItalic,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
            ) {
                Icon(Icons.Filled.FormatItalic, contentDescription = stringResource(R.string.markdown_italic))
            }
            ToolbarIconButton(
                active = richTextState.currentHeadingStyle == HeadingStyle.H1,
                onClick = {
                    richTextState.setHeadingStyle(
                        if (richTextState.currentHeadingStyle == HeadingStyle.H1) HeadingStyle.Normal else HeadingStyle.H1,
                    )
                },
            ) {
                Text("H1", style = MaterialTheme.typography.labelLarge)
            }
            ToolbarIconButton(
                active = richTextState.currentHeadingStyle == HeadingStyle.H2,
                onClick = {
                    richTextState.setHeadingStyle(
                        if (richTextState.currentHeadingStyle == HeadingStyle.H2) HeadingStyle.Normal else HeadingStyle.H2,
                    )
                },
            ) {
                Text("H2", style = MaterialTheme.typography.labelLarge)
            }
            ToolbarIconButton(active = richTextState.isUnorderedList, onClick = { richTextState.toggleUnorderedList() }) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = stringResource(R.string.markdown_bullet_list))
            }
            ToolbarIconButton(active = richTextState.isOrderedList, onClick = { richTextState.toggleOrderedList() }) {
                Icon(Icons.Filled.FormatListNumbered, contentDescription = stringResource(R.string.markdown_numbered_list))
            }
            ToolbarIconButton(active = richTextState.isCodeSpan, onClick = { richTextState.toggleCodeSpan() }) {
                Icon(Icons.Filled.Code, contentDescription = stringResource(R.string.markdown_code))
            }
        }
        // Поле — приложение для заметок, а не форма: убираем заливку/подчёркивание стандартного
        // Material-поля (RichTextEditorDefaults.filledShape/richTextEditorColors по умолчанию рисуют
        // серый контейнер + индикатор снизу, как у обычного TextField) и внутренние горизонтальные
        // отступы контента — текст должен начинаться сразу от края доступной ширины, как в
        // нативных заметочных приложениях, а не быть зажатым в декорированную коробку.
        RichTextEditor(
            state = richTextState,
            minLines = minLines,
            colors = RichTextEditorDefaults.richTextEditorColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun ToolbarIconButton(active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
            contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        icon()
    }
}
