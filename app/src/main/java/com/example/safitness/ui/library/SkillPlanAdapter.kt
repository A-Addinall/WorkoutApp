package com.example.safitness.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.safitness.R

class SkillPlanAdapter(
    private val onPrimary: (row: SkillRow, isAdded: Boolean) -> Unit
) : ListAdapter<SkillRow, SkillPlanAdapter.VH>(Diff) {

    private var addedIds: Set<Long> = emptySet()

    fun submit(items: List<SkillRow>, addedIds: Set<Long>) {
        this.addedIds = addedIds
        submitList(items)
    }

    fun updateMembership(addedIds: Set<Long>) {
        this.addedIds = addedIds
        notifyDataSetChanged()
    }

    object Diff : DiffUtil.ItemCallback<SkillRow>() {
        override fun areItemsTheSame(oldItem: SkillRow, newItem: SkillRow) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SkillRow, newItem: SkillRow) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_skill_plan_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvTitle = v.findViewById<TextView>(R.id.tvPlanTitle)
        private val tvMeta  = v.findViewById<TextView>(R.id.tvPlanMeta)
        private val btn     = v.findViewById<Button>(R.id.btnPlanPrimary)

        fun bind(row: SkillRow) {
            tvTitle.text = row.title
            tvMeta.text  = row.meta
            val isAdded = addedIds.contains(row.id)
            btn.text = if (isAdded) "Remove" else "Add to Day"
            btn.setOnClickListener { onPrimary(row, isAdded) }
        }
    }
}

data class SkillRow(
    val id: Long,
    val title: String,
    val meta: String
)
