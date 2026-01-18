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

        val packageName = currentAppPackage
        android.util.Log.d("CurrentAppTileService", "onClick: packageName=$packageName")

        if (packageName != null) {
            // Just cycle through orientations and save
            cycleOrientation(packageName)
        } else {
            android.util.Log.w("CurrentAppTileService", "No current app package - trying to refresh")
            // Try to detect the app again
            updateCurrentApp()
            // Show helpful message
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = "Tap to open app"
                contentDescription = "No foreground app detected. Grant Usage Access permission in app settings."
                updateTile()
            }
        }
    }

    private fun cycleOrientation(packageName: String) {
        serviceScope?.launch {
            try {
                android.util.Log.d("CurrentAppTileService", "Cycling orientation for $packageName")

                // Get current setting (use first one or default)
                val currentSettingList = repository?.getSetting(packageName)?.getOrNull()
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
                repository?.saveSetting(newSetting)
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
        val activityManager = getSystemService(ACTIVITY_SERVICE)
        if (activityManager is ActivityManager) {
            val packageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usageService = getSystemService(USAGE_STATS_SERVICE)
                if (usageService is android.app.usage.UsageStatsManager) {
                    val endTime = System.currentTimeMillis()
                    val startTime = endTime - 1000 * 10 // Last 10 seconds
                    usageService.queryUsageStats(
                        android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                        startTime,
                        endTime
                    )?.maxByOrNull { it.lastTimeUsed }?.packageName
                } else null
            } else null

            if (packageName != null && packageName != this.packageName) {
                currentAppPackage = packageName
                serviceScope?.launch {
                    val currentSettingList = repository?.getSetting(packageName)?.getOrNull()
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
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    label = "Current App"
                    contentDescription = "No foreground app detected"
                    updateTile()
                }
            }
        }
    }

    private fun updateTileForApp(packageName: String, appName: String, orientation: ScreenOrientation) {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "$appName: ${orientation.displayName}"
            contentDescription = "Current app: $appName, Orientation: ${orientation.displayName}. Long press to configure."

            // For Android 13+, set a PendingIntent to open MainActivity with the package
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(this@CurrentAppTileService, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_TARGET_PACKAGE, packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    this@CurrentAppTileService,
                    packageName.hashCode(),
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
