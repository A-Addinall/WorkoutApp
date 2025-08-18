package com.example.safitness.data.db

import android.content.Context
import androidx.room.Room
import com.example.safitness.data.dao.EngineLogDao
import com.example.safitness.data.dao.SkillLogDao
import com.example.safitness.data.entities.EngineLogEntity
import com.example.safitness.data.entities.SkillLogEntity
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class AppDatabasePhase3Test {

    private lateinit var db: AppDatabase
    private lateinit var engineDao: EngineLogDao
    private lateinit var skillDao: SkillLogDao

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        engineDao = db.engineLogDao()
        skillDao = db.skillLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_fetch_engine_for_time() = runBlocking {
        val id = engineDao.insert(
            EngineLogEntity(
                date = 1_700_000_000L,
                mode = "RUN",
                intent = "FOR_TIME",
                programDistanceMeters = 5000,
                resultTimeSeconds = 1500,
                scaled = false,
                notes = "5k 25:00"
            )
        )
        assertTrue(id > 0)

        val recent = engineDao.recent(mode = "RUN", limit = 10)
        assertEquals(1, recent.size)
        assertEquals(5000, recent.first().programDistanceMeters)
        assertEquals(1500, recent.first().resultTimeSeconds)
    }

    @Test
    fun insert_and_fetch_skill_max_reps() = runBlocking {
        val id = skillDao.insert(
            SkillLogEntity(
                date = 1_700_000_000L,
                skill = "DOUBLE_UNDERS",
                testType = "MAX_REPS_UNBROKEN",
                reps = 60,
                scaled = false,
                notes = "Unbroken set"
            )
        )
        assertTrue(id > 0)

        val recent = skillDao.recent("DOUBLE_UNDERS", "MAX_REPS_UNBROKEN", 10)
        assertEquals(1, recent.size)
        assertEquals(60, recent.first().reps)
    }
}