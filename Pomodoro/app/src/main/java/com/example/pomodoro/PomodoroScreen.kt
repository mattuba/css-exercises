package com.example.pomodoro

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Text

private val Orange = Color(0xFFFF6B35)
private val Green  = Color(0xFF66BB6A)

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            // Crown rotation → scrub time (ignored while running)
            .onRotaryScrollEvent { event ->
                viewModel.handleRotary(event.verticalScrollPixels)
                true
            }
            // Side button (STEM_PRIMARY) → start / pause
            .onKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                if (native.keyCode == KeyEvent.KEYCODE_STEM_PRIMARY &&
                    native.action == android.view.KeyEvent.ACTION_DOWN
                ) {
                    viewModel.toggleTimer()
                    true
                } else {
                    false
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.session.label,
                fontSize = 13.sp,
                color = if (state.session.isFocus) Orange else Green,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = state.timeString,
                fontSize = 52.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (state.isRunning) "running" else "paused",
                fontSize = 11.sp,
                color = Color.Gray,
            )
        }
    }

    // Crown rotation requires the composable to hold focus.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
