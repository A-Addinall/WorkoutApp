// app/src/main/java/com/example/safitness/data/dao/ExerciseDao.kt
package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.data.entities.Exercise

@Dao
interface ExerciseDao {
    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    // NEW (dev seed helpers)
    @Query("SELECT id FROM exercise WHERE name = :name LIMIT 1")
    suspend fun getIdByName(name: String): Long?

    @Query("SELECT id FROM exercise ORDER BY name ASC LIMIT :limit")
    suspend fun firstNIds(limit: Int): List<Long>
}
