@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.notes

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.mininote.mininote.R
import app.mininote.mininote.ui.components.CategoryBadge
import app.mininote.mininote.ui.components.ErrorBanner
import app.mininote.mininote.ui.components.StatusBadge
import app.mininote.mininote.ui.components.markdown.MarkdownEditor
import app.mininote.mininote.ui.components.markdown.MarkdownViewer
import app.mininote.mininote.ui.theme.extraColors
import app.mininote.mininote.util.formatRelativeTime

@Composable
fun NoteDetailScreen(
    localId: Long?,
    startInEdit: Boolean,
    onBack: () -> Unit,
    tabletPane: Boolean = false,
    onSaved: (Long) -> Unit = {},
    viewModelKey: String? = null,
) {
    val viewModel: NoteDetailViewModel = hiltViewModel<NoteDetailViewModel, NoteDetailViewModel.Factory>(
        key = viewModelKey,
    ) { factory -> factory.create(localId, startInEdit) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.note_saved)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                NoteDetailEvent.NavigateBack -> onBack()
                NoteDetailEvent.Saved -> {
                    snackbarHostState.showSnackbar(savedMessage)
                    viewModel.uiState.value.localId?.let(onSaved)
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        // Клавиатура иначе перекрывает FAB "сохранить" снизу экрана — с imePadding Scaffold
        // сжимается по высоте при появлении клавиатуры, и FAB всплывает над ней сам.
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) stringResource(R.string.note_editing) else stringResource(R.string.note_title)) },
                navigationIcon = {
                    // На планшете правая панель постоянно видима — стрелка "назад" в режиме
                    // просмотра не нужна (нет экрана, куда возвращаться); при редактировании
                    // она остаётся — это "отменить правку", а не выход из панели.
                    if (uiState.isEditing || !tabletPane) {
                        IconButton(onClick = { if (uiState.isEditing) viewModel.onCancelEdit() else onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (!uiState.isEditing && !uiState.isNew) {
                        IconButton(onClick = viewModel::onToggleStar) {
                            Icon(
                                imageVector = if (uiState.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = stringResource(R.string.notes_filter_starred),
                                tint = if (uiState.isStarred) MaterialTheme.extraColors.starred else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = viewModel::onRequestDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (uiState.isEditing) viewModel.onSave() else viewModel.onEnterEdit() },
            ) {
                Icon(
                    imageVector = if (uiState.isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                    contentDescription = if (uiState.isEditing) stringResource(R.string.profile_save) else stringResource(R.string.note_edit),
                )
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Горизонтальный отступ НЕ единый на весь экран: заголовок/категория/статус/метаданные
        // держат стандартные 24dp, а само поле заметки — только 8dp, чтобы текст записи занимал
        // максимум доступной ширины (нативные заметочные приложения не зажимают основной текст
        // в ту же колонку, что и служебные элементы формы).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Тап по пустому месту (не по самим полям — они перехватывают событие раньше)
                // закрывает клавиатуру и снимает фокус, без этого дотянуться до FAB было можно
                // только руками закрыв клавиатуру системной кнопкой "назад".
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    })
                }
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.errorMessage?.let { ErrorBanner(it, modifier = Modifier.padding(horizontal = 24.dp)) }

            if (uiState.isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
                    CategoryPicker(
                        categories = uiState.categories,
                        selectedLocalId = uiState.categoryLocalId,
                        onSelect = viewModel::onCategorySelected,
                        onRequestCreate = viewModel::onRequestCreateCategory,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPicker(
                        selected = uiState.status,
                        onSelect = viewModel::onStatusSelected,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.note_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
                MarkdownEditor(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            } else {
                Text(
                    text = uiState.title.ifBlank { stringResource(R.string.notes_untitled) },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
                    StatusBadge(uiState.status)
                    uiState.categories.firstOrNull { it.localId == uiState.categoryLocalId }?.let { category ->
                        CategoryBadge(category.name)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                MarkdownViewer(markdown = uiState.content, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.note_updated_at, formatRelativeTime(uiState.updatedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val wordCount = uiState.content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                    Text(
                        text = pluralStringResource(R.plurals.note_word_count, wordCount, wordCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text(stringResource(R.string.note_delete_title)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDelete) { Text(stringResource(R.string.profile_cancel)) }
            },
        )
    }

    if (uiState.showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = viewModel::onDismissCreateCategory,
            onCreate = viewModel::onCreateCategory,
        )
    }

    // Заметку правили и на сервере, и офлайн-локально — молча перезаписывать нельзя (см. SyncWorker),
    // нужен явный выбор. onDismissRequest — намеренно no-op: это не UI-флаг, а отражение реального
    // состояния заметки (uiState.hasConflict из observeNote), закрыть тапом мимо/кнопкой "назад"
    // нечего — outbox всё равно ждёт решения, диалог просто появится снова.
    if (uiState.hasConflict) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.conflict_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.conflict_message))
                    Text(
                        text = stringResource(R.string.conflict_server_preview, uiState.conflictServerTitle.orEmpty()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onResolveConflictAcceptServer) {
                    Text(stringResource(R.string.conflict_accept_server))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onResolveConflictKeepLocal) {
                    Text(stringResource(R.string.conflict_keep_local))
                }
            },
        )
    }
}

@Composable
private fun CategoryPicker(
    categories: List<app.mininote.mininote.data.local.db.entity.CategoryEntity>,
    selectedLocalId: Long?,
    onSelect: (Long?) -> Unit,
    onRequestCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.localId == selectedLocalId }?.name ?: stringResource(R.string.note_no_category)

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.note_category_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Прозрачный клик-перехватчик поверх readOnly-поля, чтобы открыть меню.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.note_no_category)) }, onClick = { onSelect(null); expanded = false })
            app.mininote.mininote.util.buildCategoryTree(categories).forEach { node ->
                DropdownMenuItem(
                    text = { Text("${"‣".repeat(node.depth)}${node.category.name}") },
                    onClick = { onSelect(node.category.localId); expanded = false },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.note_new_category)) },
                onClick = { expanded = false; onRequestCreate() },
            )
        }
    }
}

@Composable
private fun StatusPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedStatus = app.mininote.mininote.util.NoteStatus.fromApiValue(selected)

    Box(modifier = modifier) {
        OutlinedTextField(
            value = stringResource(selectedStatus.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.note_status_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            app.mininote.mininote.util.NoteStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(stringResource(status.labelRes)) },
                    onClick = { onSelect(status.apiValue); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CreateCategoryDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_new_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.servers_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        },
    )
}
