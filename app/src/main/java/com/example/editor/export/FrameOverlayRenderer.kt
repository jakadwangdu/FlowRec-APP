package com.example.editor.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import com.example.editor.model.EditorProjectState
import com.example.editor.model.ImageOverlay
import com.example.editor.model.TextOverlay
import com.example.editor.timeline.TimelineManager
import com.example.recorder.touch.FlowTouchEvent
import java.io.File
import java.io.InputStream
import kotlin.math.sin

/**
 * Frame compositing engine for video export.
 * Renders non-destructive zoom transitions, text overlays, image/logo watermarks,
 * recorded touch ripples, and FaceCam overlays directly onto output video frames.
 */
class FrameOverlayRenderer(
    private val context: Context,
    private val editorState: EditorProjectState,
    private val touchEvents: List<FlowTouchEvent>,
    private val config: ExportConfig,
    private val outputWidth: Int,
    private val outputHeight: Int
) {
    companion object {
        private const val TAG = "FrameOverlayRenderer"
    }

    // Cached image overlay bitmaps to prevent garbage collection churn during rendering
    private val imageCache = mutableMapOf<String, Bitmap?>()

    // Reusable paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val touchRipplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val touchGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val touchDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val facecamBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val facecamBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val facecamTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val textBounds = Rect()
    private val rectF = RectF()

    init {
        // Pre-cache image overlays
        for (img in editorState.imageOverlays) {
            if (!imageCache.containsKey(img.imageUriOrPath)) {
                imageCache[img.imageUriOrPath] = loadBitmap(img.imageUriOrPath)
            }
        }
    }

    /**
     * Renders all active overlays for the given timeline position onto the provided Canvas.
     */
    fun renderOverlays(canvas: Canvas, timelinePositionMs: Long) {
        // 1. Text Overlays
        renderTextOverlays(canvas, timelinePositionMs)

        // 2. Image / Logo Overlays
        renderImageOverlays(canvas, timelinePositionMs)

        // 3. Touch Gestures & Ripples (.flowtouch)
        if (config.burnTouchEffects && editorState.touchConfig.enabled) {
            renderTouchEffects(canvas, timelinePositionMs)
        }

        // 4. FaceCam Overlay (.flowcam)
        if (config.burnFaceCam && editorState.faceCamTrack.enabled) {
            renderFaceCamOverlay(canvas, timelinePositionMs)
        }
    }

    /**
     * Calculates the zoom transform matrix for the current timeline position.
     */
    fun calculateZoomMatrix(timelinePositionMs: Long): Matrix {
        val matrix = Matrix()
        val (zoomScale, focalX, focalY) = TimelineManager.getActiveZoom(editorState, timelinePositionMs)

        if (zoomScale > 1.01f) {
            val pivotX = focalX * outputWidth
            val pivotY = focalY * outputHeight
            matrix.postScale(zoomScale, zoomScale, pivotX, pivotY)
        }
        return matrix
    }

    private fun renderTextOverlays(canvas: Canvas, timelinePositionMs: Long) {
        val activeTexts = TimelineManager.getActiveTextOverlays(editorState, timelinePositionMs)
        for (textOverlay in activeTexts) {
            drawSingleTextOverlay(canvas, textOverlay)
        }
    }

    private fun drawSingleTextOverlay(canvas: Canvas, overlay: TextOverlay) {
        val cx = overlay.xPercent * outputWidth
        val cy = overlay.yPercent * outputHeight

        val baseFontSize = overlay.fontSizeSp * (outputWidth / 480f)
        textPaint.textSize = baseFontSize.coerceIn(16f, 120f)
        textPaint.color = parseColorSafely(overlay.textColorHex, Color.WHITE)
        textPaint.typeface = if (overlay.fontBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        textPaint.getTextBounds(overlay.text, 0, overlay.text.length, textBounds)
        val textWidth = textBounds.width().toFloat()
        val textHeight = textBounds.height().toFloat()

        canvas.save()
        if (overlay.rotationDeg != 0f) {
            canvas.rotate(overlay.rotationDeg, cx, cy)
        }

        // Background pill
        if (overlay.backgroundColorHex != null) {
            backgroundPaint.color = parseColorSafely(overlay.backgroundColorHex, Color.argb(180, 0, 0, 0))
            val padX = textHeight * 0.5f
            val padY = textHeight * 0.35f
            rectF.set(
                cx - (textWidth / 2f) - padX,
                cy - (textHeight / 2f) - padY,
                cx + (textWidth / 2f) + padX,
                cy + (textHeight / 2f) + padY
            )
            val cornerRadius = textHeight * 0.4f
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
        }

        // Draw text centered vertically
        val textY = cy + (textHeight / 2f) - textBounds.bottom
        canvas.drawText(overlay.text, cx, textY, textPaint)
        canvas.restore()
    }

    private fun renderImageOverlays(canvas: Canvas, timelinePositionMs: Long) {
        val activeImages = TimelineManager.getActiveImageOverlays(editorState, timelinePositionMs)
        for (img in activeImages) {
            val bmp = imageCache[img.imageUriOrPath] ?: continue
            drawSingleImageOverlay(canvas, img, bmp)
        }
    }

    private fun drawSingleImageOverlay(canvas: Canvas, overlay: ImageOverlay, bitmap: Bitmap) {
        val targetWidth = (overlay.sizePercent * outputWidth).coerceAtLeast(20f)
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetHeight = targetWidth / aspectRatio

        val cx = overlay.xPercent * outputWidth
        val cy = overlay.yPercent * outputHeight

        val left = cx - (targetWidth / 2f)
        val top = cy - (targetHeight / 2f)

        canvas.save()
        if (overlay.rotationDeg != 0f) {
            canvas.rotate(overlay.rotationDeg, cx, cy)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (overlay.opacity.coerceIn(0f, 1f) * 255).toInt()
        }

        rectF.set(left, top, left + targetWidth, top + targetHeight)
        canvas.drawBitmap(bitmap, null, rectF, paint)
        canvas.restore()
    }

    private fun renderTouchEffects(canvas: Canvas, timelinePositionMs: Long) {
        val durationMs = editorState.touchConfig.durationMs.coerceAtLeast(200)
        val baseColor = parseColorSafely(editorState.touchConfig.colorHex, Color.parseColor("#FF3B30"))

        val activeTouches = touchEvents.filter {
            val delta = timelinePositionMs - it.timestampMs
            delta in 0..durationMs
        }

        val scaleFactor = outputWidth / 1080f
        val maxRadius = editorState.touchConfig.sizeDp * 2.5f * scaleFactor

        for (event in activeTouches) {
            val delta = (timelinePositionMs - event.timestampMs).coerceAtLeast(0L)
            val progress = delta.toFloat() / durationMs.toFloat() // 0.0 to 1.0

            val px = event.normalizedX * outputWidth
            val py = event.normalizedY * outputHeight

            // 1. Expanding Ripple
            if (editorState.touchConfig.rippleEnabled) {
                val rippleRadius = (maxRadius * (0.3f + 0.7f * progress)).coerceAtLeast(4f)
                val alpha = ((1f - progress) * editorState.touchConfig.opacity * 200).toInt().coerceIn(0, 255)
                touchRipplePaint.color = baseColor
                touchRipplePaint.alpha = alpha
                touchRipplePaint.strokeWidth = (4f * (1f - progress * 0.5f) * scaleFactor).coerceAtLeast(1.5f)
                canvas.drawCircle(px, py, rippleRadius, touchRipplePaint)
            }

            // 2. Center Touch Dot / Highlight
            if (editorState.touchConfig.highlightEnabled && progress < 0.6f) {
                val dotAlpha = ((1f - progress / 0.6f) * editorState.touchConfig.opacity * 230).toInt().coerceIn(0, 255)
                touchDotPaint.color = baseColor
                touchDotPaint.alpha = dotAlpha
                val dotRadius = (16f * scaleFactor).coerceAtLeast(6f)
                canvas.drawCircle(px, py, dotRadius, touchDotPaint)

                // Outer soft halo
                touchGlowPaint.color = baseColor
                touchGlowPaint.alpha = (dotAlpha * 0.35f).toInt()
                canvas.drawCircle(px, py, dotRadius * 1.8f, touchGlowPaint)
            }
        }
    }

    private fun renderFaceCamOverlay(canvas: Canvas, timelinePositionMs: Long) {
        val fc = editorState.faceCamTrack
        if (timelinePositionMs < fc.startTimeMs || timelinePositionMs > fc.endTimeMs) return

        val size = (fc.sizePercent * outputWidth).coerceAtLeast(60f)
        val cx = fc.xPercent * outputWidth
        val cy = fc.yPercent * outputHeight

        val left = cx - size / 2f
        val top = cy - size / 2f
        rectF.set(left, top, left + size, top + size)

        val borderColor = parseColorSafely(fc.borderColorHex, Color.WHITE)
        val borderWidth = (fc.borderWidthDp * (outputWidth / 480f)).coerceAtLeast(2f)

        facecamBorderPaint.color = borderColor
        facecamBorderPaint.strokeWidth = borderWidth
        facecamBgPaint.color = Color.parseColor("#1E2028")

        canvas.save()
        when (fc.shape.uppercase()) {
            "CIRCLE" -> {
                val radius = size / 2f
                canvas.drawCircle(cx, cy, radius, facecamBgPaint)
                canvas.drawCircle(cx, cy, radius, facecamBorderPaint)

                facecamTextPaint.textSize = (size * 0.22f).coerceAtLeast(12f)
                canvas.drawText("CAM", cx, cy + facecamTextPaint.textSize * 0.35f, facecamTextPaint)
            }
            "ROUNDED_RECT" -> {
                val corner = size * 0.25f
                canvas.drawRoundRect(rectF, corner, corner, facecamBgPaint)
                canvas.drawRoundRect(rectF, corner, corner, facecamBorderPaint)

                facecamTextPaint.textSize = (size * 0.22f).coerceAtLeast(12f)
                canvas.drawText("CAM", cx, cy + facecamTextPaint.textSize * 0.35f, facecamTextPaint)
            }
            else -> { // SQUARE / RECT
                canvas.drawRect(rectF, facecamBgPaint)
                canvas.drawRect(rectF, facecamBorderPaint)

                facecamTextPaint.textSize = (size * 0.22f).coerceAtLeast(12f)
                canvas.drawText("CAM", cx, cy + facecamTextPaint.textSize * 0.35f, facecamTextPaint)
            }
        }
        canvas.restore()
    }

    private fun loadBitmap(uriOrPath: String): Bitmap? {
        return try {
            val isFile = uriOrPath.startsWith("/") || uriOrPath.startsWith("file:")
            val stream: InputStream? = if (isFile) {
                File(uriOrPath.removePrefix("file://")).inputStream()
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriOrPath))
            }
            stream?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load image overlay: $uriOrPath", e)
            null
        }
    }

    private fun parseColorSafely(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
    }

    fun release() {
        for (bmp in imageCache.values) {
            bmp?.recycle()
        }
        imageCache.clear()
    }
}
