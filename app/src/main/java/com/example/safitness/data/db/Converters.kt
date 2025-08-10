// app/src/main/java/com/example/safitness/data/db/Converters.kt
package com.example.safitness.data.db

import androidx.room.TypeConverter
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType

class Converters {
    @TypeConverter
    fun toWorkoutType(value: String?): WorkoutType? = value?.let { WorkoutType.valueOf(it) }
    @TypeConverter
    fun fromWorkoutType(type: WorkoutType?): String? = type?.name

    @TypeConverter
    fun toEquipment(value: String?): Equipment? = value?.let { Equipment.valueOf(it) }
    @TypeConverter
    fun fromEquipment(eq: Equipment?): String? = eq?.name
}
