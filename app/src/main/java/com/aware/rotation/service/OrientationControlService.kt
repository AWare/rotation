package com.aware.rotation.service

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.widget.Toast
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
import kotlinx.coroutines.launch

/**
 * Service for controlling screen orientation with multi-screen support using FP style
 */
class OrientationControlService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }

    companion object {
        private const val TAG = "OrientationControl"
        const val ACTION_SET_ORIENTATION = "com.aware.rotation.action.SET_ORIENTATION"
        const val EXTRA_ORIENTATION = "orientation"
        const val EXTRA_SCREEN_ID = "screen_id"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "handleIntent: action=$action")

        when (action) {
            ACTION_SET_ORIENTATION -> {
                val orientationValue = intent.getIntExtra(EXTRA_ORIENTATION, -1)
                val screenId = intent.getIntExtra(EXTRA_SCREEN_ID, -1)
                Log.d(TAG, "SET_ORIENTATION: orientationValue=$orientationValue, screenId=$screenId")

                ScreenOrientation.fromValue(orientationValue).fold(
                    ifLeft = { error ->
                        Log.e(TAG, "Invalid orientation value: $orientationValue")
                        showError("Invalid orientation")
                    },
                    ifRight = { orientation ->
                        Log.d(TAG, "Setting orientation to: ${orientation.displayName}")
                        val screen = TargetScreen.fromId(screenId).fold({ TargetScreen.AllScreens }, { it })

                        serviceScope.launch {
                            setOrientation(orientation, screen).fold(
                                ifLeft = { error ->
                                    Log.e(TAG, "Failed to set orientation: $error")
                                    showError(when (error) {
                                        is OrientationError.PermissionDenied -> "Permission denied: ${error.permission}"
                                        is OrientationError.DatabaseError -> "Error: ${error.message}"
                                        else -> "Failed to set orientation"
                                    })
                                },
                                ifRight = {
                                    Log.i(TAG, "Successfully set orientation to: ${orientation.displayName}")
                                    showSuccess("Orientation set to ${orientation.displayName}")
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun showError(message: String) {
        serviceScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccess(message: String) {
        serviceScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
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
            Log.d(TAG, "setOrientationForAllDisplays: ${orientation.displayName}")

            // Check permission first
            if (!Settings.System.canWrite(this)) {
                Log.e(TAG, "WRITE_SETTINGS permission not granted!")
                throw SecurityException("WRITE_SETTINGS permission required")
            }

            // Set auto-rotate setting
            val autoRotateValue = when (orientation) {
                ScreenOrientation.Unspecified, ScreenOrientation.Sensor -> 1
                else -> 0
            }

            Log.d(TAG, "Setting ACCELEROMETER_ROTATION to $autoRotateValue")
            val autoRotateSuccess = Settings.System.putInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                autoRotateValue
            )
            Log.d(TAG, "ACCELEROMETER_ROTATION putInt result: $autoRotateSuccess")

            // Set user rotation for specific orientations
            if (orientation != ScreenOrientation.Unspecified && orientation != ScreenOrientation.Sensor) {
                val rotationValue = when (orientation) {
                    ScreenOrientation.Portrait -> 0
                    ScreenOrientation.Landscape -> 1
                    ScreenOrientation.ReversePortrait -> 2
                    ScreenOrientation.ReverseLandscape -> 3
                    else -> 0
                }

                Log.d(TAG, "Setting USER_ROTATION to $rotationValue (${orientation.displayName})")
                val userRotationSuccess = Settings.System.putInt(
                    contentResolver,
                    Settings.System.USER_ROTATION,
                    rotationValue
                )
                Log.d(TAG, "USER_ROTATION putInt result: $userRotationSuccess")
            }

            Log.i(TAG, "Successfully updated system settings for orientation")
        }.mapLeft { e ->
            Log.e(TAG, "Exception in setOrientationForAllDisplays", e)
            OrientationError.DatabaseError("Failed to set orientation: ${e.message}")
        }

    private fun setOrientationForDisplay(
        orientation: ScreenOrientation,
        displayId: Int
    ): Either<OrientationError, Unit> {
        // Note: Per-display orientation requires system-level access
        // For now, we apply the same logic as all displays
        return setOrientationForAllDisplays(orientation)
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
}
