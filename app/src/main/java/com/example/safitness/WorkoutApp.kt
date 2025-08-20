package com.example.safitness

import android.app.Application
import android.os.Build
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.seed.*  // ExerciseSeed, MetconSeed, DevPhaseSeed_dev
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutApp : Application() {
    lateinit var db: AppDatabase
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.get(this)

        appScope.launch {
            // Idempotent content seeds
            ExerciseSeed.seedOrUpdate(db)
            MetconSeed.seedOrUpdate(db)
            EngineLibrarySeeder.seedIfNeeded(db)
            SkillLibrarySeeder.seedIfNeeded(db)
            }
        }
    }

