package app.mininote.mininote.data.repository

import app.mininote.mininote.data.local.db.dao.OutboxDao
import app.mininote.mininote.data.local.db.entity.OutboxEntity
import app.mininote.mininote.data.local.db.entity.OutboxEntityType
import app.mininote.mininote.data.local.db.entity.OutboxOperation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Очередь без снимков данных — [SyncWorker][app.mininote.mininote.sync.SyncWorker] перечитывает
 * актуальное состояние сущности из Room в момент отправки. Здесь — только логика постановки в
 * очередь с правилами слияния (см. план/project memory):
 * CREATE+UPDATE→CREATE, CREATE+DELETE→ничего не отправлять (удалить локально), UPDATE+UPDATE→UPDATE,
 * UPDATE+DELETE→DELETE.
 */
@Singleton
class OutboxRepository @Inject constructor(
    private val outboxDao: OutboxDao,
) {
    fun observePendingCount(): Flow<Int> = outboxDao.observeCount()

    suspend fun enqueueCreate(type: OutboxEntityType, localId: Long) {
        outboxDao.insert(
            OutboxEntity(entityType = type, operation = OutboxOperation.CREATE, localEntityId = localId, createdAt = System.currentTimeMillis()),
        )
    }

    /** Пока есть неотправленный CREATE/UPDATE — ничего менять не нужно, воркер и так прочитает свежие данные. */
    suspend fun enqueueUpdate(type: OutboxEntityType, localId: Long) {
        val pending = outboxDao.findPending(type, localId)
        if (pending == null) {
            outboxDao.insert(
                OutboxEntity(entityType = type, operation = OutboxOperation.UPDATE, localEntityId = localId, createdAt = System.currentTimeMillis()),
            )
        }
    }

    /**
     * @return true — сущность никогда не синхронизировалась (был только CREATE в очереди),
     * вызывающий код должен немедленно удалить её локально без обращения к серверу.
     * false — поставлено (или обновлено) удаление в очередь, сущность нужно скрыть из UI
     * (pendingDelete=true) и дождаться синхронизации.
     */
    suspend fun enqueueDelete(type: OutboxEntityType, localId: Long): Boolean {
        val pending = outboxDao.findPending(type, localId)
        if (pending != null && pending.operation == OutboxOperation.CREATE) {
            outboxDao.deleteById(pending.id)
            return true
        }
        if (pending != null) {
            outboxDao.update(pending.copy(operation = OutboxOperation.DELETE, attemptCount = 0, lastError = null))
        } else {
            outboxDao.insert(
                OutboxEntity(entityType = type, operation = OutboxOperation.DELETE, localEntityId = localId, createdAt = System.currentTimeMillis()),
            )
        }
        return false
    }

    suspend fun getQueueOrdered(): List<OutboxEntity> = outboxDao.getAllOrdered()

    suspend fun remove(entry: OutboxEntity) = outboxDao.deleteById(entry.id)

    suspend fun markFailed(entry: OutboxEntity, error: String) {
        outboxDao.update(entry.copy(attemptCount = entry.attemptCount + 1, lastError = error))
    }

    /** localId сущностей с ещё не отправленной операцией данного типа — используется, чтобы pull
     * (полный пул с сервера) не затирал заметки, которые ждут своей очереди на конфликт-чек/пуш. */
    suspend fun pendingLocalIds(type: OutboxEntityType, operation: OutboxOperation): Set<Long> =
        outboxDao.getAllOrdered().filter { it.entityType == type && it.operation == operation }.map { it.localEntityId }.toSet()

    /** Отменяет ещё не отправленную операцию (используется при разрешении конфликта в пользу сервера). */
    suspend fun cancelPending(type: OutboxEntityType, localId: Long) {
        outboxDao.findPending(type, localId)?.let { outboxDao.deleteById(it.id) }
    }
}
