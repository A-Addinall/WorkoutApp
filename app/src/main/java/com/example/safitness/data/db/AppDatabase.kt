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
    version = 3,
    entities = [
        Exercise::class,
        ProgramSelection::class,
        WorkoutSession::class,
        SetLog::class,
        PersonalRecord::class,
        UserSettings::class,
        // NEW metcon library:
        MetconPlan::class,
        MetconComponent::class,
        ProgramMetconSelection::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun exerciseDao(): ExerciseDao

    // NEW
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
                    .fallbackToDestructiveMigration() // dev-friendly
                    .addCallback(object : Callback() {
                        override fun onCreate(dbObj: SupportSQLiteDatabase) {
                            super.onCreate(dbObj)
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = get(context)

                                // Seed exercises if empty
                                if (db.libraryDao().countExercises() == 0) {
                                    db.libraryDao().insertAll(ExerciseSeed.DEFAULT_EXERCISES)
                                }

                                // Seed metcon plans/components if empty
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
