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
    private lateinit var preferencesManager: PreferencesManager
    private var currentOrientation: ScreenOrientation = ScreenOrientation.Unspecified

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            currentOrientation = preferencesManager.lastTileOrientation.firstOrNull()
                ?: ScreenOrientation.Unspecified
            updateTile(currentOrientation)
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            // Cycle through orientations
            val nextOrientation = currentOrientation.next()
            currentOrientation = nextOrientation

            // Save the new orientation
            preferencesManager.setLastTileOrientation(nextOrientation)

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
