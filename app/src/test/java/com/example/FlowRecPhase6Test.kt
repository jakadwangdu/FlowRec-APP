package com.example

import com.example.ai.AiDecisionManager
import com.example.ai.AiAnalysisProgress
import com.example.ai.AiAnalysisResult
import com.example.ai.AiAnalysisState
import com.example.ai.AiSuggestion
import com.example.ai.AiSuggestionType
import com.example.editor.export.ExportFps
import com.example.editor.export.ExportResolution
import com.example.editor.export.calculateBitrate
import com.example.editor.history.EditorHistoryManager
import com.example.editor.model.EditorProjectState
import com.example.editor.model.TimelineSegment
import com.example.editor.model.ZoomKeyframe
import com.example.editor.timeline.TimelineManager
import com.example.model.ExportQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowRecPhase6Test {

    @Test
    fun testTimelineManagerCanSplitAtTimelineTime() {
        val state = EditorProjectState.createDefault("test_proj", 10_000L) // 10s clip

        // Valid split at 3.0s
        assertTrue(TimelineManager.canSplitAtTimelineTime(state, 3000L))

        // Invalid split: too close to start (< 200ms)
        assertFalse(TimelineManager.canSplitAtTimelineTime(state, 100L))

        // Invalid split: too close to end (9900ms)
        assertFalse(TimelineManager.canSplitAtTimelineTime(state, 9900L))

        // Invalid split: beyond timeline
        assertFalse(TimelineManager.canSplitAtTimelineTime(state, 15_000L))
    }

    @Test
    fun testTimelineManagerSplitAndDurationPreserved() {
        val state = EditorProjectState.createDefault("test_proj", 10_000L)
        val splitState = TimelineManager.splitAtTimelineTime(state, 4000L)

        assertEquals(2, splitState.segments.size)
        assertEquals(10_000L, splitState.effectiveDurationMs)
        assertEquals(0L, splitState.segments[0].sourceStartMs)
        assertEquals(4000L, splitState.segments[0].sourceEndMs)
        assertEquals(4000L, splitState.segments[1].sourceStartMs)
        assertEquals(10_000L, splitState.segments[1].sourceEndMs)
    }

    @Test
    fun testTimelineManagerDeleteGuardsPreventEmptyTimeline() {
        val state = EditorProjectState.createDefault("test_proj", 5000L)
        val singleSegId = state.segments[0].id

        // Cannot delete the only remaining active segment
        assertFalse(TimelineManager.canDeleteSegment(state, singleSegId))
        val afterAttempt = TimelineManager.deleteSegment(state, singleSegId)
        assertFalse(afterAttempt.segments[0].isDeleted) // Protected, not deleted!

        // If split into 2 clips, can delete one of them
        val twoClipsState = TimelineManager.splitAtTimelineTime(state, 2500L)
        val firstClipId = twoClipsState.segments[0].id
        assertTrue(TimelineManager.canDeleteSegment(twoClipsState, firstClipId))

        val afterDeleteOne = TimelineManager.deleteSegment(twoClipsState, firstClipId)
        assertTrue(afterDeleteOne.segments[0].isDeleted)
        assertFalse(afterDeleteOne.segments[1].isDeleted)
        assertEquals(2500L, afterDeleteOne.effectiveDurationMs)

        // Now cannot delete the remaining clip
        assertFalse(TimelineManager.canDeleteSegment(afterDeleteOne, afterDeleteOne.segments[1].id))
    }

    @Test
    fun testExportResolutionDimensionCalculations() {
        // Source 1080x1920 (Portrait)
        val (res720W, res720H) = ExportResolution.HD_720P.calculateDimensions(1080, 1920)
        assertEquals(720, res720W)
        assertEquals(1280, res720H)

        val (res1080W, res1080H) = ExportResolution.FHD_1080P.calculateDimensions(1080, 1920)
        assertEquals(1080, res1080W)
        assertEquals(1920, res1080H)

        // Bitrate calculation
        val bitrate = ExportQuality.HIGH.calculateBitrate(1080, 1920, 60)
        assertTrue(bitrate in 8_000_000..20_000_000)
    }

    @Test
    fun testAiAnalysisResultJsonSerialization() {
        val suggestion = AiSuggestion(
            id = "sug_1",
            type = AiSuggestionType.SMART_CUT,
            title = "Trim initial silence",
            description = "Cut 1.2s dead air",
            startTimeMs = 0L,
            endTimeMs = 1200L,
            confidence = 0.95f
        )
        val result = AiAnalysisResult(
            projectId = "proj_test",
            sourceDurationMs = 10_000L,
            suggestedTitle = "Welcome to FlowRec",
            suggestedDescription = "Demo recording",
            suggestions = listOf(suggestion),
            idleCutsCount = 1
        )

        val json = result.toJson()
        val deserialized = AiAnalysisResult.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(result.projectId, deserialized!!.projectId)
        assertEquals(1, deserialized.suggestions.size)
        assertEquals(result.suggestions[0].title, deserialized.suggestions[0].title)
        assertEquals(result.suggestedTitle, deserialized.suggestedTitle)
    }

    @Test
    fun testAiDecisionManagerUndoAndAcceptReject() {
        val zoomKf = ZoomKeyframe(
            timeMs = 1500L,
            scale = 1.8f,
            durationMs = 2000L
        )
        val suggestion = AiSuggestion(
            id = "sug_test",
            type = AiSuggestionType.SMART_ZOOM,
            title = "Smooth Zoom",
            description = "Zoom into tap target",
            startTimeMs = 1500L,
            endTimeMs = 3500L,
            confidence = 0.88f,
            zoomKeyframe = zoomKf
        )

        val history = EditorHistoryManager(maxHistorySize = 10)
        val initial = EditorProjectState.createDefault("proj_ai", 10_000L)

        val updated = AiDecisionManager.applySuggestion(initial, suggestion, history)
        assertTrue(history.canUndo)
        assertEquals(1, updated.zoomKeyframes.size)

        // Undo AI action through history
        val reverted = history.undo(updated)
        assertNotNull(reverted)
        assertEquals(0, reverted!!.zoomKeyframes.size)
    }

    @Test
    fun testEditorHistoryManagerPushUndoRedo() {
        val history = EditorHistoryManager(maxHistorySize = 10)
        val state0 = EditorProjectState.createDefault("test", 5000L)
        val state1 = state0.copy(version = 2)
        val state2 = state0.copy(version = 3)

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        history.pushState(state0)
        assertTrue(history.canUndo)

        val undone = history.undo(state1)
        assertEquals(state0, undone)
        assertTrue(history.canRedo)

        val redone = history.redo(state0)
        assertEquals(state1, redone)
    }
}
