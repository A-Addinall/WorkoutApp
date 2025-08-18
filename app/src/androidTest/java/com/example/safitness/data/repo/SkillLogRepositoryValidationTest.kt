package com.example.safitness.data.repo

import android.content.Context
import androidx.room.Room
import com.example.safitness.core.SkillTestType
import com.example.safitness.core.SkillType
import com.example.safitness.data.dao.SkillLogDao
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.SkillLogEntity
import kotlinx.coroutines.runBlocking

class SkillLogRepositoryValidationTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SkillLogDao
    private lateinit var repo: SkillLogRepository

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.skillLogDao()
        repo = SkillLogRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun du_max_reps_requires_reps() = runBlocking {
        // Missing reps -> expect failure
        try {
            repo.insert(
                SkillLogEntity(
                    date = 1_700_000_000L,
                    skill = SkillType.DOUBLE_UNDERS.name,
                    testType = SkillTestType.MAX_REPS_UNBROKEN.name
                )
            )
            fail("Expected IllegalArgumentException for missing reps")
        } catch (_: IllegalArgumentException) {
        }

        // Valid row succeeds
        val id = repo.insert(
            SkillLogEntity(
                date = 1_700_000_000L,
                skill = SkillType.DOUBLE_UNDERS.name,
                testType = SkillTestType.MAX_REPS_UNBROKEN.name,
                reps = 60
            )
        )
        assertTrue(id > 0)
    }

    @Test
    fun for_time_reps_requires_time_and_target() = runBlocking {
        try {
            repo.insert(
                SkillLogEntity(
                    date = 1_700_000_000L,
                    skill = SkillType.DOUBLE_UNDERS.name,
                    testType = SkillTestType.FOR_TIME_REPS.name,
                    timeSeconds = 120 // missing targetReps
                )
            )
            fail("Expected IllegalArgumentException for missing targetReps")
        } catch (_: IllegalArgumentException) {
        }
    }
}