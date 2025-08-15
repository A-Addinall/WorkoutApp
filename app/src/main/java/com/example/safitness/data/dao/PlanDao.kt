package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.*
import kotlinx.coroutines.flow.Flow

/* ---------- DTOs for new model reads ---------- */

data class DayStrengthRow(
    @Embedded val exercise: Exercise,
    val required: Boolean,
    val targetReps: Int?,
    val sortOrder: Int
)

data class DayMetconSelectionRow(
    val planId: Long,
    val required: Boolean,
    val sortOrder: Int
)

@Dao
interface PlanDao {

    // ----- Phase -----
    @Insert
    suspend fun insertPhase(entity: PhaseEntity): Long

    @Query("SELECT COUNT(*) FROM phase")
    suspend fun countPhases(): Int

    /** "Current" phase = latest by id. */
    @Query("SELECT id FROM phase ORDER BY id DESC LIMIT 1")
    suspend fun currentPhaseId(): Long?

    // ----- Week/Day plan -----
    @Insert
    suspend fun insertWeekPlans(plans: List<WeekDayPlanEntity>)

    @Query("SELECT COUNT(*) FROM week_day_plan")
    suspend fun countPlans(): Int

    @Query("""
        SELECT * FROM week_day_plan
        WHERE phaseId = :phaseId AND weekIndex = :week AND dayIndex = :day
        LIMIT 1
    """)
    suspend fun getPlan(phaseId: Long, week: Int, day: Int): WeekDayPlanEntity?

    @Query("""
        SELECT id FROM week_day_plan
        WHERE phaseId = :phaseId AND weekIndex = :week AND dayIndex = :day
        LIMIT 1
    """)
    suspend fun getPlanId(phaseId: Long, week: Int, day: Int): Long?

    @Query("SELECT * FROM week_day_plan WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): WeekDayPlanEntity?

    @Query("""
        SELECT * FROM week_day_plan
        WHERE phaseId = :phaseId
        ORDER BY weekIndex ASC, dayIndex ASC, id ASC
    """)
    suspend fun getPlansForPhaseOrdered(phaseId: Long): List<WeekDayPlanEntity>

    @Query("""
        SELECT * FROM week_day_plan
        WHERE phaseId = :phaseId
          AND (weekIndex > :week OR (weekIndex = :week AND dayIndex > :day))
        ORDER BY weekIndex ASC, dayIndex ASC, id ASC
        LIMIT 1
    """)
    suspend fun getNextAfter(phaseId: Long, week: Int, day: Int): WeekDayPlanEntity?

    // ----- Day items (writes) -----
    @Insert
    suspend fun insertItems(items: List<DayItemEntity>)

    @Query("DELETE FROM day_item WHERE dayPlanId = :dayPlanId")
    suspend fun clearItems(dayPlanId: Long)

    // ----- Day items (reads) -----
    @Query("SELECT COUNT(*) FROM day_item WHERE dayPlanId = :dayPlanId")
    suspend fun countItemsForDay(dayPlanId: Long): Int

    @Query("""
        SELECT e.*, di.required AS required, di.targetReps AS targetReps, di.sortOrder AS sortOrder
        FROM day_item di
        JOIN exercise e ON e.id = di.refId
        WHERE di.dayPlanId = :dayPlanId AND di.itemType = 'STRENGTH'
        ORDER BY di.sortOrder ASC, di.id ASC
    """)
    fun flowDayStrengthFor(dayPlanId: Long): Flow<List<DayStrengthRow>>

    @Query("""
        SELECT di.refId AS planId, di.required AS required, di.sortOrder AS sortOrder
        FROM day_item di
        WHERE di.dayPlanId = :dayPlanId AND di.itemType = 'METCON'
        ORDER BY di.sortOrder ASC, di.id ASC
    """)
    fun flowDayMetconsFor(dayPlanId: Long): Flow<List<DayMetconSelectionRow>>

    @Query("UPDATE week_day_plan SET dateEpochDay = :dateEpochDay WHERE id = :dayPlanId")
    suspend fun updatePlanDate(dayPlanId: Long, dateEpochDay: Long?)

    /* ---------- NEW: edit helpers for METCON day_items ---------- */

    @Query("""
        SELECT COUNT(*) FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'METCON' AND refId = :planId
    """)
    suspend fun metconItemCount(dayPlanId: Long, planId: Long): Int

    @Query("""
        DELETE FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'METCON' AND refId = :planId
    """)
    suspend fun deleteMetconItem(dayPlanId: Long, planId: Long)

    @Query("""
        UPDATE day_item SET required = :required
        WHERE dayPlanId = :dayPlanId AND itemType = 'METCON' AND refId = :planId
    """)
    suspend fun updateMetconRequired(dayPlanId: Long, planId: Long, required: Boolean)

    @Query("""
        UPDATE day_item SET sortOrder = :sortOrder
        WHERE dayPlanId = :dayPlanId AND itemType = 'METCON' AND refId = :planId
    """)
    suspend fun updateMetconOrder(dayPlanId: Long, planId: Long, sortOrder: Int)
}
