package com.aware.rotation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aware.rotation.domain.model.ScreenOrientation
import com.aware.rotation.ui.MainViewModel
import com.aware.rotation.ui.components.*

/**
 * Main screen composable using FP style state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rotation Control") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission warnings
            if (!state.hasWriteSettingsPermission) {
                item {
                    PermissionWarningCard(
                        title = "Settings Permission Required",
                        description = "Grant permission to modify system settings to control screen orientation.",
                        onGrantClick = { viewModel.requestWriteSettingsPermission() }
                    )
                }
            }

            if (!state.isAccessibilityServiceEnabled) {
                item {
                    PermissionWarningCard(
                        title = "Accessibility Service Required",
                        description = "Enable the accessibility service to detect foreground apps and apply per-app rotation settings.",
                        onGrantClick = { viewModel.requestAccessibilityPermission() }
                    )
                }
            }

            // Global orientation control
            item {
                GlobalOrientationCard(
                    currentOrientation = state.globalOrientation,
                    onOrientationSelected = { viewModel.setGlobalOrientation(it) },
                    enabled = state.hasWriteSettingsPermission
                )
            }

            // Per-app settings section
            item {
                Text(
                    text = "Per-App Settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Search bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) }
                )
            }

            // App list
            if (state.isAccessibilityServiceEnabled) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    val currentSetting = state.perAppSettings[app.packageName]
                    AppOrientationCard(
                        app = app,
                        currentSetting = currentSetting,
                        onOrientationSelected = { orientation ->
                            viewModel.setAppOrientation(
                                app.packageName,
                                app.appName,
                                orientation
                            )
                        },
                        onRemoveSetting = {
                            viewModel.removeAppSetting(app.packageName)
                        }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enable accessibility service to configure per-app settings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
