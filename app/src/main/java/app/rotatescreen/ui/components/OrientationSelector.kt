package app.rotatescreen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rotatescreen.domain.model.ScreenOrientation

/**
 * Adaptive Tactile Orientation Selector
 * Designed for 1-tap instant action on Boox E-Ink and D-pad controller navigation on AYN Thor.
 */
@Composable
fun OrientationSelector(
    selectedOrientation: ScreenOrientation,
    onOrientationSelected: (ScreenOrientation) -> Unit,
    modifier: Modifier = Modifier
) {
    TactileCard(
        title = "Target Orientation",
        subtitle = "Select screen rotation rule",
        statusBadge = selectedOrientation.displayName,
        statusColor = RiscOsColors.actionBlue,
        modifier = modifier
    ) {
        // Grid of orientation buttons - 2 rows x 3 columns
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // First row: Auto, Portrait, Landscape
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OrientationTile(
                    orientation = ScreenOrientation.Unspecified,
                    isSelected = selectedOrientation == ScreenOrientation.Unspecified,
                    onClick = { onOrientationSelected(ScreenOrientation.Unspecified) },
                    modifier = Modifier.weight(1f)
                )
                OrientationTile(
                    orientation = ScreenOrientation.Portrait,
                    isSelected = selectedOrientation == ScreenOrientation.Portrait,
                    onClick = { onOrientationSelected(ScreenOrientation.Portrait) },
                    modifier = Modifier.weight(1f)
                )
                OrientationTile(
                    orientation = ScreenOrientation.Landscape,
                    isSelected = selectedOrientation == ScreenOrientation.Landscape,
                    onClick = { onOrientationSelected(ScreenOrientation.Landscape) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row: Reverse Portrait, Reverse Landscape, Sensor
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OrientationTile(
                    orientation = ScreenOrientation.ReversePortrait,
                    isSelected = selectedOrientation == ScreenOrientation.ReversePortrait,
                    onClick = { onOrientationSelected(ScreenOrientation.ReversePortrait) },
                    modifier = Modifier.weight(1f)
                )
                OrientationTile(
                    orientation = ScreenOrientation.ReverseLandscape,
                    isSelected = selectedOrientation == ScreenOrientation.ReverseLandscape,
                    onClick = { onOrientationSelected(ScreenOrientation.ReverseLandscape) },
                    modifier = Modifier.weight(1f)
                )
                OrientationTile(
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
private fun OrientationTile(
    orientation: ScreenOrientation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEInk = RiscOsColors.currentPalette == RiscOsPalette.EInk
    val strokeColor = when {
        isSelected && isEInk -> RiscOsColors.white
        isSelected -> RiscOsColors.white
        isEInk -> RiscOsColors.black
        else -> RiscOsColors.veryDarkGray
    }

    TactileButton(
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier.height(64.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Distinct Visual Phone Frame graphic
            OrientationFrameGraphic(
                orientation = orientation,
                strokeColor = strokeColor,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            TactileLabel(
                text = getShortName(orientation),
                isSelected = isSelected,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11f,
                maxLines = 1
            )
        }
    }
}

/**
 * Draws distinct visual device frame graphics for each orientation
 */
@Composable
private fun OrientationFrameGraphic(
    orientation: ScreenOrientation,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidthPx = 1.8.dp.toPx()

        when (orientation) {
            ScreenOrientation.Unspecified -> {
                // AUTO: Sharp rectangular phone outline inside double curved rotation arrows
                val frameW = w * 0.45f
                val frameH = h * 0.65f
                drawRect(
                    color = strokeColor,
                    topLeft = Offset((w - frameW) / 2f, (h - frameH) / 2f),
                    size = Size(frameW, frameH),
                    style = Stroke(width = strokeWidthPx)
                )

                // Top-right and bottom-left rotation arcs
                val arcPath = Path().apply {
                    // Top arc
                    moveTo(w * 0.15f, h * 0.25f)
                    quadraticTo(w * 0.5f, h * 0.05f, w * 0.85f, h * 0.25f)
                    // Bottom arc
                    moveTo(w * 0.85f, h * 0.75f)
                    quadraticTo(w * 0.5f, h * 0.95f, w * 0.15f, h * 0.75f)
                }
                drawPath(path = arcPath, color = strokeColor, style = Stroke(width = strokeWidthPx))
            }

            ScreenOrientation.Portrait -> {
                // PORTRAIT: Clear vertical phone frame with top notch & UP arrow
                val frameW = w * 0.5f
                val frameH = h * 0.85f
                val left = (w - frameW) / 2f
                val top = (h - frameH) / 2f

                drawRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(frameW, frameH),
                    style = Stroke(width = strokeWidthPx)
                )

                // Top speaker notch
                drawLine(
                    color = strokeColor,
                    start = Offset(w / 2f - 3.dp.toPx(), top + 3.dp.toPx()),
                    end = Offset(w / 2f + 3.dp.toPx(), top + 3.dp.toPx()),
                    strokeWidth = strokeWidthPx
                )

                // Up arrow in center
                val arrowPath = Path().apply {
                    moveTo(w / 2f, h / 2f - 4.dp.toPx())
                    lineTo(w / 2f - 4.dp.toPx(), h / 2f + 2.dp.toPx())
                    moveTo(w / 2f, h / 2f - 4.dp.toPx())
                    lineTo(w / 2f + 4.dp.toPx(), h / 2f + 2.dp.toPx())
                    moveTo(w / 2f, h / 2f - 4.dp.toPx())
                    lineTo(w / 2f, h / 2f + 5.dp.toPx())
                }
                drawPath(path = arrowPath, color = strokeColor, style = Stroke(width = strokeWidthPx))
            }

            ScreenOrientation.Landscape -> {
                // LANDSCAPE: Wide horizontal phone frame with side notch
                val frameW = w * 0.85f
                val frameH = h * 0.5f
                val left = (w - frameW) / 2f
                val top = (h - frameH) / 2f

                drawRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(frameW, frameH),
                    style = Stroke(width = strokeWidthPx)
                )

                // Left speaker notch
                drawLine(
                    color = strokeColor,
                    start = Offset(left + 3.dp.toPx(), h / 2f - 3.dp.toPx()),
                    end = Offset(left + 3.dp.toPx(), h / 2f + 3.dp.toPx()),
                    strokeWidth = strokeWidthPx
                )

                // Right arrow in center
                val arrowPath = Path().apply {
                    moveTo(w / 2f + 4.dp.toPx(), h / 2f)
                    lineTo(w / 2f - 2.dp.toPx(), h / 2f - 4.dp.toPx())
                    moveTo(w / 2f + 4.dp.toPx(), h / 2f)
                    lineTo(w / 2f - 2.dp.toPx(), h / 2f + 4.dp.toPx())
                    moveTo(w / 2f + 4.dp.toPx(), h / 2f)
                    lineTo(w / 2f - 5.dp.toPx(), h / 2f)
                }
                drawPath(path = arrowPath, color = strokeColor, style = Stroke(width = strokeWidthPx))
            }

            ScreenOrientation.ReversePortrait -> {
                // REVERSE PORTRAIT: Inverted phone (notch at bottom, DOWN arrow)
                val frameW = w * 0.5f
                val frameH = h * 0.85f
                val left = (w - frameW) / 2f
                val top = (h - frameH) / 2f

                drawRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(frameW, frameH),
                    style = Stroke(width = strokeWidthPx)
                )

                // Bottom speaker notch
                drawLine(
                    color = strokeColor,
                    start = Offset(w / 2f - 3.dp.toPx(), top + frameH - 3.dp.toPx()),
                    end = Offset(w / 2f + 3.dp.toPx(), top + frameH - 3.dp.toPx()),
                    strokeWidth = strokeWidthPx
                )

                // Down arrow in center
                val arrowPath = Path().apply {
                    moveTo(w / 2f, h / 2f + 4.dp.toPx())
                    lineTo(w / 2f - 4.dp.toPx(), h / 2f - 2.dp.toPx())
                    moveTo(w / 2f, h / 2f + 4.dp.toPx())
                    lineTo(w / 2f + 4.dp.toPx(), h / 2f - 2.dp.toPx())
                    moveTo(w / 2f, h / 2f + 4.dp.toPx())
                    lineTo(w / 2f, h / 2f - 5.dp.toPx())
                }
                drawPath(path = arrowPath, color = strokeColor, style = Stroke(width = strokeWidthPx))
            }

            ScreenOrientation.ReverseLandscape -> {
                // REVERSE LANDSCAPE: Inverted landscape (notch on right, LEFT arrow)
                val frameW = w * 0.85f
                val frameH = h * 0.5f
                val left = (w - frameW) / 2f
                val top = (h - frameH) / 2f

                drawRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(frameW, frameH),
                    style = Stroke(width = strokeWidthPx)
                )

                // Right speaker notch
                drawLine(
                    color = strokeColor,
                    start = Offset(left + frameW - 3.dp.toPx(), h / 2f - 3.dp.toPx()),
                    end = Offset(left + frameW - 3.dp.toPx(), h / 2f + 3.dp.toPx()),
                    strokeWidth = strokeWidthPx
                )

                // Left arrow in center
                val arrowPath = Path().apply {
                    moveTo(w / 2f - 4.dp.toPx(), h / 2f)
                    lineTo(w / 2f + 2.dp.toPx(), h / 2f - 4.dp.toPx())
                    moveTo(w / 2f - 4.dp.toPx(), h / 2f)
                    lineTo(w / 2f + 2.dp.toPx(), h / 2f + 4.dp.toPx())
                    moveTo(w / 2f - 4.dp.toPx(), h / 2f)
                    lineTo(w / 2f + 5.dp.toPx(), h / 2f)
                }
                drawPath(path = arrowPath, color = strokeColor, style = Stroke(width = strokeWidthPx))
            }

            ScreenOrientation.Sensor -> {
                // SENSOR: Gyroscope sensor waves + phone frame in center
                val frameW = w * 0.42f
                val frameH = h * 0.65f
                drawRect(
                    color = strokeColor,
                    topLeft = Offset((w - frameW) / 2f, (h - frameH) / 2f),
                    size = Size(frameW, frameH),
                    style = Stroke(width = strokeWidthPx)
                )

                // Gyroscope orbit circle ring surrounding phone frame
                drawCircle(
                    color = strokeColor,
                    radius = w * 0.45f,
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
    }
}

private fun getShortName(orientation: ScreenOrientation): String {
    return when (orientation) {
        ScreenOrientation.Unspecified -> "Auto"
        ScreenOrientation.Portrait -> "Portrait"
        ScreenOrientation.Landscape -> "Landscape"
        ScreenOrientation.ReversePortrait -> "Rev.Port"
        ScreenOrientation.ReverseLandscape -> "Rev.Land"
        ScreenOrientation.Sensor -> "Sensor"
    }
}
