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

    init {
        val database = RotationDatabase.getInstance(context)
        repository = OrientationRepository(database.appOrientationDao())
        preferencesManager = PreferencesManager(context)

        observeState()
        loadInstalledApps()
        loadAvailableDisplays()
        checkPermissions()
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

            // Query all apps with launcher activities directly
            // Note: Use 0 for default query, not MATCH_ALL which is for package queries
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, 0)
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
                val screens = mutableListOf<TargetScreen>(TargetScreen.AllScreens)

                displays.forEachIndexed { index, display ->
                    val screenName = if (index == 0) "Primary" else "Auxiliary"

                    // Calculate aspect ratio
                    val metrics = android.util.DisplayMetrics()
                    display.getMetrics(metrics)
                    val width = metrics.widthPixels
                    val height = metrics.heightPixels

                    val aspectRatio = when {
                        height > width * 1.1 -> AspectRatio.PORTRAIT
                        width > height * 1.1 -> AspectRatio.LANDSCAPE
                        else -> AspectRatio.SQUARE
                    }

                    screens.add(TargetScreen.SpecificScreen(display.displayId, screenName, aspectRatio))
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
            val intent = Intent(context, OrientationControlService::class.java).apply {
                action = "com.aware.rotation.action.FLASH_SCREEN"
                putExtra(OrientationControlService.EXTRA_SCREEN_ID, targetScreen.id)
            }
            context.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to flash screen", e)
        }
    }
}
