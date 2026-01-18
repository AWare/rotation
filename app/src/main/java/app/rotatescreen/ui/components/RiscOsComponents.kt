package app.rotatescreen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

/**
 * RISC OS Color Palette
 */
data class RiscOsPalette(
    val name: String,
    val background: Color,
    val lightGray: Color,
    val mediumGray: Color,
    val darkGray: Color,
    val veryDarkGray: Color,
    val white: Color,
    val black: Color,
    val actionBlue: Color,
    val actionGreen: Color,
    val actionRed: Color,
    val actionYellow: Color
) {
    companion object {
        // Classic RISC OS 3 grey palette
        val Classic = RiscOsPalette(
            name = "Classic",
            background = Color(0xFFBBBBBB),
            lightGray = Color(0xFFDDDDDD),
            mediumGray = Color(0xFFBBBBBB),
            darkGray = Color(0xFF888888),
            veryDarkGray = Color(0xFF444444),
            white = Color(0xFFFFFFFF),
            black = Color(0xFF000000),
            actionBlue = Color(0xFF0000DD),
            actionGreen = Color(0xFF00DD00),
            actionRed = Color(0xFFDD0000),
            actionYellow = Color(0xFFDDDD00)
        )

        // RISC OS "Aqua" palette (lighter, blue-tinted)
        val Aqua = RiscOsPalette(
            name = "Aqua",
            background = Color(0xFFAABBCC),
            lightGray = Color(0xFFCCDDEE),
            mediumGray = Color(0xFFAABBCC),
            darkGray = Color(0xFF778899),
            veryDarkGray = Color(0xFF445566),
            white = Color(0xFFFFFFFF),
            black = Color(0xFF000000),
            actionBlue = Color(0xFF0066CC),
            actionGreen = Color(0xFF00AA88),
            actionRed = Color(0xFFCC4444),
            actionYellow = Color(0xFFDD9900)
        )

        // RISC OS "Sand" palette (warm, beige tones)
        val Sand = RiscOsPalette(
            name = "Sand",
            background = Color(0xFFCCBBAA),
            lightGray = Color(0xFFEEDDCC),
            mediumGray = Color(0xFFCCBBAA),
            darkGray = Color(0xFF998877),
            veryDarkGray = Color(0xFF665544),
            white = Color(0xFFFFFFFF),
            black = Color(0xFF000000),
            actionBlue = Color(0xFF6666AA),
            actionGreen = Color(0xFF88AA66),
            actionRed = Color(0xFFCC6644),
            actionYellow = Color(0xFFDDAA44)
        )

        // Dark mode (for fun)
        val Dark = RiscOsPalette(
            name = "Dark",
            background = Color(0xFF222222),
            lightGray = Color(0xFF444444),
            mediumGray = Color(0xFF222222),
            darkGray = Color(0xFF111111),
            veryDarkGray = Color(0xFF000000),
            white = Color(0xFFFFFFFF),
            black = Color(0xFF000000),
            actionBlue = Color(0xFF6699FF),
            actionGreen = Color(0xFF66FF99),
            actionRed = Color(0xFFFF6666),
            actionYellow = Color(0xFFFFDD66)
        )

        val All = listOf(Classic, Aqua, Sand, Dark)
    }
}

/**
 * Current active palette - can be changed with controller buttons
 */
object RiscOsColors {
    private var _currentPalette by mutableStateOf(RiscOsPalette.Classic)

    val currentPalette: RiscOsPalette
        get() = _currentPalette

    fun setPalette(palette: RiscOsPalette) {
        _currentPalette = palette
    }

    fun nextPalette() {
        val currentIndex = RiscOsPalette.All.indexOf(_currentPalette)
        val nextIndex = (currentIndex + 1) % RiscOsPalette.All.size
        _currentPalette = RiscOsPalette.All[nextIndex]
    }

    fun previousPalette() {
        val currentIndex = RiscOsPalette.All.indexOf(_currentPalette)
        val prevIndex = if (currentIndex == 0) RiscOsPalette.All.size - 1 else currentIndex - 1
        _currentPalette = RiscOsPalette.All[prevIndex]
    }

    // Accessor properties for current palette
    val background: Color get() = _currentPalette.background
    val lightGray: Color get() = _currentPalette.lightGray
    val mediumGray: Color get() = _currentPalette.mediumGray
    val darkGray: Color get() = _currentPalette.darkGray
    val veryDarkGray: Color get() = _currentPalette.veryDarkGray
    val white: Color get() = _currentPalette.white
    val black: Color get() = _currentPalette.black
    val actionBlue: Color get() = _currentPalette.actionBlue
    val actionGreen: Color get() = _currentPalette.actionGreen
    val actionRed: Color get() = _currentPalette.actionRed
    val actionYellow: Color get() = _currentPalette.actionYellow
}

/**
 * RISC OS style mottled background texture
 * Creates a stippled/dithered effect like classic RISC OS desktop
 */
@Composable
fun MottledBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = RiscOsColors.background,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Draw mottled pattern
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width.toInt()
            val height = size.height.toInt()
            val seed = 42 // Fixed seed for consistent pattern
            val rng = Random(seed)

            // Draw stipple pattern - every other pixel with slight variation
            for (x in 0 until width step 2) {
                for (y in 0 until height step 2) {
                    val variation = (rng.nextInt(7) - 3) / 255f // -3 to +3
                    val color = Color(
                        red = (baseColor.red + variation).coerceIn(0f, 1f),
                        green = (baseColor.green + variation).coerceIn(0f, 1f),
                        blue = (baseColor.blue + variation).coerceIn(0f, 1f),
                        alpha = 1f
                    )
                    drawRect(
                        color = color,
                        topLeft = Offset(x.toFloat(), y.toFloat()),
                        size = androidx.compose.ui.geometry.Size(2f, 2f)
                    )
                }
            }
        }

        // Content on top
        content()
    }
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
 * RISC OS style text label with pixelated/retro font styling
 */
@Composable
fun RiscOsLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
    color: Color = RiscOsColors.black
) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        color = color,
        style = MaterialTheme.typography.bodySmall.copy(
            letterSpacing = 0.05.sp  // Slightly wider spacing for pixelated look
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
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
