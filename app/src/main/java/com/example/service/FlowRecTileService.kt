package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity

/**
 * System Quick Settings Tile for FlowRec Screen Recorder (as seen in Android Notification Shade / Pic 4).
 * Allows users to add a 1-tap "Screen Recorder" tile directly to their Android Quick Settings panel.
 */
@RequiresApi(Build.VERSION_CODES.N)
class FlowRecTileService : TileService() {

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
            startActivityAndCollapse(intent)
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
