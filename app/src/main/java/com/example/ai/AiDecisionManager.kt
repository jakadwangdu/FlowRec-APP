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
                // Add keyframe or subtle callout text if focal coordinates are available
                val step = suggestion.tutorialStep
                if (step != null && step.focalXPercent != null && step.focalYPercent != null) {
                    val kf = com.example.editor.model.ZoomKeyframe(
                        timeMs = step.startTimeMs,
                        focalXPercent = step.focalXPercent,
                        focalYPercent = step.focalYPercent,
                        scale = 1.4f,
                        durationMs = 600L
                    )
                    currentState.copy(zoomKeyframes = currentState.zoomKeyframes + kf)
                } else {
                    currentState
                }
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
                    if (step != null && step.focalXPercent != null && step.focalYPercent != null) {
                        val kf = com.example.editor.model.ZoomKeyframe(
                            timeMs = step.startTimeMs,
                            focalXPercent = step.focalXPercent,
                            focalYPercent = step.focalYPercent,
                            scale = 1.4f,
                            durationMs = 600L
                        )
                        runningState.copy(zoomKeyframes = runningState.zoomKeyframes + kf)
                    } else {
                        runningState
                    }
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
        var s = state
        s = TimelineManager.splitAtTimelineTime(s, startMs)
        s = TimelineManager.splitAtTimelineTime(s, endMs)

        // Mark the segment between startMs and endMs as deleted
        val updatedSegments = s.segments.map { seg ->
            if (seg.sourceStartMs >= startMs - 50L && seg.sourceEndMs <= endMs + 50L) {
                seg.copy(isDeleted = true)
            } else {
                seg
            }
        }
        return s.copy(segments = updatedSegments)
    }
}
