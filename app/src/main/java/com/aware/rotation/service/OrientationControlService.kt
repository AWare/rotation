package com.aware.rotation.service

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.provider.Settings
import android.view.Display
import arrow.core.Either
import arrow.core.raise.either
import com.aware.rotation.domain.model.OrientationError
import com.aware.rotation.domain.model.ScreenOrientation
import com.aware.rotation.domain.model.TargetScreen
import com.aware.rotation.util.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Service for controlling screen orientation with multi-screen support using FP style
 */
class OrientationControlService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            ACTION_SET_ORIENTATION -> {
                val orientationValue = intent.getIntExtra(EXTRA_ORIENTATION, -1)
                val screenId = intent.getIntExtra(EXTRA_SCREEN_ID, -1)

                ScreenOrientation.fromValue(orientationValue).fold(
                    ifLeft = { /* Log error */ },
                    ifRight = { orientation ->
                        val screen = TargetScreen.fromId(screenId).getOrNull() ?: TargetScreen.AllScreens
                        setOrientation(orientation, screen)
                    }
                )
            }
        }
    }

    private fun setOrientation(
        orientation: ScreenOrientation,
        targetScreen: TargetScreen
    ): Either<OrientationError, Unit> = either {
        PermissionChecker.checkWriteSettingsPermission(this@OrientationControlService).bind()

        when (targetScreen) {
            is TargetScreen.AllScreens -> setOrientationForAllDisplays(orientation)
            is TargetScreen.SpecificScreen -> setOrientationForDisplay(orientation, targetScreen.displayId)
        }.bind()
    }

    private fun setOrientationForAllDisplays(orientation: ScreenOrientation): Either<OrientationError, Unit> =
        Either.catch {
            // Set auto-rotate setting
            val autoRotateValue = when (orientation) {
                ScreenOrientation.Unspecified, ScreenOrientation.Sensor -> 1
                else -> 0
            }

            Settings.System.putInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                autoRotateValue
            )

            // Set user rotation for specific orientations
            if (orientation != ScreenOrientation.Unspecified && orientation != ScreenOrientation.Sensor) {
                val rotationValue = when (orientation) {
                    ScreenOrientation.Portrait -> 0
                    ScreenOrientation.Landscape -> 1
                    ScreenOrientation.ReversePortrait -> 2
                    ScreenOrientation.ReverseLandscape -> 3
                    else -> 0
                }

                Settings.System.putInt(
                    contentResolver,
                    Settings.System.USER_ROTATION,
                    rotationValue
                )
            }
        }.mapLeft { e ->
            OrientationError.DatabaseError("Failed to set orientation: ${e.message}")
        }

    private fun setOrientationForDisplay(
        orientation: ScreenOrientation,
        displayId: Int
    ): Either<OrientationError, Unit> {
        // Note: Per-display orientation requires system-level access
        // For now, we apply the same logic as all displays
        return setOrientationForAllDisplays(orientation).mapLeft { e ->
            OrientationError.DatabaseError("Failed to set orientation for display $displayId: ${e.message}")
        }
    }

    fun getAvailableDisplays(): Either<OrientationError, List<Display>> =
        Either.catch {
            displayManager.displays.toList()
        }.mapLeft { e ->
            OrientationError.DatabaseError("Failed to get displays: ${e.message}")
        }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SET_ORIENTATION = "com.aware.rotation.SET_ORIENTATION"
        const val EXTRA_ORIENTATION = "orientation"
        const val EXTRA_SCREEN_ID = "screen_id"
    }
}
