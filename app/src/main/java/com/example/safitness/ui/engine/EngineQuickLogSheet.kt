package com.example.safitness.ui.engine

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.safitness.databinding.SheetEngineQuickLogBinding
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.EngineLogEntity
import com.example.safitness.data.repo.EngineLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EngineQuickLogSheet : BottomSheetDialogFragment() {
    private var _binding: SheetEngineQuickLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = SheetEngineQuickLogBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val modes = EngineMode.values().map { it.name }
        val intents = EngineIntent.values().map { it.name }

        binding.inputMode.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, modes))
        binding.inputIntent.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, intents))

        fun updateFields() {
            val intent = binding.inputIntent.text?.toString()
            binding.fieldProgramDistance.visibility = if (intent == EngineIntent.FOR_TIME.name) View.VISIBLE else View.GONE
            val durationBased = intent == EngineIntent.FOR_DISTANCE.name || intent == EngineIntent.FOR_CALORIES.name
            binding.fieldProgramDuration.visibility = if (durationBased) View.VISIBLE else View.GONE
            binding.fieldResultTime.visibility = if (intent == EngineIntent.FOR_TIME.name) View.VISIBLE else View.GONE
            binding.fieldResultMeters.visibility = if (intent == EngineIntent.FOR_DISTANCE.name) View.VISIBLE else View.GONE
            binding.fieldResultCalories.visibility = if (intent == EngineIntent.FOR_CALORIES.name) View.VISIBLE else View.GONE
        }
        binding.inputIntent.setOnItemClickListener { _,_,_,_ -> updateFields() }
        updateFields()

        binding.btnSave.setOnClickListener {
            val mode   = binding.inputMode.text?.toString()?.ifBlank { null }
            val intent = binding.inputIntent.text?.toString()?.ifBlank { null }
            if (mode == null || intent == null) { Toast.makeText(requireContext(),"Select mode & intent",Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val distance = binding.inputProgramDistance.text?.toString()?.toIntOrNull()
            val duration = binding.inputProgramDuration.text?.toString()?.toIntOrNull()
            val time     = binding.inputResultTime.text?.toString()?.toIntOrNull()
            val meters   = binding.inputResultMeters.text?.toString()?.toIntOrNull()
            val cals     = binding.inputResultCalories.text?.toString()?.toIntOrNull()

            val now = System.currentTimeMillis() / 1000
            val entity = EngineLogEntity(
                date = now,
                mode = mode,
                intent = intent,
                programDistanceMeters = distance,
                programDurationSeconds = duration,
                programTargetCalories = null,
                resultTimeSeconds = time,
                resultDistanceMeters = meters,
                resultCalories = cals
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.get(requireContext())
                    EngineLogRepository(db.engineLogDao()).insert(entity)
                    launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Engine log saved", Toast.LENGTH_SHORT).show()
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
