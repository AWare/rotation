package app.rotatescreen.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Display
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.preferences.PreferencesManager
import app.rotatescreen.data.repository.OrientationRepository
import app.rotatescreen.domain.model.*
import app.rotatescreen.service.OrientationControlService
import app.rotatescreen.util.AccessibilityChecker
import app.rotatescreen.util.PermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing orientation state using FP style
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val repository: OrientationRepository
    private val preferencesManager: PreferencesManager
    private val displayManager: DisplayManager by lazy {
        val service = context.getSystemService(Context.DISPLAY_SERVICE)
        if (service is DisplayManager) service
        else throw IllegalStateException("DisplayManager not available")
    }

    private val _state = MutableStateFlow(OrientationState())
    val state: StateFlow<OrientationState> = _state.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _availableScreens = MutableStateFlow<List<TargetScreen>>(listOf(TargetScreen.AllScreens))
    val availableScreens: StateFlow<List<TargetScreen>> = _availableScreens.asStateFlow()

    private val _selectedGlobalScreen = MutableStateFlow<TargetScreen>(TargetScreen.AllScreens)
    val selectedGlobalScreen: StateFlow<TargetScreen> = _selectedGlobalScreen.asStateFlow()

    private val _selectedAppScreens = MutableStateFlow<Map<String, TargetScreen>>(emptyMap())

    val filteredApps: StateFlow<List<InstalledApp>> = combine(
        installedApps,
        searchQuery
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            android.util.Log.d("MainViewModel", "Display added: $displayId")
            loadAvailableDisplays()
        }

        override fun onDisplayRemoved(displayId: Int) {
            android.util.Log.d("MainViewModel", "Display removed: $displayId")
            loadAvailableDisplays()
            handleDisplayDisconnected(displayId)
        }

        override fun onDisplayChanged(displayId: Int) {
            android.util.Log.d("MainViewModel", "Display changed: $displayId")
            loadAvailableDisplays()
        }
    }

    init {
        val database = RotationDatabase.getInstance(context)
        repository = OrientationRepository(database.appOrientationDao())
        preferencesManager = PreferencesManager(context)

        observeState()
        loadInstalledApps()
        loadAvailableDisplays()
        checkPermissions()

        // Register display listener for hot-swap detection
        displayManager.registerDisplayListener(displayListener, null)
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister display listener
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun handleDisplayDisconnected(displayId: Int) {
        viewModelScope.launch {
            // Clear selected screen if the disconnected display was selected
            if (_selectedGlobalScreen.value.id == displayId) {
                _selectedGlobalScreen.value = TargetScreen.AllScreens
            }

            // Update app screen selections
            _selectedAppScreens.update { current ->
                current.filterValues { it.id != displayId }
            }

            // Optionally: clean up orphaned settings for this display
            // Note: We keep them by default for when the display reconnects
            // Uncomment to auto-delete orphaned settings:
            // repository.deleteSettingsForDisplay(displayId)
        }
    }

    /**
     * Clean up all orphaned settings for displays that no longer exist
     */
    fun cleanupOrphanedSettings() {
        viewModelScope.launch {
            try {
                val currentDisplayIds = displayManager.displays.map { it.displayId }.toSet()
                val allSettings = repository.getAllSettings().firstOrNull() ?: return@launch

                allSettings.forEach { setting ->
                    val displayId = setting.targetScreen.id
                    // Skip "All Screens" setting (id = -1) and settings for existing displays
                    if (displayId != -1 && !currentDisplayIds.contains(displayId)) {
                        android.util.Log.d(
                            "MainViewModel",
                            "Cleaning up orphaned setting for ${setting.packageName} on display $displayId"
                        )
                        repository.deleteSettingForDisplay(setting.packageName, displayId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to cleanup orphaned settings", e)
            }
        }
    }

    /**
     * Get effective orientation for an app using smart fallback
     */
    suspend fun getEffectiveOrientationForApp(packageName: String, displayId: Int): ScreenOrientation? {
        try {
            val display = displayManager.displays.find { it.displayId == displayId }
                ?: return null

            val metrics = android.util.DisplayMetrics()
            display.getMetrics(metrics)
            val aspectRatio = when {
                metrics.heightPixels > metrics.widthPixels -> AspectRatio.PORTRAIT
                metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat() < 1.3f -> AspectRatio.SQUARE
                else -> AspectRatio.LANDSCAPE
            }

            val availableDisplayIds = displayManager.displays.map { it.displayId }.toSet()

            val setting = repository.getEffectiveOrientation(
                packageName = packageName,
                currentDisplayId = displayId,
                currentAspectRatio = aspectRatio,
                availableDisplayIds = availableDisplayIds
            )

            return setting?.orientation
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to get effective orientation", e)
            return null
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                preferencesManager.globalOrientation,
                repository.getAllSettings(),
                ::Pair
            ).collect { (globalOrientation, settings) ->
                _state.update { currentState ->
                    currentState
                        .withGlobalOrientation(globalOrientation)
                        .copy(perAppSettings = settings.associateBy { it.packageName })
                }

                // Load saved screen selections for apps
                val screenSelections = settings.associate {
                    it.packageName to it.targetScreen
                }
                _selectedAppScreens.value = screenSelections
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = context.packageManager

            // Get recently used apps
            val recentApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    val service = context.getSystemService(Context.USAGE_STATS_SERVICE)
                    if (service is android.app.usage.UsageStatsManager) {
                        val endTime = System.currentTimeMillis()
                        val startTime = endTime - 1000 * 60 * 60 * 24 * 7 // Last 7 days

                        service.queryUsageStats(
                            android.app.usage.UsageStatsManager.INTERVAL_WEEKLY,
                            startTime,
                            endTime
                        )?.mapNotNull { it.packageName }?.toSet() ?: emptySet()
                    } else {
                        emptySet()
                    }
                } catch (e: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }

            // Query all apps with launcher activities
            // Use flags that Android launchers use to see all apps including disabled ones
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val queryFlags = PackageManager.MATCH_DISABLED_COMPONENTS or
                            PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(queryFlags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, queryFlags)
            }

            val installedAppsWithLauncher = resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName

                    // Exclude this app
                    if (packageName == context.packageName) {
                        return@mapNotNull null
                    }

                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    InstalledApp(
                        packageName = packageName,
                        appName = appInfo.loadLabel(packageManager).toString(),
                        isRecent = recentApps.contains(packageName),
                        isInstalled = true
                    )
                } catch (e: Exception) {
                    null
                }
            }.distinctBy { it.packageName }

            // Get all saved settings to include apps that may not have launcher activities
            val savedSettings = repository.getAllSettings().firstOrNull() ?: emptyList()
            val savedAppPackages = installedAppsWithLauncher.map { it.packageName }.toSet()

            val appsFromSettings = savedSettings.mapNotNull { setting ->
                // Skip if already in the installed apps list
                if (savedAppPackages.contains(setting.packageName)) {
                    return@mapNotNull null
                }

                // Add apps with saved settings even if not installed/no launcher
                InstalledApp(
                    packageName = setting.packageName,
                    appName = setting.appName,
                    isRecent = false,
                    isInstalled = false  // Mark as not installed (greyed out)
                )
            }

            // Combine both lists
            val allApps = (installedAppsWithLauncher + appsFromSettings)
                .distinctBy { it.packageName }
                .sortedWith(
                    compareByDescending<InstalledApp> { it.isInstalled }  // Installed first
                        .thenByDescending { it.isRecent }  // Then recent
                        .thenBy { it.appName }  // Then alphabetically
                )

            _installedApps.value = allApps
        }
    }

    private fun loadAvailableDisplays() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val displays = displayManager.displays

                // First pass: collect display info with ratios
                data class DisplayInfo(
                    val displayId: Int,
                    val name: String,
                    val width: Int,
                    val height: Int,
                    val ratio: Float  // Always >= 1.0 (larger dimension / smaller dimension)
                )

                val displayInfos = displays.mapIndexed { index, display ->
                    val screenName = if (index == 0) "Primary" else "Auxiliary"
                    val metrics = android.util.DisplayMetrics()
                    display.getMetrics(metrics)
                    val width = metrics.widthPixels
                    val height = metrics.heightPixels

                    val ratio = if (width > height) {
                        width.toFloat() / height.toFloat()
                    } else {
                        height.toFloat() / width.toFloat()
                    }

                    DisplayInfo(display.displayId, screenName, width, height, ratio)
                }

                // Comparative aspect ratio assignment
                val screens = mutableListOf<TargetScreen>(TargetScreen.AllScreens)

                if (displayInfos.size == 1) {
                    // Single display: use absolute thresholds
                    val info = displayInfos[0]
                    val aspectRatio = when {
                        info.height > info.width -> AspectRatio.PORTRAIT
                        info.ratio < 1.2 -> AspectRatio.SQUARE  // Close to 1:1
                        else -> AspectRatio.LANDSCAPE
                    }
                    screens.add(TargetScreen.SpecificScreen(info.displayId, info.name, aspectRatio))
                } else {
                    // Multiple displays: comparative approach
                    val sortedByRatio = displayInfos.sortedBy { it.ratio }
                    val minRatio = sortedByRatio.first().ratio
                    val maxRatio = sortedByRatio.last().ratio
                    val ratioRange = maxRatio - minRatio

                    displayInfos.forEach { info ->
                        val aspectRatio = when {
                            // Portrait if height > width
                            info.height > info.width -> AspectRatio.PORTRAIT
                            // If there's meaningful difference between screens
                            ratioRange > 0.15 -> {
                                // Comparative: where does this screen fall?
                                val positionInRange = (info.ratio - minRatio) / ratioRange
                                when {
                                    positionInRange > 0.6 -> AspectRatio.LANDSCAPE  // Wider end
                                    positionInRange < 0.4 -> AspectRatio.SQUARE     // More square end
                                    else -> AspectRatio.LANDSCAPE  // Middle defaults to landscape
                                }
                            }
                            // All screens very similar - use absolute threshold
                            else -> if (info.ratio < 1.3) AspectRatio.SQUARE else AspectRatio.LANDSCAPE
                        }
                        screens.add(TargetScreen.SpecificScreen(info.displayId, info.name, aspectRatio))
                    }
                }

                _availableScreens.value = screens
            } catch (e: Exception) {
                // If we can't get displays, just use AllScreens
                _availableScreens.value = listOf(TargetScreen.AllScreens)
            }
        }
    }

    fun checkPermissions() {
        viewModelScope.launch {
            val hasDrawOverlay = PermissionChecker.hasDrawOverlayPermission(context)
                .fold({ false }, { it })
            val hasAccessibility = AccessibilityChecker.isAccessibilityServiceEnabled(context)
                .fold({ false }, { it })

            _state.update {
                it.withDrawOverlayPermission(hasDrawOverlay)
                    .withAccessibilityServiceEnabled(hasAccessibility)
            }
        }
    }

    fun setGlobalOrientation(orientation: ScreenOrientation) {
        viewModelScope.launch {
            preferencesManager.setGlobalOrientation(orientation)
            applyOrientation(orientation, _selectedGlobalScreen.value)
        }
    }

    fun setGlobalTargetScreen(screen: TargetScreen) {
        _selectedGlobalScreen.value = screen
    }

    fun getSelectedScreenForApp(packageName: String): TargetScreen {
        return _selectedAppScreens.value[packageName] ?: TargetScreen.AllScreens
    }

    fun setAppTargetScreen(packageName: String, screen: TargetScreen) {
        _selectedAppScreens.update { it + (packageName to screen) }
    }

    fun setAppOrientation(
        packageName: String,
        appName: String,
        orientation: ScreenOrientation
    ) {
        viewModelScope.launch {
            val targetScreen = getSelectedScreenForApp(packageName)
            val setting = AppOrientationSetting.create(
                packageName = packageName,
                appName = appName,
                orientation = orientation,
                targetScreen = targetScreen
            )
            repository.saveSetting(setting)
            // Refresh app list to show updated visual state
            loadInstalledApps()
        }
    }

    fun removeAppSetting(packageName: String) {
        viewModelScope.launch {
            repository.deleteSetting(packageName)
            // Refresh app list to remove visual indicators
            loadInstalledApps()
        }
    }

    fun toggleAppSettingEnabled(packageName: String) {
        viewModelScope.launch {
            val currentSetting = state.value.perAppSettings[packageName] ?: return@launch
            repository.saveSetting(currentSetting.toggleEnabled())
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun requestDrawOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun hasUsageStatsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return false
        }
        val service = context.getSystemService(Context.USAGE_STATS_SERVICE)
        if (service is android.app.usage.UsageStatsManager) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60
            val stats = service.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            return stats != null && stats.isNotEmpty()
        }
        return false
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun applyOrientation(orientation: ScreenOrientation, targetScreen: TargetScreen) {
        try {
            val intent = Intent(context, OrientationControlService::class.java).apply {
                action = OrientationControlService.ACTION_SET_ORIENTATION
                putExtra(OrientationControlService.EXTRA_ORIENTATION, orientation.value)
                putExtra(OrientationControlService.EXTRA_SCREEN_ID, targetScreen.id)
            }
            context.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to apply orientation", e)
        }
    }

    fun flashScreen(targetScreen: TargetScreen) {
        try {
            val currentPalette = app.rotatescreen.ui.components.RiscOsColors.currentPalette

            // Get display information if specific screen
            var displayInfo = ""
            if (targetScreen is TargetScreen.SpecificScreen) {
                val display = displayManager.displays.find { it.displayId == targetScreen.id }
                if (display != null) {
                    val metrics = android.util.DisplayMetrics()
                    display.getMetrics(metrics)
                    displayInfo = "${metrics.widthPixels}×${metrics.heightPixels} • ${metrics.densityDpi}dpi"
                }
            }

            val intent = Intent(context, OrientationControlService::class.java).apply {
                action = "com.aware.rotation.action.FLASH_SCREEN"
                putExtra(OrientationControlService.EXTRA_SCREEN_ID, targetScreen.id)
                putExtra("SCREEN_NAME", targetScreen.displayName)
                putExtra("DISPLAY_INFO", displayInfo)
                putExtra("ASPECT_RATIO", targetScreen.aspectRatio.name)
                putExtra("PALETTE_NAME", currentPalette.name)
                putExtra("COLOR_1", currentPalette.actionBlue.value.toLong())
                putExtra("COLOR_2", currentPalette.actionGreen.value.toLong())
                putExtra("COLOR_3", currentPalette.actionYellow.value.toLong())
                putExtra("BG_COLOR", currentPalette.background.value.toLong())
                putExtra("TEXT_COLOR", currentPalette.white.value.toLong())
                // Get current orientation for this screen
                val orientation = when (targetScreen) {
                    is TargetScreen.AllScreens -> _state.value.globalOrientation
                    is TargetScreen.SpecificScreen -> {
                        // Check for per-app setting first, otherwise global
                        _state.value.perAppSettings.values.firstOrNull {
                            it.targetScreen.id == targetScreen.id
                        }?.orientation ?: _state.value.globalOrientation
                    }
                }
                putExtra("ORIENTATION", orientation.displayName)
            }
            context.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to flash screen", e)
        }
    }
}
