package app.mininote.mininote.ui.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.R
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import app.mininote.mininote.data.local.db.entity.hasConflict
import app.mininote.mininote.data.repository.CategoriesRepository
import app.mininote.mininote.data.repository.DEFAULT_NOTE_STATUS
import app.mininote.mininote.data.repository.NotesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val localId: Long? = null,
    val isNew: Boolean = false,
    val isEditing: Boolean = false,
    val title: String = "",
    val content: String = "",
    val categoryLocalId: Long? = null,
    val status: String = DEFAULT_NOTE_STATUS,
    val isStarred: Boolean = false,
    val updatedAt: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
    val showCreateCategoryDialog: Boolean = false,
    val hasConflict: Boolean = false,
    val conflictServerTitle: String? = null,
    val conflictServerNote: String? = null,
)

/**
 * Фаза 3: create/update/delete/createCategory теперь пишут в Room синхронно и ставят операцию
 * в outbox (см. [NotesRepository], [CategoriesRepository]) — сеть здесь не вызывается напрямую и
 * поэтому не может провалиться "прямо сейчас", ошибки сети всплывают позже через сам outbox.
 *
 * Планшетный layout (Фаза 5): assisted injection вместо `SavedStateHandle.toRoute()` — на
 * телефоне экран пушится через NavController (аргументы приходят из маршрута), а на планшете
 * это правая панель двухпанельного списка, которая вообще не участвует в навигации (выбор
 * заметки — локальное состояние хоста). Оба случая передают [initialLocalId]/[initialStartInEdit]
 * напрямую через фабрику, без привязки к конкретному способу создания экрана.
 */
@HiltViewModel(assistedFactory = NoteDetailViewModel.Factory::class)
class NoteDetailViewModel @AssistedInject constructor(
    @Assisted private val initialLocalId: Long?,
    @Assisted private val initialStartInEdit: Boolean,
    private val notesRepository: NotesRepository,
    private val categoriesRepository: CategoriesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(localId: Long?, startInEdit: Boolean): NoteDetailViewModel
    }

    private val draft = MutableStateFlow(
        NoteDetailUiState(
            localId = initialLocalId,
            isNew = initialLocalId == null,
            isEditing = initialLocalId == null || initialStartInEdit,
            isLoading = initialLocalId != null,
        ),
    )

    private val _events = Channel<NoteDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Заметка наблюдается реактивно (в отличие от draft'а — это только для конфликт-полей,
    // title/content/categoryLocalId остаются буфером редактирования пользователя, который сюда
    // не подмешивается) — чтобы конфликт, обнаруженный синком, всплыл в диалоге, даже если
    // экран уже был открыт в момент, когда SyncWorker его нашёл, без необходимости уйти-вернуться.
    private val observedNote = initialLocalId?.let { notesRepository.observeNote(it) } ?: flowOf(null)

    val uiState: StateFlow<NoteDetailUiState> = combine(
        draft,
        categoriesRepository.observeCategories(),
        observedNote,
    ) { state, categories, note ->
        state.copy(
            categories = categories,
            hasConflict = note?.hasConflict ?: false,
            conflictServerTitle = note?.conflictServerTitle,
            conflictServerNote = note?.conflictServerNote,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), draft.value)

    init {
        initialLocalId?.let { localId ->
            viewModelScope.launch {
                val note = notesRepository.getNote(localId)
                if (note == null) {
                    draft.update { it.copy(isLoading = false, errorMessage = context.getString(R.string.note_not_found)) }
                } else {
                    draft.update {
                        it.copy(
                            title = note.title,
                            content = note.note,
                            categoryLocalId = note.categoryLocalId,
                            status = note.status,
                            isStarred = note.isStarred,
                            updatedAt = note.updatedAt,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = draft.update { it.copy(title = value) }
    fun onContentChange(value: String) = draft.update { it.copy(content = value) }
    fun onCategorySelected(categoryLocalId: Long?) = draft.update { it.copy(categoryLocalId = categoryLocalId) }
    fun onStatusSelected(status: String) = draft.update { it.copy(status = status) }

    fun onEnterEdit() = draft.update { it.copy(isEditing = true) }

    fun onCancelEdit() {
        val state = draft.value
        if (state.isNew) {
            viewModelScope.launch { _events.send(NoteDetailEvent.NavigateBack) }
            return
        }
        viewModelScope.launch {
            val note = notesRepository.getNote(requireNotNull(state.localId))
            if (note != null) {
                draft.update {
                    it.copy(
                        title = note.title,
                        content = note.note,
                        categoryLocalId = note.categoryLocalId,
                        status = note.status,
                        isEditing = false,
                    )
                }
            }
        }
    }

    fun onToggleStar() {
        val state = draft.value
        val localId = state.localId ?: return
        viewModelScope.launch {
            val note = notesRepository.getNote(localId) ?: return@launch
            notesRepository.toggleStarred(localId, note)
            draft.update { it.copy(isStarred = !it.isStarred) }
        }
    }

    fun onResolveConflictKeepLocal() {
        val localId = draft.value.localId ?: return
        viewModelScope.launch { notesRepository.resolveConflictKeepLocal(localId) }
    }

    fun onResolveConflictAcceptServer() {
        val localId = draft.value.localId ?: return
        viewModelScope.launch {
            notesRepository.resolveConflictAcceptServer(localId)
            val note = notesRepository.getNote(localId) ?: return@launch
            draft.update {
                it.copy(
                    title = note.title,
                    content = note.note,
                    categoryLocalId = note.categoryLocalId,
                    status = note.status,
                    updatedAt = note.updatedAt,
                )
            }
        }
    }

    fun onRequestDelete() = draft.update { it.copy(showDeleteConfirm = true) }
    fun onDismissDelete() = draft.update { it.copy(showDeleteConfirm = false) }

    fun onRequestCreateCategory() = draft.update { it.copy(showCreateCategoryDialog = true) }
    fun onDismissCreateCategory() = draft.update { it.copy(showCreateCategoryDialog = false) }

    fun onCreateCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val category = categoriesRepository.createCategory(name.trim())
            draft.update { it.copy(categoryLocalId = category.localId, showCreateCategoryDialog = false) }
        }
    }

    fun onConfirmDelete() {
        val localId = draft.value.localId ?: return
        viewModelScope.launch {
            draft.update { it.copy(showDeleteConfirm = false) }
            notesRepository.deleteNote(localId)
            _events.send(NoteDetailEvent.NavigateBack)
        }
    }

    fun onSave() {
        val state = draft.value
        viewModelScope.launch {
            if (state.isNew) {
                val newLocalId = notesRepository.createNote(
                    title = state.title,
                    note = state.content,
                    categoryLocalId = state.categoryLocalId,
                    status = state.status,
                )
                val saved = notesRepository.getNote(newLocalId)
                draft.update {
                    it.copy(localId = newLocalId, isNew = false, isEditing = false, updatedAt = saved?.updatedAt ?: it.updatedAt)
                }
            } else {
                val localId = requireNotNull(state.localId)
                notesRepository.updateNote(
                    localId = localId,
                    title = state.title,
                    note = state.content,
                    status = state.status,
                    categoryLocalId = state.categoryLocalId,
                )
                val saved = notesRepository.getNote(localId)
                draft.update { it.copy(isEditing = false, updatedAt = saved?.updatedAt ?: it.updatedAt) }
            }
            _events.send(NoteDetailEvent.Saved)
        }
    }
}

sealed interface NoteDetailEvent {
    data object NavigateBack : NoteDetailEvent
    data object Saved : NoteDetailEvent
}
