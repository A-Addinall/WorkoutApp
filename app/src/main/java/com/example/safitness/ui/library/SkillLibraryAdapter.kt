package com.example.safitness.ui.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.safitness.R

data class SkillUiItem(
    val skill: String,
    val testType: String,
    val title: String,
    val subtitle: String
) {
    val key = "$skill|$testType"
}

class SkillLibraryAdapter(
    private val context: Context,
    private val onToggle: (SkillUiItem) -> Unit
) : BaseAdapter() {

    private var items: List<SkillUiItem> = emptyList()
    private var selectedKeys: Set<String> = emptySet()

    fun submit(newItems: List<SkillUiItem>, selected: Set<String>) {
        items = newItems
        selectedKeys = selected
        notifyDataSetChanged()
    }

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_metcon_plan_row, parent, false)

        val title = v.findViewById<TextView>(R.id.tvPlanTitle)
        val meta  = v.findViewById<TextView>(R.id.tvPlanMeta)
        val btn   = v.findViewById<Button>(R.id.btnPlanPrimary)

        val item = getItem(position)
        title.text = item.title
        meta.text  = item.subtitle

        val isAdded = selectedKeys.contains(item.key)
        btn.text = if (isAdded) "Remove" else "Add to Day"
        btn.setOnClickListener { onToggle(item) }

        // Row click behaves the same as the button
        v.setOnClickListener { onToggle(item) }

        return v
    }
}
