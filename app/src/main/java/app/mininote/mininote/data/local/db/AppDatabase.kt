package app.mininote.mininote.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.mininote.mininote.data.local.db.dao.CategoryDao
import app.mininote.mininote.data.local.db.dao.NoteDao
import app.mininote.mininote.data.local.db.dao.OutboxDao
import app.mininote.mininote.data.local.db.dao.ServerDao
import app.mininote.mininote.data.local.db.entity.CategoryEntity
import app.mininote.mininote.data.local.db.entity.NoteEntity
import app.mininote.mininote.data.local.db.entity.OutboxEntity
import app.mininote.mininote.data.local.db.entity.ServerEntity

@Database(
    entities = [ServerEntity::class, NoteEntity::class, CategoryEntity::class, OutboxEntity::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun outboxDao(): OutboxDao
}
