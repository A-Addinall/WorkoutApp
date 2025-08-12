package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.*
import kotlinx.coroutines.flow.Flow

/* ---------- Library side ---------- */

data class MetconWithComponents(
    @Embedded val plan: MetconPlan,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId",
        entity = MetconComponent::class
    )
    val components: List<MetconComponent>
)

@Dao
interface MetconDao {
    // Plans
    @Query("SELECT * FROM metcon_plan ORDER BY title ASC")
    fun getAllPlans(): Flow<List<MetconPlan>>

    @Transaction
    @Query("SELECT * FROM metcon_plan WHERE id = :planId LIMIT 1")
    fun getPlanWithComponents(planId: Long): Flow<MetconWithComponents?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MetconPlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<MetconComponent>)

    @Query("SELECT COUNT(*) FROM metcon_plan")
    suspend fun countPlans(): Int

    /* ---------- Program side ---------- */

    data class SelectionWithPlanAndComponents(
        @Embedded val selection: ProgramMetconSelection,
        @Relation(parentColumn = "planId", entityColumn = "id")
        val plan: MetconPlan,
        @Relation(parentColumn = "planId", entityColumn = "planId")
        val components: List<MetconComponent>
    )

    @Transaction
    @Query("""
        SELECT * FROM program_metcon_selection
        WHERE dayIndex = :day
        ORDER BY displayOrder ASC, id ASC
    """)
    fun getMetconsForDay(day: Int): Flow<List<SelectionWithPlanAndComponents>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSelection(sel: ProgramMetconSelection): Long

    @Query("DELETE FROM program_metcon_selection WHERE dayIndex = :day AND planId = :planId")
    suspend fun removeSelection(day: Int, planId: Long)

    @Query("UPDATE program_metcon_selection SET required = :required WHERE id = :id")
    suspend fun setRequired(id: Long, required: Boolean)

    @Query("UPDATE program_metcon_selection SET displayOrder = :order WHERE id = :id")
    suspend fun setDisplayOrder(id: Long, order: Int)
}
