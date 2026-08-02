package app.rotatescreen.ui.screen

import android.content.Intent
import androidx.core.net.toUri
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rotatescreen.domain.model.PermissionStatus
import app.rotatescreen.ui.components.*
import app.rotatescreen.util.ComprehensivePermissionChecker

/**
 * RISC OS styled permission check screen
 * Shows on startup if permissions are missing
 */
@Composable
fun PermissionCheckScreen(
    onDismiss: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionStatus by remember {
        mutableStateOf(ComprehensivePermissionChecker.checkAllPermissions(context))
    }

    // Refresh permissions
    val onRefresh = {
        permissionStatus = ComprehensivePermissionChecker.checkAllPermissions(context)
        if (permissionStatus.allGranted()) {
            onPermissionsGranted()
        }
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
            // Title bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RiscOsColors.actionYellow)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "⚠ Permissions Required",
                    color = RiscOsColors.black,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Instructions
            RiscOsPanel(
                modifier = Modifier.fillMaxWidth(),
                inset = true
            ) {
                RiscOsLabel(
                    text = "Grant permissions below for full functionality. Critical permissions are required.",
                    maxLines = 3,
                    color = RiscOsColors.black
                )
            }

            // Critical Permissions Section
            RiscOsWindow(
                title = "Critical Permissions",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    // System Settings
                    PermissionItem(
                        name = "System Settings",
                        description = "Change screen orientation",
                        isGranted = permissionStatus.hasWriteSettings,
                        icon = "⚙",
                        buttonLabel = "Settings",
                        onGrant = {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = "package:${context.packageName}".toUri()
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )

                    // Overlay Permission
                    PermissionItem(
                        name = "Draw Over Apps",
                        description = "Screen flash and overlays",
                        isGranted = permissionStatus.hasOverlayPermission,
                        icon = "🪟",
                        buttonLabel = "Overlay",
                        onGrant = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = "package:${context.packageName}".toUri()
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )

                    // Usage Stats
                    PermissionItem(
                        name = "Usage Stats",
                        description = "Detect current app",
                        isGranted = permissionStatus.hasUsageStatsPermission,
                        icon = "📊",
                        buttonLabel = "Usage",
                        onGrant = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )

                    // Accessibility Service
                    PermissionItem(
                        name = "Accessibility Service",
                        description = "Detect foreground apps",
                        isGranted = permissionStatus.isAccessibilityServiceEnabled,
                        icon = "♿",
                        buttonLabel = "A11y",
                        onGrant = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Progress
            RiscOsPanel(
                modifier = Modifier.fillMaxWidth(),
                inset = true,
                backgroundColor = RiscOsColors.lightGray
            ) {
                val criticalGranted = listOf(
                    permissionStatus.hasWriteSettings,
                    permissionStatus.hasOverlayPermission,
                    permissionStatus.hasUsageStatsPermission,
                    permissionStatus.isAccessibilityServiceEnabled
                ).count { it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$criticalGranted/4",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (criticalGranted == 4) RiscOsColors.actionGreen else RiscOsColors.actionRed
                        )
                        RiscOsLabel(text = "Critical Permissions", color = RiscOsColors.black)
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiscOsButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    backgroundColor = RiscOsColors.actionBlue.copy(alpha = 0.3f)
                ) {
                    RiscOsLabel(
                        text = "↻ Refresh",
                        fontWeight = FontWeight.Bold,
                        color = RiscOsColors.black
                    )
                }

                RiscOsButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    backgroundColor = if (permissionStatus.allGranted())
                        RiscOsColors.actionGreen
                    else
                        RiscOsColors.mediumGray
                ) {
                    RiscOsLabel(
                        text = if (permissionStatus.allGranted()) "✓ Continue" else "⧗ Pending",
                        fontWeight = FontWeight.Bold,
                        color = if (permissionStatus.allGranted()) RiscOsColors.black else RiscOsColors.veryDarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PermissionItem(
    name: String,
    description: String,
    isGranted: Boolean,
    icon: String,
    buttonLabel: String,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon and text
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Column {
                RiscOsLabel(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    color = RiscOsColors.black
                )
                RiscOsLabel(
                    text = description,
                    color = RiscOsColors.darkGray
                )
            }
        }

        // Status or button
        if (isGranted) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.headlineMedium,
                color = RiscOsColors.actionGreen,
                fontWeight = FontWeight.Bold
            )
        } else {
            RiscOsButton(
                onClick = onGrant,
                backgroundColor = RiscOsColors.actionYellow,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                RiscOsLabel(
                    text = buttonLabel,
                    fontWeight = FontWeight.Bold,
                    color = RiscOsColors.black
                )
            }
        }
    }
}

