package app.mininote.mininote.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * localId — стабильный id для UI/навигации (в т.ч. до синхронизации, когда serverId ещё null).
 * isStarred — избранное: теперь настоящее серверное поле (`GET /notes/favorite/{id}` — тоггл,
 * см. SyncWorker), не только локальное — см. [serverFavorite] ниже про механизм синка.
 * status — enum на бэкенде (`draft`/`public`/`archive`, см. `util/NoteStatus.kt`), больше не
 * произвольная строка (это уточнение сделано позже исходной находки "бэкенд не валидирует").
 *
 * serverUpdatedAt/conflictServer* — обнаружение конфликтов синка (см. [SyncWorker][app.mininote.mininote.sync.SyncWorker]):
 * serverUpdatedAt — updated_at сервера, каким его клиент последний раз достоверно видел (baseline
 * для сравнения); conflictServer* — снимок серверной версии на момент, когда перед отправкой
 * локальной правки обнаружилось, что сервер уже успел измениться. conflictServerUpdatedAt != null
 * означает неразрешённый конфликт (см. [hasConflict]).
 *
 * serverFavorite — baseline избранного, каким его клиент последний раз достоверно видел с сервера
 * (аналогично serverUpdatedAt). `GET /notes/favorite/{id}` — не PUT с телом, а именно тоггл: каждый
 * вызов инвертирует состояние на сервере. Поэтому избранное синкается не через outbox-очередь
 * (которая рассчитана на максимум одну ожидающую операцию на сущность), а через сверку состояния —
 * SyncWorker на каждый цикл ищет заметки, где isStarred != serverFavorite, и шлёт РОВНО один
 * тоггл-вызов на заметку (сколько бы раз её ни звездили офлайн — важна только чётность/итоговое
 * состояние, а не количество нажатий).
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val title: String,
    val note: String,
    val status: String,
    val categoryLocalId: Long? = null,
    val isStarred: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    /** Заметка отправлена в outbox на удаление, но ещё не подтверждена сервером — скрыта из UI. */
    val pendingDelete: Boolean = false,
    val serverUpdatedAt: String? = null,
    val conflictServerTitle: String? = null,
    val conflictServerNote: String? = null,
    val conflictServerStatus: String? = null,
    val conflictServerUpdatedAt: String? = null,
    val serverFavorite: Boolean = false,
)

val NoteEntity.hasConflict: Boolean get() = conflictServerUpdatedAt != null
