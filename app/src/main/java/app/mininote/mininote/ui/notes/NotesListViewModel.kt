package app.mininote.mininote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mininote.mininote.data.connectivity.ConnectivityObserver
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import app.mininote.mininote.data.local.db.entity.NoteEntity
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.repository.AuthRepository
import app.mininote.mininote.data.repository.CategoriesRepository
import app.mininote.mininote.data.repository.NotesRepository
import app.mininote.mininote.util.descendantsOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NotesFilter {
    data object All : NotesFilter
    data object Starred : NotesFilter
    data class Category(val localId: Long) : NotesFilter
    data class Status(val status: String) : NotesFilter
}

data class NotesListUiState(
    val notes: List<NoteEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val filter: NotesFilter = NotesFilter.All,
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val searchResults: List<NoteEntity>? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
) {
    val displayedNotes: List<NoteEntity>
        get() = if (searchQuery.isNotBlank()) {
            searchResults.orEmpty()
        } else {
            notes.filter { note ->
                when (val currentFilter = filter) {
                    NotesFilter.All -> true
                    NotesFilter.Starred -> note.isStarred
                    // Категория-фильтр включает и заметки дочерних категорий — выбрав родителя,
                    // пользователь ожидает увидеть всё поддерево, а не только заметки без вложенности.
                    is NotesFilter.Category -> note.categoryLocalId == currentFilter.localId ||
                        note.categoryLocalId in descendantsOf(currentFilter.localId, categories)
                    is NotesFilter.Status -> note.status == currentFilter.status
                }
            }
        }
}

private data class LocalUiState(
    val filter: NotesFilter = NotesFilter.All,
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val searchResults: List<NoteEntity>? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val categoriesRepository: CategoriesRepository,
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())
    private val searchQueryState = MutableStateFlow("")

    private val _sessionExpired = Channel<Unit>(Channel.CONFLATED)
    val sessionExpired = _sessionExpired.receiveAsFlow()

    val uiState: StateFlow<NotesListUiState> = combine(
        notesRepository.observeNotes(),
        categoriesRepository.observeCategories(),
        localState,
        connectivityObserver.observe(),
    ) { notes, categories, local, isOnline ->
        NotesListUiState(
            notes = notes,
            categories = categories,
            filter = local.filter,
            searchQuery = local.searchQuery,
            isSearchVisible = local.isSearchVisible,
            searchResults = local.searchResults,
            isLoading = local.isLoading,
            errorMessage = local.errorMessage,
            isOffline = !isOnline,
        )
        // Eagerly, не WhileSubscribed: этот экран — постоянный "хаб" (список переживает визиты в
        // NoteDetailScreen на той же backstack-записи). WhileSubscribed отписывается от Room-Flow,
        // пока список не виден, и при возврате первый кадр показывает устаревшее значение из кэша
        // StateFlow, пока апстрим не перезапустится и не догонит — на практике это выглядело как
        // "новая заметка не появилась в списке после сохранения". Eagerly держит подписку живой
        // всё время жизни ViewModel, так что .value всегда актуален к моменту возврата на экран.
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NotesListUiState())

    init {
        refresh()
        viewModelScope.launch {
            searchQueryState
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        localState.update { it.copy(searchResults = null) }
                        return@collect
                    }
                    val results = notesRepository.search(query)
                    localState.update { it.copy(searchResults = results) }
                }
        }
        // Периодическая проверка истечения токена, пока пользователь находится в приложении
        // (см. план: без сети переавторизоваться всё равно нельзя — refresh-эндпоинта нет).
        viewModelScope.launch {
            while (isActive) {
                delay(SESSION_CHECK_INTERVAL_MS)
                if (!authRepository.isLoggedIn()) {
                    _sessionExpired.send(Unit)
                    break
                }
            }
        }
    }

    /** Пул (не пуш — та половина уходит в outbox) серверного состояния. Ошибку показываем только
     * если она не объясняется банальным отсутствием сети (для офлайна есть отдельный баннер). */
    fun refresh() {
        viewModelScope.launch {
            localState.update { it.copy(isLoading = true, errorMessage = null) }
            val categoriesResult = categoriesRepository.refresh()
            val notesResult = notesRepository.refresh()
            val error = (categoriesResult as? ApiResult.Failure)?.message
                ?: (notesResult as? ApiResult.Failure)?.message
            val showError = error != null && connectivityObserver.isOnlineNow()
            localState.update { it.copy(isLoading = false, errorMessage = if (showError) error else null) }
        }
    }

    fun onFilterSelected(filter: NotesFilter) {
        localState.update { it.copy(filter = filter) }
    }

    fun onSearchQueryChange(query: String) {
        localState.update { it.copy(searchQuery = query) }
        searchQueryState.value = query
    }

    fun onToggleSearchVisible() {
        localState.update {
            val visible = !it.isSearchVisible
            it.copy(
                isSearchVisible = visible,
                searchQuery = if (visible) it.searchQuery else "",
                searchResults = if (visible) it.searchResults else null,
            )
        }
        if (!localState.value.isSearchVisible) searchQueryState.value = ""
    }

    fun onToggleStar(note: NoteEntity) {
        viewModelScope.launch { notesRepository.toggleStarred(note.localId, note) }
    }

    private companion object {
        const val SESSION_CHECK_INTERVAL_MS = 60_000L
    }
}
