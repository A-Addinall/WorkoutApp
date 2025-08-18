package com.example.safitness.ui.engine

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.core.SkillTestType
import com.example.safitness.core.SkillType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.DayEngineSkillEntity
import com.example.safitness.databinding.DialogAddEngineSkillBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEngineSkillDialog : DialogFragment() {

    interface OnAdded {
        fun onAdded()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddEngineSkillBinding.inflate(LayoutInflater.from(requireContext()))

        val kinds = listOf("ENGINE","SKILL")
        binding.inputKind.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, kinds))

        val modes = EngineMode.values().map { it.name }
        binding.inputEngineMode.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, modes))

        val intents = EngineIntent.values().map { it.name }
        binding.inputEngineIntent.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, intents))

        val skills = SkillType.values().map { it.name }
        binding.inputSkill.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, skills))

        val tests = SkillTestType.values().map { it.name }
        binding.inputSkillTest.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tests))

        fun updateVisibility() {
            val kind = binding.inputKind.text?.toString()
            val engine = kind == "ENGINE"
            val skill  = kind == "SKILL"
            binding.groupEngine.visibility = if (engine) android.view.View.VISIBLE else android.view.View.GONE
            binding.groupSkill.visibility  = if (skill)  android.view.View.VISIBLE else android.view.View.GONE

            val intent = binding.inputEngineIntent.text?.toString()
            val isForTime = intent == EngineIntent.FOR_TIME.name
            val isDur    = intent == EngineIntent.FOR_DISTANCE.name || intent == EngineIntent.FOR_CALORIES.name
            binding.fieldProgramDistance.visibility = if (isForTime) android.view.View.VISIBLE else android.view.View.GONE
            binding.fieldProgramDuration.visibility = if (isDur)    android.view.View.VISIBLE else android.view.View.GONE
            binding.fieldTargetCalories.visibility  = if (intent == EngineIntent.FOR_CALORIES.name) android.view.View.VISIBLE else android.view.View.GONE

            val test = binding.inputSkillTest.text?.toString()
            binding.fieldTargetReps.visibility     = if (test == SkillTestType.FOR_TIME_REPS.name) android.view.View.VISIBLE else android.view.View.GONE
            binding.fieldTargetDuration.visibility = if (test == SkillTestType.FOR_TIME_REPS.name) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.inputKind.setOnItemClickListener { _,_,_,_ -> updateVisibility() }
        binding.inputEngineIntent.setOnItemClickListener { _,_,_,_ -> updateVisibility() }
        binding.inputSkillTest.setOnItemClickListener { _,_,_,_ -> updateVisibility() }
        updateVisibility()

        val dayIndex = arguments?.getInt("DAY_INDEX") ?: 1
        val weekIndex = arguments?.getInt("WEEK_INDEX") ?: 1

        return AlertDialog.Builder(requireContext())
            .setTitle("Add to Day $dayIndex")
            .setView(binding.root)
            .setPositiveButton("Add") { _, _ ->
                val kind = binding.inputKind.text?.toString()
                if (kind.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Choose ENGINE or SKILL", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val db = AppDatabase.get(requireContext())
                    val entity = if (kind == "ENGINE") {
                        val mode   = binding.inputEngineMode.text?.toString()?.ifBlank { null }
                        val intent = binding.inputEngineIntent.text?.toString()?.ifBlank { null }
                        if (mode == null || intent == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Pick mode & intent", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        DayEngineSkillEntity(
                            weekIndex = weekIndex,
                            dayIndex = dayIndex,
                            kind = "ENGINE",
                            engineMode = mode,
                            engineIntent = intent,
                            programDistanceMeters = binding.inputProgramDistance.text?.toString()?.toIntOrNull(),
                            programDurationSeconds = binding.inputProgramDuration.text?.toString()?.toIntOrNull(),
                            programTargetCalories = binding.inputTargetCalories.text?.toString()?.toIntOrNull()
                        )
                    } else {
                        val skill = binding.inputSkill.text?.toString()?.ifBlank { null }
                        val test  = binding.inputSkillTest.text?.toString()?.ifBlank { null }
                        if (skill == null || test == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Pick skill & test", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        DayEngineSkillEntity(
                            weekIndex = weekIndex,
                            dayIndex = dayIndex,
                            kind = "SKILL",
                            skill = skill,
                            skillTestType = test,
                            targetReps = binding.inputTargetReps.text?.toString()?.toIntOrNull(),
                            targetDurationSeconds = binding.inputTargetDuration.text?.toString()?.toIntOrNull(),
                            progressionLevel = binding.inputProgression.text?.toString()?.ifBlank { null },
                            scaledVariant = binding.inputScaledVariant.text?.toString()?.ifBlank { null }
                        )
                    }

                    withContext(Dispatchers.IO) { db.dayEngineSkillDao().insert(entity) }
                    (parentFragment as? OnAdded)?.onAdded()
                    (activity as? OnAdded)?.onAdded()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {
        fun newInstance(weekIndex: Int, dayIndex: Int) = AddEngineSkillDialog().apply {
            arguments = Bundle().apply {
                putInt("WEEK_INDEX", weekIndex)
                putInt("DAY_INDEX", dayIndex)
            }
        }
    }
}
