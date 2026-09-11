package app.mininote.mininote.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.mininote.mininote.data.local.db.entity.OutboxEntity
import app.mininote.mininote.data.local.db.entity.OutboxEntityType
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE entityType = :type AND localEntityId = :localId LIMIT 1")
    suspend fun findPending(type: OutboxEntityType, localId: Long): OutboxEntity?

    @Insert
    suspend fun insert(entry: OutboxEntity): Long

    @Update
    suspend fun update(entry: OutboxEntity)

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM outbox ORDER BY id ASC")
    suspend fun getAllOrdered(): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM outbox")
    suspend fun deleteAll()
}
