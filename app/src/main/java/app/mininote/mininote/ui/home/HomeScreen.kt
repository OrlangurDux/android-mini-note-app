package app.mininote.mininote.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mininote.mininote.R
import app.mininote.mininote.ui.categories.CategoriesScreen
import app.mininote.mininote.ui.notes.NoteDetailScreen
import app.mininote.mininote.ui.notes.NotesListScreen
import app.mininote.mininote.ui.profile.ProfileScreen

/** Стандартный M3-брейкпоинт для перехода на expanded/двухпанельные layout'ы. */
private const val EXPANDED_WIDTH_DP = 840

private enum class HomeTab { Notes, Categories, Profile }

private sealed interface NoteSelection {
    data object None : NoteSelection
    data object New : NoteSelection
    data class Existing(val localId: Long) : NoteSelection
}

/**
 * Пост-логин хост экрана (Фаза 5, планшетный layout — см. mini-note-design/android-app.html):
 * на телефоне — bottom nav с табами "Заметки"/"Профиль", список заметок открывает деталь пушем
 * через [onOpenNoteDetail]; на планшете (ширина окна >= 840dp) — navigation rail вместо bottom
 * nav, и вкладка "Заметки" сама рисует список+деталь двумя одновременно видимыми панелями (без
 * навигации: выбор заметки — локальное состояние этого хоста, не запись в NavController).
 *
 * Известное упрощение: если окно ужимается/расширяется в момент, когда на телефонном стеке уже
 * запушен Route.NoteDetail, то экран не "перетекает" в панель на лету — это разрешается по
 * возврату назад. Не стали городить синхронизацию backstack'а с локальным NoteSelection ради
 * этого редкого edge-case (resize/фолдабл на лету).
 */
@Composable
fun HomeScreen(
    onOpenNoteDetail: (localId: Long?, startInEdit: Boolean) -> Unit,
    onNavigateToAbout: () -> Unit,
    onLoggedOut: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val expanded = LocalConfiguration.current.screenWidthDp >= EXPANDED_WIDTH_DP
    var tab by rememberSaveable { mutableStateOf(HomeTab.Notes) }

    if (expanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                NavigationRailItem(
                    selected = tab == HomeTab.Notes,
                    onClick = { tab = HomeTab.Notes },
                    icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_notes)) },
                )
                NavigationRailItem(
                    selected = tab == HomeTab.Categories,
                    onClick = { tab = HomeTab.Categories },
                    icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_categories)) },
                )
                NavigationRailItem(
                    selected = tab == HomeTab.Profile,
                    onClick = { tab = HomeTab.Profile },
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_profile)) },
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (tab) {
                    HomeTab.Notes -> NotesTwoPane(onSessionExpired = onSessionExpired)
                    HomeTab.Categories -> CategoriesScreen()
                    HomeTab.Profile -> ProfileScreen(onLoggedOut = onLoggedOut, onNavigateToAbout = onNavigateToAbout)
                }
            }
        }
    } else {
        Scaffold(
            // Внутренний экран (NotesListScreen/ProfileScreen) — сам полноценный Scaffold со своим
            // TopAppBar и уже сам отступает под статус-бар. Если этот внешний Scaffold применит
            // ещё и свой safeDrawing-инсет по умолчанию, отступ сверху задвоится — визуально
            // выглядит как "лишняя пустота сверху". Верхний инсет тут не нужен вообще (тут нет
            // topBar), а нижний (под жестовую панель) уже входит в измеренную высоту NavigationBar.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == HomeTab.Notes,
                        onClick = { tab = HomeTab.Notes },
                        icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_notes)) },
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.Categories,
                        onClick = { tab = HomeTab.Categories },
                        icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_categories)) },
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.Profile,
                        onClick = { tab = HomeTab.Profile },
                        icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_profile)) },
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    HomeTab.Notes -> NotesListScreen(
                        onOpenNote = { localId -> onOpenNoteDetail(localId, false) },
                        onCreateNote = { onOpenNoteDetail(null, true) },
                        onSessionExpired = onSessionExpired,
                    )
                    HomeTab.Categories -> CategoriesScreen()
                    HomeTab.Profile -> ProfileScreen(onLoggedOut = onLoggedOut, onNavigateToAbout = onNavigateToAbout)
                }
            }
        }
    }
}

/**
 * Список слева (360dp) + деталь/редактирование справа — обе панели видны одновременно, без
 * push-навигации. Состояние выбора хранится как два примитива (не как [NoteSelection] напрямую)
 * — `rememberSaveable` умеет сохранять `Long?`/`Boolean` из коробки (переживает пересоздание
 * Activity при повороте), а для произвольного sealed-типа потребовался бы отдельный Saver.
 */
@Composable
private fun NotesTwoPane(onSessionExpired: () -> Unit) {
    var selectedLocalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isCreatingNew by rememberSaveable { mutableStateOf(false) }

    val selection: NoteSelection = when {
        isCreatingNew -> NoteSelection.New
        selectedLocalId != null -> NoteSelection.Existing(selectedLocalId!!)
        else -> NoteSelection.None
    }

    fun clearSelection() {
        selectedLocalId = null
        isCreatingNew = false
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(360.dp).fillMaxHeight()) {
            NotesListScreen(
                onOpenNote = { localId -> selectedLocalId = localId; isCreatingNew = false },
                onCreateNote = { selectedLocalId = null; isCreatingNew = true },
                onSessionExpired = onSessionExpired,
                selectedLocalId = (selection as? NoteSelection.Existing)?.localId,
            )
        }
        VerticalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (val current = selection) {
                NoteSelection.None -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.notes_select_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NoteSelection.New -> NoteDetailScreen(
                    localId = null,
                    startInEdit = true,
                    tabletPane = true,
                    viewModelKey = "note-new",
                    onBack = ::clearSelection,
                    onSaved = { newLocalId -> selectedLocalId = newLocalId; isCreatingNew = false },
                )
                is NoteSelection.Existing -> NoteDetailScreen(
                    localId = current.localId,
                    startInEdit = false,
                    tabletPane = true,
                    viewModelKey = "note-${current.localId}",
                    onBack = ::clearSelection,
                )
            }
        }
    }
}
