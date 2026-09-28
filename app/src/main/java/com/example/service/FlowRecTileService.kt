package com.example.service

import android.app.Activity
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.R

/**
 * System Quick Settings Tile for FlowRec Screen Recorder (as seen in Android Notification Shade / Pic 4).
 * Allows users to add a 1-tap "Screen Recorder" tile directly into their phone's native Quick Settings panel.
 */
@RequiresApi(Build.VERSION_CODES.N)
class FlowRecTileService : TileService() {

    companion object {
        /**
         * Prompts Android (Android 13+ / Samsung One UI) to display the native system dialog:
         * "Add Screen Recorder to Quick settings?" with an [Add] button.
         */
        fun requestAddToQuickSettings(activity: Activity, onComplete: (Int) -> Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val statusBarManager = activity.getSystemService(StatusBarManager::class.java)
                val component = ComponentName(activity, FlowRecTileService::class.java)
                statusBarManager?.requestAddTileService(
                    component,
                    "Screen Recorder",
                    Icon.createWithResource(activity, R.drawable.ic_qs_screen_recorder),
                    activity.mainExecutor
                ) { result ->
                    onComplete(result)
                }
            } else {
                onComplete(-1)
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (ScreenRecorderService.isRunning) {
            // If already recording, stop it
            ScreenRecorderService.stopRecording(this)
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Screen Recorder"
            tile.updateTile()
        } else {
            // Launch FlowRec to start screen recording immediately
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action_quick_record", true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        if (ScreenRecorderService.isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Recording..."
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Screen Recorder"
        }
        tile.updateTile()
    }
}
