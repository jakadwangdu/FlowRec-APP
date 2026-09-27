package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun CountdownScreen(
    viewModel: FlowRecViewModel,
    onCancel: () -> Unit,
    onCountdownFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.recorderEngine.cancelCountdown()
        onCancel()
    }

    val countdown by viewModel.recorderEngine.countdown.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.recorderEngine.startCountdown {
            onCountdownFinished()
        }
    }

    val targetProgress = (4 - countdown) / 3f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "countdown_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("countdown_screen")
    ) {
        // Close Button
        IconButton(
            onClick = {
                viewModel.recorderEngine.cancelCountdown()
                onCancel()
            },
            modifier = Modifier
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .align(Alignment.TopStart)
                .testTag("btn_close_countdown")
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancel Recording",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Center Animated Ring and Number
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track Ring
                val strokeColor = MaterialTheme.colorScheme.surfaceVariant
                val activeColor = MaterialTheme.colorScheme.onBackground
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = strokeColor,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Countdown Digit with Smooth Transition
                AnimatedContent(
                    targetState = countdown,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 1.3f) + fadeOut())
                    },
                    label = "countdown_digit"
                ) { count ->
                    Text(
                        text = if (count > 0) "$count" else "REC",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (count > 0) 54.sp else 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (count > 0) MaterialTheme.colorScheme.onBackground else AccentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Get Ready",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Recording starts in $countdown seconds",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
