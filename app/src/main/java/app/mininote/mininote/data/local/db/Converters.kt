package app.mininote.mininote.data.local.db

import androidx.room.TypeConverter
import app.mininote.mininote.data.local.db.entity.OutboxEntityType
import app.mininote.mininote.data.local.db.entity.OutboxOperation

class Converters {
    @TypeConverter
    fun fromEntityType(value: OutboxEntityType): String = value.name

    @TypeConverter
    fun toEntityType(value: String): OutboxEntityType = OutboxEntityType.valueOf(value)

    @TypeConverter
    fun fromOperation(value: OutboxOperation): String = value.name

    @TypeConverter
    fun toOperation(value: String): OutboxOperation = OutboxOperation.valueOf(value)
}
