package com.example.safitness.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import com.example.safitness.data.seed.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    version = 10,                 // bump for DayEngineSkillEntity (dev: destructive OK)
    exportSchema = false,
    entities = [
        // legacy
        Exercise::class,
        ProgramSelection::class,
        WorkoutSession::class,
        SetLog::class,
        PersonalRecord::class,
        UserSettings::class,
        // metcon library
        MetconPlan::class,
        MetconComponent::class,
        ProgramMetconSelection::class,
        MetconLog::class,
        // Phase 0
        PhaseEntity::class,
        WeekDayPlanEntity::class,
        DayItemEntity::class,

        // Phase 3 logs
        EngineLogEntity::class,
        SkillLogEntity::class,

        // NEW: Attach Engine/Skill items to a day
        DayEngineSkillEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun metconDao(): MetconDao
    abstract fun planDao(): PlanDao

    // Phase 3 DAOs
    abstract fun engineLogDao(): EngineLogDao
    abstract fun skillLogDao(): SkillLogDao

    // NEW
    abstract fun dayEngineSkillDao(): DayEngineSkillDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safitness.db"
                )
                    .fallbackToDestructiveMigration() // dev only
                    // sample engine/skill rows on first create
                    .addCallback(Phase3Seeder.callback)
                    .addCallback(object : Callback() {
                        override fun onCreate(dbObj: SupportSQLiteDatabase) {
                            super.onCreate(dbObj)
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = INSTANCE ?: return@launch
                                if (db.libraryDao().countExercises() == 0) {
                                    db.libraryDao().insertAll(ExerciseSeed.DEFAULT_EXERCISES)
                                }
                                if (db.metconDao().countPlans() == 0) {
                                    MetconSeed.seed(db)
                                }
                            }
                        }

                        override fun onOpen(dbObj: SupportSQLiteDatabase) {
                            super.onOpen(dbObj)
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = INSTANCE ?: return@launch
                                MetconSeed.seedOrUpdate(db)
                            }
                        }
                    })
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}
