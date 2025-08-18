package com.example.safitness.ui.engine

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class EngineSkillsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment =
        if (position == 0) EngineLogsFragment() else SkillLogsFragment()
}
