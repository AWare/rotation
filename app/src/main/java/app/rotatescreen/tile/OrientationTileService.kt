package app.rotatescreen.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.rotatescreen.R
import app.rotatescreen.data.preferences.PreferencesManager
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import app.rotatescreen.service.OrientationControlService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile for controlling screen orientation using FP style
 */
class OrientationTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var preferencesManager: PreferencesManager? = null
    private var currentOrientation: ScreenOrientation = ScreenOrientation.Unspecified

    override fun onCreate() {
        super.onCreate()
        try {
            preferencesManager = PreferencesManager(applicationContext)
            android.util.Log.d("OrientationTileService", "Tile initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("OrientationTileService", "Failed to initialize tile", e)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val manager = preferencesManager
        if (manager == null) {
            android.util.Log.w("OrientationTileService", "PreferencesManager not initialized")
            return
        }

        serviceScope.launch {
            currentOrientation = manager.lastTileOrientation.firstOrNull()
                ?: ScreenOrientation.Unspecified
            updateTile(currentOrientation)
        }
    }

    override fun onClick() {
        super.onClick()
        val manager = preferencesManager
        if (manager == null) {
            android.util.Log.w("OrientationTileService", "PreferencesManager not initialized")
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = "Not Ready"
                updateTile()
            }
            return
        }

        serviceScope.launch {
            // Cycle through orientations
            val nextOrientation = currentOrientation.next()
            currentOrientation = nextOrientation

            // Save the new orientation
            manager.setLastTileOrientation(nextOrientation)

            // Apply the orientation
            applyOrientation(nextOrientation)

            // Update tile UI
            updateTile(nextOrientation)
        }
    }

    private fun applyOrientation(orientation: ScreenOrientation) {
        val intent = Intent(this, OrientationControlService::class.java).apply {
            action = OrientationControlService.ACTION_SET_ORIENTATION
            putExtra(OrientationControlService.EXTRA_ORIENTATION, orientation.value)
            putExtra(OrientationControlService.EXTRA_SCREEN_ID, TargetScreen.AllScreens.id)
        }
        startService(intent)
    }

    private fun updateTile(orientation: ScreenOrientation) {
        qsTile?.apply {
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_screen_rotation)
            label = orientation.displayName
            state = when (orientation) {
                ScreenOrientation.Unspecified, ScreenOrientation.Sensor -> Tile.STATE_INACTIVE
                else -> Tile.STATE_ACTIVE
            }
            updateTile()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
