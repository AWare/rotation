package app.rotatescreen.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.accessibility.AccessibilityEvent
import arrow.core.Either
import arrow.core.raise.either
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.repository.OrientationRepository
import app.rotatescreen.domain.model.AspectRatio
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Accessibility service that detects foreground app changes and applies per-app orientation settings
 * Uses FP principles with immutable state transitions
 */
class ForegroundAppDetectorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var repository: OrientationRepository? = null

    // FP: Use immutable state transitions via StateFlow
    private val _currentPackageName = MutableStateFlow<String?>(null)
    private val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

    private val _previousPackageName = MutableStateFlow<String?>(null)

    private val displayManager by lazy {
        Either.catch {
            getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        }.getOrNull()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val database = RotationDatabase.getInstance(applicationContext)
            repository = OrientationRepository(database.appOrientationDao())
            android.util.Log.d("ForegroundAppDetector", "Service initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("ForegroundAppDetector", "Failed to initialize service", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // FP: Pure validation
        val packageName = extractPackageName(event) ?: return

        // FP: Avoid processing if package unchanged
        if (packageName == _currentPackageName.value) return

        // FP: Immutable state transition
        handleAppSwitch(packageName)
    }

    /**
     * FP: Pure function to extract package name from event
     */
    private fun extractPackageName(event: AccessibilityEvent?): String? =
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()
        } else null

    /**
     * FP: Immutable state transition for app switch
     */
    private fun handleAppSwitch(newPackageName: String) {
        val previous = _currentPackageName.value
        val wasPrevious = _previousPackageName.value

        // State transition
        _previousPackageName.value = previous
        _currentPackageName.value = newPackageName

        // Side effects in separate functions
        previous?.let { prevPkg ->
            if (prevPkg != newPackageName) {
                resetOrientationForApp(prevPkg)
            }
        }

        applyOrientationForApp(newPackageName)
    }

    private fun resetOrientationForApp(packageName: String) {
        val repo = repository
        if (repo == null) {
            android.util.Log.w("ForegroundAppDetector", "Repository not initialized, cannot reset orientation")
            return
        }

        serviceScope.launch {
            Either.catch {
                android.util.Log.d("ForegroundAppDetector", "Resetting orientation for backgrounded app: $packageName")

                // Get all settings for this app
                val settings = repo.getSetting(packageName).getOrNull() ?: return@catch

                // Only reset if app has per-screen settings (not "All Screens")
                val hasPerScreenSettings = settings.any { it.targetScreen.id != -1 }

                if (hasPerScreenSettings) {
                    // Reset to system default (Unspecified/Auto)
                    displayManager?.displays?.forEach { display ->
                        sendOrientationIntent(
                            ScreenOrientation.Unspecified.value,
                            display.displayId
                        )
                    }

                    android.util.Log.d("ForegroundAppDetector", "Reset orientation for $packageName")
                }
            }.mapLeft { e ->
                android.util.Log.e("ForegroundAppDetector", "Failed to reset orientation for $packageName", e)
            }
        }
    }

    private fun applyOrientationForApp(packageName: String) {
        val repo = repository
        val dispManager = displayManager

        if (repo == null || dispManager == null) {
            android.util.Log.w("ForegroundAppDetector", "Service not ready, cannot apply orientation")
            return
        }

        serviceScope.launch {
            Either.catch {
                // FP: Extract display info as data
                val displayInfo = getDisplayInfo(dispManager).bind()

                // Use smart fallback to get the best orientation setting
                val setting = repo.getEffectiveOrientation(
                    packageName = packageName,
                    currentDisplayId = displayInfo.displayId,
                    currentAspectRatio = displayInfo.aspectRatio,
                    availableDisplayIds = displayInfo.availableDisplayIds
                )

                // Apply the setting if found and enabled
                setting?.takeIf { it.enabled }?.let { s ->
                    sendOrientationIntent(s.orientation.value, s.targetScreen.id)

                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "Applied orientation ${s.orientation.displayName} for $packageName on display ${displayInfo.displayId}"
                    )
                }
            }.mapLeft { e ->
                android.util.Log.e("ForegroundAppDetector", "Failed to apply orientation for $packageName", e)
            }
        }
    }

    /**
     * FP: Pure data class for display information
     */
    private data class DisplayInfo(
        val displayId: Int,
        val aspectRatio: AspectRatio,
        val availableDisplayIds: Set<Int>
    )

    /**
     * FP: Extract display information as pure data
     */
    private fun getDisplayInfo(dispManager: DisplayManager): Either<Throwable, DisplayInfo> = either {
        val displays = dispManager.displays
        ensure(displays.isNotEmpty()) { IllegalStateException("No displays available") }

        val defaultDisplay = displays.first()
        val displayId = defaultDisplay.displayId

        // FP: Pure aspect ratio calculation
        val aspectRatio = calculateAspectRatio(defaultDisplay).bind()

        val availableDisplayIds = displays.map { it.displayId }.toSet()

        DisplayInfo(displayId, aspectRatio, availableDisplayIds)
    }

    /**
     * FP: Pure function to calculate aspect ratio from display
     */
    private fun calculateAspectRatio(display: android.view.Display): Either<Throwable, AspectRatio> = Either.catch {
        val metrics = android.util.DisplayMetrics()
        display.getMetrics(metrics)

        when {
            metrics.heightPixels > metrics.widthPixels -> AspectRatio.PORTRAIT
            metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat() < 1.3f -> AspectRatio.SQUARE
            else -> AspectRatio.LANDSCAPE
        }
    }

    /**
     * FP: Side effect isolated - send intent to orientation service
     */
    private fun sendOrientationIntent(orientationValue: Int, screenId: Int) {
        Either.catch {
            val intent = Intent(
                this@ForegroundAppDetectorService,
                OrientationControlService::class.java
            ).apply {
                action = OrientationControlService.ACTION_SET_ORIENTATION
                putExtra(OrientationControlService.EXTRA_ORIENTATION, orientationValue)
                putExtra(OrientationControlService.EXTRA_SCREEN_ID, screenId)
            }
            startService(intent)
        }.mapLeft { e ->
            android.util.Log.e("ForegroundAppDetector", "Failed to send orientation intent", e)
        }
    }

    override fun onInterrupt() {
        // Required override
        android.util.Log.d("ForegroundAppDetector", "Service interrupted")
    }

    override fun onDestroy() {
        android.util.Log.d("ForegroundAppDetector", "Service destroyed")
        serviceScope.cancel()
        repository = null
        super.onDestroy()
    }
}
