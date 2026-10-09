package app.mininote.mininote.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["serverId"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val name: String,
    val parentLocalId: Long? = null,
    val sort: Int = 0,
    val pendingDelete: Boolean = false,
)
