package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.Equipment

@Entity(
    tableName = "personal_record",
    indices = [
        Index(
            value = ["exerciseId", "equipment", "recordType", "reps"],
            unique = true
        )
    ]
)
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val exerciseId: Long,
    /**
     * recordType:
     *  - "E1RM" (Estimated 1RM; reps MUST be null)
     *  - "RM"   (Hard Rep-Max; reps MUST be non-null)
     */
    val recordType: String,
    /** Value is kg for both E1RM and RM */
    val value: Double,
    /** epoch day (UTC) for when record was set */
    val dateEpochDay: Long,
    /** Optional: free text */
    val notes: String? = null,
    /** NEW: per-equipment tracking (BARBELL, DUMBBELL, KETTLEBELL, etc.) */
    val equipment: Equipment,
    /** NEW: reps dimension. Null for E1RM, e.g., 5 for a 5RM. */
    val reps: Int? = null,
)
