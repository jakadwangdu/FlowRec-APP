package com.example.ai

import com.example.editor.history.EditorHistoryManager
import com.example.editor.model.EditorProjectState
import com.example.editor.timeline.TimelineManager

/**
 * Executes AI recommendations safely on the non-destructive EditorProjectState.
 * Enforces strict separation between analysis and editor mutation, and records
 * every change into EditorHistoryManager so users can immediately Undo / Redo.
 */
object AiDecisionManager {

    /**
     * Applies a single AI suggestion to the editor state.
     */
    fun applySuggestion(
        currentState: EditorProjectState,
        suggestion: AiSuggestion,
        historyManager: EditorHistoryManager
    ): EditorProjectState {
        // Record undo point
        historyManager.pushState(currentState)

        val updatedState = when (suggestion.type) {
            AiSuggestionType.SMART_CUT -> {
                applySmartCut(currentState, suggestion.startTimeMs, suggestion.endTimeMs)
            }
            AiSuggestionType.SMART_ZOOM -> {
                suggestion.zoomKeyframe?.let { kf ->
                    currentState.copy(zoomKeyframes = currentState.zoomKeyframes + kf)
                } ?: currentState
            }
            AiSuggestionType.AUTO_CAPTION -> {
                suggestion.textOverlay?.let { overlay ->
                    currentState.copy(textOverlays = currentState.textOverlays + overlay)
                } ?: currentState
            }
            AiSuggestionType.AUDIO_ENHANCE -> {
                // Apply speech clarity normalization
                val currentAudio = currentState.audioConfig
                val enhancedAudio = currentAudio.copy(
                    originalAudioVolume = 1.15f,
                    originalAudioMuted = false
                )
                currentState.copy(audioConfig = enhancedAudio)
            }
            AiSuggestionType.TUTORIAL_STEP -> {
                val step = suggestion.tutorialStep
                var stepState = currentState
                if (step != null && step.focalXPercent != null && step.focalYPercent != null) {
                    val kf = com.example.editor.model.ZoomKeyframe(
                        timeMs = step.startTimeMs,
                        focalXPercent = step.focalXPercent,
                        focalYPercent = step.focalYPercent,
                        scale = 1.4f,
                        durationMs = 600L
                    )
                    stepState = stepState.copy(zoomKeyframes = stepState.zoomKeyframes + kf)
                }
                val callout = com.example.editor.model.TextOverlay(
                    id = java.util.UUID.randomUUID().toString(),
                    text = suggestion.title,
                    startTimeMs = suggestion.startTimeMs,
                    endTimeMs = (suggestion.startTimeMs + 2500L).coerceAtMost(stepState.effectiveDurationMs.coerceAtLeast(1000L)),
                    xPercent = 0.5f,
                    yPercent = 0.86f,
                    textColorHex = "#FFFFFF",
                    backgroundColorHex = "#CC121216",
                    fontSizeSp = 13
                )
                stepState.copy(textOverlays = stepState.textOverlays + callout)
            }
            AiSuggestionType.IMPORTANT_MOMENT -> {
                // Preserved on timeline highlights; no destructive change needed
                currentState
            }
        }

        suggestion.isApplied = true
        suggestion.isRejected = false
        return updatedState
    }

    /**
     * Applies all pending AI suggestions in a single atomic, undoable operation ("Make it Flow").
     */
    fun applyAll(
        currentState: EditorProjectState,
        suggestions: List<AiSuggestion>,
        historyManager: EditorHistoryManager
    ): EditorProjectState {
        // Push single root state before applying batch so one "Undo" restores everything
        historyManager.pushState(currentState)

        var runningState = currentState

        for (suggestion in suggestions.filter { !it.isApplied && !it.isRejected }) {
            runningState = when (suggestion.type) {
                AiSuggestionType.SMART_CUT -> {
                    applySmartCut(runningState, suggestion.startTimeMs, suggestion.endTimeMs)
                }
                AiSuggestionType.SMART_ZOOM -> {
                    suggestion.zoomKeyframe?.let { kf ->
                        runningState.copy(zoomKeyframes = runningState.zoomKeyframes + kf)
                    } ?: runningState
                }
                AiSuggestionType.AUTO_CAPTION -> {
                    suggestion.textOverlay?.let { overlay ->
                        runningState.copy(textOverlays = runningState.textOverlays + overlay)
                    } ?: runningState
                }
                AiSuggestionType.AUDIO_ENHANCE -> {
                    val currentAudio = runningState.audioConfig
                    val enhanced = currentAudio.copy(
                        originalAudioVolume = 1.15f,
                        originalAudioMuted = false
                    )
                    runningState.copy(audioConfig = enhanced)
                }
                AiSuggestionType.TUTORIAL_STEP -> {
                    val step = suggestion.tutorialStep
                    var stepState = runningState
                    if (step != null && step.focalXPercent != null && step.focalYPercent != null) {
                        val kf = com.example.editor.model.ZoomKeyframe(
                            timeMs = step.startTimeMs,
                            focalXPercent = step.focalXPercent,
                            focalYPercent = step.focalYPercent,
                            scale = 1.4f,
                            durationMs = 600L
                        )
                        stepState = stepState.copy(zoomKeyframes = stepState.zoomKeyframes + kf)
                    }
                    val callout = com.example.editor.model.TextOverlay(
                        id = java.util.UUID.randomUUID().toString(),
                        text = suggestion.title,
                        startTimeMs = suggestion.startTimeMs,
                        endTimeMs = (suggestion.startTimeMs + 2500L).coerceAtMost(stepState.effectiveDurationMs.coerceAtLeast(1000L)),
                        xPercent = 0.5f,
                        yPercent = 0.86f,
                        textColorHex = "#FFFFFF",
                        backgroundColorHex = "#CC121216",
                        fontSizeSp = 13
                    )
                    stepState.copy(textOverlays = stepState.textOverlays + callout)
                }
                AiSuggestionType.IMPORTANT_MOMENT -> runningState
            }

            suggestion.isApplied = true
            suggestion.isRejected = false
        }

        return runningState
    }

    /**
     * Rejects a suggestion without modifying editor state.
     */
    fun rejectSuggestion(suggestion: AiSuggestion) {
        suggestion.isRejected = true
        suggestion.isApplied = false
    }

    /**
     * Splits and marks idle segment as deleted in the timeline.
     */
    private fun applySmartCut(state: EditorProjectState, startMs: Long, endMs: Long): EditorProjectState {
        // Protect timeline: never delete all segments
        val activeCount = state.segments.count { !it.isDeleted }
        if (activeCount <= 0) return state

        var s = state
        val tlStart = TimelineManager.mapSourceToTimeline(s, startMs)
        val tlEnd = TimelineManager.mapSourceToTimeline(s, endMs)

        if (TimelineManager.canSplitAtTimelineTime(s, tlStart)) {
            s = TimelineManager.splitAtTimelineTime(s, tlStart)
        }
        val remappedEnd = TimelineManager.mapSourceToTimeline(s, endMs)
        if (TimelineManager.canSplitAtTimelineTime(s, remappedEnd)) {
            s = TimelineManager.splitAtTimelineTime(s, remappedEnd)
        }

        // Mark candidate segments inside the cut interval as deleted, ensuring at least one remains active
        val updatedSegments = s.segments.toMutableList()
        for (i in updatedSegments.indices) {
            val seg = updatedSegments[i]
            if (!seg.isDeleted && seg.sourceStartMs >= startMs - 100L && seg.sourceEndMs <= endMs + 100L) {
                val wouldRemain = updatedSegments.count { !it.isDeleted } > 1
                if (wouldRemain) {
                    updatedSegments[i] = seg.copy(isDeleted = true)
                }
            }
        }
        return s.copy(segments = updatedSegments)
    }
}
