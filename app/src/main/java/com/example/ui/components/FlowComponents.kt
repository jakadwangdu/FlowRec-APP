package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.UiFeatureFlagManager
import com.example.model.AudioSourceMode
import com.example.model.FrameRate
import com.example.model.RecordingResolution
import com.example.ui.theme.flow
import kotlin.math.sin

/**
 * FlowRec Shutter Button (as specified in compose.md)
 * 84 dp, circle morphs to rounded square during recording with spring physics and haptics.
 */
@Composable
fun Shutter(
    recording: Boolean,
    size: Dp = 84.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val inner by animateDpAsState(
        targetValue = if (recording) size * 0.36f else size * 0.76f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "shutter_inner"
    )
    val radius by animateDpAsState(
        targetValue = if (recording) 12.dp else size,
        animationSpec = tween(400),
        label = "shutter_radius"
    )

    Box(
        modifier = modifier
            .size(size)
            .border(4.dp, flow.fg, CircleShape)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = if (recording) "Stop recording" else "Start recording"
            }
            .clickable {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (e: Exception) {
                    // Ignore haptics failure on unsupported hardware
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(inner)
                .clip(RoundedCornerShape(radius))
                .background(flow.record)
        )
    }
}

/**
 * FlowRec Minimalist Segmented Control (as specified in compose.md)
 * Sliding glass pill indicator with 400ms transition and haptics.
 */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .clip(CircleShape)
            .background(flow.fill)
            .padding(3.dp)
    ) {
        val count = options.size.coerceAtLeast(1)
        val w = maxWidth / count
        val safeSelected = selected.coerceIn(0, count - 1)
        val x by animateDpAsState(
            targetValue = w * safeSelected,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            label = "segmented_indicator"
        )

        // Glass sliding indicator pill
        Box(
            modifier = Modifier
                .offset(x = x)
                .width(w)
                .height(34.dp)
                .clip(CircleShape)
                .background(flow.glass)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val isCurrent = i == safeSelected
                Box(
                    modifier = Modifier
                        .width(w)
                        .height(34.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Tab) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (e: Exception) {
                                // Ignore
                            }
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isCurrent) flow.fg else flow.muted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * FlowRec Minimalist Pill Switch (51 x 31 dp) (as specified in compose.md)
 */
@Composable
fun FlowSwitch(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val knob by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "switch_knob"
    )

    Box(
        modifier = modifier
            .size(51.dp, 31.dp)
            .clip(CircleShape)
            .background(if (checked) flow.fg else flow.fill)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (e: Exception) {
                        // Ignore
                    }
                    onChange(it)
                }
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knob)
                .size(27.dp)
                .clip(CircleShape)
                .background(if (checked) flow.onFg else Color.White)
        )
    }
}

/**
 * Subtle Animated Concentric Ripple Rings (as specified in compose.md)
 * Used in background of Home and Capture screens.
 */
@Composable
fun Rings(
    recording: Boolean,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rings_motion")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "rings_phase"
    )

    val tint = if (recording) flow.record else flow.fg

    Canvas(modifier = modifier.fillMaxSize()) {
        listOf(1f, 1.35f, 1.75f).forEachIndexed { i, m ->
            val wobble = if (reduceMotion) 0f else sin(t * 2 - i) * if (recording) 0.05f else 0.02f
            val baseRadius = size.width * 0.22f * m * (1f + wobble)
            val alpha = (0.18f - i * 0.05f).coerceAtLeast(0.04f)

            drawCircle(
                color = tint.copy(alpha = alpha),
                radius = baseRadius,
                center = Offset(size.width / 2f, size.height * 0.42f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Quality Bottom Sheet (as specified in compose.md & SKILL.md)
 * Segmented controls for resolution, fps, audio source.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySheet(
    onDismiss: () -> Unit,
    currentResolution: RecordingResolution,
    onResolutionSelected: (RecordingResolution) -> Unit,
    currentFps: FrameRate,
    onFpsSelected: (FrameRate) -> Unit,
    currentAudioSource: AudioSourceMode,
    onAudioSourceSelected: (AudioSourceMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val resOptions = listOf("720p", "1080p", "Native")
    val resValues = listOf(RecordingResolution.RES_720P, RecordingResolution.RES_1080P, RecordingResolution.RES_NATIVE)
    val selectedResIndex = when (currentResolution) {
        RecordingResolution.RES_720P -> 0
        RecordingResolution.RES_1080P -> 1
        else -> 2
    }

    val fpsOptions = listOf("24 FPS", "30 FPS", "60 FPS")
    val fpsValues = listOf(FrameRate.FPS_24, FrameRate.FPS_30, FrameRate.FPS_60)
    val selectedFpsIndex = when (currentFps) {
        FrameRate.FPS_24 -> 0
        FrameRate.FPS_30 -> 1
        FrameRate.FPS_60 -> 2
    }

    val audioOptions = listOf("Device", "Mic", "Both", "Mute")
    val audioValues = listOf(
        AudioSourceMode.INTERNAL,
        AudioSourceMode.MIC,
        AudioSourceMode.MIC_AND_INTERNAL,
        AudioSourceMode.NONE
    )
    val selectedAudioIndex = when (currentAudioSource) {
        AudioSourceMode.INTERNAL -> 0
        AudioSourceMode.MIC -> 1
        AudioSourceMode.MIC_AND_INTERNAL -> 2
        AudioSourceMode.NONE -> 3
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = flow.sheet,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Recording Quality",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = flow.fg
            )
            Text(
                text = "Configure resolution, framerate, and audio track sources",
                fontSize = 13.sp,
                color = flow.muted
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Resolution
            Text(
                text = "Resolution",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = flow.fg
            )
            Spacer(modifier = Modifier.height(8.dp))
            Segmented(
                options = resOptions,
                selected = selectedResIndex,
                onSelect = { index -> onResolutionSelected(resValues[index]) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Framerate
            Text(
                text = "Frame Rate",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = flow.fg
            )
            Spacer(modifier = Modifier.height(8.dp))
            Segmented(
                options = fpsOptions,
                selected = selectedFpsIndex,
                onSelect = { index -> onFpsSelected(fpsValues[index]) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Audio Source
            Text(
                text = "Audio Source",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = flow.fg
            )
            Spacer(modifier = Modifier.height(8.dp))
            Segmented(
                options = audioOptions,
                selected = selectedAudioIndex,
                onSelect = { index -> onAudioSourceSelected(audioValues[index]) }
            )
        }
    }
}
