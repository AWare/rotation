package app.rotatescreen.ui.screen

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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
    val availableScreens by viewModel.availableScreens.collectAsState()
    val context = LocalContext.current

    var selectedAppForConfig by remember { mutableStateOf<String?>(null) }

    // Check usage stats permission
    val hasUsageStatsPermission = remember { viewModel.hasUsageStatsPermission() }

    // Get top 5 recent apps
    val topRecentApps = remember(filteredApps) {
        filteredApps.filter { it.isRecent }.take(5)
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

        // Permission warnings
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

        if (!hasUsageStatsPermission) {
            RiscOsButton(
                onClick = { viewModel.requestUsageStatsPermission() },
                backgroundColor = RiscOsColors.actionYellow,
                modifier = Modifier.fillMaxWidth()
            ) {
                RiscOsLabel(
                    text = "⚠ Tap to grant Usage Stats permission",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.isAccessibilityServiceEnabled) {
            // App picker and configuration
            RiscOsWindow(
                title = "Recent Apps",
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top 5 recent apps with icons
                    if (topRecentApps.isNotEmpty()) {
                        RiscOsPanel(
                            inset = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                topRecentApps.forEach { app ->
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
                                            RiscOsColors.white,
                                        contentPadding = PaddingValues(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // App icon
                                            val icon = remember(app.packageName) {
                                                try {
                                                    context.packageManager.getApplicationIcon(app.packageName)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                            icon?.let { drawable ->
                                                Image(
                                                    bitmap = drawable.toBitmap(64, 64).asImageBitmap(),
                                                    contentDescription = app.appName,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            }

                                            // App name and status
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                RiscOsLabel(
                                                    text = app.appName,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                if (currentSetting != null) {
                                                    RiscOsLabel(
                                                        text = "${currentSetting.orientation.displayName} • ${currentSetting.targetScreen.displayName}",
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            // Indicator
                                            if (isSelected) {
                                                RiscOsLabel(
                                                    text = "▼",
                                                    fontWeight = FontWeight.Bold,
                                                    color = RiscOsColors.actionGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        RiscOsPanel(
                            inset = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RiscOsLabel(
                                text = "No recent apps found. Open some apps to configure them.",
                                maxLines = 3
                            )
                        }
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
