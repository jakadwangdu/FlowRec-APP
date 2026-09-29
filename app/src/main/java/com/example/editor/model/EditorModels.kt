package com.example.editor.model

import java.util.UUID

/**
 * A single non-destructive clip segment on the video timeline.
 * Points to a range within the untouched original source MP4 file.
 */
data class TimelineSegment(
    val id: String = UUID.randomUUID().toString(),
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val speed: Float = 1.0f,
    val isDeleted: Boolean = false
) {
    val durationMs: Long
        get() = (((sourceEndMs - sourceStartMs).coerceAtLeast(0L)).toFloat() / speed.coerceAtLeast(0.1f)).toLong()

    fun toJson(): String {
        return "{\"id\":\"$id\",\"s\":$sourceStartMs,\"e\":$sourceEndMs,\"sp\":$speed,\"del\":$isDeleted}"
    }

    companion object {
        fun fromJson(json: String): TimelineSegment? {
            return try {
                val id = json.substringAfter("\"id\":\"", "").substringBefore("\"")
                val s = json.substringAfter("\"s\":", "0").substringBefore(",").toLong()
                val e = json.substringAfter("\"e\":", "0").substringBefore(",").toLong()
                val sp = json.substringAfter("\"sp\":", "1.0").substringBefore(",").toFloat()
                val del = json.substringAfter("\"del\":", "false").substringBefore("}").toBoolean()
                TimelineSegment(id, s, e, sp, del)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Non-destructive text overlay layer.
 */
data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val xPercent: Float = 0.5f, // 0.0 to 1.0 relative to canvas width
    val yPercent: Float = 0.5f, // 0.0 to 1.0 relative to canvas height
    val fontSizeSp: Float = 20f,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String? = "#80000000",
    val rotationDeg: Float = 0f,
    val fontBold: Boolean = true
) {
    fun toJson(): String {
        val bgStr = if (backgroundColorHex != null) "\"$backgroundColorHex\"" else "null"
        val escText = text.replace("\"", "\\\"").replace("\n", "\\n")
        return "{\"id\":\"$id\",\"txt\":\"$escText\",\"st\":$startTimeMs,\"et\":$endTimeMs,\"x\":$xPercent,\"y\":$yPercent,\"sz\":$fontSizeSp,\"tc\":\"$textColorHex\",\"bg\":$bgStr,\"rot\":$rotationDeg,\"b\":$fontBold}"
    }

    companion object {
        fun fromJson(json: String): TextOverlay? {
            return try {
                val id = json.substringAfter("\"id\":\"", "").substringBefore("\"")
                val txt = json.substringAfter("\"txt\":\"", "").substringBefore("\",\"st\"").replace("\\\"", "\"").replace("\\n", "\n")
                val st = json.substringAfter("\"st\":", "0").substringBefore(",").toLong()
                val et = json.substringAfter("\"et\":", "0").substringBefore(",").toLong()
                val x = json.substringAfter("\"x\":", "0.5").substringBefore(",").toFloat()
                val y = json.substringAfter("\"y\":", "0.5").substringBefore(",").toFloat()
                val sz = json.substringAfter("\"sz\":", "20").substringBefore(",").toFloat()
                val tc = json.substringAfter("\"tc\":\"", "#FFFFFF").substringBefore("\"")
                val bgRaw = json.substringAfter("\"bg\":", "null").substringBefore(",")
                val bg = if (bgRaw == "null") null else bgRaw.replace("\"", "")
                val rot = json.substringAfter("\"rot\":", "0").substringBefore(",").toFloat()
                val b = json.substringAfter("\"b\":", "true").substringBefore("}").toBoolean()
                TextOverlay(id, txt, st, et, x, y, sz, tc, bg, rot, b)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Non-destructive image or logo overlay layer (e.g. branding watermark).
 */
data class ImageOverlay(
    val id: String = UUID.randomUUID().toString(),
    val imageUriOrPath: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val xPercent: Float = 0.85f,
    val yPercent: Float = 0.15f,
    val sizePercent: Float = 0.20f, // 10% to 50% of canvas width
    val opacity: Float = 0.9f,
    val rotationDeg: Float = 0f
) {
    fun toJson(): String {
        val escPath = imageUriOrPath.replace("\"", "\\\"")
        return "{\"id\":\"$id\",\"path\":\"$escPath\",\"st\":$startTimeMs,\"et\":$endTimeMs,\"x\":$xPercent,\"y\":$yPercent,\"sz\":$sizePercent,\"op\":$opacity,\"rot\":$rotationDeg}"
    }

    companion object {
        fun fromJson(json: String): ImageOverlay? {
            return try {
                val id = json.substringAfter("\"id\":\"", "").substringBefore("\"")
                val path = json.substringAfter("\"path\":\"", "").substringBefore("\",\"st\"").replace("\\\"", "\"")
                val st = json.substringAfter("\"st\":", "0").substringBefore(",").toLong()
                val et = json.substringAfter("\"et\":", "0").substringBefore(",").toLong()
                val x = json.substringAfter("\"x\":", "0.85").substringBefore(",").toFloat()
                val y = json.substringAfter("\"y\":", "0.15").substringBefore(",").toFloat()
                val sz = json.substringAfter("\"sz\":", "0.2").substringBefore(",").toFloat()
                val op = json.substringAfter("\"op\":", "0.9").substringBefore(",").toFloat()
                val rot = json.substringAfter("\"rot\":", "0").substringBefore("}").toFloat()
                ImageOverlay(id, path, st, et, x, y, sz, op, rot)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Non-destructive focal zoom keyframe.
 * Smoothly scales and centers on (focalXPercent, focalYPercent) at timeMs.
 */
data class ZoomKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val focalXPercent: Float = 0.5f,
    val focalYPercent: Float = 0.5f,
    val scale: Float = 1.6f, // 1.0f (no zoom) to 3.0f (3x zoom)
    val durationMs: Long = 400L,
    val easing: String = "Smooth"
) {
    fun toJson(): String {
        return "{\"id\":\"$id\",\"t\":$timeMs,\"fx\":$focalXPercent,\"fy\":$focalYPercent,\"sc\":$scale,\"dur\":$durationMs,\"ea\":\"$easing\"}"
    }

    companion object {
        fun fromJson(json: String): ZoomKeyframe? {
            return try {
                val id = json.substringAfter("\"id\":\"", "").substringBefore("\"")
                val t = json.substringAfter("\"t\":", "0").substringBefore(",").toLong()
                val fx = json.substringAfter("\"fx\":", "0.5").substringBefore(",").toFloat()
                val fy = json.substringAfter("\"fy\":", "0.5").substringBefore(",").toFloat()
                val sc = json.substringAfter("\"sc\":", "1.6").substringBefore(",").toFloat()
                val dur = json.substringAfter("\"dur\":", "400").substringBefore(",").toLong()
                val ea = json.substringAfter("\"ea\":\"", "Smooth").substringBefore("\"")
                ZoomKeyframe(id, t, fx, fy, sc, dur, ea)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * FaceCam editor track configuration.
 */
data class FaceCamEditorTrack(
    val enabled: Boolean = false,
    val shape: String = "CIRCLE", // CIRCLE, ROUNDED_RECT, SQUARE
    val sizePercent: Float = 0.25f, // 0.15f to 0.45f
    val xPercent: Float = 0.82f,
    val yPercent: Float = 0.18f,
    val borderWidthDp: Float = 2.5f,
    val borderColorHex: String = "#FFFFFF",
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = Long.MAX_VALUE
) {
    fun toJson(): String {
        return "{\"en\":$enabled,\"sh\":\"$shape\",\"sz\":$sizePercent,\"x\":$xPercent,\"y\":$yPercent,\"bw\":$borderWidthDp,\"bc\":\"$borderColorHex\",\"st\":$startTimeMs,\"et\":$endTimeMs}"
    }

    companion object {
        fun fromJson(json: String): FaceCamEditorTrack {
            return try {
                val en = json.substringAfter("\"en\":", "false").substringBefore(",").toBoolean()
                val sh = json.substringAfter("\"sh\":\"", "CIRCLE").substringBefore("\"")
                val sz = json.substringAfter("\"sz\":", "0.25").substringBefore(",").toFloat()
                val x = json.substringAfter("\"x\":", "0.82").substringBefore(",").toFloat()
                val y = json.substringAfter("\"y\":", "0.18").substringBefore(",").toFloat()
                val bw = json.substringAfter("\"bw\":", "2.5").substringBefore(",").toFloat()
                val bc = json.substringAfter("\"bc\":\"", "#FFFFFF").substringBefore("\"")
                val st = json.substringAfter("\"st\":", "0").substringBefore(",").toLong()
                val et = json.substringAfter("\"et\":", "${Long.MAX_VALUE}").substringBefore("}").toLong()
                FaceCamEditorTrack(en, sh, sz, x, y, bw, bc, st, et)
            } catch (_: Exception) {
                FaceCamEditorTrack()
            }
        }
    }
}

/**
 * Editor audio tracks data model.
 */
data class AudioTrackConfig(
    val originalAudioVolume: Float = 1.0f, // 0.0f to 2.0f
    val originalAudioMuted: Boolean = false,
    val bgmTrackUri: String? = null,
    val bgmVolume: Float = 0.5f,
    val voiceoverTrackUri: String? = null,
    val voiceoverVolume: Float = 1.0f
) {
    fun toJson(): String {
        val bgmStr = if (bgmTrackUri != null) "\"${bgmTrackUri.replace("\"", "\\\"")}\"" else "null"
        val voStr = if (voiceoverTrackUri != null) "\"${voiceoverTrackUri.replace("\"", "\\\"")}\"" else "null"
        return "{\"ov\":$originalAudioVolume,\"om\":$originalAudioMuted,\"bgm\":$bgmStr,\"bv\":$bgmVolume,\"vo\":$voStr,\"vv\":$voiceoverVolume}"
    }

    companion object {
        fun fromJson(json: String): AudioTrackConfig {
            return try {
                val ov = json.substringAfter("\"ov\":", "1.0").substringBefore(",").toFloat()
                val om = json.substringAfter("\"om\":", "false").substringBefore(",").toBoolean()
                val bgmRaw = json.substringAfter("\"bgm\":", "null").substringBefore(",")
                val bgm = if (bgmRaw == "null") null else bgmRaw.replace("\"", "").replace("\\\"", "\"")
                val bv = json.substringAfter("\"bv\":", "0.5").substringBefore(",").toFloat()
                val voRaw = json.substringAfter("\"vo\":", "null").substringBefore(",")
                val vo = if (voRaw == "null") null else voRaw.replace("\"", "").replace("\\\"", "\"")
                val vv = json.substringAfter("\"vv\":", "1.0").substringBefore("}").toFloat()
                AudioTrackConfig(ov, om, bgm, bv, vo, vv)
            } catch (_: Exception) {
                AudioTrackConfig()
            }
        }
    }
}

/**
 * Editor touch overlay playback configuration.
 */
data class TouchOverlayConfig(
    val enabled: Boolean = true,
    val rippleEnabled: Boolean = true,
    val highlightEnabled: Boolean = true,
    val movementTrackingEnabled: Boolean = true,
    val sizeDp: Int = 36,
    val durationMs: Int = 500,
    val opacity: Float = 0.85f,
    val colorHex: String = "#FF3B30"
) {
    fun toJson(): String {
        return "{\"en\":$enabled,\"rp\":$rippleEnabled,\"hl\":$highlightEnabled,\"mv\":$movementTrackingEnabled,\"sz\":$sizeDp,\"dur\":$durationMs,\"op\":$opacity,\"col\":\"$colorHex\"}"
    }

    companion object {
        fun fromJson(json: String): TouchOverlayConfig {
            return try {
                val en = json.substringAfter("\"en\":", "true").substringBefore(",").toBoolean()
                val rp = json.substringAfter("\"rp\":", "true").substringBefore(",").toBoolean()
                val hl = json.substringAfter("\"hl\":", "true").substringBefore(",").toBoolean()
                val mv = json.substringAfter("\"mv\":", "true").substringBefore(",").toBoolean()
                val sz = json.substringAfter("\"sz\":", "36").substringBefore(",").toInt()
                val dur = json.substringAfter("\"dur\":", "500").substringBefore(",").toInt()
                val op = json.substringAfter("\"op\":", "0.85").substringBefore(",").toFloat()
                val col = json.substringAfter("\"col\":\"", "#FF3B30").substringBefore("\"")
                TouchOverlayConfig(en, rp, hl, mv, sz, dur, op, col)
            } catch (_: Exception) {
                TouchOverlayConfig()
            }
        }
    }
}

/**
 * Top-level non-destructive Project Editor state.
 * Contains all cuts, trims, overlays, keyframes, and metadata for a video project.
 */
data class EditorProjectState(
    val version: Int = 1,
    val projectId: String,
    val totalSourceDurationMs: Long,
    val segments: List<TimelineSegment> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val imageOverlays: List<ImageOverlay> = emptyList(),
    val zoomKeyframes: List<ZoomKeyframe> = emptyList(),
    val faceCamTrack: FaceCamEditorTrack = FaceCamEditorTrack(),
    val audioConfig: AudioTrackConfig = AudioTrackConfig(),
    val touchConfig: TouchOverlayConfig = TouchOverlayConfig()
) {
    /**
     * Effective timeline duration (excluding deleted segments, scaled by speed).
     */
    val effectiveDurationMs: Long
        get() {
            val active = segments.filter { !it.isDeleted }
            return if (active.isEmpty()) totalSourceDurationMs else active.sumOf { it.durationMs }
        }

    fun toJson(): String {
        val segsJson = segments.joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }
        val textsJson = textOverlays.joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }
        val imgsJson = imageOverlays.joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }
        val zoomsJson = zoomKeyframes.joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }

        return "{" +
                "\"v\":$version," +
                "\"pid\":\"$projectId\"," +
                "\"srcDur\":$totalSourceDurationMs," +
                "\"segments\":$segsJson," +
                "\"texts\":$textsJson," +
                "\"images\":$imgsJson," +
                "\"zooms\":$zoomsJson," +
                "\"facecam\":${faceCamTrack.toJson()}," +
                "\"audio\":${audioConfig.toJson()}," +
                "\"touch\":${touchConfig.toJson()}" +
                "}"
    }

    companion object {
        fun createDefault(projectId: String, sourceDurationMs: Long): EditorProjectState {
            val initialSegment = TimelineSegment(
                sourceStartMs = 0L,
                sourceEndMs = sourceDurationMs.coerceAtLeast(1000L)
            )
            return EditorProjectState(
                version = 1,
                projectId = projectId,
                totalSourceDurationMs = sourceDurationMs,
                segments = listOf(initialSegment)
            )
        }

        fun fromJson(json: String): EditorProjectState? {
            return try {
                val pid = json.substringAfter("\"pid\":\"", "").substringBefore("\"")
                val srcDur = json.substringAfter("\"srcDur\":", "0").substringBefore(",").toLong()

                // Parse segments
                val segs = mutableListOf<TimelineSegment>()
                if (json.contains("\"segments\":[")) {
                    val raw = json.substringAfter("\"segments\":[").substringBefore("],\"texts\"")
                    if (raw.isNotBlank()) {
                        raw.split("},{").forEach { chunk ->
                            val clean = if (!chunk.startsWith("{")) "{$chunk" else chunk
                            val fixed = if (!clean.endsWith("}")) "$clean}" else clean
                            TimelineSegment.fromJson(fixed)?.let { segs.add(it) }
                        }
                    }
                }

                // Parse text overlays
                val texts = mutableListOf<TextOverlay>()
                if (json.contains("\"texts\":[")) {
                    val raw = json.substringAfter("\"texts\":[").substringBefore("],\"images\"")
                    if (raw.isNotBlank()) {
                        raw.split("},{").forEach { chunk ->
                            val clean = if (!chunk.startsWith("{")) "{$chunk" else chunk
                            val fixed = if (!clean.endsWith("}")) "$clean}" else clean
                            TextOverlay.fromJson(fixed)?.let { texts.add(it) }
                        }
                    }
                }

                // Parse image overlays
                val imgs = mutableListOf<ImageOverlay>()
                if (json.contains("\"images\":[")) {
                    val raw = json.substringAfter("\"images\":[").substringBefore("],\"zooms\"")
                    if (raw.isNotBlank()) {
                        raw.split("},{").forEach { chunk ->
                            val clean = if (!chunk.startsWith("{")) "{$chunk" else chunk
                            val fixed = if (!clean.endsWith("}")) "$clean}" else clean
                            ImageOverlay.fromJson(fixed)?.let { imgs.add(it) }
                        }
                    }
                }

                // Parse zoom keyframes
                val zooms = mutableListOf<ZoomKeyframe>()
                if (json.contains("\"zooms\":[")) {
                    val raw = json.substringAfter("\"zooms\":[").substringBefore("],\"facecam\"")
                    if (raw.isNotBlank()) {
                        raw.split("},{").forEach { chunk ->
                            val clean = if (!chunk.startsWith("{")) "{$chunk" else chunk
                            val fixed = if (!clean.endsWith("}")) "$clean}" else clean
                            ZoomKeyframe.fromJson(fixed)?.let { zooms.add(it) }
                        }
                    }
                }

                // Parse FaceCam track
                val facecam = if (json.contains("\"facecam\":{")) {
                    val raw = "{" + json.substringAfter("\"facecam\":{").substringBefore("},\"audio\"") + "}"
                    FaceCamEditorTrack.fromJson(raw)
                } else FaceCamEditorTrack()

                // Parse Audio track
                val audio = if (json.contains("\"audio\":{")) {
                    val raw = "{" + json.substringAfter("\"audio\":{").substringBefore("},\"touch\"") + "}"
                    AudioTrackConfig.fromJson(raw)
                } else AudioTrackConfig()

                // Parse Touch overlay
                val touch = if (json.contains("\"touch\":{")) {
                    val raw = "{" + json.substringAfter("\"touch\":{").substringBeforeLast("}") + "}"
                    TouchOverlayConfig.fromJson(raw)
                } else TouchOverlayConfig()

                EditorProjectState(
                    version = 1,
                    projectId = pid,
                    totalSourceDurationMs = srcDur,
                    segments = if (segs.isEmpty()) listOf(TimelineSegment(sourceStartMs = 0L, sourceEndMs = srcDur)) else segs,
                    textOverlays = texts,
                    imageOverlays = imgs,
                    zoomKeyframes = zooms,
                    faceCamTrack = facecam,
                    audioConfig = audio,
                    touchConfig = touch
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
