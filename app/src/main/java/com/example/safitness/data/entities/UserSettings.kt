package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val darkTheme: Boolean = false,
    val autoWeightIncrement: Double = 2.5,
    val defaultRestTime: Int = 120,
    val units: String = "kg",
    val showPersonalRecords: Boolean = true
) {
    @Ignore
    constructor() : this(
        id = 1,
        darkTheme = false,
        autoWeightIncrement = 2.5,
        defaultRestTime = 180,
        units = "kg",
        showPersonalRecords = true
    )
}
