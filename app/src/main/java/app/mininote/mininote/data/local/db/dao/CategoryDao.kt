package app.mininote.mininote.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE pendingDelete = 0 ORDER BY sort ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE localId = :localId")
    suspend fun getByLocalId(localId: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("UPDATE categories SET pendingDelete = :pending WHERE localId = :localId")
    suspend fun setPendingDelete(localId: Long, pending: Boolean)

    /** Реродительство детей удаляемой категории на корень — не оставляем висячих ссылок на localId. */
    @Query("UPDATE categories SET parentLocalId = NULL WHERE parentLocalId = :localId")
    suspend fun clearParent(localId: Long)

    @Query("DELETE FROM categories WHERE serverId IS NOT NULL AND pendingDelete = 0 AND serverId NOT IN (:serverIds)")
    suspend fun deleteMissing(serverIds: List<String>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
