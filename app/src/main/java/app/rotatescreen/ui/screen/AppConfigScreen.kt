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
    val selectedAppScreens by viewModel.selectedAppScreens.collectAsState()

    val app = remember(filteredApps, packageName) {
        filteredApps.find { it.packageName == packageName }
    }

    // Get the setting for the currently selected screen (reactive to screen selection changes)
    val currentTargetScreen = selectedAppScreens[packageName] ?: TargetScreen.AllScreens
    val currentSetting = state.getSettingForAppAndDisplay(packageName, currentTargetScreen.id)

    // Log when values change
    LaunchedEffect(currentTargetScreen, currentSetting) {
        android.util.Log.d("AppConfigScreen", "Screen updated: ${currentTargetScreen.displayName} (id=${currentTargetScreen.id}), setting=${currentSetting?.orientation?.displayName}")
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
                text = app?.appName ?: packageName,
                color = RiscOsColors.white,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (app != null) {
            // Screen selector
            RiscOsWindow(
                title = "Target Screen",
                modifier = Modifier.fillMaxWidth()
            ) {
                ScreenSelector(
                    availableScreens = availableScreens,
                    selectedScreen = currentTargetScreen,
                    onScreenSelected = {
                        android.util.Log.d("AppConfigScreen", "Screen selected: ${it.displayName} (id=${it.id}) for app $packageName")
                        viewModel.setAppTargetScreen(packageName, it)
                    },
                    onScreenFlash = { screen ->
                        viewModel.flashScreen(screen)
                    }
                )
            }

            // Orientation selector
            RiscOsWindow(
                title = "Orientation",
                modifier = Modifier.fillMaxWidth()
            ) {
                OrientationSelector(
                    selectedOrientation = currentSetting?.orientation
                        ?: ScreenOrientation.Unspecified,
                    onOrientationSelected = { orientation ->
                        android.util.Log.d("AppConfigScreen", "Orientation selected: ${orientation.displayName} for app $packageName on screen ${currentTargetScreen.displayName} (id=${currentTargetScreen.id})")
                        viewModel.setAppOrientation(
                            packageName,
                            app.appName,
                            orientation
                        )
                    }
                )
            }

            // Remove button if setting exists
            if (currentSetting != null) {
                RiscOsButton(
                    onClick = {
                        viewModel.removeAppSetting(packageName)
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionRed.copy(alpha = 0.4f)
                ) {
                    RiscOsLabel(
                        text = "✕ Remove Setting",
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
