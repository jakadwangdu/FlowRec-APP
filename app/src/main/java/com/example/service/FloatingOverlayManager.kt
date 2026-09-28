package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.ui.components.formatSeconds
import kotlin.math.abs

/**
 * System-Wide Floating Bubble & Overlay for FlowRec Screen Recorder.
 * Renders on top of the phone's Home Screen, Snapchat, WhatsApp, games, and any other app
 * using Android WindowManager (TYPE_APPLICATION_OVERLAY).
 */
class FloatingOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingOverlayManager"

        @SuppressLint("StaticFieldLeak")
        private var instance: FloatingOverlayManager? = null

        fun getInstance(context: Context): FloatingOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: FloatingOverlayManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: FrameLayout? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var collapsedCircle: FrameLayout? = null
    private var expandedPill: LinearLayout? = null
    private var recDot: View? = null
    private var timerTextCollapsed: TextView? = null
    private var timerTextExpanded: TextView? = null
    private var pauseIcon: ImageView? = null
    private var pauseText: TextView? = null

    private var isExpanded = false
    private var isPaused = false
    private var currentSeconds = 0

    private val handler = Handler(Looper.getMainLooper())

    fun show() {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        if (rootView != null) {
            return // Already showing
        }

        handler.post {
            createAndAttachView()
        }
    }

    fun updateDuration(seconds: Int) {
        currentSeconds = seconds
        handler.post {
            val formatted = formatSeconds(seconds)
            timerTextCollapsed?.text = formatted
            timerTextExpanded?.text = "REC $formatted"
        }
    }

    fun updatePausedState(paused: Boolean) {
        isPaused = paused
        handler.post {
            if (paused) {
                recDot?.clearAnimation()
                recDot?.background = createCircleDrawable(Color.parseColor("#FFB300"))
                pauseIcon?.setImageResource(android.R.drawable.ic_media_play)
                pauseText?.text = "Resume"
            } else {
                startPulsingAnimation()
                pauseIcon?.setImageResource(android.R.drawable.ic_media_pause)
                pauseText?.text = "Pause"
            }
        }
    }

    fun remove() {
        handler.post {
            try {
                rootView?.let { view ->
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view: ${e.message}")
            } finally {
                rootView = null
                collapsedCircle = null
                expandedPill = null
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAndAttachView() {
        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val metrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                metrics.widthPixels = bounds.width()
                metrics.heightPixels = bounds.height()
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(metrics)
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = metrics.widthPixels - dpToPx(72)
                y = dpToPx(240)
            }
            windowParams = params

            val root = FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false
            }

            // 1. COLLAPSED FLOATING CIRCLE
            val circle = buildCollapsedCircle()
            collapsedCircle = circle

            // 2. EXPANDED ACTION PILL
            val pill = buildExpandedPill()
            expandedPill = pill
            pill.visibility = View.GONE

            root.addView(circle)
            root.addView(pill)

            // Setup Touch & Dragging on Collapsed Circle
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false

            circle.setOnTouchListener { _, event ->
                val currentParams = windowParams ?: return@setOnTouchListener false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 12 || abs(dy) > 12) {
                            isDragging = true
                        }
                        if (isDragging) {
                            currentParams.x = initialX + dx
                            currentParams.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(root, currentParams)
                            } catch (e: Exception) {
                                // Ignore concurrency
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Single Tap -> Expand Pill
                            expandControls()
                        } else {
                            // Snap to closest screen edge (Left or Right)
                            val mid = metrics.widthPixels / 2
                            currentParams.x = if (currentParams.x + dpToPx(28) < mid) {
                                dpToPx(12)
                            } else {
                                metrics.widthPixels - dpToPx(68)
                            }
                            try {
                                windowManager.updateViewLayout(root, currentParams)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

            rootView = root
            windowManager.addView(root, params)
            startPulsingAnimation()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create system floating overlay: ${e.message}", e)
        }
    }

    private fun buildCollapsedCircle(): FrameLayout {
        val size = dpToPx(56)
        return FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E6141418"))
                setStroke(dpToPx(1.5f), Color.parseColor("#FF3D3D48"))
            }
            elevation = dpToPx(12).toFloat()

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            // Pulsing dot
            val dot = View(context).apply {
                val dotSize = dpToPx(10)
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize)
                background = createCircleDrawable(Color.parseColor("#FF3B30"))
            }
            recDot = dot

            // Timer Text
            val timer = TextView(context).apply {
                text = formatSeconds(currentSeconds)
                setTextColor(Color.WHITE)
                textSize = 10f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dpToPx(2), 0, 0)
            }
            timerTextCollapsed = timer

            content.addView(dot)
            content.addView(timer)
            addView(content)
        }
    }

    private var preExpandX = 0

    private fun buildExpandedPill(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(28).toFloat()
                setColor(Color.parseColor("#F216171C"))
                setStroke(dpToPx(1.5f), Color.parseColor("#44FFFFFF"))
            }
            elevation = dpToPx(16).toFloat()

            // 1. Live Timer Badge (REC 00:00)
            val timerBadge = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(8), dpToPx(5), dpToPx(8), dpToPx(5))
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor("#26FFFFFF"))
                }
            }
            val timerDot = View(context).apply {
                val sz = dpToPx(8)
                layoutParams = LinearLayout.LayoutParams(sz, sz)
                background = createCircleDrawable(Color.parseColor("#FF3B30"))
            }
            val timerBadgeText = TextView(context).apply {
                text = "REC ${formatSeconds(currentSeconds)}"
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(4), 0, 0, 0)
            }
            timerTextExpanded = timerBadgeText
            timerBadge.addView(timerDot)
            timerBadge.addView(timerBadgeText)
            addView(timerBadge)

            // Space
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1) })

            // 2. Pause / Resume Button
            val pauseBtn = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
                background = createRoundedDrawable(Color.parseColor("#26262E"), dpToPx(16))
                isClickable = true
                setOnClickListener {
                    if (isPaused) {
                        ScreenRecorderService.resumeRecording(context)
                    } else {
                        ScreenRecorderService.pauseRecording(context)
                    }
                }
            }
            val pauseImg = ImageView(context).apply {
                val sz = dpToPx(16)
                layoutParams = LinearLayout.LayoutParams(sz, sz)
                setImageResource(android.R.drawable.ic_media_pause)
                setColorFilter(Color.WHITE)
            }
            pauseIcon = pauseImg
            val pauseLbl = TextView(context).apply {
                text = "Pause"
                setTextColor(Color.WHITE)
                textSize = 12f
                setPadding(dpToPx(4), 0, 0, 0)
            }
            pauseText = pauseLbl
            pauseBtn.addView(pauseImg)
            pauseBtn.addView(pauseLbl)
            addView(pauseBtn)

            // Space
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1) })

            // 3. Stop Button (Vibrant Red)
            val stopBtn = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
                background = createRoundedDrawable(Color.parseColor("#FF3B30"), dpToPx(16))
                isClickable = true
                setOnClickListener {
                    ScreenRecorderService.stopRecording(context)
                    // Launch MainActivity to show finished video
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(launchIntent)
                    collapseControls()
                }
            }
            val stopSquare = View(context).apply {
                val sz = dpToPx(11)
                layoutParams = LinearLayout.LayoutParams(sz, sz)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(2.5f).toFloat()
                    setColor(Color.WHITE)
                }
            }
            val stopLbl = TextView(context).apply {
                text = "Stop"
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(4), 0, 0, 0)
            }
            stopBtn.addView(stopSquare)
            stopBtn.addView(stopLbl)
            addView(stopBtn)

            // Space
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1) })

            // 4. Minimize / Collapse Button (Close icon)
            val closeBtn = ImageView(context).apply {
                val sz = dpToPx(26)
                layoutParams = LinearLayout.LayoutParams(sz, sz)
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.parseColor("#B0B0B8"))
                isClickable = true
                setOnClickListener {
                    collapseControls()
                }
            }
            addView(closeBtn)

            // Touch dragging on expanded pill
            var pInitialX = 0
            var pInitialY = 0
            var pTouchX = 0f
            var pTouchY = 0f
            var pIsDragging = false

            setOnTouchListener { _, event ->
                val currentParams = windowParams ?: return@setOnTouchListener false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pInitialX = currentParams.x
                        pInitialY = currentParams.y
                        pTouchX = event.rawX
                        pTouchY = event.rawY
                        pIsDragging = false
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - pTouchX).toInt()
                        val dy = (event.rawY - pTouchY).toInt()
                        if (abs(dx) > 12 || abs(dy) > 12) {
                            pIsDragging = true
                            currentParams.x = pInitialX + dx
                            currentParams.y = pInitialY + dy
                            try {
                                windowManager.updateViewLayout(rootView, currentParams)
                            } catch (e: Exception) {
                                // Ignore
                            }
                            true
                        } else false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (pIsDragging) {
                            preExpandX = currentParams.x
                            true
                        } else false
                    }
                    else -> false
                }
            }
        }
    }

    private fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels
        }
    }

    private fun expandControls() {
        isExpanded = true
        val currentParams = windowParams ?: return
        preExpandX = currentParams.x

        val screenWidth = getScreenWidth()
        val pillEstimatedWidth = dpToPx(285)

        // Shift window to the left so that the entire pill is within screen bounds
        if (currentParams.x + pillEstimatedWidth > screenWidth - dpToPx(12)) {
            currentParams.x = screenWidth - pillEstimatedWidth - dpToPx(12)
        }
        if (currentParams.x < dpToPx(12)) {
            currentParams.x = dpToPx(12)
        }

        try {
            windowManager.updateViewLayout(rootView, currentParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window layout on expand: ${e.message}")
        }

        collapsedCircle?.visibility = View.GONE
        expandedPill?.visibility = View.VISIBLE
    }

    private fun collapseControls() {
        isExpanded = false
        val currentParams = windowParams ?: return
        val screenWidth = getScreenWidth()

        currentParams.x = if (preExpandX > screenWidth / 2) {
            screenWidth - dpToPx(68)
        } else {
            dpToPx(12)
        }

        try {
            windowManager.updateViewLayout(rootView, currentParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window layout on collapse: ${e.message}")
        }

        expandedPill?.visibility = View.GONE
        collapsedCircle?.visibility = View.VISIBLE
    }

    private fun startPulsingAnimation() {
        val anim = AlphaAnimation(1.0f, 0.25f).apply {
            duration = 600
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        recDot?.startAnimation(anim)
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun createRoundedDrawable(color: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = radiusPx.toFloat()
            setColor(color)
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
