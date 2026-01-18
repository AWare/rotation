package app.rotatescreen.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.rotatescreen.data.preferences.PreferencesManager
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.service.OrientationControlService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Quick Settings tile for global orientation control
 */
class GlobalOrientationTileService : TileService() {

    private val orientationCycle = listOf(
        ScreenOrientation.Unspecified,
        ScreenOrientation.Portrait,
        ScreenOrientation.Landscape,
        ScreenOrientation.Sensor
    )

    private var serviceScope: CoroutineScope? = null
    private var preferencesManager: PreferencesManager? = null
    private var currentOrientation: ScreenOrientation = ScreenOrientation.Unspecified

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        preferencesManager = PreferencesManager(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope?.launch {
            // Read current orientation from preferences
            val orientation = preferencesManager?.globalOrientation?.firstOrNull()
                ?: ScreenOrientation.Unspecified
            currentOrientation = orientation
            updateTileState(orientation)
        }
    }

    override fun onClick() {
        super.onClick()

        serviceScope?.launch {
            try {
                // Cycle to next orientation
                val currentIndex = orientationCycle.indexOf(currentOrientation)
                val nextIndex = (currentIndex + 1) % orientationCycle.size
                val nextOrientation = orientationCycle[nextIndex]

                // Save to preferences
                preferencesManager?.setGlobalOrientation(nextOrientation)
                currentOrientation = nextOrientation

                // Apply orientation
                val intent = Intent(this@GlobalOrientationTileService, OrientationControlService::class.java).apply {
                    action = OrientationControlService.ACTION_SET_ORIENTATION
                    putExtra(OrientationControlService.EXTRA_ORIENTATION, nextOrientation.value)
                    putExtra(OrientationControlService.EXTRA_SCREEN_ID, -1) // All screens
                }
                startService(intent)

                // Update tile
                updateTileState(nextOrientation)
            } catch (e: Exception) {
                android.util.Log.e("GlobalOrientationTile", "Error in onClick", e)
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    label = "Error"
                    updateTile()
                }
            }
        }
    }

    private fun updateTileState(orientation: ScreenOrientation) {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Global: ${orientation.displayName}"
            contentDescription = "Current global orientation: ${orientation.displayName}"
            updateTile()
        }
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        preferencesManager = null
        super.onDestroy()
    }
}
