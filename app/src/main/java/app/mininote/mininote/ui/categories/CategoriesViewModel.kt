package app.mininote.mininote.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import app.mininote.mininote.data.repository.CategoriesRepository
import app.mininote.mininote.util.CategoryNode
import app.mininote.mininote.util.buildCategoryTree
import app.mininote.mininote.util.descendantsOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val nodes: List<CategoryNode> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showEditDialog: Boolean = false,
    val editingLocalId: Long? = null,
    val editName: String = "",
    val editParentLocalId: Long? = null,
    val deleteTargetLocalId: Long? = null,
) {
    /** Категории, которые можно выбрать родителем: не сама редактируемая категория и не её потомки (иначе цикл). */
    val availableParents: List<CategoryEntity>
        get() {
            val excluded = editingLocalId?.let { descendantsOf(it, allCategories) + it }.orEmpty()
            return allCategories.filter { it.localId !in excluded }
        }
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

    private data class LocalState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val showEditDialog: Boolean = false,
        val editingLocalId: Long? = null,
        val editName: String = "",
        val editParentLocalId: Long? = null,
        val deleteTargetLocalId: Long? = null,
    )

    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoriesRepository.observeCategories(),
        localState,
    ) { categories, local ->
        CategoriesUiState(
            nodes = buildCategoryTree(categories),
            allCategories = categories,
            isLoading = local.isLoading,
            errorMessage = local.errorMessage,
            showEditDialog = local.showEditDialog,
            editingLocalId = local.editingLocalId,
            editName = local.editName,
            editParentLocalId = local.editParentLocalId,
            deleteTargetLocalId = local.deleteTargetLocalId,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CategoriesUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            localState.update { it.copy(isLoading = true, errorMessage = null) }
            categoriesRepository.refresh()
            localState.update { it.copy(isLoading = false) }
        }
    }

    fun onRequestCreate(parentLocalId: Long? = null) {
        localState.update {
            it.copy(showEditDialog = true, editingLocalId = null, editName = "", editParentLocalId = parentLocalId)
        }
    }

    fun onRequestEdit(category: CategoryEntity) {
        localState.update {
            it.copy(
                showEditDialog = true,
                editingLocalId = category.localId,
                editName = category.name,
                editParentLocalId = category.parentLocalId,
            )
        }
    }

    fun onDismissEditDialog() = localState.update { it.copy(showEditDialog = false) }
    fun onEditNameChange(value: String) = localState.update { it.copy(editName = value) }
    fun onEditParentChange(parentLocalId: Long?) = localState.update { it.copy(editParentLocalId = parentLocalId) }

    fun onSaveEdit() {
        val state = localState.value
        if (state.editName.isBlank()) return
        viewModelScope.launch {
            val editingLocalId = state.editingLocalId
            if (editingLocalId == null) {
                categoriesRepository.createCategory(state.editName.trim(), state.editParentLocalId)
            } else {
                categoriesRepository.updateCategory(editingLocalId, state.editName.trim(), state.editParentLocalId)
            }
            localState.update { it.copy(showEditDialog = false) }
        }
    }

    fun onRequestDelete(localId: Long) = localState.update { it.copy(deleteTargetLocalId = localId) }
    fun onDismissDelete() = localState.update { it.copy(deleteTargetLocalId = null) }

    fun onConfirmDelete() {
        val localId = localState.value.deleteTargetLocalId ?: return
        viewModelScope.launch {
            categoriesRepository.deleteCategory(localId)
            localState.update { it.copy(deleteTargetLocalId = null) }
        }
    }
}
