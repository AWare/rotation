package com.aware.rotation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aware.rotation.domain.model.ScreenOrientation
import com.aware.rotation.domain.model.TargetScreen
import com.aware.rotation.ui.MainViewModel
import com.aware.rotation.ui.components.*

/**
 * RISC OS styled main screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val availableScreens by viewModel.availableScreens.collectAsState()
    val selectedGlobalScreen by viewModel.selectedGlobalScreen.collectAsState()

    var showGlobalSelector by remember { mutableStateOf(false) }
    var selectedAppForConfig by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RiscOsColors.mediumGray)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RiscOsColors.actionBlue)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Screen Rotation Control",
                color = RiscOsColors.white,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Permission warnings
        if (!state.hasDrawOverlayPermission || !state.isAccessibilityServiceEnabled) {
            RiscOsButton(
                onClick = {
                    if (!state.hasDrawOverlayPermission) viewModel.requestDrawOverlayPermission()
                    else viewModel.requestAccessibilityPermission()
                },
                backgroundColor = RiscOsColors.actionYellow,
                modifier = Modifier.fillMaxWidth()
            ) {
                RiscOsLabel(
                    text = if (!state.hasDrawOverlayPermission)
                        "⚠ Tap to grant overlay permission"
                    else
                        "⚠ Tap to enable Accessibility",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Global orientation section
        RiscOsWindow(
            title = "Global Orientation",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Screen selector for global
                ScreenSelector(
                    availableScreens = availableScreens,
                    selectedScreen = selectedGlobalScreen,
                    onScreenSelected = { viewModel.setGlobalTargetScreen(it) }
                )

                // Show/hide orientation selector
                RiscOsButton(
                    onClick = { showGlobalSelector = !showGlobalSelector },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionBlue.copy(alpha = 0.2f)
                ) {
                    RiscOsLabel(
                        text = "Current: ${state.globalOrientation.displayName} ${if (showGlobalSelector) "▲" else "▼"}",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showGlobalSelector) {
                    OrientationSelector(
                        selectedOrientation = state.globalOrientation,
                        onOrientationSelected = { viewModel.setGlobalOrientation(it) }
                    )
                }
            }
        }

        // Per-app section
        if (state.isAccessibilityServiceEnabled) {
            RiscOsWindow(
                title = "Per-App Settings",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search field
                    RiscOsPanel(
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Search apps...",
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    // App list
                    RiscOsPanel(
                        inset = true,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = filteredApps,
                                key = { it.packageName }
                            ) { app ->
                                val currentSetting = state.perAppSettings[app.packageName]
                                val isConfiguring = selectedAppForConfig == app.packageName

                                Column {
                                    RiscOsButton(
                                        onClick = {
                                            selectedAppForConfig = if (isConfiguring) null else app.packageName
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        isSelected = isConfiguring,
                                        backgroundColor = if (currentSetting != null)
                                            RiscOsColors.actionGreen.copy(alpha = 0.2f)
                                        else
                                            RiscOsColors.white
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RiscOsLabel(
                                                text = app.appName,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1
                                            )
                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                RiscOsLabel(
                                                    text = currentSetting?.orientation?.displayName ?: "Default",
                                                    fontWeight = if (currentSetting != null)
                                                        FontWeight.Bold
                                                    else
                                                        FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                                if (currentSetting != null && currentSetting.targetScreen !is TargetScreen.AllScreens) {
                                                    RiscOsLabel(
                                                        text = currentSetting.targetScreen.displayName,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Expanded configuration panel
                                    if (isConfiguring) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Screen selector for this app - show saved or default
                                            val currentTargetScreen = currentSetting?.targetScreen
                                                ?: viewModel.getSelectedScreenForApp(app.packageName)

                                            ScreenSelector(
                                                availableScreens = availableScreens,
                                                selectedScreen = currentTargetScreen,
                                                onScreenSelected = {
                                                    viewModel.setAppTargetScreen(app.packageName, it)
                                                }
                                            )

                                            // Orientation selector
                                            OrientationSelector(
                                                selectedOrientation = currentSetting?.orientation
                                                    ?: ScreenOrientation.Unspecified,
                                                onOrientationSelected = {
                                                    viewModel.setAppOrientation(
                                                        app.packageName,
                                                        app.appName,
                                                        it
                                                    )
                                                }
                                            )

                                            // Remove button if setting exists
                                            if (currentSetting != null) {
                                                RiscOsButton(
                                                    onClick = {
                                                        viewModel.removeAppSetting(app.packageName)
                                                        selectedAppForConfig = null
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    backgroundColor = RiscOsColors.actionRed.copy(alpha = 0.3f)
                                                ) {
                                                    RiscOsLabel(
                                                        text = "✕ Remove Setting",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            RiscOsPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                inset = true
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    RiscOsLabel(
                        text = "Enable Accessibility Service\nto configure per-app rotations",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
