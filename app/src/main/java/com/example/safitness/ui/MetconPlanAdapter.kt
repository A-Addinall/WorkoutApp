// app/src/main/java/com/example/safitness/ui/MetconPlanAdapter.kt
package com.example.safitness.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safitness.R
import com.example.safitness.core.MetconType
import com.example.safitness.data.entities.MetconPlan

class MetconPlanAdapter(
    private val onPrimary: (plan: MetconPlan, isAlreadyAdded: Boolean) -> Unit
) : RecyclerView.Adapter<MetconPlanAdapter.VH>() {

    private var items: List<MetconPlan> = emptyList()
    private var added: Set<Long> = emptySet()

    fun submit(plans: List<MetconPlan>, addedIds: Set<Long>) {
        items = plans
        added = addedIds
        notifyDataSetChanged()
    }

    fun updateMembership(addedIds: Set<Long>) {
        added = addedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_metcon_plan_row, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], added.contains(items[position].id))
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvTitle: TextView = v.findViewById(R.id.tvPlanTitle)
        private val tvMeta: TextView = v.findViewById(R.id.tvPlanMeta)
        private val btnPrimary: Button = v.findViewById(R.id.btnPlanPrimary)

        fun bind(plan: MetconPlan, isAdded: Boolean) {
            tvTitle.text = plan.title
            tvMeta.text = when (plan.type) {
                MetconType.FOR_TIME -> "For Time"
                MetconType.AMRAP -> plan.durationMinutes?.let { "AMRAP ${it} min" } ?: "AMRAP"
                MetconType.EMOM -> {
                    val mins = plan.durationMinutes?.toString() ?: "—"
                    val interval = plan.emomIntervalSec?.toString() ?: "—"
                    "EMOM ${mins} min • ${interval}s"
                }
            }
            btnPrimary.text = if (isAdded) "Remove from Day" else "Add to Day"
            btnPrimary.setOnClickListener { onPrimary(plan, isAdded) }
        }
    }
}
