package com.example.safitness.ui.dev

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.safitness.databinding.ItemEngineLogBinding

data class EngineLogUi(
    val date: String,
    val mode: String,
    val intent: String,
    val program: String,
    val result: String,
    val pace: String
)

class EngineLogAdapter :
    ListAdapter<EngineLogUi, EngineLogAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEngineLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val b: ItemEngineLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: EngineLogUi) {
            b.txtTitle.text = "${item.mode} • ${item.intent}"
            b.txtSub.text   = "${item.program} • ${item.result}"
            b.txtMeta.text  = listOf(item.date, item.pace).filter { it.isNotBlank() }.joinToString("   ")
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EngineLogUi>() {
            override fun areItemsTheSame(old: EngineLogUi, new: EngineLogUi) = old === new
            override fun areContentsTheSame(old: EngineLogUi, new: EngineLogUi) = old == new
        }
    }
}
