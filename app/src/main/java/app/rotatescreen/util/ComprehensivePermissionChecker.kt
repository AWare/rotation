package app.rotatescreen.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.service.quicksettings.TileService
import app.rotatescreen.domain.model.PermissionStatus
import app.rotatescreen.domain.model.TileStatus
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Check if SYSTEM_ALERT_WINDOW (overlay) permission is granted
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Check if PACKAGE_USAGE_STATS permission is granted
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps?.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return AccessibilityChecker.isAccessibilityServiceEnabled(context).fold(
            { false },
            { it }
        )
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileStatus(
                orientationTileAdded = isTileAdded(context, OrientationTileService::class.java),
                globalOrientationTileAdded = isTileAdded(context, GlobalOrientationTileService::class.java),
                currentAppTileAdded = isTileAdded(context, CurrentAppTileService::class.java)
            )
        } else {
            // Quick Settings tiles not available on older versions
            TileStatus.ALL // Assume OK on older versions
        }
    }

    /**
     * Attempt to detect if a tile is added
     *
     * Returns true if we can confirm tile was added, false if uncertain.
     * Due to Android limitations, this is a best-effort check.
     */
    private fun isTileAdded(context: Context, tileServiceClass: Class<out TileService>): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val componentName = ComponentName(context, tileServiceClass)

                // Request listening state - this will succeed if tile was ever added
                TileService.requestListeningState(context, componentName)

                // If no exception, tile is likely added (or was added at some point)
                // We can't definitively tell if user removed it, so we return true
                true
            } else {
                true // Not applicable on older versions
            }
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
