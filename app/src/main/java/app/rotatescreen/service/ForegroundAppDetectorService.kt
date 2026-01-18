package app.rotatescreen.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.accessibility.AccessibilityEvent
import arrow.core.Either
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.repository.OrientationRepository
import app.rotatescreen.domain.model.AspectRatio
import app.rotatescreen.domain.model.TargetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Accessibility service that detects foreground app changes and applies per-app orientation settings
 */
class ForegroundAppDetectorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: OrientationRepository
    private var currentPackageName: String? = null
    private var previousPackageName: String? = null
    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    override fun onCreate() {
        super.onCreate()
        val database = RotationDatabase.getInstance(applicationContext)
        repository = OrientationRepository(database.appOrientationDao())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentPackageName) return

        // App switched - reset orientation for previous app if it had per-screen settings
        if (previousPackageName != null && previousPackageName != packageName) {
            resetOrientationForApp(previousPackageName!!)
        }

        previousPackageName = currentPackageName
        currentPackageName = packageName
        applyOrientationForApp(packageName)
    }

    private fun resetOrientationForApp(packageName: String) {
        serviceScope.launch {
            try {
                android.util.Log.d("ForegroundAppDetector", "Resetting orientation for backgrounded app: $packageName")

                // Get all settings for this app
                val settings = repository.getSetting(packageName).getOrNull() ?: return@launch

                // Only reset if app has per-screen settings (not "All Screens")
                val hasPerScreenSettings = settings.any { it.targetScreen.id != -1 }

                if (hasPerScreenSettings) {
                    // Reset to system default (Unspecified/Auto)
                    val displays = displayManager.displays
                    displays.forEach { display ->
                        val intent = Intent(
                            this@ForegroundAppDetectorService,
                            OrientationControlService::class.java
                        ).apply {
                            action = OrientationControlService.ACTION_SET_ORIENTATION
                            putExtra(OrientationControlService.EXTRA_ORIENTATION, ScreenOrientation.Unspecified.value)
                            putExtra(OrientationControlService.EXTRA_SCREEN_ID, display.displayId)
                        }
                        startService(intent)
                    }

                    android.util.Log.d("ForegroundAppDetector", "Reset orientation for $packageName")
                }
            } catch (e: Exception) {
                android.util.Log.e("ForegroundAppDetector", "Failed to reset orientation for $packageName", e)
            }
        }
    }

    private fun applyOrientationForApp(packageName: String) {
        serviceScope.launch {
            try {
                // Get current display information
                val displays = displayManager.displays
                if (displays.isEmpty()) return@launch

                // Get default display (usually where the app is shown)
                val defaultDisplay = displays.firstOrNull() ?: return@launch
                val displayId = defaultDisplay.displayId

                // Calculate aspect ratio
                val metrics = android.util.DisplayMetrics()
                defaultDisplay.getMetrics(metrics)
                val aspectRatio = when {
                    metrics.heightPixels > metrics.widthPixels -> AspectRatio.PORTRAIT
                    metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat() < 1.3f -> AspectRatio.SQUARE
                    else -> AspectRatio.LANDSCAPE
                }

                // Get available display IDs
                val availableDisplayIds = displays.map { it.displayId }.toSet()

                // Use smart fallback to get the best orientation setting
                val setting = repository.getEffectiveOrientation(
                    packageName = packageName,
                    currentDisplayId = displayId,
                    currentAspectRatio = aspectRatio,
                    availableDisplayIds = availableDisplayIds
                )

                // Apply the setting if found and enabled
                if (setting != null && setting.enabled) {
                    val intent = Intent(
                        this@ForegroundAppDetectorService,
                        OrientationControlService::class.java
                    ).apply {
                        action = OrientationControlService.ACTION_SET_ORIENTATION
                        putExtra(OrientationControlService.EXTRA_ORIENTATION, setting.orientation.value)
                        putExtra(OrientationControlService.EXTRA_SCREEN_ID, setting.targetScreen.id)
                    }
                    startService(intent)

                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "Applied orientation ${setting.orientation.displayName} for $packageName on display $displayId (fallback strategy used)"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ForegroundAppDetector", "Failed to apply orientation for $packageName", e)
            }
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
