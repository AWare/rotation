package app.rotatescreen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import app.rotatescreen.ui.MainViewModel
import app.rotatescreen.ui.components.*

/**
 * RISC OS styled per-app settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppSettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val availableScreens by viewModel.availableScreens.collectAsState()

    var selectedAppForConfig by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Request focus on the search field when screen is shown
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RiscOsColors.mediumGray)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RiscOsColors.actionBlue)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RiscOsButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(4.dp),
                backgroundColor = RiscOsColors.lightGray
            ) {
                RiscOsLabel(
                    text = "◀",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Per-App Settings",
                color = RiscOsColors.white,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Permission warning
        if (!state.isAccessibilityServiceEnabled) {
            RiscOsButton(
                onClick = { viewModel.requestAccessibilityPermission() },
                backgroundColor = RiscOsColors.actionYellow,
                modifier = Modifier.fillMaxWidth()
            ) {
                RiscOsLabel(
                    text = "⚠ Tap to enable Accessibility Service",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.isAccessibilityServiceEnabled) {
            // App picker and configuration
            RiscOsWindow(
                title = "Select App",
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search field
                    RiscOsPanel(
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
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

                    // Configuration panel (shows when app selected)
                    selectedAppForConfig?.let { packageName ->
                        val app = filteredApps.find { it.packageName == packageName }
                        val currentSetting = state.perAppSettings[packageName]

                        if (app != null) {
                            RiscOsPanel(
                                modifier = Modifier.fillMaxWidth(),
                                inset = false,
                                backgroundColor = RiscOsColors.actionGreen.copy(alpha = 0.2f)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // App name header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RiscOsLabel(
                                            text = "Configure: ${app.appName}",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2
                                        )
                                        RiscOsButton(
                                            onClick = { selectedAppForConfig = null },
                                            modifier = Modifier.size(32.dp),
                                            contentPadding = PaddingValues(4.dp),
                                            backgroundColor = RiscOsColors.actionRed.copy(alpha = 0.5f)
                                        ) {
                                            RiscOsLabel(
                                                text = "✕",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Screen selector
                                    val currentTargetScreen = currentSetting?.targetScreen
                                        ?: viewModel.getSelectedScreenForApp(packageName)

                                    ScreenSelector(
                                        availableScreens = availableScreens,
                                        selectedScreen = currentTargetScreen,
                                        onScreenSelected = {
                                            viewModel.setAppTargetScreen(packageName, it)
                                        },
                                        onScreenFlash = { screen ->
                                            viewModel.flashScreen(screen)
                                        }
                                    )

                                    // Orientation selector
                                    OrientationSelector(
                                        selectedOrientation = currentSetting?.orientation
                                            ?: ScreenOrientation.Unspecified,
                                        onOrientationSelected = {
                                            viewModel.setAppOrientation(
                                                packageName,
                                                app.appName,
                                                it
                                            )
                                        }
                                    )

                                    // Remove button if setting exists
                                    if (currentSetting != null) {
                                        RiscOsButton(
                                            onClick = {
                                                viewModel.removeAppSetting(packageName)
                                                selectedAppForConfig = null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            backgroundColor = RiscOsColors.actionRed.copy(alpha = 0.4f)
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

                    // App list
                    RiscOsPanel(
                        inset = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = filteredApps,
                                key = { it.packageName }
                            ) { app ->
                                val currentSetting = state.perAppSettings[app.packageName]
                                val isSelected = selectedAppForConfig == app.packageName

                                RiscOsButton(
                                    onClick = {
                                        selectedAppForConfig = if (isSelected) null else app.packageName
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    isSelected = isSelected,
                                    backgroundColor = if (currentSetting != null)
                                        RiscOsColors.actionGreen.copy(alpha = 0.15f)
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
                            }
                        }
                    }
                }
            }
        } else {
            // Accessibility not enabled message
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
