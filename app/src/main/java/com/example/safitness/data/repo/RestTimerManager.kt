package com.example.safitness.data.repo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

/**
 * Session-scoped, in-memory rest timer.
 * No persistence; does not touch Room or existing flows.
 */
class RestTimerManager(
    private val scope: CoroutineScope
) {
    data class State(
        val sessionId: Long,
        val exerciseId: Long,
        val durationMs: Long,
        val remainingMs: Long,
        val isRunning: Boolean
    )

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    private var tickerJob: Job? = null

    fun start(sessionId: Long, exerciseId: Long, durationMs: Long) {
        _state.value = State(sessionId, exerciseId, durationMs, durationMs, true)
        restartTicker()
    }

    fun restartWith(durationMs: Long) {
        _state.value = _state.value?.copy(durationMs = durationMs, remainingMs = durationMs, isRunning = true)
        restartTicker()
    }

    fun addBonus(ms: Long) {
        _state.value = _state.value?.let { it.copy(remainingMs = it.remainingMs + ms) }
    }

    fun pause() {
        _state.value = _state.value?.copy(isRunning = false)
        tickerJob?.cancel()
    }

    fun resume() {
        _state.value = _state.value?.copy(isRunning = true)
        restartTicker()
    }

    fun clear() {
        tickerJob?.cancel()
        _state.value = null
    }

    private fun restartTicker() {
        tickerJob?.cancel()
        val s = _state.value ?: return
        if (!s.isRunning) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val cur = _state.value ?: return@launch
                if (!cur.isRunning) return@launch
                val next = max(0L, cur.remainingMs - 1000L)
                _state.value = cur.copy(remainingMs = next)
                if (next == 0L) {
                    pause()
                    return@launch
                }
            }
        }
    }
}
