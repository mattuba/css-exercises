package com.example.pomodoro

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Session(val label: String, val durationSecs: Int, val isFocus: Boolean)

val SESSIONS = listOf(
    Session("Focus 1",    25 * 60, isFocus = true),
    Session("Break",       5 * 60, isFocus = false),
    Session("Focus 2",    25 * 60, isFocus = true),
    Session("Break",       5 * 60, isFocus = false),
    Session("Focus 3",    25 * 60, isFocus = true),
    Session("Break",       5 * 60, isFocus = false),
    Session("Focus 4",    25 * 60, isFocus = true),
    Session("Long Break", 15 * 60, isFocus = false),
)

data class TimerState(
    val sessionIndex: Int = 0,
    val timeRemainingSecs: Double = SESSIONS[0].durationSecs.toDouble(),
    val isRunning: Boolean = false,
) {
    val session: Session get() = SESSIONS[sessionIndex]
    val timeString: String get() {
        val t = maxOf(0, timeRemainingSecs.toInt())
        return "%02d:%02d".format(t / 60, t % 60)
    }
}

class PomodoroViewModel(app: Application) : AndroidViewModel(app) {

    private val vibrator = app.getSystemService<Vibrator>()

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var timerJob: Job? = null

    // Accumulates sub-step crown pixels before committing a 30-second step.
    // Requires ~20 pixels per step — tunable below.
    private var rotaryAccumulator = 0f
    private val PIXELS_PER_STEP = 20f   // raise to reduce sensitivity
    private val SECS_PER_STEP    = 30   // seconds changed per step

    // ── Timer control ──────────────────────────────────────────────────────

    fun toggleTimer() {
        if (_state.value.isRunning) pause() else start()
    }

    private fun start() {
        _state.value = _state.value.copy(isRunning = true)
        haptic(Haptic.CLICK)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val s = _state.value
                if (!s.isRunning) break
                if (s.timeRemainingSecs <= 1.0) {
                    advanceSession()
                    break
                } else {
                    _state.value = s.copy(timeRemainingSecs = s.timeRemainingSecs - 1.0)
                }
            }
        }
    }

    private fun pause() {
        timerJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
        haptic(Haptic.CLICK)
    }

    private fun advanceSession() {
        timerJob?.cancel()
        val next = (_state.value.sessionIndex + 1) % SESSIONS.size
        _state.value = TimerState(sessionIndex = next)
        haptic(Haptic.DOUBLE_CLICK)
    }

    // ── Crown / rotary input ───────────────────────────────────────────────
    // Called from onRotaryScrollEvent; positive pixels = clockwise = forward in time.

    fun handleRotary(pixelsScrolled: Float) {
        val s = _state.value
        if (s.isRunning) return

        rotaryAccumulator += pixelsScrolled
        val steps = (rotaryAccumulator / PIXELS_PER_STEP).toInt()
        if (steps == 0) return

        rotaryAccumulator -= steps * PIXELS_PER_STEP
        applyTimeSteps(steps)
    }

    private fun applyTimeSteps(steps: Int) {
        val s = _state.value
        val newTime = s.timeRemainingSecs + steps * SECS_PER_STEP

        when {
            newTime >= s.session.durationSecs -> {
                val next = (s.sessionIndex + 1) % SESSIONS.size
                _state.value = TimerState(sessionIndex = next)
                rotaryAccumulator = 0f
                haptic(Haptic.TICK)
            }
            newTime < 0 -> {
                val prev = (s.sessionIndex - 1 + SESSIONS.size) % SESSIONS.size
                _state.value = TimerState(
                    sessionIndex = prev,
                    timeRemainingSecs = 0.0
                )
                rotaryAccumulator = 0f
                haptic(Haptic.TICK)
            }
            else -> {
                _state.value = s.copy(timeRemainingSecs = newTime)
                haptic(Haptic.TICK)
            }
        }
    }

    // ── Haptics ────────────────────────────────────────────────────────────

    private enum class Haptic { CLICK, DOUBLE_CLICK, TICK }

    private fun haptic(type: Haptic) {
        val effect = when (type) {
            Haptic.CLICK        -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            Haptic.DOUBLE_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            Haptic.TICK         -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
        vibrator?.vibrate(effect)
    }
}
