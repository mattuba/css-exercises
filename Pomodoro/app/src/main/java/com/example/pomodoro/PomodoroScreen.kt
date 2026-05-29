package com.example.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
            .onRotaryScrollEvent { event ->
                viewModel.handleRotary(event.verticalScrollPixels)
                true
            }
            .focusable()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Session label — not tappable
            Text(
                text = state.session.label,
                fontSize = 13.sp,
                color = if (state.session.isFocus) Orange else Green,
            )

            Spacer(Modifier.height(6.dp))

            // Tap target: just the time + hint, circular, ~140dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { viewModel.toggleTimer() }
                    .padding(20.dp)
            ) {
                Text(
                    text = state.timeString,
                    fontSize = 52.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (state.isRunning) "▐▐" else "▶",
                    fontSize = 10.sp,
                    color = Color.Gray,
                )
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
