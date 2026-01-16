package com.aware.rotation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aware.rotation.domain.model.ScreenOrientation
import com.aware.rotation.ui.MainViewModel

/**
 * Minimal main screen focusing on essential functionality
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
                title = { Text("Rotation") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission warnings - compact
            if (!state.hasDrawOverlayPermission || !state.isAccessibilityServiceEnabled) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    onClick = {
                        if (!state.hasDrawOverlayPermission) viewModel.requestDrawOverlayPermission()
                        else viewModel.requestAccessibilityPermission()
                    }
                ) {
                    Text(
                        text = if (!state.hasDrawOverlayPermission) "Tap to grant Draw Over Apps permission"
                        else "Tap to enable Accessibility",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Global orientation - compact
            ListItem(
                headlineContent = { Text("Global") },
                trailingContent = {
                    Text(
                        text = state.globalOrientation.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable {
                    viewModel.setGlobalOrientation(state.globalOrientation.next())
                }
            )

            Divider()

            // Search - minimal
            if (state.isAccessibilityServiceEnabled) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search") },
                    singleLine = true
                )

                // App list - minimal
                LazyColumn {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        val currentSetting = state.perAppSettings[app.packageName]
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = app.appName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = currentSetting?.orientation?.displayName ?: "Default",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (currentSetting != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable {
                                val current = currentSetting?.orientation ?: ScreenOrientation.Unspecified
                                viewModel.setAppOrientation(app.packageName, app.appName, current.next())
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enable Accessibility to set per-app rotations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
