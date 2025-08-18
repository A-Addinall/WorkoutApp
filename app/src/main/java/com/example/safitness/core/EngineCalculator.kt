package com.example.safitness.core

import kotlin.math.round

object EngineCalculator {

    /** Pace in seconds/km for RUN given distance(m) & time(s). */
    fun paceSecondsPerKm(distanceMeters: Int?, timeSeconds: Int?): Double? {
        if (distanceMeters == null || timeSeconds == null || distanceMeters <= 0 || timeSeconds <= 0) return null
        return timeSeconds / (distanceMeters / 1000.0)
    }

    /** Rowing split in seconds/500m. */
    fun splitSecondsPer500m(distanceMeters: Int?, timeSeconds: Int?): Double? {
        if (distanceMeters == null || timeSeconds == null || distanceMeters <= 0 || timeSeconds <= 0) return null
        return timeSeconds / (distanceMeters / 500.0)
    }

    fun formatSeconds(total: Int?): String? {
        if (total == null) return null
        val m = total / 60
        val s = total % 60
        return "%d:%02d".format(m, s)
    }

    fun formatSecPerKm(pace: Double?): String? {
        if (pace == null) return null
        val p = round(pace).toInt()
        val m = p / 60
        val s = p % 60
        return "$m:${"%02d".format(s)}/km"
    }

    fun formatSecPer500m(split: Double?): String? {
        if (split == null) return null
        val p = round(split).toInt()
        val m = p / 60
        val s = p % 60
        return "$m:${"%02d".format(s)}/500m"
    }
}
