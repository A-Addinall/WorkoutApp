package com.example.safitness

import android.app.Application
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.seed.*  // <-- add this import
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutApp : Application() {
    lateinit var db: AppDatabase
        private set

    // keep a dedicated Application-level scope
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.get(this)

        // run idempotent seed on every launch
        appScope.launch {
            ExerciseSeed.seedOrUpdate(db)
            MetconSeed.seedOrUpdate(db)
        }
    }
}
