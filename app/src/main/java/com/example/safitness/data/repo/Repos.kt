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
            planDao = planDao() // NEW
        )
    }
}
