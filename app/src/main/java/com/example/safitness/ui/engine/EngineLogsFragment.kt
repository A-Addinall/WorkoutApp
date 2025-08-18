package com.example.safitness.ui.engine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.core.EngineCalculator
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.databinding.FragmentEngineLogsBinding
import com.example.safitness.ui.dev.EngineLogAdapter
import com.example.safitness.ui.dev.EngineLogUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EngineLogsFragment : Fragment() {

    private var _binding: FragmentEngineLogsBinding? = null
    private val binding get() = _binding!!
    private val adapter by lazy { EngineLogAdapter() }
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentEngineLogsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        binding.swipe.setOnRefreshListener { load() }
        binding.fab.setOnClickListener {
            EngineQuickLogSheet().show(parentFragmentManager, "engineQuickLog")
        }

        load()
    }

    private fun load() {
        binding.swipe.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                val db = AppDatabase.get(requireContext())
                val d = db.engineLogDao()
                val run  = d.recent(EngineMode.RUN.name, 50)
                val row  = d.recent(EngineMode.ROW.name, 50)
                val bike = d.recent(EngineMode.BIKE.name, 50)
                (run + row + bike).sortedByDescending { it.date }.map {
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
                            EngineIntent.FOR_TIME.name     -> "Time: ${EngineCalculator.formatSeconds(it.resultTimeSeconds) ?: "—"}"
                            EngineIntent.FOR_DISTANCE.name -> "Meters: ${it.resultDistanceMeters ?: 0}"
                            EngineIntent.FOR_CALORIES.name -> "Calories: ${it.resultCalories ?: 0}"
                            else -> "Result: —"
                        },
                        pace = it.pace?.let(EngineCalculator::formatSecPerKm) ?: ""
                    )
                }
            }
            adapter.submitList(items)
            binding.empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.swipe.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
