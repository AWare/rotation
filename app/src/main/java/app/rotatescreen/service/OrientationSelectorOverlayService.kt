package app.rotatescreen.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.repository.OrientationRepository
import app.rotatescreen.domain.model.AppOrientationSetting
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Service that displays an interactive orientation selector overlay
 * Shows on screens where the app is active
 */
class OrientationSelectorOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private lateinit var repository: OrientationRepository

    // Store overlay views per display ID
    private val overlayViews = mutableMapOf<Int, View>()

    companion object {
        private const val TAG = "OrientationSelector"
        const val ACTION_SHOW_SELECTOR = "app.rotatescreen.action.SHOW_SELECTOR"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_DISPLAY_IDS = "display_ids"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OrientationSelectorOverlayService onCreate()")
        try {
            val database = RotationDatabase.getInstance(applicationContext)
            repository = OrientationRepository(database.appOrientationDao())
            Log.d(TAG, "Repository initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing repository", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent: action=${intent.action}")
        when (intent.action) {
            ACTION_SHOW_SELECTOR -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val appName = intent.getStringExtra(EXTRA_APP_NAME)
                val displayIds = intent.getIntArrayExtra(EXTRA_DISPLAY_IDS)

                Log.d(TAG, "ACTION_SHOW_SELECTOR: pkg=$packageName, app=$appName, displays=${displayIds?.contentToString()}")

                if (packageName == null) {
                    Log.e(TAG, "Missing package name!")
                    stopSelf()
                    return
                }

                showSelectorOverlay(
                    packageName,
                    appName ?: packageName,
                    displayIds?.toList() ?: listOf(0)
                )
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
                stopSelf()
            }
        }
    }

    private fun showSelectorOverlay(packageName: String, appName: String, displayIds: List<Int>) {
        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted!")
            android.widget.Toast.makeText(
                this,
                "Display over other apps permission required",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            stopSelf()
            return
        }

        serviceScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "Showing selector for $appName on ${displayIds.size} displays")

                // Remove any existing overlays first
                dismissAllOverlays()

                Log.d(TAG, "Removed existing overlays, now showing new ones")

                // Show overlay on each specified display
                displayIds.forEach { displayId ->
                    Log.d(TAG, "Attempting to show overlay on display $displayId")
                    val display = displayManager.displays.find { it.displayId == displayId }
                    if (display != null) {
                        showOverlayOnDisplay(display.displayId, packageName, appName)
                    } else {
                        Log.w(TAG, "Display $displayId not found")
                    }
                }

                if (overlayViews.isEmpty()) {
                    Log.e(TAG, "No overlays were added!")
                    android.widget.Toast.makeText(
                        this@OrientationSelectorOverlayService,
                        "Failed to show overlay - no displays available",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    stopSelf()
                } else {
                    Log.d(TAG, "Successfully showed ${overlayViews.size} overlays")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show selector overlay", e)
                android.widget.Toast.makeText(
                    this@OrientationSelectorOverlayService,
                    "Error showing overlay: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                stopSelf()
            }
        }
    }

    private fun showOverlayOnDisplay(displayId: Int, packageName: String, appName: String) {
        try {
            Log.d(TAG, "showOverlayOnDisplay: displayId=$displayId, appName=$appName")

            val display = displayManager.displays.find { it.displayId == displayId }
            if (display == null) {
                Log.e(TAG, "Display $displayId not found")
                return
            }

            // Create display-specific context
            val displayContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                createDisplayContext(display)
            } else {
                this
            }

            val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager

            Log.d(TAG, "Creating ComposeView for display $displayId")

            // Create Compose view - must be created on main thread
            val composeView = ComposeView(displayContext).apply {
                try {
                    setContent {
                        MaterialTheme {
                            OrientationSelectorUI(
                                appName = appName,
                                onOrientationSelected = { orientation ->
                                    handleOrientationSelection(packageName, appName, orientation, displayId)
                                },
                                onDismiss = {
                                    dismissAllOverlays()
                                    stopSelf()
                                }
                            )
                        }
                    }
                    Log.d(TAG, "ComposeView content set successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting ComposeView content", e)
                    throw e
                }
            }

            // Configure window layout parameters
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
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            layoutParams.gravity = Gravity.CENTER

            Log.d(TAG, "Adding view to WindowManager for display $displayId")

            // Add the overlay
            try {
                displayWindowManager.addView(composeView, layoutParams)
                overlayViews[displayId] = composeView
                Log.d(TAG, "Successfully added selector overlay to display $displayId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add selector overlay to WindowManager on display $displayId", e)
                android.widget.Toast.makeText(
                    this,
                    "Failed to show overlay on display $displayId: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in showOverlayOnDisplay for display $displayId", e)
            android.widget.Toast.makeText(
                this,
                "Error showing overlay: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleOrientationSelection(
        packageName: String,
        appName: String,
        orientation: ScreenOrientation,
        displayId: Int
    ) {
        serviceScope.launch {
            try {
                // Save the setting for this specific display
                val setting = AppOrientationSetting.create(
                    packageName = packageName,
                    appName = appName,
                    orientation = orientation,
                    targetScreen = TargetScreen.SpecificScreen(displayId, "Display $displayId")
                )
                repository.saveSetting(setting)

                // Apply the orientation immediately
                val intent = Intent(this@OrientationSelectorOverlayService, OrientationControlService::class.java).apply {
                    action = OrientationControlService.ACTION_SET_ORIENTATION
                    putExtra(OrientationControlService.EXTRA_ORIENTATION, orientation.value)
                    putExtra(OrientationControlService.EXTRA_SCREEN_ID, displayId)
                }
                startService(intent)

                Log.d(TAG, "Applied $orientation to $appName on display $displayId")

                // Dismiss the overlay after a short delay
                delay(200)
                dismissAllOverlays()
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle orientation selection", e)
            }
        }
    }

    private fun dismissAllOverlays() {
        overlayViews.forEach { (displayId, view) ->
            try {
                val display = displayManager.displays.find { it.displayId == displayId }
                val displayContext = if (display != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    createDisplayContext(display)
                } else {
                    this
                }
                val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
                displayWindowManager.removeView(view)
                Log.d(TAG, "Removed overlay from display $displayId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay from display $displayId", e)
            }
        }
        overlayViews.clear()
    }

    override fun onDestroy() {
        dismissAllOverlays()
        serviceScope.cancel()
        super.onDestroy()
    }
}

@Composable
fun OrientationSelectorUI(
    appName: String,
    onOrientationSelected: (ScreenOrientation) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .clickable(enabled = false) { }, // Prevent click-through
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ComposeColor(0xFF1A0D2E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = appName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select Orientation",
                    fontSize = 16.sp,
                    color = ComposeColor.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Orientation buttons in a 2x3 grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OrientationButton(
                            label = "Auto",
                            orientation = ScreenOrientation.Unspecified,
                            rotation = 0f,
                            onClick = { onOrientationSelected(ScreenOrientation.Unspecified) }
                        )
                        OrientationButton(
                            label = "Portrait",
                            orientation = ScreenOrientation.Portrait,
                            rotation = 0f,
                            onClick = { onOrientationSelected(ScreenOrientation.Portrait) }
                        )
                        OrientationButton(
                            label = "Landscape",
                            orientation = ScreenOrientation.Landscape,
                            rotation = 90f,
                            onClick = { onOrientationSelected(ScreenOrientation.Landscape) }
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OrientationButton(
                            label = "Sensor",
                            orientation = ScreenOrientation.Sensor,
                            rotation = 0f,
                            onClick = { onOrientationSelected(ScreenOrientation.Sensor) }
                        )
                        OrientationButton(
                            label = "↓Portrait",
                            orientation = ScreenOrientation.ReversePortrait,
                            rotation = 180f,
                            onClick = { onOrientationSelected(ScreenOrientation.ReversePortrait) }
                        )
                        OrientationButton(
                            label = "←Landscape",
                            orientation = ScreenOrientation.ReverseLandscape,
                            rotation = 270f,
                            onClick = { onOrientationSelected(ScreenOrientation.ReverseLandscape) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dismiss button
                Text(
                    text = "Tap outside to cancel",
                    fontSize = 12.sp,
                    color = ComposeColor.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun OrientationButton(
    label: String,
    orientation: ScreenOrientation,
    rotation: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(140.dp, 100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor(0xFF2E1A47)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // "Up" indicator with rotation
                Text(
                    text = "↑",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor(0xFF00FF41),
                    modifier = Modifier.rotate(rotation)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Label
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ComposeColor.White
                )
            }
        }
    }
}
