package app.rotatescreen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rotatescreen.ui.MainViewModel
import app.rotatescreen.ui.components.*

/**
 * RISC OS styled main screen - global orientation control
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToPerApp: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val availableScreens by viewModel.availableScreens.collectAsState()
    val selectedGlobalScreen by viewModel.selectedGlobalScreen.collectAsState()

    var showGlobalSelector by remember { mutableStateOf(false) }

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
                    onScreenSelected = { viewModel.setGlobalTargetScreen(it) },
                    onScreenFlash = { screen ->
                        viewModel.flashScreen(screen)
                    }
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

        // Per-app settings button
        RiscOsWindow(
            title = "Per-App Settings",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RiscOsLabel(
                    text = "Set different orientations for individual apps.",
                    maxLines = 2
                )

                // Stats display
                val configuredApps = state.perAppSettings.size
                if (configuredApps > 0) {
                    RiscOsLabel(
                        text = "Configured: $configuredApps app${if (configuredApps != 1) "s" else ""}",
                        fontWeight = FontWeight.Bold
                    )
                }

                RiscOsButton(
                    onClick = onNavigateToPerApp,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionGreen.copy(alpha = 0.3f)
                ) {
                    RiscOsLabel(
                        text = "Configure Apps ▶",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
