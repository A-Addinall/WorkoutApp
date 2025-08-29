package com.example.safitness.settings

import android.content.Context
import android.content.SharedPreferences

object Settings {
    private const val PREFS_NAME = "user_settings"
    private const val KEY_EMOM_WORK_SECONDS = "emom_work_seconds"
    private const val KEY_EMOM_SAY_REST = "emom_say_rest"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun emomWorkSeconds(ctx: Context): Int =
        prefs(ctx).getInt(KEY_EMOM_WORK_SECONDS, 40).coerceIn(5, 55)

    fun emomSayRest(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_EMOM_SAY_REST, true)
}