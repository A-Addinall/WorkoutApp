package com.example.safitness.ui

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.example.safitness.R

object MetconUiHelpers {
    fun bindPlanCard(activity: Activity, vm: WorkoutViewModel, planId: Long) {
        if (planId <= 0) return
        vm.planWithComponents(planId).observe(activity as LifecycleOwner) { pwc ->
            val plan = pwc.plan
            val comps = pwc.components.sortedBy { it.orderInPlan }

            activity.findViewById<TextView?>(R.id.tvPlanCardTitle)?.text = plan.title
            val container = activity.findViewById<LinearLayout?>(R.id.layoutPlanComponents)
            container?.removeAllViews()
            comps.forEach { c ->
                container?.addView(TextView(activity).apply {
                    text = "• ${c.text}"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }
            // You can also mirror last time if you want:
            // activity.findViewById<TextView?>(R.id.tvPlanLastTime)?.text = ...
        }
    }
}
