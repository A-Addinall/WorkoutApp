package com.example.safitness.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import com.example.safitness.data.seed.ExerciseSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    version = 2, // bumped from 1 → 2 to add SetLog.metconResult
    entities = [
        Exercise::class,
        ProgramSelection::class,
        WorkoutSession::class,
        SetLog::class,
        PersonalRecord::class,
        UserSettings::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Simple additive migration: add TEXT column for enum name
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE SetLog ADD COLUMN metconResult TEXT")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safitness.db"
                )
                    .fallbackToDestructiveMigration() // dev-friendly; keep until real paths exist
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(dbObj: SupportSQLiteDatabase) {
                            super.onCreate(dbObj)
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = get(context)
                                if (db.libraryDao().countExercises() == 0) {
                                    db.libraryDao().insertAll(ExerciseSeed.DEFAULT_EXERCISES)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialExercises() {
        if (libraryDao().countExercises() == 0) {
            libraryDao().insertAll(ExerciseSeed.DEFAULT_EXERCISES)
        }
    }
}
