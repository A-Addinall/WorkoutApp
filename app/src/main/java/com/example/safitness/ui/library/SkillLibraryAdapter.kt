package com.example.safitness.ui.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
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
    private var items: List<SkillUiItem> = emptyList(),
    private var selectedKeys: Set<String> = emptySet()
) : BaseAdapter() {

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
            .inflate(R.layout.item_library_simple, parent, false)
        val title = v.findViewById<TextView>(R.id.txtTitle)
        val sub   = v.findViewById<TextView>(R.id.txtSubtitle)
        val check = v.findViewById<TextView>(R.id.txtCheck)

        val item = getItem(position)
        title.text = item.title
        sub.text   = item.subtitle
        check.visibility = if (selectedKeys.contains(item.key)) View.VISIBLE else View.GONE
        return v
    }
}
