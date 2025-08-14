package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.MetconComponent
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.entities.ProgramMetconSelection
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM metcon_plan ORDER BY title ASC")
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
}
