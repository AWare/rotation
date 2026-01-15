package com.aware.rotation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aware.rotation.domain.model.AppOrientationSetting
import com.aware.rotation.domain.model.InstalledApp
import com.aware.rotation.domain.model.ScreenOrientation

@Composable
fun AppOrientationCard(
    app: InstalledApp,
    currentSetting: AppOrientationSetting?,
    onOrientationSelected: (ScreenOrientation) -> Unit,
    onRemoveSetting: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val hasSetting = currentSetting != null

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasSetting) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (hasSetting) {
                    IconButton(onClick = onRemoveSetting) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove setting"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentSetting?.orientation?.displayName ?: "Default",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ScreenOrientation.all().forEach { orientation ->
                        DropdownMenuItem(
                            text = { Text(orientation.displayName) },
                            onClick = {
                                onOrientationSelected(orientation)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
