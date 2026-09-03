package com.omsharma.iykyk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RING_SIZE = 84.dp
private val IDLE_INNER_SIZE = 68.dp
private val RECORDING_INNER_SIZE = 28.dp
private val RECORDING_CORNER_RADIUS = 8.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShutterButton(
    hasPermission: Boolean,
    isRecording: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val innerSize by animateDpAsState(
        targetValue = if (isRecording) RECORDING_INNER_SIZE else IDLE_INNER_SIZE,
        label = "shutterInnerSize"
    )
    val innerCornerRadius by animateDpAsState(
        targetValue = if (isRecording) RECORDING_CORNER_RADIUS else (IDLE_INNER_SIZE / 2),
        label = "shutterCornerRadius"
    )
    val innerColor by animateColorAsState(
        targetValue = when {
            isRecording -> Color.Red
            hasPermission -> Color.White
            else -> Color.White.copy(alpha = 0.4f)
        },
        label = "shutterColor"
    )

    Box(
        modifier = modifier
            .size(RING_SIZE)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            CircularWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(RING_SIZE),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(RING_SIZE)
                    .border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(RoundedCornerShape(innerCornerRadius))
                .background(innerColor)
        )
    }
}
