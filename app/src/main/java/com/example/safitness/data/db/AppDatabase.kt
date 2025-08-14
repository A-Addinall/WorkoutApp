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
import com.example.safitness.data.seed.MetconSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    version = 5, // bumped from 4 -> 5
    entities = [
        Exercise::class,
        ProgramSelection::class,
        WorkoutSession::class,
        SetLog::class,
        PersonalRecord::class,
        UserSettings::class,
        // Metcon library:
        MetconPlan::class,
        MetconComponent::class,
        ProgramMetconSelection::class,
        // NEW: plan-scoped metcon results
        MetconLog::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun exerciseDao(): ExerciseDao

    // Metcon
    abstract fun metconDao(): MetconDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safitness.db"
                )
                    .fallbackToDestructiveMigration() // dev-friendly while schema is evolving
                    .addCallback(object : Callback() {
                        override fun onCreate(dbObj: SupportSQLiteDatabase) {
                            super.onCreate(dbObj)
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = get(context)

                                if (db.libraryDao().countExercises() == 0) {
                                    db.libraryDao().insertAll(ExerciseSeed.DEFAULT_EXERCISES)
                                }
                                if (db.metconDao().countPlans() == 0) {
                                    MetconSeed.seed(db)
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
}
