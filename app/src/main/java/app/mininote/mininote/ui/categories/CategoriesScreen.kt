@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.mininote.mininote.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.mininote.mininote.R
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import app.mininote.mininote.ui.components.ErrorBanner
import app.mininote.mininote.util.CategoryNode

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_categories)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onRequestCreate() }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.category_new_title))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            uiState.errorMessage?.let { ErrorBanner(it, modifier = Modifier.padding(16.dp)) }

            if (uiState.nodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.isLoading) stringResource(R.string.state_loading) else stringResource(R.string.category_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(uiState.nodes, key = { it.category.localId }) { node ->
                        CategoryRow(
                            node = node,
                            onEdit = { viewModel.onRequestEdit(node.category) },
                            onDelete = { viewModel.onRequestDelete(node.category.localId) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showEditDialog) {
        CategoryEditDialog(uiState = uiState, viewModel = viewModel)
    }

    uiState.deleteTargetLocalId?.let { targetId ->
        val targetName = uiState.allCategories.firstOrNull { it.localId == targetId }?.name.orEmpty()
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text(stringResource(R.string.category_delete_title, targetName)) },
            text = { Text(stringResource(R.string.category_delete_message)) },
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
}

@Composable
private fun CategoryRow(node: CategoryNode, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (16 + node.depth * 20).dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = node.category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.category_rename))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(uiState: CategoriesUiState, viewModel: CategoriesViewModel) {
    val isNew = uiState.editingLocalId == null
    AlertDialog(
        onDismissRequest = viewModel::onDismissEditDialog,
        title = { Text(if (isNew) stringResource(R.string.category_new_title) else stringResource(R.string.category_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.editName,
                    onValueChange = viewModel::onEditNameChange,
                    label = { Text(stringResource(R.string.servers_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ParentPicker(
                    availableParents = uiState.availableParents,
                    selectedLocalId = uiState.editParentLocalId,
                    onSelect = viewModel::onEditParentChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::onSaveEdit, enabled = uiState.editName.isNotBlank()) {
                Text(if (isNew) stringResource(R.string.action_create) else stringResource(R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onDismissEditDialog) { Text(stringResource(R.string.profile_cancel)) }
        },
    )
}

@Composable
private fun ParentPicker(
    availableParents: List<CategoryEntity>,
    selectedLocalId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val topLevelLabel = stringResource(R.string.category_top_level)
    val selectedName = availableParents.firstOrNull { it.localId == selectedLocalId }?.name ?: topLevelLabel

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category_parent_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(topLevelLabel) }, onClick = { onSelect(null); expanded = false })
            availableParents.forEach { category ->
                DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelect(category.localId); expanded = false })
            }
        }
    }
}
