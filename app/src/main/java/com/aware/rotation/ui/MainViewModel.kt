package com.aware.rotation.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.aware.rotation.data.local.RotationDatabase
import com.aware.rotation.data.preferences.PreferencesManager
import com.aware.rotation.data.repository.OrientationRepository
import com.aware.rotation.domain.model.*
import com.aware.rotation.service.OrientationControlService
import com.aware.rotation.util.AccessibilityChecker
import com.aware.rotation.util.PermissionChecker
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

    private val _state = MutableStateFlow(OrientationState())
    val state: StateFlow<OrientationState> = _state.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = context.packageManager
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Filter out system apps
                .map { appInfo ->
                    InstalledApp(
                        packageName = appInfo.packageName,
                        appName = appInfo.loadLabel(packageManager).toString()
                    )
                }
                .sortedBy { it.appName }

            _installedApps.value = apps
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
            applyOrientation(orientation, TargetScreen.AllScreens)
        }
    }

    fun setAppOrientation(
        packageName: String,
        appName: String,
        orientation: ScreenOrientation
    ) {
        viewModelScope.launch {
            val setting = AppOrientationSetting.create(
                packageName = packageName,
                appName = appName,
                orientation = orientation
            )
            repository.saveSetting(setting)
        }
    }

    fun removeAppSetting(packageName: String) {
        viewModelScope.launch {
            repository.deleteSetting(packageName)
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

    private fun applyOrientation(orientation: ScreenOrientation, targetScreen: TargetScreen) {
        val intent = Intent(context, OrientationControlService::class.java).apply {
            action = OrientationControlService.ACTION_SET_ORIENTATION
            putExtra(OrientationControlService.EXTRA_ORIENTATION, orientation.value)
            putExtra(OrientationControlService.EXTRA_SCREEN_ID, targetScreen.id)
        }
        context.startService(intent)
    }
}
