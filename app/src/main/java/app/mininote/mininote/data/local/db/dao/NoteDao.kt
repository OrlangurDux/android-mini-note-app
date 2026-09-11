package app.mininote.mininote.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.mininote.mininote.data.local.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE pendingDelete = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE localId = :localId")
    fun observeByLocalId(localId: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE localId = :localId")
    suspend fun getByLocalId(localId: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("UPDATE notes SET pendingDelete = :pending WHERE localId = :localId")
    suspend fun setPendingDelete(localId: Long, pending: Boolean)

    /** Убирает из кэша заметки, удалённые на сервере другими клиентами (только среди уже синхронизированных). */
    @Query("DELETE FROM notes WHERE serverId IS NOT NULL AND pendingDelete = 0 AND serverId NOT IN (:serverIds)")
    suspend fun deleteMissing(serverIds: List<String>)

    /** Заметки, где локальное избранное разошлось с последним известным состоянием сервера —
     * см. NoteEntity.serverFavorite: SyncWorker шлёт ровно один тоггл-вызов на каждую такую запись. */
    @Query("SELECT * FROM notes WHERE pendingDelete = 0 AND serverId IS NOT NULL AND isStarred != serverFavorite")
    suspend fun getFavoriteDirty(): List<NoteEntity>

    /** Локальный поиск (офлайн-фолбэк, когда недоступен /notes/search). */
    @Query(
        "SELECT * FROM notes WHERE pendingDelete = 0 AND (title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') " +
            "ORDER BY updatedAt DESC",
    )
    suspend fun searchLocal(query: String): List<NoteEntity>

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
