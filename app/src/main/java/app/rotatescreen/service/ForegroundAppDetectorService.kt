package app.rotatescreen.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.view.accessibility.AccessibilityEvent
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.preferences.PreferencesManager
import app.rotatescreen.data.repository.OrientationRepository
import app.rotatescreen.domain.model.AspectRatio
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Accessibility service that detects foreground app changes and applies per-app orientation settings
 * Uses FP principles with immutable state transitions
 */
class ForegroundAppDetectorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var repository: OrientationRepository? = null
    private var preferencesManager: PreferencesManager? = null

    // FP: Use immutable state transitions via StateFlow
    private val _currentPackageName = MutableStateFlow<String?>(null)
    private val _previousPackageName = MutableStateFlow<String?>(null)

    // Track launcher packages for better exit detection
    private val launcherPackages = MutableStateFlow<Set<String>>(emptySet())

    // Debouncing: track last event timestamp
    private var lastEventTimestamp = 0L
    private val debounceDelayMs = 150L // Ignore events within 150ms of each other

    // Track current applied orientation for proper restoration
    private val _currentAppliedOrientation = MutableStateFlow<ScreenOrientation?>(null)

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
            preferencesManager = PreferencesManager(applicationContext)

            // Detect launcher packages for better exit detection
            detectLauncherPackages()

            android.util.Log.d("ForegroundAppDetector", "Service initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("ForegroundAppDetector", "Failed to initialize service", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // FP: Pure validation using helper functions
        val packageName = ForegroundAppDetectorHelpers.extractPackageName(event) ?: return

        // FP: Debouncing - ignore rapid-fire events
        val currentTime = System.currentTimeMillis()
        if (ForegroundAppDetectorHelpers.shouldDebounce(currentTime, lastEventTimestamp, debounceDelayMs)) {
            android.util.Log.d("ForegroundAppDetector", "Debounced event for $packageName")
            return
        }
        lastEventTimestamp = currentTime

        // FP: Avoid processing if package unchanged
        if (ForegroundAppDetectorHelpers.isSamePackage(packageName, _currentPackageName.value)) return

        // FP: Immutable state transition
        handleAppSwitch(packageName)
    }

    /**
     * FP: Immutable state transition for app switch
     * Synchronized to prevent race conditions from rapid accessibility events
     */
    @Synchronized
    private fun handleAppSwitch(newPackageName: String) {
        val previous = _currentPackageName.value
        val launchers = launcherPackages.value

        // FP: Use pure helper functions for state checks
        val isEnteringLauncher = ForegroundAppDetectorHelpers.isLauncherPackage(newPackageName, launchers)
        val leaving = ForegroundAppDetectorHelpers.isLeavingApp(previous, newPackageName)

        // Atomic state transition
        _previousPackageName.value = previous
        _currentPackageName.value = newPackageName

        // Side effects in separate functions
        if (leaving) {
            previous?.let { prevPkg ->
                // Better exit detection: always reset when leaving an app
                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Leaving app: $prevPkg, entering: $newPackageName (launcher: $isEnteringLauncher)"
                )
                resetOrientationForApp(prevPkg, isEnteringLauncher)
            }
        }

        // Only apply orientation if NOT entering launcher (launchers should use global orientation)
        if (!isEnteringLauncher) {
            applyOrientationForApp(newPackageName)
        } else {
            // Entering launcher - apply global orientation
            android.util.Log.d("ForegroundAppDetector", "Entering launcher, applying global orientation")
            applyGlobalOrientation()
        }
    }

    /**
     * Reset orientation when leaving an app
     * Restores to global orientation preference instead of system default
     */
    private fun resetOrientationForApp(packageName: String, isEnteringLauncher: Boolean) {
        val repo = repository
        val prefManager = preferencesManager

        if (repo == null || prefManager == null) {
            android.util.Log.w("ForegroundAppDetector", "Service not initialized, cannot reset orientation")
            return
        }

        serviceScope.launch {
            Either.catch {
                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Resetting orientation for backgrounded app: $packageName (entering launcher: $isEnteringLauncher)"
                )

                // Get all settings for this app
                val settings = repo.getSetting(packageName).getOrNull() ?: return@catch

                // FP: Use pure helper function to check custom orientation
                if (ForegroundAppDetectorHelpers.hasCustomOrientation(settings)) {
                    // Get the user's global orientation preference
                    val globalOrientation = prefManager.globalOrientation.first()

                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "Restoring to global orientation: ${globalOrientation.displayName}"
                    )

                    // Restore to global orientation on all displays
                    displayManager?.displays?.forEach { display ->
                        sendOrientationIntent(
                            globalOrientation.value,
                            display.displayId
                        )
                    }

                    // Track that we've restored to global orientation
                    _currentAppliedOrientation.value = globalOrientation

                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "Reset orientation for $packageName to ${globalOrientation.displayName}"
                    )
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
            either {
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

                    // Track the applied orientation
                    _currentAppliedOrientation.value = s.orientation

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

    /**
     * Detect all launcher (home screen) packages
     * These are apps that handle the HOME intent category
     */
    private fun detectLauncherPackages() {
        serviceScope.launch(Dispatchers.IO) {
            Either.catch {
                val pm = packageManager
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }

                val launchers = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(
                        homeIntent,
                        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                }

                val launcherPackageNames = launchers.mapNotNull { it.activityInfo?.packageName }.toSet()
                launcherPackages.value = launcherPackageNames

                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Detected ${launcherPackageNames.size} launcher packages: $launcherPackageNames"
                )
            }.mapLeft { e ->
                android.util.Log.e("ForegroundAppDetector", "Failed to detect launcher packages", e)
            }
        }
    }

    /**
     * Apply the user's global orientation preference
     * Used when entering launcher or when no app-specific setting exists
     */
    private fun applyGlobalOrientation() {
        val prefManager = preferencesManager
        val dispManager = displayManager

        if (prefManager == null || dispManager == null) {
            android.util.Log.w("ForegroundAppDetector", "Service not ready, cannot apply global orientation")
            return
        }

        serviceScope.launch {
            Either.catch {
                val globalOrientation = prefManager.globalOrientation.first()

                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Applying global orientation: ${globalOrientation.displayName}"
                )

                // Apply to all displays
                dispManager.displays?.forEach { display ->
                    sendOrientationIntent(globalOrientation.value, display.displayId)
                }

                _currentAppliedOrientation.value = globalOrientation
            }.mapLeft { e ->
                android.util.Log.e("ForegroundAppDetector", "Failed to apply global orientation", e)
            }
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
        preferencesManager = null
        super.onDestroy()
    }
}
