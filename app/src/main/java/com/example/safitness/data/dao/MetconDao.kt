package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.MetconComponent
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.entities.ProgramMetconSelection
import com.example.safitness.data.entities.MetconLog   // <-- added
import kotlinx.coroutines.flow.Flow
import com.example.safitness.core.MetconType


/* -- Relations for convenient reads -- */

data class PlanWithComponents(
    @Embedded val plan: MetconPlan,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId",
        entity = MetconComponent::class
    )
    val components: List<MetconComponent>
)

data class SelectionWithPlanAndComponents(
    @Embedded val selection: ProgramMetconSelection,
    @Relation(
        parentColumn = "planId",
        entityColumn = "id",
        entity = MetconPlan::class
    )
    val planWithComponents: PlanWithComponents
)

@Dao
interface MetconDao {

    /* ----- Library (plans) ----- */

    @Query("SELECT * FROM metcon_plan WHERE isArchived = 0 ORDER BY title ASC")
    fun getAllPlans(): Flow<List<MetconPlan>>

    @Transaction
    @Query("SELECT * FROM metcon_plan WHERE id = :planId LIMIT 1")
    fun getPlanWithComponents(planId: Long): Flow<PlanWithComponents>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<MetconPlan>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<MetconComponent>): List<Long>

    @Query("SELECT COUNT(*) FROM metcon_plan")
    suspend fun countPlans(): Int

    /* ----- Program selections per day ----- */

    @Transaction
    @Query("""
        SELECT * FROM program_metcon_selection
        WHERE dayIndex = :day
        ORDER BY displayOrder ASC, id ASC
    """)
    fun getMetconsForDay(day: Int): Flow<List<SelectionWithPlanAndComponents>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSelection(selection: ProgramMetconSelection): Long

    @Query("DELETE FROM program_metcon_selection WHERE dayIndex = :day AND planId = :planId")
    suspend fun removeSelection(day: Int, planId: Long)

    @Query("""
        UPDATE program_metcon_selection
        SET required = :required
        WHERE dayIndex = :day AND planId = :planId
    """)
    suspend fun setRequired(day: Int, planId: Long, required: Boolean)

    @Query("""
        UPDATE program_metcon_selection
        SET displayOrder = :orderInDay
        WHERE dayIndex = :day AND planId = :planId
    """)
    suspend fun setDisplayOrder(day: Int, planId: Long, orderInDay: Int)

    /* ----- NEW: Plan-scoped metcon logs ----- */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MetconLog): Long

    @Query("""
        SELECT * FROM metcon_log
        WHERE planId = :planId
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    fun lastForPlan(planId: Long): Flow<MetconLog?>

    @Query("""
        SELECT * FROM metcon_log
        WHERE dayIndex = :day
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    fun lastForDay(day: Int): Flow<MetconLog?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlansIgnore(plans: List<MetconPlan>): List<Long>

    @Query("SELECT id FROM metcon_plan WHERE title = :title LIMIT 1")
    suspend fun getPlanIdByTitle(title: String): Long

    @Query("SELECT id FROM metcon_plan WHERE canonicalKey = :key LIMIT 1")
    suspend fun getPlanIdByKey(key: String): Long

    @Query("""
        UPDATE metcon_plan
        SET title = :title,
            type = :type,
            durationMinutes = :durationMinutes,
            emomIntervalSec = :emomIntervalSec,
            isArchived = :isArchived
        WHERE canonicalKey = :key
    """)
    suspend fun updatePlanByKey(
        key: String,
        title: String,
        type: MetconType,
        durationMinutes: Int?,
        emomIntervalSec: Int?,
        isArchived: Boolean = false
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComponentsIgnore(components: List<MetconComponent>)

    @Query("""
        UPDATE metcon_component
        SET text = :text
        WHERE planId = :planId AND orderInPlan = :orderInPlan
    """)
    suspend fun updateComponentText(planId: Long, orderInPlan: Int, text: String)

    @Query("DELETE FROM metcon_component WHERE planId = :planId")
    suspend fun deleteAllComponentsForPlan(planId: Long)

    @Query("""
        DELETE FROM metcon_component
        WHERE planId = :planId AND orderInPlan NOT IN (:validOrders)
    """)
    suspend fun deleteComponentsNotIn(planId: Long, validOrders: List<Int>)

}
