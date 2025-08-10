// app/src/main/java/com/example/safitness/data/db/AppDatabase.kt
package com.example.safitness.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import com.example.safitness.data.seed.ExerciseSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    version = 1,
    entities = [
        Exercise::class,
        ProgramSelection::class,
        WorkoutSession::class,
        SetLog::class,
        PersonalRecord::class,
        UserSettings::class
    ]
)
@TypeConverters(Converters::class)  // ← add this
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safitness.db"
                )
                    .fallbackToDestructiveMigration() // dev-friendly
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
