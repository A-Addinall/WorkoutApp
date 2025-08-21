package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun flowProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)

    @Query("DELETE FROM user_profile WHERE id = 1")
    suspend fun clear()
}
