package app.mininote.mininote.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import app.mininote.mininote.data.local.db.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY isDefault DESC, id ASC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: Long): ServerEntity?

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun count(): Int

    @Insert
    suspend fun insert(server: ServerEntity): Long

    @Delete
    suspend fun delete(server: ServerEntity)
}
