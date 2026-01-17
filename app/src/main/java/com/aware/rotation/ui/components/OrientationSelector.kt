package com.aware.rotation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aware.rotation.domain.model.ScreenOrientation

/**
 * RISC OS style orientation selector
 */
@Composable
fun OrientationSelector(
    selectedOrientation: ScreenOrientation,
    onOrientationSelected: (ScreenOrientation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RiscOsLabel(
            text = "Orientation:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Grid of orientation buttons - 3 columns
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // First row: Auto, Portrait, Landscape
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OrientationButton(
                    orientation = ScreenOrientation.Unspecified,
                    isSelected = selectedOrientation == ScreenOrientation.Unspecified,
                    onClick = { onOrientationSelected(ScreenOrientation.Unspecified) },
                    modifier = Modifier.weight(1f)
                )
                OrientationButton(
                    orientation = ScreenOrientation.Portrait,
                    isSelected = selectedOrientation == ScreenOrientation.Portrait,
                    onClick = { onOrientationSelected(ScreenOrientation.Portrait) },
                    modifier = Modifier.weight(1f)
                )
                OrientationButton(
                    orientation = ScreenOrientation.Landscape,
                    isSelected = selectedOrientation == ScreenOrientation.Landscape,
                    onClick = { onOrientationSelected(ScreenOrientation.Landscape) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row: Reverse orientations and Sensor
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OrientationButton(
                    orientation = ScreenOrientation.ReversePortrait,
                    isSelected = selectedOrientation == ScreenOrientation.ReversePortrait,
                    onClick = { onOrientationSelected(ScreenOrientation.ReversePortrait) },
                    modifier = Modifier.weight(1f)
                )
                OrientationButton(
                    orientation = ScreenOrientation.ReverseLandscape,
                    isSelected = selectedOrientation == ScreenOrientation.ReverseLandscape,
                    onClick = { onOrientationSelected(ScreenOrientation.ReverseLandscape) },
                    modifier = Modifier.weight(1f)
                )
                OrientationButton(
                    orientation = ScreenOrientation.Sensor,
                    isSelected = selectedOrientation == ScreenOrientation.Sensor,
                    onClick = { onOrientationSelected(ScreenOrientation.Sensor) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OrientationButton(
    orientation: ScreenOrientation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RiscOsButton(
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier.height(56.dp),
        contentPadding = PaddingValues(6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon representation with text characters
            RiscOsLabel(
                text = getOrientationSymbol(orientation),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            RiscOsLabel(
                text = getShortName(orientation),
                maxLines = 1
            )
        }
    }
}

private fun getOrientationSymbol(orientation: ScreenOrientation): String {
    return when (orientation) {
        ScreenOrientation.Unspecified -> "↻"
        ScreenOrientation.Portrait -> "▯"
        ScreenOrientation.Landscape -> "▭"
        ScreenOrientation.ReversePortrait -> "⤸"
        ScreenOrientation.ReverseLandscape -> "⤹"
        ScreenOrientation.Sensor -> "◎"
    }
}

private fun getShortName(orientation: ScreenOrientation): String {
    return when (orientation) {
        ScreenOrientation.Unspecified -> "Auto"
        ScreenOrientation.Portrait -> "Port"
        ScreenOrientation.Landscape -> "Land"
        ScreenOrientation.ReversePortrait -> "R.Port"
        ScreenOrientation.ReverseLandscape -> "R.Land"
        ScreenOrientation.Sensor -> "Sensor"
    }
}
