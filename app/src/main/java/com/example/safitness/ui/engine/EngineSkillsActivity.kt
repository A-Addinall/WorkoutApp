package com.example.safitness.ui.engine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.databinding.ActivityEngineSkillsBinding
import com.google.android.material.tabs.TabLayoutMediator

class EngineSkillsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEngineSkillsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEngineSkillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Engine & Skills"

        binding.pager.adapter = EngineSkillsPagerAdapter(this)
        TabLayoutMediator(binding.tabs, binding.pager) { tab, pos ->
            tab.text = if (pos == 0) "Engine" else "Skills"
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
