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
import com.example.safitness.data.seed.SkillLibrarySeeder
import com.example.safitness.data.seed.EngineLibrarySeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.safitness.data.dao.EnginePlanDao
import com.example.safitness.data.dao.SkillPlanDao
import com.example.safitness.data.dao.EngineLogDao
import com.example.safitness.data.dao.SkillLogDao
import com.example.safitness.data.entities.EnginePlanEntity
import com.example.safitness.data.entities.EngineComponentEntity
import com.example.safitness.data.entities.SkillPlanEntity
import com.example.safitness.data.entities.SkillComponentEntity
import com.example.safitness.data.entities.EngineLogEntity
import com.example.safitness.data.entities.SkillLogEntity


@Database(
    version = 8,                 // bump for new tables
    exportSchema = false,        // fine for dev; turn on for prod
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

        EnginePlanEntity::class,
        EngineComponentEntity::class,
        SkillPlanEntity::class,
        SkillComponentEntity::class,

// Engine/Skill log entities
        EngineLogEntity::class,
        SkillLogEntity::class,
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

    // NEW
    abstract fun planDao(): PlanDao
    // Library DAOs
    abstract fun enginePlanDao(): EnginePlanDao
    abstract fun skillPlanDao(): SkillPlanDao

    // Log DAOs
    abstract fun engineLogDao(): EngineLogDao
    abstract fun skillLogDao(): SkillLogDao

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
                                    SkillLibrarySeeder.seedIfNeeded(db)
                                    EngineLibrarySeeder.seedIfNeeded(db)
                                }
                            }
                        }

                        override fun onOpen(dbObj: SupportSQLiteDatabase) {
                            super.onOpen(dbObj)
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = INSTANCE ?: return@launch
                                MetconSeed.seedOrUpdate(db)
                                SkillLibrarySeeder.seedIfNeeded(db)
                                EngineLibrarySeeder.seedIfNeeded(db)
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
