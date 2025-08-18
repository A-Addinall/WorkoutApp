package com.example.safitness.ui.engine

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.safitness.databinding.SheetSkillQuickLogBinding
import com.example.safitness.core.SkillTestType
import com.example.safitness.core.SkillType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.SkillLogEntity
import com.example.safitness.data.repo.SkillLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SkillQuickLogSheet : BottomSheetDialogFragment() {
    private var _binding: SheetSkillQuickLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = SheetSkillQuickLogBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val skills = SkillType.values().map { it.name }
        val tests  = SkillTestType.values().map { it.name }
        binding.inputSkill.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, skills))
        binding.inputTest.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tests))

        fun updateFields() {
            val test = binding.inputTest.text?.toString()
            binding.fieldReps.visibility      = if (test == SkillTestType.MAX_REPS_UNBROKEN.name) View.VISIBLE else View.GONE
            binding.fieldTime.visibility      = if (test == SkillTestType.FOR_TIME_REPS.name) View.VISIBLE else View.GONE
            binding.fieldTargetReps.visibility= if (test == SkillTestType.FOR_TIME_REPS.name) View.VISIBLE else View.GONE
            binding.fieldHold.visibility      = if (test == SkillTestType.MAX_HOLD_SECONDS.name) View.VISIBLE else View.GONE
            binding.fieldAttempts.visibility  = if (test == SkillTestType.ATTEMPTS.name) View.VISIBLE else View.GONE
        }
        binding.inputTest.setOnItemClickListener { _,_,_,_ -> updateFields() }
        updateFields()

        binding.btnSave.setOnClickListener {
            val skill = binding.inputSkill.text?.toString()?.ifBlank { null }
            val test  = binding.inputTest.text?.toString()?.ifBlank { null }
            if (skill == null || test == null) { Toast.makeText(requireContext(),"Select skill & test",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val reps     = binding.inputReps.text?.toString()?.toIntOrNull()
            val time     = binding.inputTime.text?.toString()?.toIntOrNull()
            val target   = binding.inputTargetReps.text?.toString()?.toIntOrNull()
            val hold     = binding.inputHold.text?.toString()?.toIntOrNull()
            val attempts = binding.inputAttempts.text?.toString()?.toIntOrNull()

            val now = System.currentTimeMillis() / 1000
            val entity = SkillLogEntity(
                date = now,
                skill = skill,
                testType = test,
                targetReps = target,
                reps = reps,
                timeSeconds = time,
                maxHoldSeconds = hold,
                attempts = attempts
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.get(requireContext())
                    SkillLogRepository(db.skillLogDao()).insert(entity)
                    launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Skill log saved", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                } catch (t: Throwable) {
                    launch(Dispatchers.Main) { Toast.makeText(requireContext(), t.message ?: "Validation failed", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
