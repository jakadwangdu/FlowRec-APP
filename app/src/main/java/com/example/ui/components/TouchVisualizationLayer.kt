package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.recorder.touch.FlowTouchEvent
import com.example.recorder.touch.TouchEffectConfig
import kotlinx.coroutines.delay

/**
 * Animated ripple item state for touch visualization
 */
private class ActiveRipple(
    val id: Long,
    val center: Offset,
    val animProgress: Animatable<Float, *> = Animatable(0f)
)

/**
 * Animated trail point for movement tracking
 */
private data class TrailPoint(
    val offset: Offset,
    val timestamp: Long
)

/**
 * Reusable touch visualization layer.
 * Renders configurable tap ripples, highlights, center touch points, and movement trails.
 */
@Composable
fun TouchVisualizationLayer(
    config: TouchEffectConfig,
    modifier: Modifier = Modifier,
    activeTouches: List<FlowTouchEvent> = emptyList(),
    interactive: Boolean = true,
    onPointerEvent: ((x: Float, y: Float, action: String) -> Unit)? = null
) {
    if (!config.enabled) return

    val baseColor = try {
        Color(android.graphics.Color.parseColor(config.colorHex))
    } catch (_: Exception) {
        Color(0xFFFF3B30)
    }

    val activeRipples = remember { mutableStateListOf<ActiveRipple>() }
    val trailPoints = remember { mutableStateListOf<TrailPoint>() }
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }

    // Synchronize external FlowTouchEvents (if driven by TouchTracker or playback)
    LaunchedEffect(activeTouches) {
        if (activeTouches.isNotEmpty()) {
            val latest = activeTouches.last()
            activeTouchPoint = Offset(latest.x, latest.y)
            if (config.rippleEnabled && latest.action == "DOWN") {
                val ripple = ActiveRipple(System.currentTimeMillis(), Offset(latest.x, latest.y))
                activeRipples.add(ripple)
            }
            if (config.movementTrackingEnabled) {
                trailPoints.add(TrailPoint(Offset(latest.x, latest.y), System.currentTimeMillis()))
                if (trailPoints.size > 20) {
                    trailPoints.removeAt(0)
                }
            }
        } else {
            activeTouchPoint = null
        }
    }

    // Clean old trail points
    LaunchedEffect(trailPoints.size) {
        if (trailPoints.isNotEmpty()) {
            delay(120L)
            val now = System.currentTimeMillis()
            trailPoints.removeAll { now - it.timestamp > config.durationMs }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (interactive) {
                    Modifier
                        .pointerInput(config) {
                            detectTapGestures(
                                onPress = { offset ->
                                    activeTouchPoint = offset
                                    onPointerEvent?.invoke(offset.x, offset.y, "DOWN")

                                    if (config.rippleEnabled) {
                                        val ripple = ActiveRipple(System.currentTimeMillis(), offset)
                                        activeRipples.add(ripple)
                                    }

                                    tryAwaitRelease()
                                    activeTouchPoint = null
                                    onPointerEvent?.invoke(offset.x, offset.y, "UP")
                                }
                            )
                        }
                        .pointerInput(config) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    activeTouchPoint = offset
                                    onPointerEvent?.invoke(offset.x, offset.y, "DOWN")
                                    if (config.rippleEnabled) {
                                        val ripple = ActiveRipple(System.currentTimeMillis(), offset)
                                        activeRipples.add(ripple)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pos = change.position
                                    activeTouchPoint = pos
                                    onPointerEvent?.invoke(pos.x, pos.y, "MOVE")
                                    if (config.movementTrackingEnabled) {
                                        trailPoints.add(TrailPoint(pos, System.currentTimeMillis()))
                                        if (trailPoints.size > 25) {
                                            trailPoints.removeAt(0)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    activeTouchPoint?.let { pos ->
                                        onPointerEvent?.invoke(pos.x, pos.y, "UP")
                                    }
                                    activeTouchPoint = null
                                },
                                onDragCancel = {
                                    activeTouchPoint = null
                                }
                            )
                        }
                } else Modifier
            )
    ) {
        // Animate each active ripple
        activeRipples.forEach { ripple ->
            LaunchedEffect(ripple.id) {
                ripple.animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = config.durationMs.coerceAtLeast(150),
                        easing = FastOutSlowInEasing
                    )
                )
                activeRipples.remove(ripple)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizePx = config.sizeDp.toFloat() * density
            val maxRippleRadius = sizePx * 2.2f

            // 1. Draw Movement Trail
            if (config.movementTrackingEnabled && trailPoints.size > 1) {
                val path = Path()
                path.moveTo(trailPoints.first().offset.x, trailPoints.first().offset.y)
                for (i in 1 until trailPoints.size) {
                    val p = trailPoints[i].offset
                    path.lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = baseColor.copy(alpha = config.opacity * 0.45f),
                    style = Stroke(
                        width = (config.sizeDp / 4f) * density,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 2. Draw Active Expanding Ripples
            if (config.rippleEnabled) {
                activeRipples.forEach { ripple ->
                    val progress = ripple.animProgress.value
                    val currentRadius = sizePx * 0.5f + (maxRippleRadius * progress)
                    val alpha = (1f - progress) * config.opacity

                    // Outer expanding ring
                    drawCircle(
                        color = baseColor.copy(alpha = alpha),
                        radius = currentRadius,
                        center = ripple.center,
                        style = Stroke(width = 2.5f * density)
                    )

                    // Inner soft wave
                    drawCircle(
                        color = baseColor.copy(alpha = alpha * 0.25f),
                        radius = currentRadius * 0.7f,
                        center = ripple.center
                    )
                }
            }

            // 3. Draw Tap Highlight & Center Touch Point
            activeTouchPoint?.let { pos ->
                if (config.highlightEnabled) {
                    // Soft luminous halo
                    drawCircle(
                        color = baseColor.copy(alpha = config.opacity * 0.35f),
                        radius = sizePx * 0.85f,
                        center = pos
                    )
                    // Glow border
                    drawCircle(
                        color = Color.White.copy(alpha = config.opacity * 0.8f),
                        radius = sizePx * 0.55f,
                        center = pos,
                        style = Stroke(width = 2f * density)
                    )
                }

                // Center Touch Point (Solid dot)
                drawCircle(
                    color = baseColor.copy(alpha = config.opacity),
                    radius = sizePx * 0.35f,
                    center = pos
                )
                // Center white pupil
                drawCircle(
                    color = Color.White.copy(alpha = config.opacity * 0.9f),
                    radius = sizePx * 0.15f,
                    center = pos
                )
            }
        }
    }
}
