package app.rotatescreen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rotatescreen.ui.MainViewModel
import app.rotatescreen.ui.components.*

/**
 * Adaptive Tactile Deck Main Screen
 * Optimized for dual-screen AYN Thor handhelds and Boox E-Ink readers.
 * Features status bar safe insets, multi-display auto-detection, and a clean dropdown menu for secondary functions.
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
    var showMenu by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    // Multi-display is only relevant if device actually has more than 1 display
    val isMultiDisplayDevice = availableScreens.size > 1

    MottledBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Generous status bar breathing space
                .padding(top = 12.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tactile Deck Top Bar with status bar breathing space & Menu trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isEInk) RiscOsColors.black else RiscOsColors.actionBlue)
                    .border(width = 1.dp, color = if (isEInk) RiscOsColors.white else Color.Transparent, shape = RectangleShape)
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
                        text = if (isMultiDisplayDevice) "THOR DUAL DISPLAY DECK" else "E-INK & TACTILE DECK",
                        color = RiscOsColors.white.copy(alpha = 0.85f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Box {
                    // Menu Action Button (replaces lozenge badge with interactive sharp menu trigger)
                    TactileButton(
                        onClick = { showMenu = !showMenu },
                        isSelected = showMenu,
                        backgroundColor = if (isEInk) RiscOsColors.white else RiscOsColors.actionGreen,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        TactileLabel(
                            text = "MENU ▾",
                            fontWeight = FontWeight.Bold,
                            color = if (isEInk && !showMenu) RiscOsColors.black else RiscOsColors.white,
                            fontSize = 11f
                        )
                    }

                    // Dropdown menu for secondary functions
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(if (isEInk) RiscOsColors.white else RiscOsColors.lightGray)
                            .border(width = 1.5.dp, color = RiscOsColors.black, shape = RectangleShape)
                    ) {
                        DropdownMenuItem(
                            text = {
                                TactileLabel(
                                    text = "⚡ CONFIGURE PER-APP RULES",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToPerApp()
                            }
                        )

                        // Multi-display manager option is ONLY shown if device actually has multiple displays
                        if (isMultiDisplayDevice) {
                            DropdownMenuItem(
                                text = {
                                    TactileLabel(
                                        text = "🖥 MULTI-DISPLAY MANAGER",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onNavigateToMultiScreenManager()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                TactileLabel(
                                    text = "🎨 PALETTE: ${RiscOsColors.currentPalette.name}",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                viewModel.cyclePalette()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                TactileLabel(
                                    text = "🔍 DIAGNOSTICS & LOGS",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToLogs()
                            }
                        )
                    }
                }
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

            // Target Display selector: Only displayed if device has multiple displays (e.g. AYN Thor)
            if (isMultiDisplayDevice) {
                ScreenSelector(
                    availableScreens = availableScreens,
                    selectedScreen = selectedGlobalScreen,
                    onScreenSelected = { viewModel.setGlobalTargetScreen(it) },
                    onScreenFlash = { screen ->
                        viewModel.flashScreen(screen)
                    }
                )
            }

            // Primary Orientation Control (Direct Action Grid)
            OrientationSelector(
                selectedOrientation = state.globalOrientation,
                onOrientationSelected = { viewModel.setGlobalOrientation(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Primary Per-App Action Card
            TactileCard(
                title = "Per-App Rules",
                subtitle = "Apply automatic orientation per application",
                statusBadge = if (state.perAppSettings.isNotEmpty()) "${state.perAppSettings.size} Rules" else "Standard",
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

            // Secondary functions drawer toggle
            TactileButton(
                onClick = { showMoreOptions = !showMoreOptions },
                isSelected = showMoreOptions,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = RiscOsColors.lightGray
            ) {
                TactileLabel(
                    text = if (showMoreOptions) "▲ HIDE ADVANCED OPTIONS" else "▼ SHOW ADVANCED OPTIONS",
                    fontWeight = FontWeight.Bold
                )
            }

            // Advanced / Secondary Functions (Hidden until requested or via menu)
            if (showMoreOptions) {
                if (isMultiDisplayDevice) {
                    TactileCard(
                        title = "Multi-Display Target",
                        subtitle = "Manage screens on dual-display devices",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TactileButton(
                            onClick = onNavigateToMultiScreenManager,
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = RiscOsColors.actionBlue.copy(alpha = if (isEInk) 1f else 0.25f)
                        ) {
                            TactileLabel(
                                text = "MANAGE MULTI-DISPLAY ▶",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                TactileCard(
                    title = "Appearance & Deck Palette",
                    subtitle = "Current Palette: ${RiscOsColors.currentPalette.name}",
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
                            TactileLabel(text = "◀ PREV", fontWeight = FontWeight.Bold)
                        }
                        TactileButton(
                            onClick = { viewModel.cyclePalette() },
                            modifier = Modifier.weight(1f)
                        ) {
                            TactileLabel(text = "NEXT ▶", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TactileCard(
                    title = "Diagnostics & System Logs",
                    subtitle = "View system logs and troubleshoot hooks",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TactileButton(
                        onClick = onNavigateToLogs,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = RiscOsColors.actionYellow.copy(alpha = if (isEInk) 1f else 0.25f)
                    ) {
                        TactileLabel(
                            text = "VIEW DIAGNOSTIC LOGS ▶",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
