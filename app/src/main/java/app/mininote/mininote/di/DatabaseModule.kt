package app.mininote.mininote.di

import android.content.Context
import androidx.room.Room
import app.mininote.mininote.data.local.db.AppDatabase
import app.mininote.mininote.data.local.db.dao.CategoryDao
import app.mininote.mininote.data.local.db.dao.NoteDao
import app.mininote.mininote.data.local.db.dao.OutboxDao
import app.mininote.mininote.data.local.db.dao.ServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mininote.db")
            // До релиза реальных миграций нет — при изменении схемы просто пересоздаём локальную БД
            // (это только кэш серверных данных + недосинхронизированная локальная очередь; в проде,
            // когда закладывать назад будет поздно, здесь нужны настоящие Migration).
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideServerDao(db: AppDatabase): ServerDao = db.serverDao()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideOutboxDao(db: AppDatabase): OutboxDao = db.outboxDao()
}
