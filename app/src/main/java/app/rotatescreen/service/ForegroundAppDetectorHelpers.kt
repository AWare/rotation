package app.rotatescreen.service

import android.view.accessibility.AccessibilityEvent
import app.rotatescreen.domain.model.AppOrientationSetting

/**
 * Pure helper functions for ForegroundAppDetectorService
 * Extracted for testability and FP principles
 */
object ForegroundAppDetectorHelpers {

    /**
     * FP: Pure function to extract package name from event
     */
    fun extractPackageName(event: AccessibilityEvent?): String? =
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()
        } else null

    /**
     * FP: Pure function to determine if event should be debounced
     * @param currentTime Current timestamp in milliseconds
     * @param lastTime Last processed event timestamp in milliseconds
     * @param debounceDelay Minimum time between events in milliseconds
     * @return true if event should be ignored (debounced), false otherwise
     */
    fun shouldDebounce(currentTime: Long, lastTime: Long, debounceDelay: Long): Boolean =
        currentTime - lastTime < debounceDelay

    /**
     * FP: Pure function to check if package names are the same
     * @param pkg1 First package name (nullable)
     * @param pkg2 Second package name (nullable)
     * @return true if both packages are identical
     */
    fun isSamePackage(pkg1: String?, pkg2: String?): Boolean =
        pkg1 == pkg2

    /**
     * FP: Pure function to check if package is a launcher
     * @param packageName Package name to check
     * @param launchers Set of known launcher package names
     * @return true if package is a launcher
     */
    fun isLauncherPackage(packageName: String, launchers: Set<String>): Boolean =
        launchers.contains(packageName)

    /**
     * FP: Pure function to check if leaving an app
     * @param previous Previous package name
     * @param current Current package name
     * @return true if switching from one app to another
     */
    fun isLeavingApp(previous: String?, current: String): Boolean =
        previous != null && previous != current

    /**
     * FP: Pure function to check if app has custom orientation
     * @param settings List of app orientation settings
     * @return true if any setting is enabled
     */
    fun hasCustomOrientation(settings: List<AppOrientationSetting>): Boolean =
        settings.any { it.enabled }

    /**
     * FP: Pure function to check if package is a known system launcher
     * @param packageName Package name to check
     * @return true if package is a known system launcher
     */
    fun isSystemLauncher(packageName: String): Boolean =
        packageName.contains("launcher", ignoreCase = true) ||
        packageName == "com.android.launcher3" ||
        packageName == "com.google.android.apps.nexuslauncher" ||
        packageName == "com.teslacoilsw.launcher" || // Nova Launcher
        packageName == "ch.deletescape.lawnchair.plah" // Lawnchair
}
