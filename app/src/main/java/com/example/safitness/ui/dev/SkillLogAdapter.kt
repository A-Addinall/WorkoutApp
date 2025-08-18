package com.example.safitness.ui.dev

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.safitness.databinding.ItemSkillLogBinding

data class SkillLogUi(
    val date: String,
    val skill: String,
    val type: String,
    val detail: String,
    val scaled: String
)

class SkillLogAdapter :
    ListAdapter<SkillLogUi, SkillLogAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSkillLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val b: ItemSkillLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: SkillLogUi) {
            b.txtTitle.text = "${item.skill} • ${item.type}"
            b.txtSub.text   = item.detail
            b.txtMeta.text  = "${item.date}   ${item.scaled}"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SkillLogUi>() {
            override fun areItemsTheSame(old: SkillLogUi, new: SkillLogUi) = old === new
            override fun areContentsTheSame(old: SkillLogUi, new: SkillLogUi) = old == new
        }
    }
}
