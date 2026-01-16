package com.aware.rotation.service

import android.app.Service
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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
 * Service for controlling screen orientation using overlay window approach
 * This is more reliable than Settings.System approach across different devices
 */
class OrientationControlService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    private var overlayView: View? = null
    private var currentOrientation: ScreenOrientation = ScreenOrientation.Unspecified

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

                        serviceScope.launch(Dispatchers.Main) {
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
        PermissionChecker.checkDrawOverlayPermission(this@OrientationControlService).bind()

        when (targetScreen) {
            is TargetScreen.AllScreens -> setOrientationWithOverlay(orientation)
            is TargetScreen.SpecificScreen -> setOrientationWithOverlay(orientation)
        }.bind()
    }

    private fun setOrientationWithOverlay(orientation: ScreenOrientation): Either<OrientationError, Unit> =
        Either.catch {
            Log.d(TAG, "setOrientationWithOverlay: ${orientation.displayName}")

            // Check permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted!")
                throw SecurityException("SYSTEM_ALERT_WINDOW permission required")
            }

            // Remove existing overlay if present
            removeOverlay()

            // For Unspecified/Sensor, we remove the overlay and let the system handle rotation
            if (orientation == ScreenOrientation.Unspecified || orientation == ScreenOrientation.Sensor) {
                Log.d(TAG, "Removing overlay for sensor-based orientation")
                currentOrientation = orientation
                return@catch
            }

            // Create and add new overlay with the desired orientation
            currentOrientation = orientation
            createAndShowOverlay(orientation)

            Log.i(TAG, "Successfully set orientation using overlay")
        }.mapLeft { e ->
            Log.e(TAG, "Exception in setOrientationWithOverlay", e)
            OrientationError.DatabaseError("Failed to set orientation: ${e.message}")
        }

    private fun createAndShowOverlay(orientation: ScreenOrientation) {
        Log.d(TAG, "createAndShowOverlay: ${orientation.displayName}")

        // Create a minimal invisible view
        val view = View(this)
        overlayView = view

        // Configure window layout parameters
        val layoutParams = WindowManager.LayoutParams(
            1, // width: 1 pixel (minimal)
            1, // height: 1 pixel (minimal)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Set the desired screen orientation
        layoutParams.screenOrientation = when (orientation) {
            ScreenOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            ScreenOrientation.ReverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            ScreenOrientation.Sensor -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            ScreenOrientation.Unspecified -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        // Position at top-left corner
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 0

        // Add the overlay to the window
        try {
            windowManager.addView(view, layoutParams)
            Log.d(TAG, "Overlay view added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            overlayView = null
            throw e
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                Log.d(TAG, "Removing overlay view")
                windowManager.removeView(view)
                overlayView = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
                overlayView = null
            }
        }
    }

    fun getAvailableDisplays(): Either<OrientationError, List<Display>> =
        Either.catch {
            displayManager.displays.toList()
        }.mapLeft { e ->
            OrientationError.DatabaseError("Failed to get displays: ${e.message}")
        }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up")
        removeOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }
}
