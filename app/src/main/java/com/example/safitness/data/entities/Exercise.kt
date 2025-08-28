package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.MovementPattern
import com.example.safitness.core.Modality
import com.example.safitness.core.WorkoutType

@Entity(tableName = "exercise",
        indices = [Index(value = ["name"], unique = true)]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,

    // LEGACY fields: keep NON-NULL for UI compatibility
    val workoutType: WorkoutType,           // <- non-null
    val primaryEquipment: Equipment,        // <- non-null
    val modality: Modality = Modality.STRENGTH,

    // Optional extras (can remain nullable)
    val isUnilateral: Boolean = false,      // if you added this earlier
    // (If you later add new metadata fields, keep those nullable)

    // NEW metadata for ML/planner
    val movement: MovementPattern? = null,
    val unilateralCapable: Boolean? = null,
    val technicalDemand: Int? = null,          // 1..5, optional
    val defaultTempo: String? = null,
    val repRangeMin: Int? = null,
    val repRangeMax: Int? = null,
    val progressionFamilyKey: String? = null   // e.g., "SQUAT_BARBELL"
)
