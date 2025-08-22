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

    /* ---------- NEW: edit helpers for STRENGTH day_items ---------- */

    @Query("""
        SELECT COALESCE(MAX(sortOrder), -1) + 1
        FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH'
    """)
    suspend fun nextStrengthSortOrder(dayPlanId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH' AND refId = :exerciseId
    """)
    suspend fun strengthItemCount(dayPlanId: Long, exerciseId: Long): Int

    @Query("""
        DELETE FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH' AND refId = :exerciseId
    """)
    suspend fun deleteStrengthItem(dayPlanId: Long, exerciseId: Long)

    @Query("""
        UPDATE day_item SET required = :required
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH' AND refId = :exerciseId
    """)
    suspend fun updateStrengthRequired(dayPlanId: Long, exerciseId: Long, required: Boolean)

    @Query("""
        UPDATE day_item SET targetReps = :reps
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH' AND refId = :exerciseId
    """)
    suspend fun updateStrengthTargetReps(dayPlanId: Long, exerciseId: Long, reps: Int?)

    @Query("""
        SELECT COUNT(*) FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH' AND refId = :exerciseId
    """)
    suspend fun existsStrength(dayPlanId: Long, exerciseId: Long): Int

    @Query("""
        SELECT targetReps FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH' AND refId = :exerciseId
        LIMIT 1
    """)
    suspend fun getStrengthTargetReps(dayPlanId: Long, exerciseId: Long): Int?

    @Query("""
        SELECT required FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH' AND refId = :exerciseId
        LIMIT 1
    """)
    suspend fun getStrengthRequired(dayPlanId: Long, exerciseId: Long): Boolean?

    /* ---------- NEW: Engine membership (day_item) ---------- */

    @Query("""
        SELECT refId FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'ENGINE'
        ORDER BY sortOrder ASC
    """)
    fun engineItemIds(dayPlanId: Long): Flow<List<Long>>

    @Query("""
        SELECT COUNT(*) FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'ENGINE' AND refId = :planId
    """)
    suspend fun engineItemCount(dayPlanId: Long, planId: Long): Int

    @Query("""
        DELETE FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'ENGINE' AND refId = :planId
    """)
    suspend fun deleteEngineItem(dayPlanId: Long, planId: Long)

    @Query("""
        UPDATE day_item SET required = :required
        WHERE dayPlanId = :dayPlanId AND itemType = 'ENGINE' AND refId = :planId
    """)
    suspend fun updateEngineRequired(dayPlanId: Long, planId: Long, required: Boolean)

    @Query("""
        UPDATE day_item SET sortOrder = :orderInDay
        WHERE dayPlanId = :dayPlanId AND itemType = 'ENGINE' AND refId = :planId
    """)
    suspend fun updateEngineOrder(dayPlanId: Long, planId: Long, orderInDay: Int)

    /* ---------- NEW: Skill membership (day_item) ---------- */

    @Query("""
        SELECT refId FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'SKILL'
        ORDER BY sortOrder ASC
    """)
    fun skillItemIds(dayPlanId: Long): Flow<List<Long>>

    @Query("""
        SELECT COUNT(*) FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'SKILL' AND refId = :planId
    """)
    suspend fun skillItemCount(dayPlanId: Long, planId: Long): Int

    @Query("""
        DELETE FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = 'SKILL' AND refId = :planId
    """)
    suspend fun deleteSkillItem(dayPlanId: Long, planId: Long)

    @Query("""
        UPDATE day_item SET required = :required
        WHERE dayPlanId = :dayPlanId AND itemType = 'SKILL' AND refId = :planId
    """)
    suspend fun updateSkillRequired(dayPlanId: Long, planId: Long, required: Boolean)

    @Query("""
        UPDATE day_item SET sortOrder = :orderInDay
        WHERE dayPlanId = :dayPlanId AND itemType = 'SKILL' AND refId = :planId
    """)
    suspend fun updateSkillOrder(dayPlanId: Long, planId: Long, orderInDay: Int)

    /** Check if a specific item (by type + refId) already exists for the day. */
    @Query("""
        SELECT COUNT(*) FROM day_item
        WHERE dayPlanId = :dayPlanId AND itemType = :itemType AND refId = :refId
    """)
    suspend fun existsDayItem(dayPlanId: Long, itemType: String, refId: Long): Int

    /** All ENGINE refIds already on the day (convenience). */
    @Query("SELECT refId FROM day_item WHERE dayPlanId = :dayPlanId AND itemType = 'ENGINE'")
    suspend fun engineItemIdsForDay(dayPlanId: Long): List<Long>

    /** All SKILL refIds already on the day (convenience). */
    @Query("SELECT refId FROM day_item WHERE dayPlanId = :dayPlanId AND itemType = 'SKILL'")
    suspend fun skillItemIdsForDay(dayPlanId: Long): List<Long>

    // Clear helpers
    @Query("DELETE FROM day_item WHERE dayPlanId = :dayPlanId AND itemType = 'STRENGTH'")
    suspend fun clearStrength(dayPlanId: Long)

    @Query("DELETE FROM day_item WHERE dayPlanId = :dayPlanId AND itemType = 'METCON'")
    suspend fun clearMetcon(dayPlanId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM day_item WHERE dayPlanId = :dayPlanId")
    suspend fun nextSortOrder(dayPlanId: Long): Int

    // --- Date-first lookups ---

    /** Get a plan id for a real calendar date within a specific phase. */
    @Query("""
    SELECT id FROM week_day_plan
    WHERE phaseId = :phaseId AND dateEpochDay = :epochDay
    LIMIT 1
""")
    suspend fun getPlanIdByDateInPhase(phaseId: Long, epochDay: Long): Long?

    /** Reactive variant (phase-scoped). */
    @Query("""
    SELECT id FROM week_day_plan
    WHERE phaseId = :phaseId AND dateEpochDay = :epochDay
    LIMIT 1
""")
    fun flowPlanIdByDateInPhase(phaseId: Long, epochDay: Long): Flow<Long?>

    /** If you know dates are globally unique across phases, this is convenient. */
    @Query("SELECT id FROM week_day_plan WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun getPlanIdByDate(epochDay: Long): Long?

    /** Reactive variant (global). */
    @Query("SELECT id FROM week_day_plan WHERE dateEpochDay = :epochDay LIMIT 1")
    fun flowPlanIdByDate(epochDay: Long): Flow<Long?>
}
