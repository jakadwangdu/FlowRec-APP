package com.example.ai

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.example.editor.model.AudioTrackConfig
import com.example.editor.model.EditorProjectState
import com.example.editor.model.TextOverlay
import com.example.editor.model.ZoomKeyframe
import com.example.recorder.touch.FlowTouchEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Real multi-signal AI analysis engine.
 * Inspects recorded gestures, audio tracks, idle intervals, and timeline structures
 * to generate actionable Smart Cuts, Smart Zooms, Captions, and Tutorial Steps.
 */
class AiAnalysisEngine(
    private val context: Context,
    private val modelProvider: AiModelProvider = OnDeviceAiModelProvider(context)
) {
    companion object {
        private const val TAG = "AiAnalysisEngine"
        private const val MIN_IDLE_GAP_FOR_SMART_CUT_MS = 4000L // Min 4s inactivity to propose a cut
        private const val SPATIAL_CLUSTER_MAX_DIST = 0.18f // 18% of screen distance
        private const val TEMPORAL_CLUSTER_MAX_DIFF_MS = 2500L
    }

    /**
     * Entry point to analyze by file and project ID.
     */
    suspend fun analyze(
        projectId: String,
        videoFile: File,
        touchEvents: List<FlowTouchEvent>,
        onProgress: (AiAnalysisProgress) -> Unit
    ): AiAnalysisResult {
        var durationMs = 1000L
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 1000L
            retriever.release()
        } catch (_: Exception) {}

        val defaultState = EditorProjectState.createDefault(projectId, durationMs)
        return analyzeRecording(
            sourceVideoFile = videoFile,
            editorState = defaultState,
            touchEvents = touchEvents,
            projectName = videoFile.nameWithoutExtension,
            onProgress = onProgress
        )
    }

    /**
     * Executes the AI analysis pipeline on the recording.
     */
    suspend fun analyzeRecording(
        sourceVideoFile: File,
        editorState: EditorProjectState,
        touchEvents: List<FlowTouchEvent>,
        projectName: String,
        onProgress: (AiAnalysisProgress) -> Unit
    ): AiAnalysisResult = withContext(Dispatchers.Default) {
        val totalDurationMs = editorState.totalSourceDurationMs.coerceAtLeast(1000L)
        val suggestions = mutableListOf<AiSuggestion>()
        val tutorialSteps = mutableListOf<TutorialStep>()
        val highlightMoments = mutableListOf<AiHighlightMoment>()

        onProgress(
            AiAnalysisProgress(
                state = AiAnalysisState.ANALYZING_TOUCHES,
                progressPercent = 10f,
                currentStepMessage = "Analyzing interaction patterns & touch gestures..."
            )
        )
        ensureActive()

        // ==========================================
        // 1. TOUCH & GESTURE INTELLIGENCE
        // ==========================================
        var smartZoomsCount = 0
        if (touchEvents.isNotEmpty()) {
            val tapEvents = touchEvents.filter { it.action == "DOWN" || it.action == "UP" }
            val clusters = findTouchClusters(tapEvents)

            for (cluster in clusters) {
                ensureActive()
                val avgX = (cluster.map { it.normalizedX }.average().toFloat()).coerceIn(0.15f, 0.85f)
                val avgY = (cluster.map { it.normalizedY }.average().toFloat()).coerceIn(0.15f, 0.85f)
                val firstTime = cluster.first().timestampMs
                val lastTime = cluster.last().timestampMs
                val duration = (lastTime - firstTime + 400L).coerceIn(500L, 1200L)

                val tapCount = cluster.size
                val confidence = (0.75f + (tapCount * 0.05f)).coerceAtMost(0.96f)

                val zoomKeyframe = ZoomKeyframe(
                    id = UUID.randomUUID().toString(),
                    timeMs = firstTime,
                    focalXPercent = avgX,
                    focalYPercent = avgY,
                    scale = 1.65f,
                    durationMs = duration
                )

                suggestions.add(
                    AiSuggestion(
                        type = AiSuggestionType.SMART_ZOOM,
                        title = "Smart Zoom: Focus on Tap Interaction",
                        description = "Smooth 1.6x focal zoom centered at ${(avgX * 100).toInt()}% x ${(avgY * 100).toInt()}% during repeated interaction sequence ($tapCount taps).",
                        startTimeMs = firstTime,
                        endTimeMs = firstTime + duration,
                        confidence = confidence,
                        zoomKeyframe = zoomKeyframe
                    )
                )
                smartZoomsCount++
            }

            // Generate Tutorial Steps from distinct interaction sequences
            val stepIntervals = partitionInteractionSequences(touchEvents, totalDurationMs)
            for ((idx, interval) in stepIntervals.withIndex()) {
                val stepNum = idx + 1
                val leadTouch = interval.touches.firstOrNull()
                val focalX = leadTouch?.normalizedX
                val focalY = leadTouch?.normalizedY

                val title = generateStepTitle(idx, interval.touches.size, leadTouch?.normalizedY ?: 0.5f)
                val desc = "User performed ${interval.touches.size} actions between ${formatTime(interval.startMs)} and ${formatTime(interval.endMs)}."

                val step = TutorialStep(
                    stepNumber = stepNum,
                    title = title,
                    description = desc,
                    startTimeMs = interval.startMs,
                    endTimeMs = interval.endMs,
                    focalXPercent = focalX,
                    focalYPercent = focalY,
                    confidence = 0.85f
                )
                tutorialSteps.add(step)

                suggestions.add(
                    AiSuggestion(
                        type = AiSuggestionType.TUTORIAL_STEP,
                        title = "Step $stepNum: $title",
                        description = desc,
                        startTimeMs = interval.startMs,
                        endTimeMs = interval.endMs,
                        confidence = 0.85f,
                        tutorialStep = step
                    )
                )
            }
        }

        onProgress(
            AiAnalysisProgress(
                state = AiAnalysisState.ANALYZING_AUDIO,
                progressPercent = 40f,
                currentStepMessage = "Inspecting audio energy & speech track..."
            )
        )
        ensureActive()

        // ==========================================
        // 2. AUDIO & SPEECH INTELLIGENCE
        // ==========================================
        val captions = modelProvider.transcribeAudio(sourceVideoFile, totalDurationMs)
        val speechDetected = captions.isNotEmpty()
        var captionsCount = 0

        for (cap in captions) {
            val overlay = TextOverlay(
                id = UUID.randomUUID().toString(),
                text = cap.text,
                startTimeMs = cap.startTimeMs,
                endTimeMs = cap.endTimeMs,
                xPercent = 0.5f,
                yPercent = 0.88f, // bottom subtitle position
                fontSizeSp = 18f,
                textColorHex = "#FFFFFF",
                backgroundColorHex = "#CC000000",
                fontBold = true
            )

            suggestions.add(
                AiSuggestion(
                    type = AiSuggestionType.AUTO_CAPTION,
                    title = "Caption: \"${cap.text.take(30)}...\"",
                    description = "Subtitle segment (${formatTime(cap.startTimeMs)} – ${formatTime(cap.endTimeMs)}) with speech confidence ${(cap.confidence * 100).toInt()}%.",
                    startTimeMs = cap.startTimeMs,
                    endTimeMs = cap.endTimeMs,
                    confidence = cap.confidence,
                    textOverlay = overlay
                )
            )
            captionsCount++
        }

        // Propose Audio Polish if audio is present and not muted
        if (!editorState.audioConfig.originalAudioMuted) {
            suggestions.add(
                AiSuggestion(
                    type = AiSuggestionType.AUDIO_ENHANCE,
                    title = "AI Audio Enhancement (Normal)",
                    description = "Apply background noise floor reduction, speech presence leveling, and dynamic range limiting.",
                    startTimeMs = 0L,
                    endTimeMs = totalDurationMs,
                    confidence = 0.90f,
                    audioPreset = AudioEnhancePreset.NORMAL
                )
            )
        }

        onProgress(
            AiAnalysisProgress(
                state = AiAnalysisState.DETECTING_MOMENTS,
                progressPercent = 70f,
                currentStepMessage = "Detecting idle gaps & key highlight moments..."
            )
        )
        ensureActive()

        // ==========================================
        // 3. SMART CUT (INACTIVITY & IDLE DETECTION)
        // ==========================================
        var idleCutsCount = 0
        val idleIntervals = findIdleIntervals(touchEvents, totalDurationMs)

        for (interval in idleIntervals) {
            val durationSec = (interval.second - interval.first) / 1000f
            val confidence = (0.78f + (durationSec * 0.03f)).coerceAtMost(0.96f)
            val cutStart = interval.first + 400L // leave 400ms margin
            val cutEnd = interval.second - 400L  // leave 400ms margin

            if (cutEnd > cutStart + 1500L) {
                suggestions.add(
                    AiSuggestion(
                        type = AiSuggestionType.SMART_CUT,
                        title = "Smart Cut: Remove Inactivity (${String.format("%.1fs", durationSec)})",
                        description = "Long period without screen interaction (${formatTime(cutStart)} – ${formatTime(cutEnd)}). Trimming this section creates a punchier walkthrough.",
                        startTimeMs = cutStart,
                        endTimeMs = cutEnd,
                        confidence = confidence
                    )
                )
                idleCutsCount++
            }
        }

        // Detect Highlights based on highest action densities
        if (touchEvents.isNotEmpty()) {
            val windowSizeMs = 4000L
            var maxDensity = 0
            var bestWindowStart = 0L

            var t = 0L
            while (t < totalDurationMs) {
                val count = touchEvents.count { it.timestampMs in t..(t + windowSizeMs) }
                if (count > maxDensity) {
                    maxDensity = count
                    bestWindowStart = t
                }
                t += 2000L
            }

            if (maxDensity >= 4) {
                val hStart = bestWindowStart
                val hEnd = (bestWindowStart + windowSizeMs).coerceAtMost(totalDurationMs)
                suggestions.add(
                    AiSuggestion(
                        type = AiSuggestionType.IMPORTANT_MOMENT,
                        title = "Key Highlight: Peak User Activity",
                        description = "High interaction density with $maxDensity distinct touch actions detected around ${formatTime(hStart)}.",
                        startTimeMs = hStart,
                        endTimeMs = hEnd,
                        confidence = 0.91f
                    )
                )
            }
        }

        onProgress(
            AiAnalysisProgress(
                state = AiAnalysisState.GENERATING_SUGGESTIONS,
                progressPercent = 90f,
                currentStepMessage = "Synthesizing title, description & edit proposal..."
            )
        )
        ensureActive()

        // ==========================================
        // 4. TITLE & DESCRIPTION SYNTHESIS
        // ==========================================
        val (suggestedTitle, suggestedDescription) = modelProvider.generateTitleAndDescription(
            projectName = projectName,
            durationMs = totalDurationMs,
            totalTouches = touchEvents.size,
            tutorialSteps = tutorialSteps,
            transcriptSnippets = captions.map { it.text }
        )

        val result = AiAnalysisResult(
            projectId = editorState.projectId,
            sourceDurationMs = totalDurationMs,
            suggestedTitle = suggestedTitle,
            suggestedDescription = suggestedDescription,
            suggestions = suggestions.sortedBy { it.startTimeMs },
            idleCutsCount = idleCutsCount,
            smartZoomsCount = smartZoomsCount,
            highlightsCount = suggestions.count { it.type == AiSuggestionType.IMPORTANT_MOMENT },
            captionsCount = captionsCount,
            tutorialStepsCount = tutorialSteps.size,
            speechDetected = speechDetected
        )

        onProgress(
            AiAnalysisProgress(
                state = AiAnalysisState.READY,
                progressPercent = 100f,
                currentStepMessage = "AI analysis complete!"
            )
        )

        Log.i(TAG, "Analysis complete for project ${editorState.projectId}: ${suggestions.size} suggestions generated (cuts: $idleCutsCount, zooms: $smartZoomsCount, steps: ${tutorialSteps.size})")
        return@withContext result
    }

    /**
     * Spatial and temporal clustering of tap events to identify focal interaction hotspots.
     */
    private fun findTouchClusters(touches: List<FlowTouchEvent>): List<List<FlowTouchEvent>> {
        val clusters = mutableListOf<MutableList<FlowTouchEvent>>()
        val sorted = touches.sortedBy { it.timestampMs }

        for (touch in sorted) {
            var matchedCluster: MutableList<FlowTouchEvent>? = null
            for (cluster in clusters) {
                val lastInCluster = cluster.last()
                val dt = touch.timestampMs - lastInCluster.timestampMs
                if (dt in 0..TEMPORAL_CLUSTER_MAX_DIFF_MS) {
                    val dist = sqrt((touch.normalizedX - lastInCluster.normalizedX).pow(2) + (touch.normalizedY - lastInCluster.normalizedY).pow(2))
                    if (dist <= SPATIAL_CLUSTER_MAX_DIST) {
                        matchedCluster = cluster
                        break
                    }
                }
            }

            if (matchedCluster != null) {
                matchedCluster.add(touch)
            } else {
                clusters.add(mutableListOf(touch))
            }
        }

        // Only return clusters that demonstrate repeated/deliberate interaction (at least 2 taps)
        return clusters.filter { it.size >= 2 }
    }

    /**
     * Identifies long continuous idle intervals with zero touch interactions.
     */
    private fun findIdleIntervals(touches: List<FlowTouchEvent>, totalDurationMs: Long): List<Pair<Long, Long>> {
        val idleIntervals = mutableListOf<Pair<Long, Long>>()
        val sortedTouches = touches.sortedBy { it.timestampMs }

        var lastActiveTimeMs = 0L
        for (touch in sortedTouches) {
            val gap = touch.timestampMs - lastActiveTimeMs
            if (gap >= MIN_IDLE_GAP_FOR_SMART_CUT_MS) {
                idleIntervals.add(Pair(lastActiveTimeMs, touch.timestampMs))
            }
            lastActiveTimeMs = touch.timestampMs
        }

        // Check trailing idle interval
        val tailGap = totalDurationMs - lastActiveTimeMs
        if (tailGap >= MIN_IDLE_GAP_FOR_SMART_CUT_MS) {
            idleIntervals.add(Pair(lastActiveTimeMs, totalDurationMs))
        }

        return idleIntervals
    }

    private data class InteractionInterval(val startMs: Long, val endMs: Long, val touches: List<FlowTouchEvent>)

    private fun partitionInteractionSequences(touches: List<FlowTouchEvent>, totalDurationMs: Long): List<InteractionInterval> {
        if (touches.isEmpty()) return emptyList()
        val intervals = mutableListOf<InteractionInterval>()
        val sorted = touches.sortedBy { it.timestampMs }

        var group = mutableListOf<FlowTouchEvent>()
        var lastTime = sorted.first().timestampMs

        for (touch in sorted) {
            if (touch.timestampMs - lastTime > 3000L && group.isNotEmpty()) {
                intervals.add(InteractionInterval(group.first().timestampMs, group.last().timestampMs + 400L, group.toList()))
                group = mutableListOf()
            }
            group.add(touch)
            lastTime = touch.timestampMs
        }
        if (group.isNotEmpty()) {
            intervals.add(InteractionInterval(group.first().timestampMs, group.last().timestampMs + 400L, group.toList()))
        }
        return intervals.take(10) // Limit to top 10 steps
    }

    private fun generateStepTitle(stepIndex: Int, touchCount: Int, leadY: Float): String {
        val positionLabel = when {
            leadY < 0.33f -> "Top Screen Action"
            leadY > 0.66f -> "Bottom Navigation"
            else -> "Center Area Interaction"
        }
        return "$positionLabel ($touchCount taps)"
    }

    private fun formatTime(timeMs: Long): String {
        val totalSec = (timeMs / 1000L).toInt()
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
    }
}
