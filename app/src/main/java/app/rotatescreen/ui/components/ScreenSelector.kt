package app.rotatescreen.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rotatescreen.domain.model.AspectRatio
import app.rotatescreen.domain.model.TargetScreen

/**
 * Adaptive Display Selector - Visualizes dual-screen setups (AYN Thor Screen 1 vs Screen 2)
 */
@Composable
fun ScreenSelector(
    availableScreens: List<TargetScreen>,
    selectedScreen: TargetScreen,
    onScreenSelected: (TargetScreen) -> Unit,
    modifier: Modifier = Modifier,
    onScreenFlash: ((TargetScreen) -> Unit)? = null
) {
    if (availableScreens.isEmpty() || availableScreens.size == 1) {
        return
    }

    TactileCard(
        title = "Target Display",
        subtitle = "AYN Thor / Multi-Screen Display Mapping",
        statusBadge = "${availableScreens.size} Displays",
        statusColor = RiscOsColors.actionGreen,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableScreens.forEachIndexed { index, screen ->
                ScreenTile(
                    screen = screen,
                    index = index,
                    isSelected = selectedScreen.id == screen.id,
                    onClick = { onScreenSelected(screen) },
                    onFlash = onScreenFlash,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScreenTile(
    screen: TargetScreen,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFlash: ((TargetScreen) -> Unit)?,
    modifier: Modifier = Modifier
) {
    TactileButton(
        onClick = {
            onClick()
            if (screen !is TargetScreen.AllScreens) {
                onFlash?.invoke(screen)
            }
        },
        isSelected = isSelected,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val icon = when (screen) {
                is TargetScreen.AllScreens -> "[ALL]"
                is TargetScreen.SpecificScreen -> when (index) {
                    0 -> "[SCR 1]" // Top Screen on Thor
                    1 -> "[SCR 2]" // Bottom Screen on Thor
                    else -> "[SCR ${index + 1}]"
                }
            }

            TactileLabel(
                text = icon,
                isSelected = isSelected,
                fontWeight = FontWeight.Bold,
                fontSize = 11f
            )

            Spacer(modifier = Modifier.width(6.dp))

            TactileLabel(
                text = screen.displayName,
                isSelected = isSelected,
                maxLines = 1,
                fontSize = 12f
            )
        }
    }
}
