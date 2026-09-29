package com.example.ai

import com.example.editor.model.AudioTrackConfig
import com.example.editor.model.TextOverlay
import com.example.editor.model.ZoomKeyframe
import java.util.UUID

/**
 * Types of AI video intelligence suggestions.
 */
enum class AiSuggestionType(val label: String, val iconName: String) {
    SMART_CUT("Smart Cut", "ContentCut"),
    SMART_ZOOM("Smart Zoom", "ZoomIn"),
    IMPORTANT_MOMENT("Key Moment", "Star"),
    AUTO_CAPTION("Caption", "Subtitles"),
    TUTORIAL_STEP("Tutorial Step", "ListAlt"),
    AUDIO_ENHANCE("Audio Polish", "GraphicEq")
}

/**
 * Lifecycle states of AI analysis pipeline.
 */
enum class AiAnalysisState {
    IDLE,
    ANALYZING_TOUCHES,
    ANALYZING_AUDIO,
    DETECTING_MOMENTS,
    GENERATING_SUGGESTIONS,
    READY,
    ERROR,
    CANCELLED
}

/**
 * Audio enhancement presets supported by the on-device audio processor.
 */
enum class AudioEnhancePreset(val label: String, val description: String) {
    OFF("Original Audio", "No audio processing applied"),
    NORMAL("Balanced Speech", "Gentle noise floor reduction & speech leveling"),
    ENHANCED("Vocal Clarity", "Dynamic range control, high-pass vocal boost & background gate")
}

/**
 * Step-by-step tutorial card generated from detected interactions.
 */
data class TutorialStep(
    val id: String = UUID.randomUUID().toString(),
    val stepNumber: Int,
    val title: String,
    val description: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val focalXPercent: Float? = null,
    val focalYPercent: Float? = null,
    val confidence: Float = 0.85f
)

/**
 * Detected highlight moment on the timeline.
 */
data class AiHighlightMoment(
    val id: String = UUID.randomUUID().toString(),
    val startTimeMs: Long,
    val endTimeMs: Long,
    val reason: String,
    val confidence: Float
)

/**
 * Auto-caption subtitle segment from speech analysis.
 */
data class CaptionSegment(
    val id: String = UUID.randomUUID().toString(),
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float
)

/**
 * An actionable AI recommendation with user review capabilities.
 */
data class AiSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val type: AiSuggestionType,
    val title: String,
    val description: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float, // 0.0 to 1.0 based on real signal strength
    val confidenceLabel: String = when {
        confidence >= 0.88f -> "High Confidence"
        confidence >= 0.70f -> "Likely"
        else -> "Detected"
    },
    var isApplied: Boolean = false,
    var isRejected: Boolean = false,
    // Payloads
    val zoomKeyframe: ZoomKeyframe? = null,
    val textOverlay: TextOverlay? = null,
    val tutorialStep: TutorialStep? = null,
    val audioPreset: AudioEnhancePreset? = null
) {
    fun toJson(): org.json.JSONObject {
        val obj = org.json.JSONObject()
        obj.put("id", id)
        obj.put("type", type.name)
        obj.put("title", title)
        obj.put("description", description)
        obj.put("startTimeMs", startTimeMs)
        obj.put("endTimeMs", endTimeMs)
        obj.put("confidence", confidence.toDouble())
        obj.put("confidenceLabel", confidenceLabel)
        obj.put("isApplied", isApplied)
        obj.put("isRejected", isRejected)
        zoomKeyframe?.let { zk ->
            val zkObj = org.json.JSONObject()
            zkObj.put("timeMs", zk.timeMs)
            zkObj.put("scale", zk.scale.toDouble())
            zkObj.put("focalXPercent", zk.focalXPercent.toDouble())
            zkObj.put("focalYPercent", zk.focalYPercent.toDouble())
            zkObj.put("durationMs", zk.durationMs)
            obj.put("zoomKeyframe", zkObj)
        }
        textOverlay?.let { to ->
            val toObj = org.json.JSONObject()
            toObj.put("id", to.id)
            toObj.put("text", to.text)
            toObj.put("startTimeMs", to.startTimeMs)
            toObj.put("endTimeMs", to.endTimeMs)
            toObj.put("xPercent", to.xPercent.toDouble())
            toObj.put("yPercent", to.yPercent.toDouble())
            toObj.put("fontSizeSp", to.fontSizeSp)
            toObj.put("textColorHex", to.textColorHex)
            toObj.put("backgroundColorHex", to.backgroundColorHex)
            obj.put("textOverlay", toObj)
        }
        tutorialStep?.let { ts ->
            val tsObj = org.json.JSONObject()
            tsObj.put("id", ts.id)
            tsObj.put("stepNumber", ts.stepNumber)
            tsObj.put("title", ts.title)
            tsObj.put("description", ts.description)
            tsObj.put("startTimeMs", ts.startTimeMs)
            tsObj.put("endTimeMs", ts.endTimeMs)
            ts.focalXPercent?.let { tsObj.put("focalXPercent", it.toDouble()) }
            ts.focalYPercent?.let { tsObj.put("focalYPercent", it.toDouble()) }
            tsObj.put("confidence", ts.confidence.toDouble())
            obj.put("tutorialStep", tsObj)
        }
        audioPreset?.let {
            obj.put("audioPreset", it.name)
        }
        return obj
    }

    companion object {
        fun fromJson(obj: org.json.JSONObject): AiSuggestion {
            val type = try { AiSuggestionType.valueOf(obj.getString("type")) } catch (_: Exception) { AiSuggestionType.IMPORTANT_MOMENT }
            var zk: ZoomKeyframe? = null
            if (obj.has("zoomKeyframe")) {
                val zObj = obj.getJSONObject("zoomKeyframe")
                zk = ZoomKeyframe(
                    timeMs = zObj.optLong("timeMs"),
                    scale = zObj.optDouble("scale", 1.6).toFloat(),
                    focalXPercent = zObj.optDouble("focalXPercent", 0.5).toFloat(),
                    focalYPercent = zObj.optDouble("focalYPercent", 0.5).toFloat(),
                    durationMs = zObj.optLong("durationMs", 800L)
                )
            }
            var to: TextOverlay? = null
            if (obj.has("textOverlay")) {
                val tObj = obj.getJSONObject("textOverlay")
                to = TextOverlay(
                    id = tObj.optString("id", UUID.randomUUID().toString()),
                    text = tObj.optString("text", ""),
                    startTimeMs = tObj.optLong("startTimeMs", 0L),
                    endTimeMs = tObj.optLong("endTimeMs", 3000L),
                    xPercent = tObj.optDouble("xPercent", 0.5).toFloat(),
                    yPercent = tObj.optDouble("yPercent", 0.85).toFloat(),
                    fontSizeSp = tObj.optDouble("fontSizeSp", 18.0).toFloat(),
                    textColorHex = tObj.optString("textColorHex", "#FFFFFF"),
                    backgroundColorHex = tObj.optString("backgroundColorHex", "#B0000000")
                )
            }
            var ts: TutorialStep? = null
            if (obj.has("tutorialStep")) {
                val sObj = obj.getJSONObject("tutorialStep")
                ts = TutorialStep(
                    id = sObj.optString("id", UUID.randomUUID().toString()),
                    stepNumber = sObj.optInt("stepNumber", 1),
                    title = sObj.optString("title", ""),
                    description = sObj.optString("description", ""),
                    startTimeMs = sObj.optLong("startTimeMs", 0L),
                    endTimeMs = sObj.optLong("endTimeMs", 3000L),
                    focalXPercent = if (sObj.has("focalXPercent")) sObj.getDouble("focalXPercent").toFloat() else null,
                    focalYPercent = if (sObj.has("focalYPercent")) sObj.getDouble("focalYPercent").toFloat() else null,
                    confidence = sObj.optDouble("confidence", 0.85).toFloat()
                )
            }
            val ap = if (obj.has("audioPreset")) {
                try { AudioEnhancePreset.valueOf(obj.getString("audioPreset")) } catch (_: Exception) { null }
            } else null

            return AiSuggestion(
                id = obj.optString("id", UUID.randomUUID().toString()),
                type = type,
                title = obj.optString("title", ""),
                description = obj.optString("description", ""),
                startTimeMs = obj.optLong("startTimeMs", 0L),
                endTimeMs = obj.optLong("endTimeMs", 0L),
                confidence = obj.optDouble("confidence", 0.8).toFloat(),
                confidenceLabel = obj.optString("confidenceLabel", "Detected"),
                isApplied = obj.optBoolean("isApplied", false),
                isRejected = obj.optBoolean("isRejected", false),
                zoomKeyframe = zk,
                textOverlay = to,
                tutorialStep = ts,
                audioPreset = ap
            )
        }
    }
}

/**
 * Overall progress emitted during multi-stage AI analysis.
 */
data class AiAnalysisProgress(
    val state: AiAnalysisState = AiAnalysisState.IDLE,
    val progressPercent: Float = 0f,
    val currentStepMessage: String = "",
    val errorMessage: String? = null
)

/**
 * Complete AI analysis report for a project recording.
 */
data class AiAnalysisResult(
    val projectId: String,
    val analyzedAt: Long = System.currentTimeMillis(),
    val sourceDurationMs: Long,
    val suggestedTitle: String,
    val suggestedDescription: String,
    val suggestions: List<AiSuggestion> = emptyList(),
    val idleCutsCount: Int = 0,
    val smartZoomsCount: Int = 0,
    val highlightsCount: Int = 0,
    val captionsCount: Int = 0,
    val tutorialStepsCount: Int = 0,
    val speechDetected: Boolean = false
) {
    val pendingSuggestions: List<AiSuggestion>
        get() = suggestions.filter { !it.isApplied && !it.isRejected }

    val appliedSuggestions: List<AiSuggestion>
        get() = suggestions.filter { it.isApplied }

    fun toJson(): String {
        val root = org.json.JSONObject()
        root.put("projectId", projectId)
        root.put("analyzedAt", analyzedAt)
        root.put("sourceDurationMs", sourceDurationMs)
        root.put("suggestedTitle", suggestedTitle)
        root.put("suggestedDescription", suggestedDescription)
        root.put("idleCutsCount", idleCutsCount)
        root.put("smartZoomsCount", smartZoomsCount)
        root.put("highlightsCount", highlightsCount)
        root.put("captionsCount", captionsCount)
        root.put("tutorialStepsCount", tutorialStepsCount)
        root.put("speechDetected", speechDetected)

        val arr = org.json.JSONArray()
        suggestions.forEach { arr.put(it.toJson()) }
        root.put("suggestions", arr)

        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): AiAnalysisResult? {
            return try {
                val root = org.json.JSONObject(jsonStr)
                val projId = root.optString("projectId", "")
                val analyzedAt = root.optLong("analyzedAt", System.currentTimeMillis())
                val dur = root.optLong("sourceDurationMs", 0L)
                val title = root.optString("suggestedTitle", "")
                val desc = root.optString("suggestedDescription", "")
                val idleCuts = root.optInt("idleCutsCount", 0)
                val zooms = root.optInt("smartZoomsCount", 0)
                val highlights = root.optInt("highlightsCount", 0)
                val captions = root.optInt("captionsCount", 0)
                val tutorials = root.optInt("tutorialStepsCount", 0)
                val speech = root.optBoolean("speechDetected", false)

                val suggestionsList = mutableListOf<AiSuggestion>()
                val arr = root.optJSONArray("suggestions")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val itemObj = arr.getJSONObject(i)
                        suggestionsList.add(AiSuggestion.fromJson(itemObj))
                    }
                }

                AiAnalysisResult(
                    projectId = projId,
                    analyzedAt = analyzedAt,
                    sourceDurationMs = dur,
                    suggestedTitle = title,
                    suggestedDescription = desc,
                    suggestions = suggestionsList,
                    idleCutsCount = idleCuts,
                    smartZoomsCount = zooms,
                    highlightsCount = highlights,
                    captionsCount = captions,
                    tutorialStepsCount = tutorials,
                    speechDetected = speech
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
