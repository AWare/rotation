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
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import app.rotatescreen.ui.MainViewModel
import app.rotatescreen.ui.components.*

/**
 * Screen for configuring rotation settings for a specific app
 * Shows separate settings panel for each available screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    packageName: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val availableScreens by viewModel.availableScreens.collectAsState()

    val app = remember(filteredApps, packageName) {
        filteredApps.find { it.packageName == packageName }
    }

    // Filter out AllScreens and keep only specific screens
    val specificScreens = availableScreens.filterIsInstance<TargetScreen.SpecificScreen>()

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
                text = app?.appName ?: packageName,
                color = RiscOsColors.white,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (app != null) {
            // Show settings panel for each screen
            specificScreens.forEach { screen ->
                val setting = state.getSettingForAppAndDisplay(packageName, screen.id)

                ScreenSettingsPanel(
                    screen = screen,
                    packageName = packageName,
                    appName = app.appName,
                    currentSetting = setting,
                    viewModel = viewModel
                )
            }

            // Remove all button if any settings exist
            val hasAnySettings = specificScreens.any { screen ->
                state.getSettingForAppAndDisplay(packageName, screen.id) != null
            }
            if (hasAnySettings) {
                RiscOsButton(
                    onClick = {
                        viewModel.removeAppSetting(packageName)
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionRed.copy(alpha = 0.4f)
                ) {
                    RiscOsLabel(
                        text = "✕ Remove All Settings",
                        fontWeight = FontWeight.Bold,
                        color = RiscOsColors.white
                    )
                }
            }
        } else {
            // App not found
            RiscOsPanel(
                modifier = Modifier.fillMaxWidth(),
                inset = true
            ) {
                RiscOsLabel(
                    text = "App not found",
                    maxLines = 1
                )
            }
        }
        }
    }
}

/**
 * Settings panel for a specific screen
 */
@Composable
fun ScreenSettingsPanel(
    screen: TargetScreen.SpecificScreen,
    packageName: String,
    appName: String,
    currentSetting: AppOrientationSetting?,
    viewModel: MainViewModel
) {
    RiscOsWindow(
        title = screen.displayName,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Screen info with flash button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Screen icon and name
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (screen.ratio) {
                        app.rotatescreen.domain.model.AspectRatio.PORTRAIT -> "▯"
                        app.rotatescreen.domain.model.AspectRatio.LANDSCAPE -> "▬"
                        app.rotatescreen.domain.model.AspectRatio.SQUARE -> "▪"
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

                // Flash button
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

            // Orientation selector
            OrientationSelector(
                selectedOrientation = currentSetting?.orientation
                    ?: ScreenOrientation.Unspecified,
                onOrientationSelected = { orientation ->
                    android.util.Log.d("ScreenSettingsPanel", "Orientation selected: ${orientation.displayName} for app $packageName on screen ${screen.displayName} (id=${screen.id})")
                    // Set the target screen for this app
                    viewModel.setAppTargetScreen(packageName, screen)
                    // Then set the orientation
                    viewModel.setAppOrientation(
                        packageName,
                        appName,
                        orientation
                    )
                }
            )

            // Remove button for this screen if setting exists
            if (currentSetting != null) {
                RiscOsButton(
                    onClick = {
                        viewModel.removeAppSettingForScreen(packageName, screen.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionRed.copy(alpha = 0.2f)
                ) {
                    RiscOsLabel(
                        text = "✕ Remove",
                        fontWeight = FontWeight.Bold,
                        color = RiscOsColors.white
                    )
                }
            }
        }
    }
}
