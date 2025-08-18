package com.example.safitness.ui.dev

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safitness.R
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.core.EngineCalculator
import com.example.safitness.core.SkillTestType
import com.example.safitness.core.SkillType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.EngineLogEntity
import com.example.safitness.data.repo.EngineLogRepository
import com.example.safitness.data.repo.SkillLogRepository
import com.example.safitness.databinding.ActivityPhase3DevBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Phase3DevActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhase3DevBinding
    private lateinit var engineRepo: EngineLogRepository
    private lateinit var skillRepo:   com.example.safitness.data.repo.SkillLogRepository

    private val engineAdapter by lazy { EngineLogAdapter() }
    private val skillAdapter  by lazy { SkillLogAdapter() }

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPhase3DevBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DB + repos
        val db = AppDatabase.get(this)
        engineRepo = EngineLogRepository(db.engineLogDao())
        skillRepo  = SkillLogRepository(db.skillLogDao())

        // RecyclerViews
        binding.rvEngine.apply {
            layoutManager = LinearLayoutManager(this@Phase3DevActivity)
            adapter = engineAdapter
        }
        binding.rvSkills.apply {
            layoutManager = LinearLayoutManager(this@Phase3DevActivity)
            adapter = skillAdapter
        }

        binding.btnRefresh.setOnClickListener { refresh() }

        binding.fabAddEngine.setOnClickListener {
            lifecycleScope.launch {
                // Example: add a RUN 5k @ 25:00 (computed pace)
                engineRepo.logRunForTime(
                    epochSeconds = System.currentTimeMillis() / 1000,
                    distanceMeters = 5000,
                    timeSeconds = 1500,
                    notes = "Dev add: 5k 25:00"
                )
                // Example: add a ROW 10-min distance (FOR_DISTANCE)
                engineRepo.insert(
                    EngineLogEntity(
                        date = System.currentTimeMillis() / 1000,
                        mode = EngineMode.ROW.name,
                        intent = EngineIntent.FOR_DISTANCE.name,
                        programDurationSeconds = 600,
                        resultDistanceMeters = 2100,
                        notes = "Dev add: 10-min row"
                    )
                )
                refresh()
            }
        }

        binding.fabAddSkill.setOnClickListener {
            lifecycleScope.launch {
                // Example: DU unbroken reps
                skillRepo.logDoubleUndersMaxReps(
                    epochSeconds = System.currentTimeMillis() / 1000,
                    reps = 50,
                    notes = "Dev add: DU set"
                )
                // Example: HS hold 40s
                skillRepo.insert(
                    com.example.safitness.data.entities.SkillLogEntity(
                        date = System.currentTimeMillis() / 1000,
                        skill = SkillType.HANDSTAND_HOLD.name,
                        testType = SkillTestType.MAX_HOLD_SECONDS.name,
                        maxHoldSeconds = 40,
                        scaled = false,
                        notes = "Dev add: HS 40s"
                    )
                )
                refresh()
            }
        }

        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val engine = withContext(Dispatchers.IO) {
                // Using existing DAO shape: fetch per-mode and merge
                val db = AppDatabase.get(this@Phase3DevActivity)
                val d = db.engineLogDao()
                val run  = d.recent(EngineMode.RUN.name, 20)
                val row  = d.recent(EngineMode.ROW.name, 20)
                val bike = d.recent(EngineMode.BIKE.name, 20)
                (run + row + bike).sortedByDescending { it.date }
            }
            engineAdapter.submitList(engine.map {
                EngineLogUi(
                    date = dateFmt.format(Date(it.date * 1000)),
                    mode = it.mode,
                    intent = it.intent,
                    program = when {
                        it.programDistanceMeters != null -> "Target: ${it.programDistanceMeters}m"
                        it.programDurationSeconds != null -> "Target: ${it.programDurationSeconds}s"
                        it.programTargetCalories != null -> "Target: ${it.programTargetCalories} cal"
                        else -> "Target: —"
                    },
                    result = when (it.intent) {
                        EngineIntent.FOR_TIME.name      -> "Time: ${EngineCalculator.formatSeconds(it.resultTimeSeconds) ?: "—"}"
                        EngineIntent.FOR_DISTANCE.name  -> "Meters: ${it.resultDistanceMeters ?: 0}"
                        EngineIntent.FOR_CALORIES.name  -> "Calories: ${it.resultCalories ?: 0}"
                        else -> "Result: —"
                    },
                    pace = it.pace?.let { p -> EngineCalculator.formatSecPerKm(p) } ?: ""
                )
            })

            val skills = withContext(Dispatchers.IO) {
                val db = AppDatabase.get(this@Phase3DevActivity)
                val d = db.skillLogDao()
                val du   = d.recent(SkillType.DOUBLE_UNDERS.name, SkillTestType.MAX_REPS_UNBROKEN.name, 20)
                val hs   = d.recent(SkillType.HANDSTAND_HOLD.name, SkillTestType.MAX_HOLD_SECONDS.name, 20)
                val mu   = d.recent(SkillType.MUSCLE_UP.name, SkillTestType.ATTEMPTS.name, 20)
                (du + hs + mu).sortedByDescending { it.date }
            }
            skillAdapter.submitList(skills.map {
                SkillLogUi(
                    date = dateFmt.format(Date(it.date * 1000)),
                    skill = it.skill,
                    type  = it.testType,
                    detail = when (it.testType) {
                        SkillTestType.MAX_REPS_UNBROKEN.name -> "Reps: ${it.reps ?: 0}"
                        SkillTestType.FOR_TIME_REPS.name     -> "Time: ${EngineCalculator.formatSeconds(it.timeSeconds) ?: "—"} for ${it.targetReps ?: 0} reps"
                        SkillTestType.MAX_HOLD_SECONDS.name  -> "Hold: ${it.maxHoldSeconds ?: 0}s"
                        SkillTestType.ATTEMPTS.name          -> "Attempts: ${it.attempts ?: 0}"
                        else -> "—"
                    },
                    scaled = if (it.scaled) "Scaled" else "RX"
                )
            })
        }
    }
}
