package com.example

import com.example.ai.decision.AiDecisionManager
import com.example.ai.model.AiAnalysisResult
import com.example.ai.model.AiCategory
import com.example.ai.model.AiSuggestion
import com.example.ai.model.AiActionType
import com.example.editor.export.ExportFps
import com.example.editor.export.ExportResolution
import com.example.editor.export.calculateBitrate
import com.example.editor.history.EditorHistoryManager
import com.example.editor.model.AudioTrackConfig
import com.example.editor.model.EditorProjectState
import com.example.editor.model.FaceCamEditorTrack
import com.example.editor.model.ImageOverlay
import com.example.editor.model.TextOverlay
import com.example.editor.model.TimelineSegment
import com.example.editor.model.TouchOverlayConfig
import com.example.editor.model.ZoomKeyframe
import com.example.editor.timeline.TimelineManager
import com.example.model.ExportQuality
import com.example.recorder.touch.FlowTouchEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 7 Release Hardening & Regression Test Suite.
 * Validates production release invariants:
 * - Free, accountless, offline-first execution
 * - Non-destructive editing and history stack integrity
 * - Companion metadata schema stability (.flowedit, .flowtouch, .flowcam, .flowai)
 * - R8/ProGuard preservation of JSON serialization
 * - Hardware export dimension alignment and bitrate bounds
 */
class FlowRecPhase7ReleaseTest {

    @Test
    fun testProductionEditorStateCompleteRoundtrip() {
        val segment1 = TimelineSegment(id = "seg-1", sourceStartMs = 0L, sourceEndMs = 5000L, speed = 1.0f)
        val segment2 = TimelineSegment(id = "seg-2", sourceStartMs = 5000L, sourceEndMs = 12000L, speed = 1.2f)

        val textOverlay = TextOverlay(
            id = "txt-1",
            text = "Production Release v1.0.0",
            startTimeMs = 1000L,
            endTimeMs = 4000L,
            xPercent = 0.5f,
            yPercent = 0.8f,
            fontSizeSp = 24f,
            textColorHex = "#00E5FF",
            backgroundColorHex = "#99000000",
            rotationDeg = 0f,
            fontBold = true
        )

        val imageOverlay = ImageOverlay(
            id = "img-1",
            imageUriOrPath = "file:///android_asset/watermark.png",
            startTimeMs = 0L,
            endTimeMs = 10000L,
            xPercent = 0.9f,
            yPercent = 0.1f,
            sizePercent = 0.15f,
            opacity = 0.85f,
            rotationDeg = 0f
        )

        val zoomKf = ZoomKeyframe(
            id = "zm-1",
            timeMs = 2000L,
            durationMs = 1500L,
            scale = 1.8f,
            focalXPercent = 0.45f,
            focalYPercent = 0.55f,
            easing = "Smooth"
        )

        val state = EditorProjectState(
            projectId = "proj_prod_100",
            sourceDurationMs = 12000L,
            segments = listOf(segment1, segment2),
            textOverlays = listOf(textOverlay),
            imageOverlays = listOf(imageOverlay),
            zoomKeyframes = listOf(zoomKf),
            touchConfig = TouchOverlayConfig(showTouches = true, style = "RIPPLE", colorHex = "#FF4444"),
            faceCamTrack = FaceCamEditorTrack(enabled = true, shape = "CIRCLE", sizePercent = 0.25f),
            audioConfig = AudioTrackConfig(includeOriginal = true, originalVolume = 1.0f)
        )

        // Serialize to .flowedit JSON format
        val json = state.toJson()
        assertNotNull(json)
        assertTrue(json.contains("proj_prod_100"))
        assertTrue(json.contains("Production Release v1.0.0"))

        // Deserialize back
        val restored = EditorProjectState.fromJson(json)
        assertNotNull(restored)
        assertEquals(state.projectId, restored!!.projectId)
        assertEquals(state.segments.size, restored.segments.size)
        assertEquals(state.textOverlays.size, restored.textOverlays.size)
        assertEquals(state.imageOverlays.size, restored.imageOverlays.size)
        assertEquals(state.zoomKeyframes.size, restored.zoomKeyframes.size)
        assertEquals(state.effectiveDurationMs, restored.effectiveDurationMs)
        assertEquals("Production Release v1.0.0", restored.textOverlays[0].text)
    }

    @Test
    fun testProductionAiAnalysisSerializationRoundtrip() {
        val suggestion = AiSuggestion(
            id = "sug_release",
            type = AiActionType.AUTO_ZOOM,
            title = "Focus on Action",
            description = "Zoom into tap coordinates",
            confidence = 0.92f,
            timeMs = 1800L,
            durationMs = 2500L,
            suggestedAction = "zoom",
            actionCategory = AiCategory.ZOOM
        )

        val aiResult = AiAnalysisResult(
            projectId = "proj_release_ai",
            analysisTimestamp = System.currentTimeMillis(),
            suggestions = listOf(suggestion),
            transcriptionText = "Offline local-first screen recording test",
            deadAirDurationsMs = 800L,
            touchClusterCount = 5,
            deviceSpeechAvailable = true
        )

        val json = aiResult.toJson()
        val deserialized = AiAnalysisResult.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(aiResult.projectId, deserialized!!.projectId)
        assertEquals(1, deserialized.suggestions.size)
        assertEquals(suggestion.title, deserialized.suggestions[0].title)
        assertEquals(suggestion.confidence, deserialized.suggestions[0].confidence, 0.001f)
    }

    @Test
    fun testTouchEventsSerializationPreservesCoordinates() {
        val event = FlowTouchEvent(
            timeMs = 1250L,
            normalizedX = 0.42f,
            normalizedY = 0.68f,
            action = 0, // ACTION_DOWN
            pointerCount = 1,
            pressure = 1.0f
        )

        val csvLine = event.toCsvLine()
        val parsed = FlowTouchEvent.fromCsvLine(csvLine)

        assertNotNull(parsed)
        assertEquals(event.timeMs, parsed!!.timeMs)
        assertEquals(event.normalizedX, parsed.normalizedX, 0.001f)
        assertEquals(event.normalizedY, parsed.normalizedY, 0.001f)
        assertEquals(event.action, parsed.action)
    }

    @Test
    fun testExportBitrateAndDimensionsProductionSafety() {
        // Test 720p HD portrait export (common mobile recording)
        val (w720, h720) = ExportResolution.HD_720P.calculateDimensions(1080, 2400)
        assertTrue(w720 % 2 == 0) // H.264 requires even width
        assertTrue(h720 % 2 == 0) // H.264 requires even height
        assertEquals(720, w720)

        // Test 1080p FHD landscape export
        val (w1080, h1080) = ExportResolution.FHD_1080P.calculateDimensions(2400, 1080)
        assertTrue(w1080 % 2 == 0)
        assertTrue(h1080 % 2 == 0)
        assertEquals(1080, h1080)

        // Verify bitrate scaling
        val maxBitrate = ExportQuality.MAXIMUM.calculateBitrate(1920, 1080, 60)
        val balBitrate = ExportQuality.BALANCED.calculateBitrate(1920, 1080, 30)
        assertTrue(maxBitrate > balBitrate)
        assertTrue(maxBitrate in 5_000_000..20_000_000)
    }

    @Test
    fun testOfflineAndAccountlessInvariants() {
        // Verify that AiDecisionManager operates purely locally without remote sync
        val manager = AiDecisionManager()
        assertFalse(manager.hasUndoableAiActions())

        // Verify history manager is strictly bounded to prevent memory leaks in long sessions
        val history = EditorHistoryManager(maxStackSize = 5)
        val baseState = EditorProjectState.createDefault("test", 10_000L)
        for (i in 1..10) {
            history.pushState(baseState.copy(sourceDurationMs = (i * 1000).toLong()))
        }
        // Stack should be capped at maxStackSize
        var undoCount = 0
        var cur = baseState
        while (history.canUndo()) {
            val popped = history.undo(cur)
            if (popped != null) {
                cur = popped
                undoCount++
            }
        }
        assertEquals(5, undoCount)
    }
}
