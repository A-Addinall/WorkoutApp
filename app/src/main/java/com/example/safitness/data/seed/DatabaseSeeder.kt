package com.example.safitness.data.seed

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.safitness.data.dao.PlanDao
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.PhaseEntity
import com.example.safitness.data.entities.WeekDayPlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

object DatabaseSeeder {

    /**
     * Idempotent scaffold: if no phases/plans exist, create
     * Phase(4 weeks) and WeekDayPlan rows for days 1..5 each week.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun seedPhaseScaffold(db: AppDatabase) = withContext(Dispatchers.IO) {
        val planDao: PlanDao = db.planDao()

        val hasPhase = planDao.countPhases() > 0
        val hasPlans = planDao.countPlans() > 0
        if (hasPhase || hasPlans) return@withContext

        val phaseId = planDao.insertPhase(
            PhaseEntity(
                name = "Phase 1",
                startDateEpochDay = LocalDate.now().toEpochDay(),
                weeks = 4
            )
        )

        val plans = buildList {
            for (w in 1..4) {
                for (d in 1..5) {
                    add(
                        WeekDayPlanEntity(
                            phaseId = phaseId,
                            weekIndex = w,
                            dayIndex = d,
                            dateEpochDay = null
                        )
                    )
                }
            }
        }
        planDao.insertWeekPlans(plans)
    }
}
