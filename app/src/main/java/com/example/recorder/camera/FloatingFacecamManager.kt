package com.example.recorder.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.io.File
import kotlin.math.abs

/**
 * Custom LifecycleOwner enabling CameraX binding in WindowManager / Service contexts.
 */
class FacecamLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    fun start() {
        Handler(Looper.getMainLooper()).post {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }
    }

    fun stop() {
        Handler(Looper.getMainLooper()).post {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }
}

/**
 * Floating FaceCam manager that renders a front camera preview bubble over the screen
 * using WindowManager (TYPE_APPLICATION_OVERLAY) and CameraX.
 */
class FloatingFacecamManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingFacecamManager"
        private const val PREFS_NAME = "flowrec_facecam_prefs"
        private const val KEY_POS_X = "facecam_pos_x"
        private const val KEY_POS_Y = "facecam_pos_y"
        private const val KEY_SHAPE = "facecam_shape"
        private const val KEY_SIZE = "facecam_size"
        private const val KEY_LENS = "facecam_lens"

        @SuppressLint("StaticFieldLeak")
        private var instance: FloatingFacecamManager? = null

        fun getInstance(context: Context): FloatingFacecamManager {
            return instance ?: synchronized(this) {
                instance ?: FloatingFacecamManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootView: FrameLayout? = null
    private var previewContainer: FrameLayout? = null
    private var previewView: PreviewView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var lifecycleOwner: FacecamLifecycleOwner? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var currentLens: Int = CameraSelector.LENS_FACING_FRONT
    private var currentShape: String = prefs.getString(KEY_SHAPE, "CIRCLE") ?: "CIRCLE"
    private var currentSize: String = prefs.getString(KEY_SIZE, "MEDIUM") ?: "MEDIUM"

    var onErrorListener: ((String) -> Unit)? = null
    var onTouchEventListener: ((MotionEvent) -> Unit)? = null

    val isShowing: Boolean get() = rootView != null

    /**
     * Show the floating FaceCam bubble.
     */
    fun show(
        shape: String = currentShape,
        size: String = currentSize,
        useFrontCamera: Boolean = true
    ) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show FaceCam: SYSTEM_ALERT_WINDOW permission not granted")
            onErrorListener?.invoke("Overlay permission required for FaceCam")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot show FaceCam: CAMERA permission not granted")
            onErrorListener?.invoke("Camera permission required for FaceCam")
            return
        }

        currentShape = shape
        currentSize = size
        currentLens = if (useFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK

        mainHandler.post {
            if (rootView != null) {
                updateConfiguration(shape, size)
                return@post
            }
            createAndAttachFacecam()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAndAttachFacecam() {
        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val metrics = getScreenMetrics()
            val sizePx = getDimensionPxForSize(currentSize)

            val savedX = prefs.getInt(KEY_POS_X, metrics.widthPixels - sizePx - 40)
            val savedY = prefs.getInt(KEY_POS_Y, 140)

            windowParams = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = savedX.coerceIn(20, (metrics.widthPixels - sizePx - 20).coerceAtLeast(20))
                y = savedY.coerceIn(40, (metrics.heightPixels - sizePx - 80).coerceAtLeast(40))
            }

            rootView = FrameLayout(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }

            previewContainer = FrameLayout(context).apply {
                clipToOutline = true
                applyShapeClipping(this, currentShape, sizePx)
            }

            previewView = PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            previewContainer?.addView(
                previewView,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )

            // Switch Camera quick button overlay
            val switchCamButton = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_camera)
                setColorFilter(Color.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#80000000"))
                }
                setPadding(10, 10, 10, 10)
                setOnClickListener {
                    toggleCameraLens()
                }
            }
            val btnParams = FrameLayout.LayoutParams(64, 64).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, 8, 8)
            }
            previewContainer?.addView(switchCamButton, btnParams)

            // Outer decorative ring
            val borderView = View(context).apply {
                background = GradientDrawable().apply {
                    val corner = getCornerRadiusForShape(currentShape, sizePx)
                    cornerRadius = corner
                    setStroke(4, Color.WHITE)
                }
            }
            previewContainer?.addView(
                borderView,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )

            rootView?.addView(
                previewContainer,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )

            // Drag listener for moving FaceCam across screen
            setupDragListener(rootView!!, sizePx)

            windowManager.addView(rootView, windowParams)
            Log.d(TAG, "Facecam attached to WindowManager successfully")

            startCameraX()

        } catch (e: Exception) {
            Log.e(TAG, "Error attaching FaceCam view: ${e.message}", e)
            onErrorListener?.invoke("Failed to display FaceCam: ${e.message}")
            remove()
        }
    }

    private fun startCameraX() {
        lifecycleOwner?.stop()
        val lo = FacecamLifecycleOwner()
        lifecycleOwner = lo
        lo.start()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraPreview(cameraProvider!!, lo)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get ProcessCameraProvider: ${e.message}", e)
                onErrorListener?.invoke("Camera unavailable: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraPreview(provider: ProcessCameraProvider, lo: FacecamLifecycleOwner) {
        val pv = previewView ?: return
        try {
            provider.unbindAll()

            // Check if requested camera is available
            var selector = CameraSelector.Builder().requireLensFacing(currentLens).build()
            if (!provider.hasCamera(selector)) {
                Log.w(TAG, "Lens $currentLens not available, falling back")
                val fallbackLens = if (currentLens == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                val fallbackSelector = CameraSelector.Builder().requireLensFacing(fallbackLens).build()
                if (provider.hasCamera(fallbackSelector)) {
                    selector = fallbackSelector
                    currentLens = fallbackLens
                } else {
                    onErrorListener?.invoke("No compatible camera found on device")
                    return
                }
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pv.surfaceProvider)
            }

            provider.bindToLifecycle(lo, selector, preview)
            Log.d(TAG, "CameraX preview bound successfully with lens: $currentLens")

        } catch (e: Exception) {
            Log.e(TAG, "Error binding CameraX preview: ${e.message}", e)
            onErrorListener?.invoke("Camera preview error: ${e.message}")
        }
    }

    private fun toggleCameraLens() {
        currentLens = if (currentLens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        val provider = cameraProvider
        val lo = lifecycleOwner
        if (provider != null && lo != null) {
            bindCameraPreview(provider, lo)
        }
    }

    fun updateConfiguration(shape: String, size: String) {
        currentShape = shape
        currentSize = size
        prefs.edit().putString(KEY_SHAPE, shape).putString(KEY_SIZE, size).apply()

        mainHandler.post {
            val sizePx = getDimensionPxForSize(size)
            windowParams?.let { params ->
                params.width = sizePx
                params.height = sizePx
                try {
                    windowManager.updateViewLayout(rootView, params)
                } catch (_: Exception) {}
            }
            previewContainer?.let { container ->
                applyShapeClipping(container, shape, sizePx)
            }
        }
    }

    private fun applyShapeClipping(view: View, shape: String, sizePx: Int) {
        val corner = getCornerRadiusForShape(shape, sizePx)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, corner)
            }
        }
        view.clipToOutline = true
    }

    private fun getCornerRadiusForShape(shape: String, sizePx: Int): Float {
        return when (shape.uppercase()) {
            "CIRCLE" -> sizePx / 2f
            "RECT", "SQUARE" -> 8f * context.resources.displayMetrics.density
            else -> 24f * context.resources.displayMetrics.density // ROUNDED_RECT
        }
    }

    private fun getDimensionPxForSize(size: String): Int {
        val density = context.resources.displayMetrics.density
        return when (size.uppercase()) {
            "SMALL" -> (110 * density).toInt()
            "LARGE" -> (190 * density).toInt()
            else -> (150 * density).toInt() // MEDIUM
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener(view: View, sizePx: Int) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            onTouchEventListener?.invoke(event)
            val params = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val metrics = getScreenMetrics()
                        params.x = (initialX + dx).coerceIn(10, (metrics.widthPixels - sizePx - 10).coerceAtLeast(10))
                        params.y = (initialY + dy).coerceIn(30, (metrics.heightPixels - sizePx - 50).coerceAtLeast(30))
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        prefs.edit()
                            .putInt(KEY_POS_X, params.x)
                            .putInt(KEY_POS_Y, params.y)
                            .apply()
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun resetPosition() {
        val metrics = getScreenMetrics()
        val sizePx = getDimensionPxForSize(currentSize)
        val defaultX = metrics.widthPixels - sizePx - 40
        val defaultY = 140
        prefs.edit().putInt(KEY_POS_X, defaultX).putInt(KEY_POS_Y, defaultY).apply()

        mainHandler.post {
            windowParams?.let { params ->
                params.x = defaultX
                params.y = defaultY
                try {
                    windowManager.updateViewLayout(rootView, params)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Stop CameraX and remove FaceCam overlay from screen safely.
     */
    fun remove() {
        mainHandler.post {
            try {
                cameraProvider?.unbindAll()
                cameraProvider = null
                lifecycleOwner?.stop()
                lifecycleOwner = null

                rootView?.let { view ->
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing FaceCam view: ${e.message}")
            } finally {
                rootView = null
                previewContainer = null
                previewView = null
                windowParams = null
            }
        }
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
        }
        return metrics
    }
}
