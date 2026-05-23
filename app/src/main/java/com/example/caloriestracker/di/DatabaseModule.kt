package com.example.caloriestracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.caloriestracker.data.database.AppDatabase
import com.example.caloriestracker.data.database.JournalDao
import com.example.caloriestracker.data.repository.JournalRepositoryImpl
import com.example.caloriestracker.domain.repository.JournalRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindJournalRepository(
        journalRepositoryImpl: JournalRepositoryImpl
    ): JournalRepository

    companion object {
        
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "journal_database"
            ).addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Drop the standard table and replace it with FTS5 virtual table
                    db.execSQL("DROP TABLE IF EXISTS entries_fts")
                    db.execSQL("CREATE VIRTUAL TABLE entries_fts USING fts5(title, content)")
                }
            }).fallbackToDestructiveMigration() // Simple strategy for personal use
             .build()
        }

        @Provides
        fun provideJournalDao(database: AppDatabase): JournalDao {
            return database.journalDao()
        }
    }
}
