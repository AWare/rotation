package app.rotatescreen.service

import android.app.Service
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
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
import app.rotatescreen.domain.model.OrientationError
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import app.rotatescreen.util.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Service for controlling screen orientation using overlay window approach
 * This is more reliable than Settings.System approach across different devices
 */
class OrientationControlService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    // Store overlay views per display ID
    private val overlayViews = mutableMapOf<Int, View>()
    private var currentOrientation: ScreenOrientation = ScreenOrientation.Unspecified

    companion object {
        private const val TAG = "OrientationControl"
        const val ACTION_SET_ORIENTATION = "com.aware.rotation.action.SET_ORIENTATION"
        const val ACTION_FLASH_SCREEN = "com.aware.rotation.action.FLASH_SCREEN"
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
            ACTION_FLASH_SCREEN -> {
                val screenId = intent.getIntExtra(EXTRA_SCREEN_ID, -1)
                val screenName = intent.getStringExtra("SCREEN_NAME") ?: "Screen"
                val orientation = intent.getStringExtra("ORIENTATION") ?: "Auto"
                val color1 = intent.getLongExtra("COLOR_1", 0xFFB565FFL).toInt()
                val color2 = intent.getLongExtra("COLOR_2", 0xFF00FF41L).toInt()
                val color3 = intent.getLongExtra("COLOR_3", 0xFFFF8C00L).toInt()
                val bgColor = intent.getLongExtra("BG_COLOR", 0xFF1A0D2EL).toInt()
                val textColor = intent.getLongExtra("TEXT_COLOR", 0xFFFFFFFFL).toInt()

                Log.d(TAG, "FLASH_SCREEN: screenId=$screenId, name=$screenName, orientation=$orientation")

                serviceScope.launch(Dispatchers.Main) {
                    flashScreen(screenId, screenName, orientation, color1, color2, color3, bgColor, textColor)
                }
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
            is TargetScreen.AllScreens -> {
                // Apply to all displays
                Log.d(TAG, "Applying orientation to all screens")
                val displays = displayManager.displays
                displays.forEach { display ->
                    setOrientationWithOverlay(orientation, display.displayId).bind()
                }
                Either.Right(Unit)
            }
            is TargetScreen.SpecificScreen -> {
                // Apply to specific display
                Log.d(TAG, "Applying orientation to display ${targetScreen.id}")
                setOrientationWithOverlay(orientation, targetScreen.id)
            }
        }.bind()
    }

    private fun setOrientationWithOverlay(
        orientation: ScreenOrientation,
        displayId: Int
    ): Either<OrientationError, Unit> =
        Either.catch {
            Log.d(TAG, "setOrientationWithOverlay: ${orientation.displayName} on display $displayId")

            // Check permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted!")
                throw SecurityException("SYSTEM_ALERT_WINDOW permission required")
            }

            // Remove existing overlay for this display if present
            removeOverlay(displayId)

            // For Unspecified/Sensor, we remove the overlay and let the system handle rotation
            if (orientation == ScreenOrientation.Unspecified || orientation == ScreenOrientation.Sensor) {
                Log.d(TAG, "Removing overlay for sensor-based orientation on display $displayId")
                currentOrientation = orientation
                return@catch
            }

            // Create and add new overlay with the desired orientation
            currentOrientation = orientation
            createAndShowOverlay(orientation, displayId)

            Log.i(TAG, "Successfully set orientation using overlay on display $displayId")
        }.mapLeft { e ->
            Log.e(TAG, "Exception in setOrientationWithOverlay", e)
            OrientationError.DatabaseError("Failed to set orientation: ${e.message}")
        }

    private fun createAndShowOverlay(orientation: ScreenOrientation, displayId: Int) {
        Log.d(TAG, "createAndShowOverlay: ${orientation.displayName} on display $displayId")

        // Get the display-specific context and window manager
        val display = displayManager.displays.find { it.displayId == displayId }
        if (display == null) {
            Log.e(TAG, "Display $displayId not found")
            return
        }

        // Create a display-specific context
        val displayContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            createDisplayContext(display)
        } else {
            this
        }

        // Get WindowManager for this specific display
        val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager

        // Create a minimal invisible view
        val view = View(displayContext)
        overlayViews[displayId] = view

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

        // Add the overlay to the window on the specific display
        try {
            displayWindowManager.addView(view, layoutParams)
            Log.d(TAG, "Overlay view added successfully on display $displayId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view on display $displayId", e)
            overlayViews.remove(displayId)
            throw e
        }
    }

    private fun removeOverlay(displayId: Int) {
        overlayViews[displayId]?.let { view ->
            try {
                Log.d(TAG, "Removing overlay view from display $displayId")

                // Get the display-specific context to get its WindowManager
                val display = displayManager.displays.find { it.displayId == displayId }
                val displayContext = if (display != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    createDisplayContext(display)
                } else {
                    this
                }
                val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager

                displayWindowManager.removeView(view)
                overlayViews.remove(displayId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view from display $displayId", e)
                overlayViews.remove(displayId)
            }
        }
    }

    private fun removeAllOverlays() {
        Log.d(TAG, "Removing all overlay views")
        val displayIds = overlayViews.keys.toList() // Make a copy to avoid concurrent modification
        displayIds.forEach { displayId ->
            removeOverlay(displayId)
        }
    }

    fun getAvailableDisplays(): Either<OrientationError, List<Display>> =
        Either.catch {
            displayManager.displays.toList()
        }.mapLeft { e ->
            OrientationError.DatabaseError("Failed to get displays: ${e.message}")
        }

    private suspend fun flashScreen(
        displayId: Int,
        screenName: String = "Screen",
        orientation: String = "Auto",
        color1: Int = android.graphics.Color.parseColor("#B565FF"),
        color2: Int = android.graphics.Color.parseColor("#00FF41"),
        color3: Int = android.graphics.Color.parseColor("#FF8C00"),
        bgColor: Int = android.graphics.Color.parseColor("#1A0D2E"),
        textColor: Int = android.graphics.Color.WHITE
    ) {
        Either.catch {
            Log.d(TAG, "flashScreen: displayId=$displayId, screenName=$screenName, orientation=$orientation")

            // Check permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted!")
                return
            }

            // Get the display
            val display = displayManager.displays.find { it.displayId == displayId }
            if (display == null) {
                Log.e(TAG, "Display $displayId not found")
                return
            }

            // Create a display-specific context
            val displayContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                createDisplayContext(display)
            } else {
                this
            }

            // Get WindowManager for this specific display
            val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager

            // Create custom flash view with diagonal stripes
            val flashView = object : View(displayContext) {
                private val paint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = false // Pixel-perfect for retro look
                }

                private val textPaint = android.graphics.Paint().apply {
                    color = textColor
                    textSize = 80f
                    typeface = android.graphics.Typeface.MONOSPACE
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                override fun onDraw(canvas: android.graphics.Canvas) {
                    super.onDraw(canvas)
                    val width = width.toFloat()
                    val height = height.toFloat()

                    // Draw diagonal stripe pattern
                    val stripeWidth = 60f
                    val colors = arrayOf(color1, color2, color3, bgColor)

                    // Calculate diagonal stripes
                    val diagonal = Math.sqrt((width * width + height * height).toDouble()).toFloat()
                    val numStripes = (diagonal / stripeWidth).toInt() + 2

                    for (i in 0 until numStripes) {
                        paint.color = colors[i % colors.size]
                        val offset = i * stripeWidth - diagonal / 2

                        // Draw diagonal stripe from bottom-left to top-right
                        val path = android.graphics.Path().apply {
                            moveTo(offset, height)
                            lineTo(offset + stripeWidth, height)
                            lineTo(offset + stripeWidth + height, 0f)
                            lineTo(offset + height, 0f)
                            close()
                        }
                        canvas.drawPath(path, paint)
                    }

                    // Draw semi-transparent overlay for text readability
                    paint.color = android.graphics.Color.argb(180, 0, 0, 0)
                    canvas.drawRect(0f, height / 2 - 120f, width, height / 2 + 120f, paint)

                    // Draw screen name
                    canvas.drawText(screenName, width / 2, height / 2 - 20f, textPaint)

                    // Draw orientation
                    textPaint.textSize = 60f
                    canvas.drawText(orientation, width / 2, height / 2 + 50f, textPaint)
                }
            }

            flashView.alpha = 0.85f

            // Configure window layout parameters for full-screen flash
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
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

            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = 0
            layoutParams.y = 0

            // Add the flash overlay
            try {
                displayWindowManager.addView(flashView, layoutParams)
                Log.d(TAG, "Flash overlay added on display $displayId")

                // Wait for 500ms to give users time to see the info
                delay(500)

                // Remove the flash overlay
                displayWindowManager.removeView(flashView)
                Log.d(TAG, "Flash overlay removed from display $displayId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flash screen on display $displayId", e)
            }
        }.mapLeft { e ->
            Log.e(TAG, "Exception in flashScreen", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up")
        removeAllOverlays()
        serviceScope.cancel()
        super.onDestroy()
    }
}
