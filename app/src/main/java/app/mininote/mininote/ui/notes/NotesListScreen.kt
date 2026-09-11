@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.mininote.mininote.R
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import app.mininote.mininote.data.local.db.entity.NoteEntity
import app.mininote.mininote.data.local.db.entity.hasConflict
import app.mininote.mininote.ui.components.CategoryBadge
import app.mininote.mininote.ui.components.ErrorBanner
import app.mininote.mininote.ui.components.OfflineBanner
import app.mininote.mininote.ui.components.StatusBadge
import app.mininote.mininote.ui.theme.extraColors
import app.mininote.mininote.util.NoteStatus
import app.mininote.mininote.util.buildCategoryTree
import app.mininote.mininote.util.deriveExcerpt
import app.mininote.mininote.util.formatRelativeTime

@Composable
fun NotesListScreen(
    onOpenNote: (localId: Long) -> Unit,
    onCreateNote: () -> Unit,
    onSessionExpired: () -> Unit,
    selectedLocalId: Long? = null,
    viewModel: NotesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sessionExpired.collect { onSessionExpired() }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.headlineSmall) },
                    actions = {
                        IconButton(onClick = viewModel::onToggleSearchVisible) {
                            Icon(
                                imageVector = if (uiState.isSearchVisible) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = stringResource(R.string.action_search),
                            )
                        }
                    },
                )
                if (uiState.isSearchVisible) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.notes_search_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                FilterChipsRow(
                    categories = uiState.categories,
                    selected = uiState.filter,
                    onSelect = viewModel::onFilterSelected,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.notes_new))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.padding(16.dp))
            }
            uiState.errorMessage?.let { ErrorBanner(it, modifier = Modifier.padding(16.dp)) }

            val notes = uiState.displayedNotes
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.isLoading) stringResource(R.string.state_loading) else stringResource(R.string.notes_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notes, key = { it.localId }) { note ->
                        val category = uiState.categories.firstOrNull { it.localId == note.categoryLocalId }
                        NoteCard(
                            note = note,
                            categoryName = category?.name,
                            selected = note.localId == selectedLocalId,
                            onClick = { onOpenNote(note.localId) },
                            onToggleStar = { viewModel.onToggleStar(note) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Категории — не фиксированный набор чипов (их может быть сколько угодно, растущая LazyRow из
 * одного чипа на категорию перестаёт быть юзабельной), поэтому вынесены в отдельный триггер-чип,
 * открывающий ModalBottomSheet со списком по иерархии. Статус-чипы — рядом с "Все" в
 * прокручиваемой части; "Избранное" — вне LazyRow, зафиксировано у правого края, чтобы визуально
 * не смешиваться с фильтрами по статусу/категории.
 */
@Composable
private fun FilterChipsRow(
    categories: List<CategoryEntity>,
    selected: NotesFilter,
    onSelect: (NotesFilter) -> Unit,
) {
    var showCategorySheet by remember { mutableStateOf(false) }
    val selectedCategoryName = (selected as? NotesFilter.Category)
        ?.let { filter -> categories.firstOrNull { it.localId == filter.localId }?.name }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selected == NotesFilter.All,
                    onClick = { onSelect(NotesFilter.All) },
                    label = { Text(stringResource(R.string.notes_filter_all)) },
                )
            }
            items(NoteStatus.entries.toList()) { status ->
                FilterChip(
                    selected = selected == NotesFilter.Status(status.apiValue),
                    onClick = { onSelect(NotesFilter.Status(status.apiValue)) },
                    label = { Text(stringResource(status.labelRes)) },
                )
            }
            item {
                FilterChip(
                    selected = selected is NotesFilter.Category,
                    onClick = { showCategorySheet = true },
                    label = {
                        Text(
                            selectedCategoryName?.let { stringResource(R.string.notes_filter_category_selected, it) }
                                ?: stringResource(R.string.notes_filter_category),
                        )
                    },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                )
            }
        }
        FilterChip(
            selected = selected == NotesFilter.Starred,
            onClick = { onSelect(NotesFilter.Starred) },
            label = { Text(stringResource(R.string.notes_filter_starred)) },
            modifier = Modifier.padding(end = 16.dp),
        )
    }

    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = categories,
            onSelect = { localId ->
                onSelect(if (localId == null) NotesFilter.All else NotesFilter.Category(localId))
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false },
        )
    }
}

@Composable
private fun CategoryPickerSheet(
    categories: List<CategoryEntity>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.category_all_categories)) },
                    modifier = Modifier.clickable { onSelect(null) },
                )
            }
            items(buildCategoryTree(categories), key = { it.category.localId }) { node ->
                ListItem(
                    headlineContent = { Text(node.category.name) },
                    modifier = Modifier
                        .padding(start = (node.depth * 16).dp)
                        .clickable { onSelect(node.category.localId) },
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    categoryName: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = if (selected) {
            androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            androidx.compose.material3.CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.hasConflict) {
                    Icon(
                        imageVector = Icons.Filled.SyncProblem,
                        contentDescription = stringResource(R.string.conflict_title),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp, end = 4.dp),
                    )
                }
                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = if (note.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.notes_filter_starred),
                        tint = if (note.isStarred) MaterialTheme.extraColors.starred else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = deriveExcerpt(note.note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(note.status)
                    categoryName?.let { CategoryBadge(it) }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatRelativeTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
