// app/src/main/java/com/example/safitness/data/repo/Repos.kt
package com.example.safitness.data.repo

import android.content.Context
import com.example.safitness.data.db.AppDatabase

object Repos {
    fun workoutRepository(context: Context) = with(AppDatabase.get(context)) {
        WorkoutRepository(
            libraryDao = libraryDao(),
            programDao = programDao(),
            sessionDao = sessionDao(),
            prDao = personalRecordDao(),
            metconDao = metconDao(),
            planDao = planDao()
        )
    }

    fun planDao(context: Context) = AppDatabase.get(context).planDao()
    fun libraryDao(context: Context) = AppDatabase.get(context).libraryDao()
    fun metconDao(context: Context) = AppDatabase.get(context).metconDao()

    // ✅ correct name: userProfileDao() (lowercase u), no extra params
    fun userProfileDao(context: Context) = AppDatabase.get(context).userProfileDao()
    fun mlService(ctx: android.content.Context): com.example.safitness.ml.MLService {
        val db = com.example.safitness.data.db.AppDatabase.get(ctx)
        // Local stub for now; you can later swap this to a network-backed implementation.
        return com.example.safitness.ml.LocalMLStub(db.libraryDao())
    }

    fun plannerRepository(ctx: android.content.Context): com.example.safitness.data.repo.PlannerRepository {
        val db = com.example.safitness.data.db.AppDatabase.get(ctx)
        return com.example.safitness.data.repo.PlannerRepository(
            planDao = db.planDao(),
            libraryDao = db.libraryDao(),
            metconDao = db.metconDao()
        )
    }

}
