package com.example.safitness

import android.app.Application
import com.example.safitness.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.get(this)   // No manual seeding call needed
        // init repositories/viewModels as you prefer…
    }
}
