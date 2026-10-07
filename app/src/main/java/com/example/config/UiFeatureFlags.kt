package com.example.config

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Feature Flags and Remote Configuration for FlowRec UI.
 * Allows switching between the new Minimalist Monochrome UI (ink-on-paper)
 * and classic styles, as well as toggling fine-grained UI features locally or remotely.
 */
data class UiFeatureFlagsState(
    val useMinimalistMonochromeUi: Boolean = true,
    val enableShutterMorphAnimation: Boolean = true,
    val enableBackgroundRings: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val enableQualityBottomSheet: Boolean = true,
    val enableReducedMotionAutoDetect: Boolean = true,
    val remoteConfigUrl: String = "",
    val lastSyncTimestamp: Long = 0L
)

class UiFeatureFlagManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _flags = MutableStateFlow(loadFlagsFromPrefs())
    val flags: StateFlow<UiFeatureFlagsState> = _flags.asStateFlow()

    companion object {
        private const val PREFS_NAME = "flowrec_feature_flags"
        private const val KEY_USE_MINIMALIST = "flag_use_minimalist_ui"
        private const val KEY_SHUTTER_ANIM = "flag_shutter_anim"
        private const val KEY_BG_RINGS = "flag_bg_rings"
        private const val KEY_HAPTICS = "flag_haptics"
        private const val KEY_QUALITY_SHEET = "flag_quality_sheet"
        private const val KEY_REDUCED_MOTION = "flag_reduced_motion"
        private const val KEY_REMOTE_URL = "flag_remote_config_url"
        private const val KEY_LAST_SYNC = "flag_last_sync_timestamp"

        @Volatile
        private var instance: UiFeatureFlagManager? = null

        fun getInstance(context: Context): UiFeatureFlagManager {
            return instance ?: synchronized(this) {
                instance ?: UiFeatureFlagManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun loadFlagsFromPrefs(): UiFeatureFlagsState {
        return UiFeatureFlagsState(
            useMinimalistMonochromeUi = prefs.getBoolean(KEY_USE_MINIMALIST, true),
            enableShutterMorphAnimation = prefs.getBoolean(KEY_SHUTTER_ANIM, true),
            enableBackgroundRings = prefs.getBoolean(KEY_BG_RINGS, true),
            enableHapticFeedback = prefs.getBoolean(KEY_HAPTICS, true),
            enableQualityBottomSheet = prefs.getBoolean(KEY_QUALITY_SHEET, true),
            enableReducedMotionAutoDetect = prefs.getBoolean(KEY_REDUCED_MOTION, true),
            remoteConfigUrl = prefs.getString(KEY_REMOTE_URL, "") ?: "",
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC, 0L)
        )
    }

    fun setMinimalistUiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_MINIMALIST, enabled).apply()
        _flags.value = _flags.value.copy(useMinimalistMonochromeUi = enabled)
    }

    fun setShutterMorphEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUTTER_ANIM, enabled).apply()
        _flags.value = _flags.value.copy(enableShutterMorphAnimation = enabled)
    }

    fun setBackgroundRingsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_RINGS, enabled).apply()
        _flags.value = _flags.value.copy(enableBackgroundRings = enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _flags.value = _flags.value.copy(enableHapticFeedback = enabled)
    }

    fun setQualitySheetEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_QUALITY_SHEET, enabled).apply()
        _flags.value = _flags.value.copy(enableQualityBottomSheet = enabled)
    }

    fun setReducedMotionAutoDetect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
        _flags.value = _flags.value.copy(enableReducedMotionAutoDetect = enabled)
    }

    fun setRemoteConfigUrl(url: String) {
        prefs.edit().putString(KEY_REMOTE_URL, url).apply()
        _flags.value = _flags.value.copy(remoteConfigUrl = url)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _flags.value = UiFeatureFlagsState()
    }

    /**
     * Checks device accessibility reduced motion setting.
     */
    fun isReducedMotion(context: Context): Boolean {
        if (!_flags.value.enableReducedMotionAutoDetect) return false
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetches and applies remote config flags from a remote JSON endpoint asynchronously.
     * Expected JSON schema:
     * {
     *   "useMinimalistMonochromeUi": true,
     *   "enableShutterMorphAnimation": true,
     *   "enableBackgroundRings": true,
     *   "enableHapticFeedback": true,
     *   "enableQualityBottomSheet": true
     * }
     */
    fun fetchAndActivateRemoteConfig(scope: CoroutineScope = CoroutineScope(Dispatchers.IO), onResult: ((Boolean) -> Unit)? = null) {
        val targetUrl = _flags.value.remoteConfigUrl.trim()
        if (targetUrl.isBlank()) {
            onResult?.invoke(false)
            return
        }

        scope.launch {
            val success = try {
                val url = URL(targetUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)

                    val newMinimalist = json.optBoolean("useMinimalistMonochromeUi", _flags.value.useMinimalistMonochromeUi)
                    val newShutter = json.optBoolean("enableShutterMorphAnimation", _flags.value.enableShutterMorphAnimation)
                    val newRings = json.optBoolean("enableBackgroundRings", _flags.value.enableBackgroundRings)
                    val newHaptics = json.optBoolean("enableHapticFeedback", _flags.value.enableHapticFeedback)
                    val newSheet = json.optBoolean("enableQualityBottomSheet", _flags.value.enableQualityBottomSheet)

                    withContext(Dispatchers.Main) {
                        prefs.edit()
                            .putBoolean(KEY_USE_MINIMALIST, newMinimalist)
                            .putBoolean(KEY_SHUTTER_ANIM, newShutter)
                            .putBoolean(KEY_BG_RINGS, newRings)
                            .putBoolean(KEY_HAPTICS, newHaptics)
                            .putBoolean(KEY_QUALITY_SHEET, newSheet)
                            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                            .apply()

                        _flags.value = _flags.value.copy(
                            useMinimalistMonochromeUi = newMinimalist,
                            enableShutterMorphAnimation = newShutter,
                            enableBackgroundRings = newRings,
                            enableHapticFeedback = newHaptics,
                            enableQualityBottomSheet = newSheet,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                onResult?.invoke(success)
            }
        }
    }
}
