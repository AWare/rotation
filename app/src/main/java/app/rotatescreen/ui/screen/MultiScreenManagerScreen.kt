package app.rotatescreen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rotatescreen.domain.model.AspectRatio
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import app.rotatescreen.ui.MainViewModel
import app.rotatescreen.ui.components.*

/**
 * Multi-screen manager - shows current apps on each screen and allows configuration
 * Shows which apps are configured for each screen and allows moving between screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiScreenManagerScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val availableScreens by viewModel.availableScreens.collectAsState()

    // Get only specific screens (not AllScreens)
    val specificScreens = availableScreens.filterIsInstance<TargetScreen.SpecificScreen>()

    // Group settings by screen
    val settingsByScreen = state.perAppSettings.values.flatten()
        .groupBy { it.targetScreen }
        .mapKeys { (screen, _) ->
            specificScreens.find { it.id == screen.id } ?: screen
        }

    MottledBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
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
                    text = "Multi-Screen Manager",
                    color = RiscOsColors.white,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Show panel for each screen
            specificScreens.forEach { screen ->
                val appsOnScreen = settingsByScreen[screen] ?: emptyList()

                ScreenAppsPanel(
                    screen = screen,
                    apps = appsOnScreen,
                    allScreens = specificScreens,
                    viewModel = viewModel
                )
            }
        }
    }
}

/**
 * Panel showing all apps configured for a specific screen
 */
@Composable
fun ScreenAppsPanel(
    screen: TargetScreen.SpecificScreen,
    apps: List<app.rotatescreen.domain.model.AppOrientationSetting>,
    allScreens: List<TargetScreen.SpecificScreen>,
    viewModel: MainViewModel
) {
    RiscOsWindow(
        title = "${screen.displayName} (${apps.size} apps)",
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Screen info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (screen.ratio) {
                        AspectRatio.PORTRAIT -> "▯"
                        AspectRatio.LANDSCAPE -> "▬"
                        AspectRatio.SQUARE -> "▪"
                    }
                    RiscOsLabel(
                        text = icon,
                        fontWeight = FontWeight.Bold
                    )
                    RiscOsLabel(
                        text = "Display ${screen.displayId}",
                        fontWeight = FontWeight.Bold
                    )
                }

                RiscOsButton(
                    onClick = { viewModel.flashScreen(screen) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    backgroundColor = RiscOsColors.actionYellow
                ) {
                    RiscOsLabel(
                        text = "⚡ Flash",
                        fontWeight = FontWeight.Bold,
                        color = RiscOsColors.black
                    )
                }
            }

            // List of apps on this screen
            if (apps.isEmpty()) {
                RiscOsPanel(
                    modifier = Modifier.fillMaxWidth(),
                    inset = true
                ) {
                    RiscOsLabel(
                        text = "No apps configured for this screen",
                        color = RiscOsColors.darkGray
                    )
                }
            } else {
                apps.forEach { setting ->
                    AppScreenItem(
                        setting = setting,
                        currentScreen = screen,
                        availableScreens = allScreens,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

/**
 * Individual app item showing orientation and move options
 */
@Composable
fun AppScreenItem(
    setting: app.rotatescreen.domain.model.AppOrientationSetting,
    currentScreen: TargetScreen.SpecificScreen,
    availableScreens: List<TargetScreen.SpecificScreen>,
    viewModel: MainViewModel
) {
    var showMoveMenu by remember { mutableStateOf(false) }

    RiscOsPanel(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = RiscOsColors.white
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App name and current orientation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    RiscOsLabel(
                        text = setting.appName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    RiscOsLabel(
                        text = setting.packageName,
                        color = RiscOsColors.darkGray,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiscOsLabel(
                        text = setting.orientation.displayName,
                        fontWeight = FontWeight.Bold,
                        color = RiscOsColors.actionBlue
                    )
                }
            }

            // Orientation selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScreenOrientation.all().forEach { orientation ->
                    RiscOsButton(
                        onClick = {
                            viewModel.setAppTargetScreen(setting.packageName, currentScreen)
                            viewModel.setAppOrientation(
                                setting.packageName,
                                setting.appName,
                                orientation
                            )
                        },
                        isSelected = setting.orientation == orientation,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        backgroundColor = if (setting.orientation == orientation)
                            RiscOsColors.actionBlue
                        else
                            RiscOsColors.lightGray
                    ) {
                        RiscOsLabel(
                            text = getOrientationIcon(orientation),
                            color = if (setting.orientation == orientation)
                                RiscOsColors.white
                            else
                                RiscOsColors.black,
                            maxLines = 1
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Move to other screen button
                if (availableScreens.size > 1) {
                    RiscOsButton(
                        onClick = { showMoveMenu = !showMoveMenu },
                        modifier = Modifier.weight(1f),
                        backgroundColor = RiscOsColors.actionGreen.copy(alpha = 0.3f)
                    ) {
                        RiscOsLabel(
                            text = "↔ Move",
                            fontWeight = FontWeight.Bold,
                            color = RiscOsColors.black
                        )
                    }
                }

                // Remove button
                RiscOsButton(
                    onClick = {
                        viewModel.removeAppSettingForScreen(
                            setting.packageName,
                            currentScreen.id
                        )
                    },
                    modifier = Modifier.weight(1f),
                    backgroundColor = RiscOsColors.actionRed.copy(alpha = 0.3f)
                ) {
                    RiscOsLabel(
                        text = "✕ Remove",
                        fontWeight = FontWeight.Bold,
                        color = RiscOsColors.white
                    )
                }
            }

            // Move menu
            if (showMoveMenu) {
                RiscOsPanel(
                    modifier = Modifier.fillMaxWidth(),
                    inset = true,
                    backgroundColor = RiscOsColors.actionGreen.copy(alpha = 0.1f)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RiscOsLabel(
                            text = "Move to:",
                            fontWeight = FontWeight.Bold
                        )

                        availableScreens.filter { it.id != currentScreen.id }.forEach { targetScreen ->
                            RiscOsButton(
                                onClick = {
                                    // Remove from current screen
                                    viewModel.removeAppSettingForScreen(
                                        setting.packageName,
                                        currentScreen.id
                                    )
                                    // Add to target screen
                                    viewModel.setAppTargetScreen(setting.packageName, targetScreen)
                                    viewModel.setAppOrientation(
                                        setting.packageName,
                                        setting.appName,
                                        setting.orientation
                                    )
                                    showMoveMenu = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = RiscOsColors.white
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (targetScreen.ratio) {
                                        AspectRatio.PORTRAIT -> "▯"
                                        AspectRatio.LANDSCAPE -> "▬"
                                        AspectRatio.SQUARE -> "▪"
                                    }
                                    RiscOsLabel(text = icon)
                                    RiscOsLabel(
                                        text = targetScreen.displayName,
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

/**
 * Get icon/symbol for orientation
 */
private fun getOrientationIcon(orientation: ScreenOrientation): String {
    return when (orientation) {
        ScreenOrientation.Unspecified -> "⟲"
        ScreenOrientation.Portrait -> "☰"
        ScreenOrientation.Landscape -> "☷"
        ScreenOrientation.ReversePortrait -> "⥯"
        ScreenOrientation.ReverseLandscape -> "⥮"
        ScreenOrientation.Sensor -> "◎"
    }
}
