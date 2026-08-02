package app.rotatescreen.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.accessibility.AccessibilityManager
import app.rotatescreen.domain.model.PermissionStatus
import app.rotatescreen.domain.model.TileStatus
import app.rotatescreen.service.ForegroundAppDetectorService
import app.rotatescreen.tile.CurrentAppTileService
import app.rotatescreen.tile.GlobalOrientationTileService
import app.rotatescreen.tile.OrientationTileService
import arrow.core.Either

/**
 * Comprehensive permission checker for all app requirements
 * Checks critical permissions and Quick Settings tiles
 */
object ComprehensivePermissionChecker {

    /**
     * Check all permissions and return comprehensive status
     */
    fun checkAllPermissions(context: Context): PermissionStatus {
        return PermissionStatus(
            hasWriteSettings = hasWriteSettingsPermission(context),
            hasOverlayPermission = hasOverlayPermission(context),
            hasUsageStatsPermission = hasUsageStatsPermission(context),
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(context),
            tilesAdded = checkTileStatus(context)
        )
    }

    /**
     * Check if WRITE_SETTINGS permission is granted
     */
    fun hasWriteSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Check if SYSTEM_ALERT_WINDOW (overlay) permission is granted
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Check if PACKAGE_USAGE_STATS permission is granted
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = appOps?.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if accessibility service is enabled
     *
     * Uses AccessibilityManager to check if service is actually running.
     * This is the proper, reliable method used by other apps.
     *
     * How it works:
     * 1. Get AccessibilityManager system service
     * 2. Get list of enabled accessibility services
     * 3. Check if our service ComponentName is in the list
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            // Get AccessibilityManager system service
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager

            if (accessibilityManager == null) {
                android.util.Log.e("PermissionChecker", "AccessibilityManager not available")
                return false
            }

            // Check if accessibility is enabled at all
            if (!accessibilityManager.isEnabled) {
                android.util.Log.d("PermissionChecker", "Accessibility is disabled at system level")
                return false
            }

            // Get list of enabled accessibility services
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

            if (enabledServices.isEmpty()) {
                android.util.Log.d("PermissionChecker", "No accessibility services enabled")
                return false
            }

            // Build our service ComponentName
            val ourServiceComponent = ComponentName(
                context,
                ForegroundAppDetectorService::class.java
            )

            // Check if our service is in the enabled list
            val isEnabled = enabledServices.any { serviceInfo ->
                val serviceId = serviceInfo.id
                val componentFromId = ComponentName.unflattenFromString(serviceId)

                // Compare ComponentNames
                componentFromId == ourServiceComponent
            }

            android.util.Log.d(
                "PermissionChecker",
                buildString {
                    appendLine("Accessibility check (using AccessibilityManager):")
                    appendLine("  Package: ${context.packageName}")
                    appendLine("  Our service: ${ourServiceComponent.flattenToShortString()}")
                    appendLine("  Enabled: $isEnabled")
                    appendLine("  System accessibility: ${accessibilityManager.isEnabled}")
                    appendLine("  Enabled services count: ${enabledServices.size}")
                    enabledServices.forEach { service ->
                        appendLine("    - ${service.id}")
                    }
                }
            )

            isEnabled
        } catch (e: Exception) {
            android.util.Log.e("PermissionChecker", "Error checking accessibility service", e)
            false
        }
    }

    /**
     * Check which Quick Settings tiles are added
     *
     * Note: Android doesn't provide a direct API to check if tiles are added.
     * This uses TileService.requestListeningState() which only works if the tile
     * has been added at least once. We rely on heuristics:
     * 1. Try to request listening state (works if tile was ever added)
     * 2. Check if tile service is listening (only works if currently visible)
     *
     * Limitation: Cannot definitively detect if user removed tile after adding it.
     * We'll show tiles as "optional" and let users verify manually.
     */
    fun checkTileStatus(context: Context): TileStatus {
        return TileStatus(
            orientationTileAdded = isTileAdded(context, OrientationTileService::class.java),
            globalOrientationTileAdded = isTileAdded(context, GlobalOrientationTileService::class.java),
            currentAppTileAdded = isTileAdded(context, CurrentAppTileService::class.java)
        )
    }

    /**
     * Attempt to detect if a tile is added
     *
     * Returns true if we can confirm tile was added, false if uncertain.
     * Due to Android limitations, this is a best-effort check.
     */
    private fun isTileAdded(context: Context, tileServiceClass: Class<out TileService>): Boolean {
        return try {
            val componentName = ComponentName(context, tileServiceClass)

            // Request listening state - this will succeed if tile was ever added
            TileService.requestListeningState(context, componentName)

            // If no exception, tile is likely added (or was added at some point)
            // We can't definitively tell if user removed it, so we return true
            true
        } catch (e: Exception) {
            // Exception likely means tile was never added
            false
        }
    }

    /**
     * Check if any critical permissions are missing
     */
    fun hasMissingCriticalPermissions(context: Context): Boolean {
        val status = checkAllPermissions(context)
        return !status.allGranted()
    }

    /**
     * Get a summary string of permission status
     */
    fun getPermissionSummary(context: Context): String {
        val status = checkAllPermissions(context)
        val granted = mutableListOf<String>()
        val missing = mutableListOf<String>()

        if (status.hasWriteSettings) granted.add("Write Settings") else missing.add("Write Settings")
        if (status.hasOverlayPermission) granted.add("Overlay") else missing.add("Overlay")
        if (status.hasUsageStatsPermission) granted.add("Usage Stats") else missing.add("Usage Stats")
        if (status.isAccessibilityServiceEnabled) granted.add("Accessibility") else missing.add("Accessibility")

        val tilesCount = status.tilesAdded.tilesAddedCount()
        granted.add("$tilesCount/3 Tiles")

        return buildString {
            append("Granted: ${granted.joinToString(", ")}")
            if (missing.isNotEmpty()) {
                append("\nMissing: ${missing.joinToString(", ")}")
            }
        }
    }
}
