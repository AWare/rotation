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
import androidx.compose.ui.unit.sp
import app.rotatescreen.ui.MainViewModel
import app.rotatescreen.ui.components.*

/**
 * Adaptive Tactile Deck Main Screen
 * Optimized for dual-screen AYN Thor handhelds (D-pad focus + multi-display target)
 * and Boox E-Ink readers (direct 1-tap orientation grid + 100% contrast).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToPerApp: () -> Unit,
    onNavigateToMultiScreenManager: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val availableScreens by viewModel.availableScreens.collectAsState()
    val selectedGlobalScreen by viewModel.selectedGlobalScreen.collectAsState()

    val isEInk = RiscOsColors.currentPalette == RiscOsPalette.EInk

    MottledBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tactile Deck Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isEInk) RiscOsColors.black else RiscOsColors.actionBlue)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROTATION CONTROL",
                        color = RiscOsColors.white,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "AYN THOR & BOOX E-INK DECK",
                        color = RiscOsColors.white.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                TactileBadge(
                    text = RiscOsColors.currentPalette.name.uppercase(),
                    backgroundColor = if (isEInk) RiscOsColors.white else RiscOsColors.actionGreen,
                    textColor = if (isEInk) RiscOsColors.black else RiscOsColors.white
                )
            }

            // Permission warnings
            if (!state.hasDrawOverlayPermission || !state.isAccessibilityServiceEnabled) {
                TactileButton(
                    onClick = {
                        if (!state.hasDrawOverlayPermission) viewModel.requestDrawOverlayPermission()
                        else viewModel.requestAccessibilityPermission()
                    },
                    backgroundColor = RiscOsColors.actionYellow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TactileLabel(
                        text = if (!state.hasDrawOverlayPermission)
                            "⚠ TAP TO GRANT OVERLAY PERMISSION"
                        else
                            "⚠ TAP TO ENABLE ACCESSIBILITY SERVICE",
                        fontWeight = FontWeight.Bold,
                        color = RiscOsColors.black
                    )
                }
            }

            // Screen selector for dual-screen setups (AYN Thor Screen 1 vs Screen 2)
            if (availableScreens.size > 1) {
                ScreenSelector(
                    availableScreens = availableScreens,
                    selectedScreen = selectedGlobalScreen,
                    onScreenSelected = { viewModel.setGlobalTargetScreen(it) },
                    onScreenFlash = { screen ->
                        viewModel.flashScreen(screen)
                    }
                )
            }

            // Global Orientation direct-action grid
            OrientationSelector(
                selectedOrientation = state.globalOrientation,
                onOrientationSelected = { viewModel.setGlobalOrientation(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Per-app rules card
            TactileCard(
                title = "Per-App Rules",
                subtitle = "Set different orientations for individual apps",
                statusBadge = if (state.perAppSettings.isNotEmpty()) "${state.perAppSettings.size} Configured" else "None",
                statusColor = RiscOsColors.actionGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                TactileButton(
                    onClick = onNavigateToPerApp,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionGreen.copy(alpha = if (isEInk) 1f else 0.25f)
                ) {
                    TactileLabel(
                        text = "CONFIGURE PER-APP RULES ▶",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Multi-Screen Manager card
            TactileCard(
                title = "Multi-Screen Manager",
                subtitle = "Manage & target apps across dual displays",
                statusBadge = "Thor Dual View",
                statusColor = RiscOsColors.actionBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                TactileButton(
                    onClick = onNavigateToMultiScreenManager,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionBlue.copy(alpha = if (isEInk) 1f else 0.25f)
                ) {
                    TactileLabel(
                        text = "MANAGE SCREENS ▶",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Appearance & Deck Palette Card
            TactileCard(
                title = "Appearance & E-Ink Deck Mode",
                subtitle = "Switch palettes or optimize for Boox E-Ink display",
                statusBadge = RiscOsColors.currentPalette.name,
                statusColor = RiscOsColors.actionYellow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TactileButton(
                        onClick = { viewModel.cyclePaletteBack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        TactileLabel(text = "◀ PREV PALETTE", fontWeight = FontWeight.Bold)
                    }
                    TactileButton(
                        onClick = { viewModel.cyclePalette() },
                        modifier = Modifier.weight(1f)
                    ) {
                        TactileLabel(text = "NEXT PALETTE ▶", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Diagnostics Card
            TactileCard(
                title = "Diagnostics & System Logs",
                subtitle = "View system logs and troubleshoot orientation hooks",
                modifier = Modifier.fillMaxWidth()
            ) {
                TactileButton(
                    onClick = onNavigateToLogs,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RiscOsColors.actionYellow.copy(alpha = if (isEInk) 1f else 0.25f)
                ) {
                    TactileLabel(
                        text = "VIEW SYSTEM LOGS ▶",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
