package com.aware.rotation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RISC OS themed colors
 */
object RiscOsColors {
    val lightGray = Color(0xFFCCCCCC)
    val mediumGray = Color(0xFFAAAAAA)
    val darkGray = Color(0xFF888888)
    val veryDarkGray = Color(0xFF444444)
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)

    // Classic RISC OS action buttons
    val actionBlue = Color(0xFF0066CC)
    val actionGreen = Color(0xFF00AA00)
    val actionRed = Color(0xFFCC0000)
    val actionYellow = Color(0xFFFFDD00)
}

/**
 * RISC OS style 3D beveled button
 */
@Composable
fun RiscOsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    backgroundColor: Color = RiscOsColors.lightGray,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .drawRiscOsBevel(isPressed = isSelected)
            .background(if (isSelected) RiscOsColors.mediumGray else backgroundColor)
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
 * RISC OS style panel with 3D bevel
 */
@Composable
fun RiscOsPanel(
    modifier: Modifier = Modifier,
    inset: Boolean = true,
    backgroundColor: Color = RiscOsColors.white,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawRiscOsBevel(isPressed = inset)
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        content()
    }
}

/**
 * RISC OS style window/card
 */
@Composable
fun RiscOsWindow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .drawRiscOsBevel(isPressed = false)
            .background(RiscOsColors.lightGray)
    ) {
        // Title bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RiscOsColors.actionBlue)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                color = RiscOsColors.white,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RiscOsColors.white)
                .padding(8.dp)
        ) {
            content()
        }
    }
}

/**
 * RISC OS style text label
 */
@Composable
fun RiscOsLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        color = RiscOsColors.black,
        style = MaterialTheme.typography.bodySmall,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
}

/**
 * Extension to draw RISC OS style 3D bevel effect
 */
fun Modifier.drawRiscOsBevel(
    isPressed: Boolean = false,
    bevelSize: Dp = 2.dp
): Modifier = this.then(
    drawBehind {
        val bevelPx = bevelSize.toPx()
        val width = size.width
        val height = size.height

        if (isPressed) {
            // Inset/pressed look - dark on top-left, light on bottom-right
            // Top edge
            drawLine(
                color = RiscOsColors.veryDarkGray,
                start = Offset(0f, 0f),
                end = Offset(width, 0f),
                strokeWidth = bevelPx
            )
            // Left edge
            drawLine(
                color = RiscOsColors.veryDarkGray,
                start = Offset(0f, 0f),
                end = Offset(0f, height),
                strokeWidth = bevelPx
            )
            // Inner shadow on top-left
            drawLine(
                color = RiscOsColors.darkGray,
                start = Offset(bevelPx, bevelPx),
                end = Offset(width - bevelPx, bevelPx),
                strokeWidth = bevelPx
            )
            drawLine(
                color = RiscOsColors.darkGray,
                start = Offset(bevelPx, bevelPx),
                end = Offset(bevelPx, height - bevelPx),
                strokeWidth = bevelPx
            )
        } else {
            // Raised/outset look - light on top-left, dark on bottom-right
            // Top edge (white highlight)
            drawLine(
                color = RiscOsColors.white,
                start = Offset(0f, 0f),
                end = Offset(width, 0f),
                strokeWidth = bevelPx
            )
            // Left edge (white highlight)
            drawLine(
                color = RiscOsColors.white,
                start = Offset(0f, 0f),
                end = Offset(0f, height),
                strokeWidth = bevelPx
            )
            // Bottom edge (dark shadow)
            drawLine(
                color = RiscOsColors.veryDarkGray,
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = bevelPx
            )
            // Right edge (dark shadow)
            drawLine(
                color = RiscOsColors.veryDarkGray,
                start = Offset(width, 0f),
                end = Offset(width, height),
                strokeWidth = bevelPx
            )
            // Inner shadow on bottom-right
            drawLine(
                color = RiscOsColors.darkGray,
                start = Offset(bevelPx, height - bevelPx),
                end = Offset(width - bevelPx, height - bevelPx),
                strokeWidth = bevelPx
            )
            drawLine(
                color = RiscOsColors.darkGray,
                start = Offset(width - bevelPx, bevelPx),
                end = Offset(width - bevelPx, height - bevelPx),
                strokeWidth = bevelPx
            )
        }
    }
)
