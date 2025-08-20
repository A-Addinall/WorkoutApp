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
            planDao = planDao() // already here
        )
    }

    // NEW: expose raw DAOs so MainActivity can use them
    fun planDao(context: Context) = AppDatabase.get(context).planDao()
    fun libraryDao(context: Context) = AppDatabase.get(context).libraryDao()

    fun metconDao(context: Context) = AppDatabase.get(context).metconDao()
}
