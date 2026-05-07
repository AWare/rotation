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

    // Debouncing: track last event timestamp. Volatile because accessibility
    // events can be delivered from arbitrary threads and the read-modify-write
    // below would otherwise race.
    @Volatile
    private var lastEventTimestamp = 0L
    private val debounceDelayMs = 150L // Ignore events within 150ms of each other

    /**
     * Track what orientation is currently applied to each screen
     * Key: Display ID, Value: Applied orientation state
     * This allows us to know exactly which screens to reset when leaving an app
     */
    private val appliedOrientations = MutableStateFlow<Map<Int, AppliedOrientationState>>(emptyMap())

    /**
     * Data class representing the orientation state applied to a specific screen
     */
    private data class AppliedOrientationState(
        val packageName: String,
        val orientation: ScreenOrientation,
        val targetScreenId: Int
    )

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
     *
     * Algorithm:
     * 1. RESET PHASE: Reset all screens that had custom orientations from previous app
     * 2. APPLY PHASE: Apply new orientations for the new app (or global if launcher)
     */
    @Synchronized
    private fun handleAppSwitch(newPackageName: String) {
        val previous = _currentPackageName.value
        val launchers = launcherPackages.value

        // FP: Use pure helper functions for state checks
        val isEnteringLauncher = ForegroundAppDetectorHelpers.isLauncherPackage(newPackageName, launchers) ||
                                 ForegroundAppDetectorHelpers.isSystemLauncher(newPackageName)
        val leaving = ForegroundAppDetectorHelpers.isLeavingApp(previous, newPackageName)

        // Atomic state transition
        _previousPackageName.value = previous
        _currentPackageName.value = newPackageName

        android.util.Log.d(
            "ForegroundAppDetector",
            "App switch: $previous → $newPackageName (launcher: $isEnteringLauncher)"
        )

        // === PHASE 1: RESET ===
        // Always reset orientations from previous app to prevent persistence
        if (leaving) {
            previous?.let { prevPkg ->
                resetAllOrientationsForApp(prevPkg)
            }
        }

        // === PHASE 2: APPLY ===
        if (isEnteringLauncher) {
            // Entering launcher - apply global orientation to all screens
            android.util.Log.d("ForegroundAppDetector", "Entering launcher, applying global orientation")
            applyGlobalOrientation()
        } else {
            // Entering regular app - apply per-app orientation settings
            applyOrientationForApp(newPackageName)
        }
    }

    /**
     * Reset all orientations that were applied for a specific app
     * This ensures orientations don't persist after leaving the app
     *
     * Algorithm:
     * 1. Get all screens that currently have orientations applied for this app
     * 2. For each screen, restore to global orientation preference
     * 3. Clear from tracking map
     */
    private fun resetAllOrientationsForApp(packageName: String) {
        val prefManager = preferencesManager
        val dispManager = displayManager

        if (prefManager == null || dispManager == null) {
            android.util.Log.w("ForegroundAppDetector", "Service not initialized, cannot reset orientation")
            return
        }

        serviceScope.launch {
            Either.catch {
                // Get all screens that have orientations applied for this package
                val currentApplied = appliedOrientations.value
                val screensToReset = currentApplied.filter { it.value.packageName == packageName }

                if (screensToReset.isEmpty()) {
                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "No orientations to reset for $packageName"
                    )
                    return@catch
                }

                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Resetting ${screensToReset.size} screen(s) for $packageName: ${screensToReset.keys}"
                )

                // Get the user's global orientation preference
                val globalOrientation = prefManager.globalOrientation.first()

                // Reset each screen to global orientation
                screensToReset.forEach { (displayId, state) ->
                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "Resetting Display $displayId: ${state.orientation.displayName} → ${globalOrientation.displayName}"
                    )

                    sendOrientationIntent(
                        globalOrientation.value,
                        displayId
                    )
                }

                // Remove from tracking map
                val newAppliedMap = currentApplied.filterKeys { !screensToReset.containsKey(it) }
                appliedOrientations.value = newAppliedMap

                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Reset complete. Remaining tracked screens: ${newAppliedMap.keys}"
                )
            }.mapLeft { e ->
                android.util.Log.e("ForegroundAppDetector", "Failed to reset orientations for $packageName", e)
            }
        }
    }

    /**
     * Apply per-app orientation settings
     *
     * Algorithm:
     * 1. Get all orientation settings for this app
     * 2. For each enabled setting:
     *    a. Apply orientation to target screen
     *    b. Add to tracking map
     * 3. Handle "All Screens" setting by applying to all available displays
     */
    private fun applyOrientationForApp(packageName: String) {
        val repo = repository
        val dispManager = displayManager

        if (repo == null || dispManager == null) {
            android.util.Log.w("ForegroundAppDetector", "Service not ready, cannot apply orientation")
            return
        }

        serviceScope.launch {
            Either.catch {
                // Get all settings for this app
                val allSettings = repo.getSetting(packageName).getOrNull() ?: emptyList()
                val enabledSettings = allSettings.filter { it.enabled }

                if (enabledSettings.isEmpty()) {
                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "No enabled orientation settings for $packageName"
                    )
                    return@catch
                }

                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Applying ${enabledSettings.size} orientation setting(s) for $packageName"
                )

                val newTracking = mutableMapOf<Int, AppliedOrientationState>()

                enabledSettings.forEach { setting ->
                    if (setting.targetScreen.id == -1) {
                        // "All Screens" - apply to all displays
                        dispManager.displays?.forEach { display ->
                            val displayId = display.displayId

                            android.util.Log.d(
                                "ForegroundAppDetector",
                                "Applying ${setting.orientation.displayName} to Display $displayId (All Screens)"
                            )

                            sendOrientationIntent(setting.orientation.value, displayId)

                            newTracking[displayId] = AppliedOrientationState(
                                packageName = packageName,
                                orientation = setting.orientation,
                                targetScreenId = displayId
                            )
                        }
                    } else {
                        // Specific screen
                        val displayId = setting.targetScreen.id

                        android.util.Log.d(
                            "ForegroundAppDetector",
                            "Applying ${setting.orientation.displayName} to Display $displayId"
                        )

                        sendOrientationIntent(setting.orientation.value, displayId)

                        newTracking[displayId] = AppliedOrientationState(
                            packageName = packageName,
                            orientation = setting.orientation,
                            targetScreenId = displayId
                        )
                    }
                }

                // Update tracking map with new applied orientations
                appliedOrientations.value = appliedOrientations.value + newTracking

                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Tracking ${appliedOrientations.value.size} applied orientation(s) across displays: ${appliedOrientations.value.keys}"
                )
            }.mapLeft { e ->
                android.util.Log.e("ForegroundAppDetector", "Failed to apply orientations for $packageName", e)
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
     * Apply the user's global orientation preference to all screens
     * Used when entering launcher - no tracking needed as this is the default state
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
                    "Applying global orientation: ${globalOrientation.displayName} to all displays"
                )

                // Apply to all displays
                dispManager.displays?.forEach { display ->
                    android.util.Log.d(
                        "ForegroundAppDetector",
                        "Global orientation ${globalOrientation.displayName} → Display ${display.displayId}"
                    )
                    sendOrientationIntent(globalOrientation.value, display.displayId)
                }

                // No tracking needed - global orientation is the default state
                android.util.Log.d(
                    "ForegroundAppDetector",
                    "Global orientation applied, no tracking needed"
                )
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
