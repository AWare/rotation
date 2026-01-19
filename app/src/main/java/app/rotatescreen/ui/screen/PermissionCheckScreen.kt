package app.rotatescreen.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rotatescreen.domain.model.PermissionStatus
import app.rotatescreen.util.ComprehensivePermissionChecker

/**
 * Permission check screen that shows on startup if permissions are missing
 * Displays visual ticks for granted permissions and guides users to grant missing ones
 */
@Composable
fun PermissionCheckScreen(
    onDismiss: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(ComprehensivePermissionChecker.checkAllPermissions(context)) }

    // Refresh permissions when screen regains focus
    DisposableEffect(Unit) {
        val refresher = {
            permissionStatus = ComprehensivePermissionChecker.checkAllPermissions(context)
            if (permissionStatus.allGranted()) {
                onPermissionsGranted()
            }
        }
        onDispose { }
    }

    // Refresh button callback
    val onRefresh = {
        permissionStatus = ComprehensivePermissionChecker.checkAllPermissions(context)
        if (permissionStatus.allGranted()) {
            onPermissionsGranted()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Permissions",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Grant the following permissions for full functionality",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Critical Permissions
            Text(
                text = "CRITICAL PERMISSIONS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Write Settings Permission
            PermissionItem(
                name = "System Settings",
                description = "Required to change screen orientation",
                isGranted = permissionStatus.hasWriteSettings,
                icon = Icons.Default.Settings,
                onGrant = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Overlay Permission
            PermissionItem(
                name = "Draw Over Apps",
                description = "Required for screen flash and overlays",
                isGranted = permissionStatus.hasOverlayPermission,
                icon = Icons.Default.Layers,
                onGrant = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Usage Stats Permission
            PermissionItem(
                name = "Usage Stats",
                description = "Required to detect current app for tiles",
                isGranted = permissionStatus.hasUsageStatsPermission,
                icon = Icons.Default.History,
                onGrant = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Accessibility Service
            PermissionItem(
                name = "Accessibility Service",
                description = "Required to detect foreground app changes",
                isGranted = permissionStatus.isAccessibilityServiceEnabled,
                icon = Icons.Default.Accessibility,
                onGrant = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Optional: Quick Settings Tiles
            Text(
                text = "QUICK SETTINGS TILES (OPTIONAL)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Swipe down from top → Edit tiles → Drag tiles to Quick Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TileItem(
                name = "Screen Rotation",
                description = "Cycle through rotation modes",
                isAdded = permissionStatus.tilesAdded.orientationTileAdded
            )

            Spacer(modifier = Modifier.height(12.dp))

            TileItem(
                name = "Global Rotation",
                description = "Change global rotation setting",
                isAdded = permissionStatus.tilesAdded.globalOrientationTileAdded
            )

            Spacer(modifier = Modifier.height(12.dp))

            TileItem(
                name = "Current App Rotation",
                description = "Change rotation for current app",
                isAdded = permissionStatus.tilesAdded.currentAppTileAdded
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Refresh Button
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }

                // Continue Button - enabled if all critical permissions granted
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = permissionStatus.allGranted()
                ) {
                    Text(if (permissionStatus.allGranted()) "Continue" else "Pending...")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicator
            PermissionProgress(permissionStatus)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PermissionItem(
    name: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = name,
                modifier = Modifier.size(32.dp),
                tint = if (isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Status / Action
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = onGrant,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Grant", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun TileItem(
    name: String,
    description: String,
    isAdded: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = name,
                modifier = Modifier.size(28.dp),
                tint = if (isAdded)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Status
            if (isAdded) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Added",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Not Added",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionProgress(status: PermissionStatus) {
    val criticalGranted = listOf(
        status.hasWriteSettings,
        status.hasOverlayPermission,
        status.hasUsageStatsPermission,
        status.isAccessibilityServiceEnabled
    ).count { it }

    val tilesAdded = status.tilesAdded.tilesAddedCount()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Progress",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProgressIndicator(
                label = "Critical",
                current = criticalGranted,
                total = 4,
                color = MaterialTheme.colorScheme.primary
            )

            ProgressIndicator(
                label = "Tiles",
                current = tilesAdded,
                total = 3,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun ProgressIndicator(
    label: String,
    current: Int,
    total: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$current/$total",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
