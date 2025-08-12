package com.example.safitness.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import com.example.safitness.data.seed.*
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
        // NEW:
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metcon_plan (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        durationMinutes INTEGER,
                        emomIntervalSec INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metcon_component (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        planId INTEGER NOT NULL,
                        orderInPlan INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        FOREIGN KEY(planId) REFERENCES metcon_plan(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_metcon_component_planId ON metcon_component(planId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_metcon_component_planId_orderInPlan ON metcon_component(planId, orderInPlan)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS program_metcon_selection (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dayIndex INTEGER NOT NULL,
                        planId INTEGER NOT NULL,
                        required INTEGER NOT NULL,
                        displayOrder INTEGER NOT NULL,
                        FOREIGN KEY(planId) REFERENCES metcon_plan(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_program_metcon_selection_dayIndex ON program_metcon_selection(dayIndex)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_program_metcon_selection_dayIndex_displayOrder ON program_metcon_selection(dayIndex, displayOrder)")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safitness.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration() // dev-friendly; remove for prod
                    .addCallback(object : Callback() {
                        override fun onCreate(dbObj: SupportSQLiteDatabase) {
                            super.onCreate(dbObj)
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = get(context)
                                if (db.libraryDao().countExercises() == 0) {
                                    db.libraryDao().insertAll(ExerciseSeed.DEFAULT_EXERCISES)
                                }
                                if (db.metconDao().countPlans() == 0) {
                                    MetconSeed.seedDefaults(db)
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
