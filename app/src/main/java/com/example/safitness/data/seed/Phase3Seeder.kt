package com.example.safitness.data.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Dev-only sample data for Phase 3.
 * Triggered via RoomDatabase.Callback.onCreate (brand-new DB only).
 */
object Phase3Seeder {

    val callback: RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            // ENGINE samples
            db.execSQL("""
        INSERT INTO engine_log(date, mode, intent, programDistanceMeters, resultTimeSeconds, pace, scaled, notes)
        VALUES (strftime('%s','now')-86400, 'RUN', 'FOR_TIME', 5000, 1500, 300.0, 0, 'Sample 5k run in 25:00')
      """.trimIndent())

            db.execSQL("""
        INSERT INTO engine_log(date, mode, intent, programDurationSeconds, resultDistanceMeters, scaled, notes)
        VALUES (strftime('%s','now')-432000, 'ROW', 'FOR_DISTANCE', 600, 2200, 0, '10-min row, 2.2km')
      """.trimIndent())

            db.execSQL("""
        INSERT INTO engine_log(date, mode, intent, programDurationSeconds, resultCalories, scaled, notes)
        VALUES (strftime('%s','now')-2592000, 'BIKE', 'FOR_CALORIES', 600, 180, 0, '10-min bike for calories')
      """.trimIndent())

            // SKILL samples
            db.execSQL("""
        INSERT INTO skill_log(date, skill, testType, reps, scaled, notes)
        VALUES (strftime('%s','now')-7200, 'DOUBLE_UNDERS', 'MAX_REPS_UNBROKEN', 62, 0, 'Unbroken DU set')
      """.trimIndent())

            db.execSQL("""
        INSERT INTO skill_log(date, skill, testType, maxHoldSeconds, scaled, notes)
        VALUES (strftime('%s','now')-604800, 'HANDSTAND_HOLD', 'MAX_HOLD_SECONDS', 38, 1, 'Wall-assisted')
      """.trimIndent())

            db.execSQL("""
        INSERT INTO skill_log(date, skill, testType, attempts, reps, scaled, notes)
        VALUES (strftime('%s','now')-1814400, 'MUSCLE_UP', 'ATTEMPTS', 5, 0, 1, 'Banded progression')
      """.trimIndent())
        }
    }
}
