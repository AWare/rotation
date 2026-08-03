package app.rotatescreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tactile Focus Ring modifier for Gamepad/D-Pad navigation (AYN Thor handhelds)
 * Draws a sharp high-contrast focus indicator ring when focused via D-pad or physical controller.
 */
@Composable
fun Modifier.tactileFocusRing(
    isFocused: Boolean,
    focusColor: Color = if (RiscOsColors.currentPalette == RiscOsPalette.EInk) Color.Black else Color(0xFF00F0FF),
    strokeWidth: Dp = 2.dp
): Modifier {
    return this.border(
        width = if (isFocused) strokeWidth else 0.dp,
        color = if (isFocused) focusColor else Color.Transparent,
        shape = RectangleShape
    )
}

/**
 * Industrial Tactile Card - Clean sharp rectangular container with high-contrast borders.
 * Zero noisy lozenges or canvas shaders for instant E-Ink redraws on Boox devices.
 */
@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    statusBadge: String? = null,
    statusColor: Color = RiscOsColors.actionBlue,
    content: @Composable ColumnScope.() -> Unit
) {
    val isEInk = RiscOsColors.currentPalette == RiscOsPalette.EInk
    val cardBackground = if (isEInk) RiscOsColors.white else RiscOsColors.lightGray.copy(alpha = 0.85f)
    val borderColor = if (isEInk) RiscOsColors.black else RiscOsColors.darkGray.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .clip(RectangleShape)
            .background(cardBackground)
            .border(
                width = if (isEInk) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RectangleShape
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.uppercase(),
                        color = RiscOsColors.veryDarkGray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = RiscOsColors.darkGray,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (statusBadge != null) {
                    TactileBadge(
                        text = statusBadge,
                        backgroundColor = statusColor
                    )
                }
            }
        }

        content()
    }
}

/**
 * Rectangular Status Badge for Dual-Screen displays and orientation state
 */
@Composable
fun TactileBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = RiscOsColors.actionBlue,
    textColor: Color = RiscOsColors.white
) {
    val isEInk = RiscOsColors.currentPalette == RiscOsPalette.EInk
    val bg = if (isEInk) RiscOsColors.black else backgroundColor
    val fg = if (isEInk) RiscOsColors.white else textColor

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(bg)
            .border(width = 1.dp, color = if (isEInk) RiscOsColors.white else Color.Transparent, shape = RectangleShape)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

/**
 * Industrial Tactile Button with sharp rectangular edges and D-Pad controller focus support
 */
@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    backgroundColor: Color = RiscOsColors.lightGray,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val isEInk = RiscOsColors.currentPalette == RiscOsPalette.EInk

    val bg = when {
        isSelected && isEInk -> RiscOsColors.black
        isSelected -> RiscOsColors.actionBlue
        isEInk -> RiscOsColors.white
        else -> backgroundColor
    }

    val borderCol = when {
        isFocused && isEInk -> RiscOsColors.black
        isFocused -> Color(0xFF00F0FF)
        isSelected && !isEInk -> RiscOsColors.actionBlue
        isEInk -> RiscOsColors.black
        else -> RiscOsColors.darkGray.copy(alpha = 0.5f)
    }

    val borderWidth = when {
        isFocused -> 2.dp
        isSelected -> 1.5.dp
        isEInk -> 1.dp
        else -> 1.dp
    }

    Box(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = enabled)
            .clip(RectangleShape)
            .background(bg)
            .border(width = borderWidth, color = borderCol, shape = RectangleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Text label for Tactile Deck components with dynamic palette color support
 */
@Composable
fun TactileLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    isSelected: Boolean = false,
    color: Color = RiscOsColors.black,
    fontSize: Float = 13f,
    maxLines: Int = 1
) {
    val isEInk = RiscOsColors.currentPalette == RiscOsPalette.EInk
    val textColor = when {
        isSelected && isEInk -> RiscOsColors.white
        isSelected -> RiscOsColors.white
        else -> color
    }

    Text(
        text = text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        color = textColor,
        fontSize = fontSize.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
