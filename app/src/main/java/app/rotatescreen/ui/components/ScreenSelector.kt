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
 * RISC OS style display/screen selector
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

    Column(modifier = modifier) {
        RiscOsLabel(
            text = "Target Display:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableScreens.forEach { screen ->
                ScreenButton(
                    screen = screen,
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
private fun ScreenButton(
    screen: TargetScreen,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFlash: ((TargetScreen) -> Unit)?,
    modifier: Modifier = Modifier
) {
    RiscOsButton(
        onClick = {
            onClick()
            // Flash the screen when clicked
            if (screen !is TargetScreen.AllScreens) {
                onFlash?.invoke(screen)
            }
        },
        isSelected = isSelected,
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        backgroundColor = if (screen is TargetScreen.AllScreens)
            RiscOsColors.actionGreen.copy(alpha = 0.3f)
        else
            RiscOsColors.lightGray
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon symbol - aspect ratio aware with distinct shapes
            val icon = when (screen) {
                is TargetScreen.AllScreens -> "⊞"
                is TargetScreen.SpecificScreen -> when (screen.ratio) {
                    AspectRatio.PORTRAIT -> "▯"     // Tall rectangle (portrait)
                    AspectRatio.LANDSCAPE -> "▬"    // Very wide rectangle (widescreen)
                    AspectRatio.SQUARE -> "▪"       // Small square (nearly square display)
                }
            }

            RiscOsLabel(
                text = icon,
                fontWeight = FontWeight.Bold,
                color = RiscOsColors.white
            )
            Spacer(modifier = Modifier.width(4.dp))
            RiscOsLabel(
                text = screen.displayName,
                maxLines = 1,
                color = RiscOsColors.white
            )
        }
    }
}
