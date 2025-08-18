package com.example.safitness.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class EngineCalculatorTest {

    @Test
    fun pace_seconds_per_km_run_5k_25min() {
        val pace = EngineCalculator.paceSecondsPerKm(5000, 1500)!!
        assertEquals(300, pace.roundToInt()) // 5:00/km
    }

    @Test
    fun split_seconds_per_500m_row_2k_7m20() {
        val split = EngineCalculator.splitSecondsPer500m(2000, 440)!!
        assertEquals(110, split.roundToInt()) // 1:50/500m
    }

    @Test
    fun invalid_inputs_return_null() {
        assertNull(EngineCalculator.paceSecondsPerKm(null, 1))
        assertNull(EngineCalculator.paceSecondsPerKm(1000, null))
        assertNull(EngineCalculator.paceSecondsPerKm(-1, 100))
        assertNull(EngineCalculator.splitSecondsPer500m(0, 10))
    }
}
