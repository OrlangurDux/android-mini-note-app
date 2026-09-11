package app.mininote.mininote.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.mininote.mininote.data.local.db.dao.CategoryDao
import app.mininote.mininote.data.local.db.dao.NoteDao
import app.mininote.mininote.data.local.db.entity.OutboxEntity
import app.mininote.mininote.data.local.db.entity.OutboxEntityType
import app.mininote.mininote.data.local.db.entity.OutboxOperation
import app.mininote.mininote.data.local.security.TokenStore
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.remote.api.CategoriesApi
import app.mininote.mininote.data.remote.api.NotesApi
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import app.mininote.mininote.data.remote.safeApiCall
import app.mininote.mininote.data.repository.CategoriesRepository
import app.mininote.mininote.data.repository.NotesRepository
import app.mininote.mininote.data.repository.OutboxRepository
import com.squareup.moshi.JsonAdapter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Отправляет очередь [OutboxRepository] на сервер (FIFO, без сохранённых снимков — перечитывает
 * актуальное состояние из Room на каждую запись) и затем подтягивает серверную правду обратно
 * (pull). Гейтинг по токену: если он истёк — no-op успех, а не failure (см. план: не долбить retry
 * впустую, "нужен повторный вход" показывает отдельный наблюдатель в UI).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outboxRepository: OutboxRepository,
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val notesApi: NotesApi,
    private val categoriesApi: CategoriesApi,
    private val notesRepository: NotesRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tokenStore: TokenStore,
    private val errorAdapter: JsonAdapter<ErrorEnvelopeDto>,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!tokenStore.isValid()) {
            return Result.success()
        }

        for (entry in outboxRepository.getQueueOrdered()) {
            if (processEntry(entry)) {
                outboxRepository.remove(entry)
            }
            // false — оставляем запись в очереди и переходим к следующей (skip-and-continue),
            // не блокируя отправку остальных независимых операций.
        }

        syncFavorites()

        categoriesRepository.refresh()
        notesRepository.refresh()

        return Result.success()
    }

    /** Избранное не идёт через outbox (см. NoteEntity.serverFavorite) — сверка вместо очереди
     * команд: одна заметка с расхождением isStarred/serverFavorite получает ровно один
     * тоггл-вызов, независимо от того, сколько раз её нажимали офлайн. */
    private suspend fun syncFavorites() {
        for (note in noteDao.getFavoriteDirty()) {
            val serverId = note.serverId ?: continue
            when (safeApiCall(applicationContext, errorAdapter) { notesApi.toggleFavorite(serverId) }) {
                is ApiResult.Success -> noteDao.upsert(note.copy(serverFavorite = note.isStarred))
                is ApiResult.Failure -> Unit // попробуем снова в следующем цикле синка
            }
        }
    }

    private suspend fun processEntry(entry: OutboxEntity): Boolean {
        return try {
            when (entry.entityType) {
                OutboxEntityType.NOTE -> processNote(entry)
                OutboxEntityType.CATEGORY -> processCategory(entry)
            }
        } catch (e: Exception) {
            outboxRepository.markFailed(entry, e.message ?: "Unknown error")
            false
        }
    }

    private suspend fun processNote(entry: OutboxEntity): Boolean {
        val note = noteDao.getByLocalId(entry.localEntityId) ?: return true // сущности уже нет — запись устарела

        return when (entry.operation) {
            OutboxOperation.CREATE -> {
                val category = resolveCategory(note.categoryLocalId)
                if (category is CategoryResolution.Pending) return false
                when (
                    val result = safeApiCall(applicationContext, errorAdapter) {
                        notesApi.createNote(title = note.title, note = note.note, status = note.status, categoryId = category.serverIdOrNull())
                    }
                ) {
                    is ApiResult.Success -> {
                        val created = result.data.data ?: return false
                        noteDao.upsert(note.copy(serverId = created.id))
                        true
                    }
                    is ApiResult.Failure -> {
                        outboxRepository.markFailed(entry, result.message)
                        false
                    }
                }
            }
            OutboxOperation.UPDATE -> {
                val serverId = note.serverId ?: return false // CREATE для этой же заметки идёт раньше в FIFO — ждём его

                // Конфликт-чек: если сервер успел измениться с момента нашего последнего известного
                // baseline (serverUpdatedAt), значит заметку правили и на сервере, и офлайн локально —
                // не перезаписываем молча, оставляем запись в очереди и ждём решения пользователя
                // (см. NotesRepository.resolveConflictKeepLocal/AcceptServer, NoteDetailScreen).
                if (note.serverUpdatedAt != null) {
                    val serverNote = (safeApiCall(applicationContext, errorAdapter) { notesApi.getNote(serverId) } as? ApiResult.Success)?.data?.data
                    if (serverNote != null && serverNote.updated_at != note.serverUpdatedAt) {
                        noteDao.upsert(
                            note.copy(
                                conflictServerTitle = serverNote.title,
                                conflictServerNote = serverNote.note,
                                conflictServerStatus = serverNote.status,
                                conflictServerUpdatedAt = serverNote.updated_at,
                            ),
                        )
                        return false
                    }
                }

                val category = resolveCategory(note.categoryLocalId)
                if (category is CategoryResolution.Pending) return false
                when (
                    val result = safeApiCall(applicationContext, errorAdapter) {
                        notesApi.updateNote(serverId, title = note.title, note = note.note, status = note.status, categoryId = category.serverIdOrNull())
                    }
                ) {
                    is ApiResult.Success -> {
                        // PUT возвращает только AckEnvelopeDto без актуального updated_at (известный
                        // квирк бэкенда, см. project memory про created_at) — берём момент отправки
                        // как приближение нового baseline.
                        noteDao.upsert(note.copy(serverUpdatedAt = java.time.Instant.now().toString()))
                        true
                    }
                    is ApiResult.Failure -> {
                        outboxRepository.markFailed(entry, result.message)
                        false
                    }
                }
            }
            OutboxOperation.DELETE -> {
                val serverId = note.serverId
                if (serverId == null) {
                    noteDao.deleteByLocalId(note.localId)
                    return true
                }
                when (val result = safeApiCall(applicationContext, errorAdapter) { notesApi.deleteNote(serverId) }) {
                    is ApiResult.Success -> {
                        noteDao.deleteByLocalId(note.localId)
                        true
                    }
                    is ApiResult.Failure -> {
                        outboxRepository.markFailed(entry, result.message)
                        false
                    }
                }
            }
        }
    }

    private suspend fun processCategory(entry: OutboxEntity): Boolean {
        val category = categoryDao.getByLocalId(entry.localEntityId) ?: return true

        return when (entry.operation) {
            OutboxOperation.CREATE -> {
                val parent = resolveCategory(category.parentLocalId)
                if (parent is CategoryResolution.Pending) return false // родитель ещё не засинкан — ждём его очередь
                when (
                    val result = safeApiCall(applicationContext, errorAdapter) {
                        categoriesApi.createCategory(name = category.name, parentId = parent.serverIdOrNull(), sort = category.sort)
                    }
                ) {
                    is ApiResult.Success -> {
                        val created = result.data.data ?: return false
                        categoryDao.upsert(category.copy(serverId = created.id))
                        true
                    }
                    is ApiResult.Failure -> {
                        outboxRepository.markFailed(entry, result.message)
                        false
                    }
                }
            }
            OutboxOperation.UPDATE -> {
                val serverId = category.serverId ?: return false // CREATE для этой же категории идёт раньше в FIFO — ждём его
                val parent = resolveCategory(category.parentLocalId)
                if (parent is CategoryResolution.Pending) return false
                when (
                    val result = safeApiCall(applicationContext, errorAdapter) {
                        categoriesApi.updateCategory(serverId, name = category.name, parentId = parent.serverIdOrNull(), sort = category.sort)
                    }
                ) {
                    is ApiResult.Success -> true
                    is ApiResult.Failure -> {
                        outboxRepository.markFailed(entry, result.message)
                        false
                    }
                }
            }
            OutboxOperation.DELETE -> {
                val serverId = category.serverId
                if (serverId == null) {
                    categoryDao.deleteByLocalId(category.localId)
                    return true
                }
                when (val result = safeApiCall(applicationContext, errorAdapter) { categoriesApi.deleteCategory(serverId) }) {
                    is ApiResult.Success -> {
                        categoryDao.deleteByLocalId(category.localId)
                        true
                    }
                    is ApiResult.Failure -> {
                        outboxRepository.markFailed(entry, result.message)
                        false
                    }
                }
            }
        }
    }

    /**
     * "Нет категории"/"нет родителя" и "категория ещё не синхронизирована" — принципиально разные
     * исходы: в первом случае поле нужно НЕ передавать вовсе (Retrofit опускает null-поле — именно
     * так подтверждено эмпирически, что бэкенд создаёт заметку без категории; пустая строка
     * рискует повторить баг с `sort`, см. project memory). Nullable String не различил бы их,
     * поэтому — отдельный sealed-тип. Используется и для note→category, и для category→parent —
     * форма ссылки (nullable localId категории) одинаковая в обоих случаях.
     */
    private suspend fun resolveCategory(categoryLocalId: Long?): CategoryResolution {
        if (categoryLocalId == null) return CategoryResolution.NoCategory
        val category = categoryDao.getByLocalId(categoryLocalId) ?: return CategoryResolution.NoCategory
        return category.serverId?.let { CategoryResolution.Resolved(it) } ?: CategoryResolution.Pending
    }

    private sealed interface CategoryResolution {
        data object NoCategory : CategoryResolution
        data object Pending : CategoryResolution
        data class Resolved(val serverId: String) : CategoryResolution

        fun serverIdOrNull(): String? = (this as? Resolved)?.serverId
    }
}
