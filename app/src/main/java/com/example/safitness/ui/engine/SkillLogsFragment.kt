package com.example.safitness.ui.engine

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safitness.core.SkillTestType
import com.example.safitness.core.SkillType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.databinding.FragmentSkillLogsBinding
import com.example.safitness.ui.dev.SkillLogAdapter
import com.example.safitness.ui.dev.SkillLogUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SkillLogsFragment : Fragment() {

    private var _binding: FragmentSkillLogsBinding? = null
    private val binding get() = _binding!!
    private val adapter by lazy { SkillLogAdapter() }
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentSkillLogsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        binding.swipe.setOnRefreshListener { load() }
        binding.fab.setOnClickListener {
            SkillQuickLogSheet().show(parentFragmentManager, "skillQuickLog")
        }

        load()
    }

    private fun load() {
        binding.swipe.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                val db = AppDatabase.get(requireContext())
                val d = db.skillLogDao()
                val du = d.recent(SkillType.DOUBLE_UNDERS.name, SkillTestType.MAX_REPS_UNBROKEN.name, 50)
                val hs = d.recent(SkillType.HANDSTAND_HOLD.name, SkillTestType.MAX_HOLD_SECONDS.name, 50)
                val mu = d.recent(SkillType.MUSCLE_UP.name, SkillTestType.ATTEMPTS.name, 50)
                (du + hs + mu).sortedByDescending { it.date }.map {
                    SkillLogUi(
                        date = dateFmt.format(Date(it.date * 1000)),
                        skill = it.skill,
                        type  = it.testType,
                        detail = when (it.testType) {
                            SkillTestType.MAX_REPS_UNBROKEN.name -> "Reps: ${it.reps ?: 0}"
                            SkillTestType.FOR_TIME_REPS.name     -> "Time: ${com.example.safitness.core.EngineCalculator.formatSeconds(it.timeSeconds) ?: "—"} for ${it.targetReps ?: 0} reps"
                            SkillTestType.MAX_HOLD_SECONDS.name  -> "Hold: ${it.maxHoldSeconds ?: 0}s"
                            SkillTestType.ATTEMPTS.name          -> "Attempts: ${it.attempts ?: 0}"
                            else -> "—"
                        },
                        scaled = if (it.scaled) "Scaled" else "RX"
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
