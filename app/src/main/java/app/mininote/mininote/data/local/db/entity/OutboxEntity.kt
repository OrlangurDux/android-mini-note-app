package app.mininote.mininote.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OutboxEntityType { NOTE, CATEGORY }
enum class OutboxOperation { CREATE, UPDATE, DELETE }

/**
 * Не хранит снимок данных заметки/категории — воркер синхронизации перечитывает актуальное
 * состояние из Room в момент отправки (см. project memory / план: "outbox без payload").
 * Порядок отправки — глобальный FIFO по [id] (autoincrement).
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: OutboxEntityType,
    val operation: OutboxOperation,
    val localEntityId: Long,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)
