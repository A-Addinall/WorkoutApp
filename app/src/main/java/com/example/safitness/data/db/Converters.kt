package com.example.safitness.data.db

import androidx.room.TypeConverter
import com.example.safitness.core.*

class Converters {
    @TypeConverter fun toWorkoutType(value: String?): WorkoutType? = value?.let { WorkoutType.valueOf(it) }
    @TypeConverter fun fromWorkoutType(type: WorkoutType?): String? = type?.name

    @TypeConverter fun toEquipment(value: String?): Equipment? = value?.let { Equipment.valueOf(it) }
    @TypeConverter fun fromEquipment(eq: Equipment?): String? = eq?.name

    @TypeConverter fun toMetconResult(value: String?): MetconResult? = value?.let { MetconResult.valueOf(it) }
    @TypeConverter fun fromMetconResult(v: MetconResult?): String? = v?.name

    @TypeConverter fun toMetconType(value: String?): MetconType? = value?.let { MetconType.valueOf(it) }
    @TypeConverter fun fromMetconType(v: MetconType?): String? = v?.name
}
