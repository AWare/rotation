package app.rotatescreen.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.service.OrientationControlService

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

    private var currentIndex = 0

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(orientationCycle[currentIndex])
    }

    override fun onClick() {
        super.onClick()

        // Cycle to next orientation
        currentIndex = (currentIndex + 1) % orientationCycle.size
        val nextOrientation = orientationCycle[currentIndex]

        // Apply orientation
        val intent = Intent(this, OrientationControlService::class.java).apply {
            action = OrientationControlService.ACTION_SET_ORIENTATION
            putExtra(OrientationControlService.EXTRA_ORIENTATION, nextOrientation.value)
            putExtra(OrientationControlService.EXTRA_SCREEN_ID, -1) // All screens
        }
        startService(intent)

        // Update tile
        updateTileState(nextOrientation)
    }

    private fun updateTileState(orientation: ScreenOrientation) {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Global: ${orientation.displayName}"
            contentDescription = "Current global orientation: ${orientation.displayName}"
            updateTile()
        }
    }
}
