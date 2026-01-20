package app.rotatescreen.tile

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.repository.OrientationRepository
import app.rotatescreen.domain.model.AppOrientationSetting
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import app.rotatescreen.service.OrientationControlService
import app.rotatescreen.service.OrientationSelectorOverlayService
import app.rotatescreen.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile for current app orientation control
 */
class CurrentAppTileService : TileService() {

    private var serviceScope: CoroutineScope? = null
    private var repository: OrientationRepository? = null
    private var currentAppPackage: String? = null
    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private val orientationCycle = listOf(
        ScreenOrientation.Unspecified,
        ScreenOrientation.Portrait,
        ScreenOrientation.Landscape,
        ScreenOrientation.Sensor,
        ScreenOrientation.ReversePortrait,
        ScreenOrientation.ReverseLandscape
    )

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val database = RotationDatabase.getInstance(applicationContext)
        repository = OrientationRepository(database.appOrientationDao())
    }

    override fun onStartListening() {
        super.onStartListening()
        updateCurrentApp()
    }

    override fun onClick() {
        super.onClick()

        // First check permission
        val hasPermission = checkUsageStatsPermission()
        if (!hasPermission) {
            android.util.Log.w("CurrentAppTileService", "No usage stats permission - opening settings")

            // Show helpful message and open settings
            android.widget.Toast.makeText(
                this,
                "Grant Usage Access permission to detect current app",
                android.widget.Toast.LENGTH_LONG
            ).show()

            // Open Usage Access settings
            try {
                val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("CurrentAppTileService", "Failed to open Usage Access settings", e)
            }

            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = "Needs Usage Access"
                contentDescription = "Grant Usage Access permission to detect current app"
                updateTile()
            }
            return
        }

        // Refresh current app detection
        updateCurrentApp()

        val packageName = currentAppPackage
        android.util.Log.d("CurrentAppTileService", "onClick: packageName=$packageName, hasPermission=$hasPermission")

        if (packageName != null) {
            // Just cycle through orientations and save
            cycleOrientation(packageName)
        } else {
            android.util.Log.w("CurrentAppTileService", "No current app package detected")

            // Show info message
            android.widget.Toast.makeText(
                this,
                "No foreground app detected. Open an app first.",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = "Current App"
                contentDescription = "No foreground app detected"
                updateTile()
            }
        }
    }

    private fun cycleOrientation(packageName: String) {
        val scope = serviceScope
        val repo = repository

        if (scope == null || repo == null) {
            android.util.Log.e("CurrentAppTileService", "Service not initialized - scope=$scope, repo=$repo")
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = "Not Ready"
                updateTile()
            }
            return
        }

        scope.launch {
            try {
                android.util.Log.d("CurrentAppTileService", "Cycling orientation for $packageName")

                // Get current setting (use first one or default)
                val currentSettingList = repo.getSetting(packageName).getOrNull()
                val currentSetting = currentSettingList?.firstOrNull()
                val currentOrientation = currentSetting?.orientation ?: ScreenOrientation.Unspecified

                // Find next orientation
                val currentIndex = orientationCycle.indexOf(currentOrientation)
                val nextIndex = (currentIndex + 1) % orientationCycle.size
                val nextOrientation = orientationCycle[nextIndex]

                android.util.Log.d("CurrentAppTileService", "Cycling from ${currentOrientation.displayName} to ${nextOrientation.displayName}")

                // Get app name
                val appName = try {
                    packageManager.getApplicationInfo(packageName, 0)
                        .loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    packageName
                }

                // Get target screen (use first display if no setting)
                val targetScreen = currentSetting?.targetScreen ?: TargetScreen.AllScreens

                // Save setting
                val newSetting = AppOrientationSetting.create(
                    packageName = packageName,
                    appName = appName,
                    orientation = nextOrientation,
                    targetScreen = targetScreen
                )
                repo.saveSetting(newSetting)
                android.util.Log.d("CurrentAppTileService", "Saved setting for $appName: ${nextOrientation.displayName}")

                // Apply the orientation immediately
                val intent = Intent(this@CurrentAppTileService, OrientationControlService::class.java).apply {
                    action = OrientationControlService.ACTION_SET_ORIENTATION
                    putExtra(OrientationControlService.EXTRA_ORIENTATION, nextOrientation.value)
                    putExtra(OrientationControlService.EXTRA_SCREEN_ID, targetScreen.id)
                }
                startService(intent)
                android.util.Log.d("CurrentAppTileService", "Applied orientation")

                // Update tile
                updateTileForApp(packageName, appName, nextOrientation)

                // Show toast feedback
                android.widget.Toast.makeText(
                    this@CurrentAppTileService,
                    "$appName: ${nextOrientation.displayName}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("CurrentAppTileService", "Error cycling orientation", e)
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    label = "Error: ${e.message}"
                    updateTile()
                }
            }
        }
    }

    private fun updateCurrentApp() {
        // First check if we have usage stats permission
        val hasPermission = checkUsageStatsPermission()
        if (!hasPermission) {
            android.util.Log.w("CurrentAppTileService", "Usage stats permission not granted")
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = "Current App"
                contentDescription = "Tap to grant Usage Access permission"
                updateTile()
            }
            return
        }

        val packageName = getCurrentForegroundApp()
        android.util.Log.d("CurrentAppTileService", "updateCurrentApp: detected packageName=$packageName")

        val scope = serviceScope
        val repo = repository

        if (packageName != null && packageName != this.packageName) {
            currentAppPackage = packageName

            if (scope == null || repo == null) {
                android.util.Log.w("CurrentAppTileService", "Service not initialized in updateCurrentApp")
                return
            }

            scope.launch {
                val currentSettingList = repo.getSetting(packageName).getOrNull()
                val currentSetting = currentSettingList?.firstOrNull()
                val appName = try {
                    packageManager.getApplicationInfo(packageName, 0)
                        .loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    packageName
                }
                updateTileForApp(
                    packageName,
                    appName,
                    currentSetting?.orientation ?: ScreenOrientation.Unspecified
                )
            }
        } else {
            android.util.Log.w("CurrentAppTileService", "No foreground app detected (packageName=$packageName)")
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = "Current App"
                contentDescription = "No foreground app detected"
                updateTile()
            }
        }
    }

    /**
     * Check if usage stats permission is granted
     */
    private fun checkUsageStatsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return false
        }

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            ?: return false

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 // Last minute

        // Try to query usage stats - if we can't, we don't have permission
        return try {
            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e("CurrentAppTileService", "Permission check failed", e)
            false
        }
    }

    /**
     * Get the current foreground app package using UsageEvents API
     * This is more reliable than queryUsageStats for real-time detection
     * FP: Uses immutable data structures and pure transformations
     */
    private fun getCurrentForegroundApp(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            android.util.Log.w("CurrentAppTileService", "UsageStats not available on this Android version")
            return null
        }

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
        if (usageStatsManager == null) {
            android.util.Log.e("CurrentAppTileService", "UsageStatsManager not available")
            return null
        }

        return try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 * 10 // Last 10 minutes (increased from 5)

            // FP: Extract events to immutable list
            val foregroundEvents = extractForegroundEvents(usageStatsManager, startTime, endTime)

            android.util.Log.d("CurrentAppTileService", "Found ${foregroundEvents.size} foreground events")

            if (foregroundEvents.isEmpty()) {
                android.util.Log.w("CurrentAppTileService", "No foreground events found in last 10 minutes")
                return null
            }

            // FP: Find most recent using pure function (maxByOrNull)
            val mostRecent = foregroundEvents.maxByOrNull { it.timestamp }

            android.util.Log.d("CurrentAppTileService", "Detected foreground app: ${mostRecent?.packageName} (at ${mostRecent?.timestamp})")
            mostRecent?.packageName
        } catch (e: Exception) {
            android.util.Log.e("CurrentAppTileService", "Error querying usage events", e)
            null
        }
    }

    /**
     * FP: Pure data class for foreground event
     */
    private data class ForegroundEvent(
        val packageName: String,
        val timestamp: Long
    )

    /**
     * FP: Extract foreground events to immutable list
     */
    private fun extractForegroundEvents(
        usageStatsManager: android.app.usage.UsageStatsManager,
        startTime: Long,
        endTime: Long
    ): List<ForegroundEvent> {
        val events = mutableListOf<ForegroundEvent>()
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        while (usageEvents.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            usageEvents.getNextEvent(event)

            // Filter for foreground events
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                events.add(ForegroundEvent(event.packageName, event.timeStamp))
            }
        }

        return events.toList() // Convert to immutable list
    }

    private fun updateTileForApp(packageName: String, appName: String, orientation: ScreenOrientation) {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "$appName: ${orientation.displayName}"
            contentDescription = "Current app: $appName, Orientation: ${orientation.displayName}. Long press to configure."

            // For Android 13+, set a PendingIntent to open PerApp screen with [open] filter
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(this@CurrentAppTileService, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_FILTER, MainActivity.FILTER_OPEN)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    this@CurrentAppTileService,
                    "open_apps_filter".hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setActivityLaunchForClick(pendingIntent)
            }

            updateTile()
        }
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        repository = null
        super.onDestroy()
    }
}
