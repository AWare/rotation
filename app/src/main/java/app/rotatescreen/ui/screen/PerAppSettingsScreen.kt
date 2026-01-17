package app.rotatescreen.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.rotatescreen.ui.MainViewModel
import app.rotatescreen.ui.components.*

/**
 * Screen showing list of apps for per-app rotation settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppSettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onAppClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // Check usage stats permission
    val hasUsageStatsPermission = remember { viewModel.hasUsageStatsPermission() }

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
                    fontWeight = FontWeight.Bold,
                    color = RiscOsColors.white
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Select App",
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
                    fontWeight = FontWeight.Bold,
                    color = RiscOsColors.black
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
                    fontWeight = FontWeight.Bold,
                    color = RiscOsColors.black
                )
            }
        }

        if (state.isAccessibilityServiceEnabled) {
            // Search field
            RiscOsWindow(
                title = "Search",
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
                            "Type to filter apps...",
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
            RiscOsWindow(
                title = "Apps (${filteredApps.size})",
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredApps.isNotEmpty()) {
                        filteredApps.forEach { app ->
                            val currentSetting = state.perAppSettings[app.packageName]

                            RiscOsButton(
                                onClick = {
                                    onAppClick(app.packageName)
                                },
                                modifier = Modifier.fillMaxWidth(),
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
                                            fontWeight = if (app.isRecent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                        if (currentSetting != null) {
                                            RiscOsLabel(
                                                text = "${currentSetting.orientation.displayName} • ${currentSetting.targetScreen.displayName}",
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Arrow indicator
                                    RiscOsLabel(
                                        text = "▶",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        RiscOsPanel(
                            inset = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RiscOsLabel(
                                text = if (searchQuery.isEmpty()) "No apps found." else "No apps match \"$searchQuery\"",
                                maxLines = 2
                            )
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
