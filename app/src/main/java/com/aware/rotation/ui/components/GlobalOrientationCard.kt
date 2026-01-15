package com.aware.rotation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aware.rotation.domain.model.ScreenOrientation

@Composable
fun GlobalOrientationCard(
    currentOrientation: ScreenOrientation,
    onOrientationSelected: (ScreenOrientation) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Global Orientation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentOrientation.displayName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
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
