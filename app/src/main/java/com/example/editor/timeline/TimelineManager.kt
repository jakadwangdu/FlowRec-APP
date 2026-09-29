package com.example.editor.timeline

import com.example.editor.model.EditorProjectState
import com.example.editor.model.ImageOverlay
import com.example.editor.model.TextOverlay
import com.example.editor.model.TimelineSegment
import com.example.editor.model.ZoomKeyframe
import java.util.UUID
import kotlin.math.abs

/**
 * Non-destructive timeline engine.
 * Computes timeline-to-source mappings, clip trims, splits, and deletions,
 * ensuring original recording MP4 files remain 100% untouched.
 */
object TimelineManager {

    /**
     * Maps a playhead position on the edited timeline (timelineTimeMs)
     * to the exact source video timestamp (sourceVideoTimeMs) and segment index.
     * Skips deleted segments.
     */
    fun mapTimelineToSource(state: EditorProjectState, timelineTimeMs: Long): Pair<Int, Long> {
        val activeSegments = state.segments.filter { !it.isDeleted }
        if (activeSegments.isEmpty()) {
            return Pair(0, timelineTimeMs.coerceIn(0L, state.totalSourceDurationMs))
        }

        var accumulatedMs = 0L
        for (i in activeSegments.indices) {
            val seg = activeSegments[i]
            val segDur = seg.durationMs
            if (timelineTimeMs < accumulatedMs + segDur || i == activeSegments.lastIndex) {
                val offsetInSeg = (timelineTimeMs - accumulatedMs).coerceAtLeast(0L)
                val sourceOffset = (offsetInSeg.toFloat() * seg.speed).toLong()
                val sourceTime = (seg.sourceStartMs + sourceOffset).coerceIn(seg.sourceStartMs, seg.sourceEndMs)
                val originalIndex = state.segments.indexOf(seg)
                return Pair(originalIndex, sourceTime)
            }
            accumulatedMs += segDur
        }

        val last = activeSegments.last()
        return Pair(state.segments.indexOf(last), last.sourceEndMs)
    }

    /**
     * Maps a source video timestamp (from ExoPlayer) back to timeline display time.
     */
    fun mapSourceToTimeline(state: EditorProjectState, sourceVideoTimeMs: Long): Long {
        val activeSegments = state.segments.filter { !it.isDeleted }
        if (activeSegments.isEmpty()) return sourceVideoTimeMs

        var accumulatedMs = 0L
        for (seg in activeSegments) {
            if (sourceVideoTimeMs in seg.sourceStartMs..seg.sourceEndMs) {
                val offsetInSource = (sourceVideoTimeMs - seg.sourceStartMs).coerceAtLeast(0L)
                val timelineOffset = (offsetInSource.toFloat() / seg.speed.coerceAtLeast(0.1f)).toLong()
                return accumulatedMs + timelineOffset
            }
            accumulatedMs += seg.durationMs
        }

        return accumulatedMs.coerceAtLeast(0L)
    }

    /**
     * Trim the start or end of a segment non-destructively.
     */
    fun trimSegment(
        state: EditorProjectState,
        segmentId: String,
        newStartMs: Long,
        newEndMs: Long
    ): EditorProjectState {
        val updatedSegments = state.segments.map { seg ->
            if (seg.id == segmentId) {
                val validStart = newStartMs.coerceIn(0L, state.totalSourceDurationMs)
                val validEnd = newEndMs.coerceIn(validStart + 200L, state.totalSourceDurationMs)
                seg.copy(sourceStartMs = validStart, sourceEndMs = validEnd)
            } else seg
        }
        return state.copy(segments = updatedSegments)
    }

    /**
     * Split a segment at the specified timeline playhead timestamp.
     * Generates two independent non-destructive segments at the split point.
     */
    fun splitAtTimelineTime(
        state: EditorProjectState,
        timelineTimeMs: Long
    ): EditorProjectState {
        val (segIndex, sourceTimeMs) = mapTimelineToSource(state, timelineTimeMs)
        if (segIndex !in state.segments.indices) return state

        val targetSeg = state.segments[segIndex]
        if (targetSeg.isDeleted) return state

        // Don't split too close to edges (minimum 200ms)
        if (sourceTimeMs <= targetSeg.sourceStartMs + 200L || sourceTimeMs >= targetSeg.sourceEndMs - 200L) {
            return state
        }

        val segA = targetSeg.copy(
            id = UUID.randomUUID().toString(),
            sourceStartMs = targetSeg.sourceStartMs,
            sourceEndMs = sourceTimeMs
        )
        val segB = targetSeg.copy(
            id = UUID.randomUUID().toString(),
            sourceStartMs = sourceTimeMs,
            sourceEndMs = targetSeg.sourceEndMs
        )

        val newSegments = state.segments.toMutableList()
        newSegments.removeAt(segIndex)
        newSegments.add(segIndex, segB)
        newSegments.add(segIndex, segA)

        return state.copy(segments = newSegments)
    }

    /**
     * Non-destructively marks a segment as deleted (skipping it from playback and duration).
     */
    fun deleteSegment(
        state: EditorProjectState,
        segmentId: String
    ): EditorProjectState {
        val updatedSegments = state.segments.map { seg ->
            if (seg.id == segmentId) seg.copy(isDeleted = true) else seg
        }
        return state.copy(segments = updatedSegments)
    }

    /**
     * Calculates the active zoom scale and focal center (focalX, focalY) for the given timeline time.
     * Smoothly interpolates during the transition window of each zoom keyframe.
     */
    fun getActiveZoom(state: EditorProjectState, timelineTimeMs: Long): Triple<Float, Float, Float> {
        val keyframes = state.zoomKeyframes.sortedBy { it.timeMs }
        if (keyframes.isEmpty()) return Triple(1.0f, 0.5f, 0.5f)

        // Find active keyframe
        for (kf in keyframes) {
            val start = kf.timeMs
            val end = kf.timeMs + kf.durationMs
            if (timelineTimeMs in start..end) {
                val progress = ((timelineTimeMs - start).toFloat() / kf.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                val easedProgress = when (kf.easing) {
                    "Linear" -> progress
                    "Spring" -> (1f - kotlin.math.cos(progress * Math.PI.toFloat())) / 2f
                    else -> progress * progress * (3f - 2f * progress) // Smooth Hermite
                }
                val currentScale = 1.0f + (kf.scale - 1.0f) * easedProgress
                val currentX = 0.5f + (kf.focalXPercent - 0.5f) * easedProgress
                val currentY = 0.5f + (kf.focalYPercent - 0.5f) * easedProgress
                return Triple(currentScale, currentX, currentY)
            } else if (timelineTimeMs in end..(end + 1800L)) {
                // Keyframe sustain window
                return Triple(kf.scale, kf.focalXPercent, kf.focalYPercent)
            }
        }

        return Triple(1.0f, 0.5f, 0.5f)
    }

    /**
     * Returns all text overlays visible at the given timeline timestamp.
     */
    fun getActiveTextOverlays(state: EditorProjectState, timelineTimeMs: Long): List<TextOverlay> {
        return state.textOverlays.filter { timelineTimeMs in it.startTimeMs..it.endTimeMs }
    }

    /**
     * Returns all image/logo overlays visible at the given timeline timestamp.
     */
    fun getActiveImageOverlays(state: EditorProjectState, timelineTimeMs: Long): List<ImageOverlay> {
        return state.imageOverlays.filter { timelineTimeMs in it.startTimeMs..it.endTimeMs }
    }
}
