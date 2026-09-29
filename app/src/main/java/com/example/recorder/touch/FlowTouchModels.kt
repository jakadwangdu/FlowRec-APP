package com.example.recorder.touch

/**
 * Represents a single touch/pointer event recorded during a screen recording session.
 */
data class FlowTouchEvent(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val action: String, // "DOWN", "MOVE", "UP", "CANCEL"
    val pointerId: Int = 0,
    val normalizedX: Float = 0f, // 0.0 to 1.0 relative to recording width
    val normalizedY: Float = 0f, // 0.0 to 1.0 relative to recording height
    val pressure: Float = 1.0f
) {
    fun toJson(): String {
        return "{\"t\":$timestampMs,\"x\":$x,\"y\":$y,\"a\":\"$action\",\"p\":$pointerId,\"nx\":$normalizedX,\"ny\":$normalizedY,\"pr\":$pressure}"
    }

    companion object {
        fun fromJson(json: String): FlowTouchEvent? {
            return try {
                val t = json.substringAfter("\"t\":").substringBefore(",").toLong()
                val x = json.substringAfter("\"x\":").substringBefore(",").toFloat()
                val y = json.substringAfter("\"y\":").substringBefore(",").toFloat()
                val a = json.substringAfter("\"a\":\"").substringBefore("\"")
                val p = json.substringAfter("\"p\":").substringBefore(",").toInt()
                val nx = json.substringAfter("\"nx\":").substringBefore(",").toFloat()
                val ny = json.substringAfter("\"ny\":").substringBefore(",").toFloat()
                val pr = json.substringAfter("\"pr\":").substringBefore("}").toFloat()
                FlowTouchEvent(t, x, y, a, p, nx, ny, pr)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Configuration options for touch visual feedback during recording and in the editor.
 */
data class TouchEffectConfig(
    val enabled: Boolean = true,
    val rippleEnabled: Boolean = true,
    val highlightEnabled: Boolean = true,
    val movementTrackingEnabled: Boolean = false,
    val sizeDp: Int = 36, // Small: 24, Medium: 36, Large: 48
    val durationMs: Int = 500, // Short: 250ms, Normal: 500ms, Long: 800ms
    val opacity: Float = 0.8f, // 0.2f to 1.0f
    val colorHex: String = "#FF3B30"
)

/**
 * Top-level metadata written to the companion `<recording>.flowtouch` file.
 */
data class FlowTouchMetadata(
    val version: Int = 1,
    val recordingId: String,
    val videoWidth: Int,
    val videoHeight: Int,
    val startTimeEpochMs: Long,
    val durationMs: Long,
    val totalEvents: Int,
    val events: List<FlowTouchEvent> = emptyList()
)
