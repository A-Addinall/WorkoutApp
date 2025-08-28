package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.data.entities.SetLog
import com.example.safitness.data.entities.WorkoutSessionEntity

// Top-level so other packages can import it directly.
data class MetconSummary(
    val timeSeconds: Int,
    val metconResult: MetconResult?
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSet(set: SetLog): Long

    @Query(
        """
        SELECT * FROM SetLog 
        WHERE exerciseId = :exerciseId 
          AND equipment = :equipment
          AND (:reps IS NULL OR reps = :reps)
        ORDER BY id DESC 
        LIMIT :limit
        """
    )
    suspend fun lastSets(
        exerciseId: Long,
        equipment: Equipment,
        limit: Int = 10,
        reps: Int? = null
    ): List<SetLog>

    /* ---- Date-first only ---- */

    @Query("""
    SELECT sl.timeSeconds FROM SetLog sl
    JOIN workout_session ws ON ws.id = sl.sessionId
    WHERE ws.dateEpochDay = :epochDay
      AND sl.timeSeconds IS NOT NULL
    ORDER BY sl.id DESC
    LIMIT 1
""")
    suspend fun lastMetconSecondsForDate(epochDay: Long): Int?

    @Query("""
    SELECT 
        sl.timeSeconds AS timeSeconds,
        NULL AS metconResult  -- until you add the column to SetLog
    FROM SetLog sl
    JOIN workout_session ws ON ws.id = sl.sessionId
    WHERE ws.dateEpochDay = :epochDay
      AND sl.timeSeconds IS NOT NULL
    ORDER BY sl.id DESC
    LIMIT 1
""")
    suspend fun lastMetconForDate(epochDay: Long): MetconSummary?

    @Query("SELECT COUNT(*) FROM SetLog WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun countSetsFor(sessionId: Long, exerciseId: Long): Int

    data class ExerciseSetCount(
        val exerciseId: Long,
        val cnt: Int
    )

    @Query("""
    SELECT exerciseId, COUNT(*) AS cnt
    FROM SetLog
    WHERE sessionId = :sessionId
    GROUP BY exerciseId
""")
    suspend fun countsByExercise(sessionId: Long): List<ExerciseSetCount>

    @Query("""
    SELECT *
    FROM SetLog
    WHERE sessionId = :sessionId AND exerciseId = :exerciseId
    ORDER BY setNumber ASC
""")
    suspend fun setsForSessionExercise(sessionId: Long, exerciseId: Long): List<SetLog>

    @Query("SELECT id FROM workout_session WHERE dateEpochDay = :epochDay ORDER BY id DESC LIMIT 1")
    suspend fun latestSessionIdForDate(epochDay: Long): Long?


    @Query("""
SELECT MAX(weight) FROM SetLog
WHERE exerciseId = :exerciseId AND equipment = :equipment
  AND success = 1 AND reps = :reps AND weight IS NOT NULL
""")
    suspend fun maxSuccessfulWeightAtReps(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int
    ): Double?

}
