package app.mininote.mininote.data.repository

import android.content.Context
import app.mininote.mininote.data.local.db.dao.CategoryDao
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import app.mininote.mininote.data.local.db.entity.OutboxEntityType
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.remote.ZERO_OBJECT_ID
import app.mininote.mininote.data.remote.api.CategoriesApi
import app.mininote.mininote.data.remote.dto.CategoryDto
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import app.mininote.mininote.data.remote.safeApiCall
import app.mininote.mininote.sync.SyncScheduler
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoriesApi: CategoriesApi,
    private val categoryDao: CategoryDao,
    private val outboxRepository: OutboxRepository,
    private val syncScheduler: SyncScheduler,
    private val errorAdapter: JsonAdapter<ErrorEnvelopeDto>,
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun refresh(): ApiResult<Unit> {
        return when (val result = safeApiCall(context, errorAdapter) { categoriesApi.getCategories() }) {
            is ApiResult.Success -> {
                val dtos = result.data.data?.items.orEmpty()
                // Категории-родители должны быть засеяны раньше детей, чтобы parentLocalId резолвился с первого прохода.
                val ordered = dtos.sortedBy { if (it.parent_id == ZERO_OBJECT_ID) 0 else 1 }
                for (dto in ordered) {
                    upsertFromDto(dto)
                }
                categoryDao.deleteMissing(dtos.map { it.id })
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> result
        }
    }

    /** Офлайн-первое создание: пишем локально и ставим CREATE в очередь — сеть отсюда не
     * вызывается напрямую, её дожимает SyncWorker. */
    suspend fun createCategory(name: String, parentLocalId: Long? = null): CategoryEntity {
        val entity = CategoryEntity(name = name, parentLocalId = parentLocalId, sort = 0)
        val localId = categoryDao.upsert(entity)
        outboxRepository.enqueueCreate(OutboxEntityType.CATEGORY, localId)
        syncScheduler.enqueueOneTime()
        return entity.copy(localId = localId)
    }

    /** Офлайн-первое обновление — та же схема, что и у заметок (см. NotesRepository.updateNote). */
    suspend fun updateCategory(localId: Long, name: String, parentLocalId: Long?) {
        val existing = categoryDao.getByLocalId(localId) ?: return
        categoryDao.upsert(existing.copy(name = name, parentLocalId = parentLocalId))
        outboxRepository.enqueueUpdate(OutboxEntityType.CATEGORY, localId)
        syncScheduler.enqueueOneTime()
    }

    /**
     * Дети удаляемой категории репереходят на корень (не оставляем висячих parentLocalId).
     * Если категория никогда не синхронизировалась — [OutboxRepository.enqueueDelete] вернёт
     * true и мы просто удаляем её локально; иначе скрываем через pendingDelete и ждём синка.
     */
    suspend fun deleteCategory(localId: Long) {
        categoryDao.clearParent(localId)
        val neverSynced = outboxRepository.enqueueDelete(OutboxEntityType.CATEGORY, localId)
        if (neverSynced) {
            categoryDao.deleteByLocalId(localId)
        } else {
            categoryDao.setPendingDelete(localId, true)
        }
        syncScheduler.enqueueOneTime()
    }

    private suspend fun upsertFromDto(dto: CategoryDto) {
        val existing = categoryDao.getByServerId(dto.id)
        val parentLocalId = if (dto.parent_id == ZERO_OBJECT_ID) {
            null
        } else {
            categoryDao.getByServerId(dto.parent_id)?.localId
        }
        val entity = CategoryEntity(
            localId = existing?.localId ?: 0,
            serverId = dto.id,
            name = dto.name,
            parentLocalId = parentLocalId,
            sort = dto.sort,
        )
        categoryDao.upsert(entity)
    }
}
