package com.example.safitness.data.repo

import android.content.Context
import androidx.room.Room
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.data.dao.EngineLogDao
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.EngineLogEntity
import kotlinx.coroutines.runBlocking

class EngineLogRepositoryValidationTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EngineLogDao
    private lateinit var repo: EngineLogRepository

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.engineLogDao()
        repo = EngineLogRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun for_time_requires_distance_and_time() = runBlocking {
        // Missing time -> expect failure
        try {
            repo.insert(
                EngineLogEntity(
                    date = 1_700_000_000L,
                    mode = EngineMode.RUN.name,
                    intent = EngineIntent.FOR_TIME.name,
                    programDistanceMeters = 5000
                )
            )
            fail("Expected IllegalArgumentException for missing resultTimeSeconds")
        } catch (_: IllegalArgumentException) {
        }

        // Valid row succeeds
        val id = repo.insert(
            EngineLogEntity(
                date = 1_700_000_000L,
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_TIME.name,
                programDistanceMeters = 5000,
                resultTimeSeconds = 1500
            )
        )
        assertTrue(id > 0)
    }

    @Test
    fun exactly_one_program_target() = runBlocking {
        // Two targets -> expect failure
        try {
            repo.insert(
                EngineLogEntity(
                    date = 1_700_000_000L,
                    mode = EngineMode.ROW.name,
                    intent = EngineIntent.FOR_DISTANCE.name,
                    programDurationSeconds = 600,
                    programDistanceMeters = 2000, // invalid combo
                    resultDistanceMeters = 2200
                )
            )
            fail("Expected IllegalArgumentException for multiple program targets")
        } catch (_: IllegalArgumentException) {
        }
    }
}