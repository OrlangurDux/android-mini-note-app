package app.mininote.mininote.data.repository

import android.content.Context
import app.mininote.mininote.data.connectivity.ConnectivityObserver
import app.mininote.mininote.data.local.db.dao.CategoryDao
import app.mininote.mininote.data.local.db.dao.NoteDao
import app.mininote.mininote.data.local.db.entity.NoteEntity
import app.mininote.mininote.data.local.db.entity.OutboxEntityType
import app.mininote.mininote.data.local.db.entity.OutboxOperation
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.remote.ZERO_OBJECT_ID
import app.mininote.mininote.data.remote.api.NotesApi
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import app.mininote.mininote.data.remote.dto.NoteDto
import app.mininote.mininote.data.remote.safeApiCall
import app.mininote.mininote.sync.SyncScheduler
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Значение по умолчанию для нового статуса — enum на бэкенде (draft/public/archive, см.
// util/NoteStatus.kt); "public" = "Публичный" в UI.
const val DEFAULT_NOTE_STATUS = "public"

/**
 * Офлайн-первая запись (Фаза 3): create/update/delete пишут в Room синхронно и ставят операцию
 * в [OutboxRepository], сеть отсюда напрямую не вызывается — её дожимает [app.mininote.mininote.sync.SyncWorker].
 * `refresh`/`search` — по-прежнему прямые сетевые вызовы (это "pull"-половина синка, читающая, а не пишущая).
 */
@Singleton
class NotesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notesApi: NotesApi,
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val outboxRepository: OutboxRepository,
    private val syncScheduler: SyncScheduler,
    private val connectivityObserver: ConnectivityObserver,
    private val errorAdapter: JsonAdapter<ErrorEnvelopeDto>,
) {
    fun observeNotes(): Flow<List<NoteEntity>> = noteDao.observeAll()

    fun observeNote(localId: Long): Flow<NoteEntity?> = noteDao.observeByLocalId(localId)

    suspend fun getNote(localId: Long): NoteEntity? = noteDao.getByLocalId(localId)

    suspend fun refresh(): ApiResult<Unit> {
        return when (val result = safeApiCall(context, errorAdapter) { notesApi.getNotes() }) {
            is ApiResult.Success -> {
                val dtos = result.data.data?.items.orEmpty()
                // Заметки с незапушенным UPDATE не трогаем — их разрешит конфликт-чек в SyncWorker
                // перед отправкой; если пул перезапишет их первым, локальная правка молча потеряется.
                val dirtyLocalIds = outboxRepository.pendingLocalIds(OutboxEntityType.NOTE, OutboxOperation.UPDATE)
                for (dto in dtos) upsertFromDto(dto, dirtyLocalIds)
                noteDao.deleteMissing(dtos.map { it.id })
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> result
        }
    }

    /** Онлайн — /notes/search (см. project memory: ищет по title+note). Офлайн или при сетевой
     * ошибке — тихий фолбэк на локальный LIKE-поиск по Room, без ошибки в UI. */
    suspend fun search(query: String): List<NoteEntity> {
        if (!connectivityObserver.isOnlineNow()) {
            return noteDao.searchLocal(query)
        }
        return when (val result = safeApiCall(context, errorAdapter) { notesApi.search(query) }) {
            is ApiResult.Success -> {
                val dirtyLocalIds = outboxRepository.pendingLocalIds(OutboxEntityType.NOTE, OutboxOperation.UPDATE)
                result.data.data.orEmpty().map { upsertFromDto(it, dirtyLocalIds) }
            }
            is ApiResult.Failure -> noteDao.searchLocal(query)
        }
    }

    suspend fun createNote(title: String, note: String, categoryLocalId: Long?, status: String = DEFAULT_NOTE_STATUS): Long {
        val now = Instant.now().toString()
        val entity = NoteEntity(
            title = title,
            note = note,
            status = status,
            categoryLocalId = categoryLocalId,
            createdAt = now,
            updatedAt = now,
        )
        val localId = noteDao.upsert(entity)
        outboxRepository.enqueueCreate(OutboxEntityType.NOTE, localId)
        syncScheduler.enqueueOneTime()
        return localId
    }

    suspend fun updateNote(localId: Long, title: String, note: String, status: String, categoryLocalId: Long?) {
        val current = noteDao.getByLocalId(localId) ?: return
        noteDao.upsert(
            current.copy(
                title = title,
                note = note,
                status = status,
                categoryLocalId = categoryLocalId,
                updatedAt = Instant.now().toString(),
            ),
        )
        outboxRepository.enqueueUpdate(OutboxEntityType.NOTE, localId)
        syncScheduler.enqueueOneTime()
    }

    suspend fun deleteNote(localId: Long) {
        val neverSynced = outboxRepository.enqueueDelete(OutboxEntityType.NOTE, localId)
        if (neverSynced) {
            noteDao.deleteByLocalId(localId)
        } else {
            noteDao.setPendingDelete(localId, true)
            syncScheduler.enqueueOneTime()
        }
    }

    /** Пишем локально сразу (мгновенный отклик UI), сеть дожимает SyncWorker — сверкой isStarred
     * vs serverFavorite, а не через outbox (см. NoteEntity.serverFavorite: `GET /notes/favorite/{id}`
     * это тоггл, а не установка конкретного значения, поэтому очередь команд здесь не подходит). */
    suspend fun toggleStarred(localId: Long, current: NoteEntity) {
        noteDao.upsert(current.copy(isStarred = !current.isStarred))
        syncScheduler.enqueueOneTime()
    }

    /** Пользователь выбрал "оставить мою версию" в диалоге конфликта: принимаем серверный
     * updatedAt как новый baseline (мы его видели и осознанно перекрываем) и оставляем
     * незапушенный UPDATE в очереди — следующий цикл синка теперь пройдёт конфликт-чек и отправит
     * локальное содержимое. */
    suspend fun resolveConflictKeepLocal(localId: Long) {
        val note = noteDao.getByLocalId(localId) ?: return
        noteDao.upsert(
            note.copy(
                serverUpdatedAt = note.conflictServerUpdatedAt,
                conflictServerTitle = null,
                conflictServerNote = null,
                conflictServerStatus = null,
                conflictServerUpdatedAt = null,
            ),
        )
        syncScheduler.enqueueOneTime()
    }

    /** Пользователь выбрал "принять версию сервера": локальная копия перезаписывается снимком
     * конфликта, незапушенный UPDATE отменяется — пушить больше нечего. */
    suspend fun resolveConflictAcceptServer(localId: Long) {
        val note = noteDao.getByLocalId(localId) ?: return
        val conflictUpdatedAt = note.conflictServerUpdatedAt ?: return
        noteDao.upsert(
            note.copy(
                title = note.conflictServerTitle ?: note.title,
                note = note.conflictServerNote ?: note.note,
                status = note.conflictServerStatus ?: note.status,
                updatedAt = conflictUpdatedAt,
                serverUpdatedAt = conflictUpdatedAt,
                conflictServerTitle = null,
                conflictServerNote = null,
                conflictServerStatus = null,
                conflictServerUpdatedAt = null,
            ),
        )
        outboxRepository.cancelPending(OutboxEntityType.NOTE, localId)
    }

    private suspend fun upsertFromDto(dto: NoteDto, dirtyLocalIds: Set<Long> = emptySet()): NoteEntity {
        val existing = noteDao.getByServerId(dto.id)
        if (existing != null && existing.localId in dirtyLocalIds) return existing
        val categoryLocalId = if (dto.category_id == ZERO_OBJECT_ID) {
            null
        } else {
            categoryDao.getByServerId(dto.category_id)?.localId
        }
        // Избранное разошлось с baseline — значит есть непушнутый тоггл, который сверочный проход
        // SyncWorker ещё не отправил; не затираем его пулом (иначе локальное намерение потеряется).
        val favoriteDirty = existing != null && existing.isStarred != existing.serverFavorite
        val entity = NoteEntity(
            localId = existing?.localId ?: 0,
            serverId = dto.id,
            title = dto.title,
            note = dto.note,
            status = dto.status,
            categoryLocalId = categoryLocalId,
            isStarred = if (favoriteDirty) existing!!.isStarred else dto.favorite,
            createdAt = dto.created_at,
            updatedAt = dto.updated_at,
            serverUpdatedAt = dto.updated_at,
            serverFavorite = if (favoriteDirty) existing!!.serverFavorite else dto.favorite,
        )
        val localId = noteDao.upsert(entity)
        return entity.copy(localId = localId)
    }
}
