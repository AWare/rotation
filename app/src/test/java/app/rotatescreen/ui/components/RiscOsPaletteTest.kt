package app.rotatescreen.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RISC OS Palette functionality
 */
class RiscOsPaletteTest {

    @Before
    fun setup() {
        // Reset to Classic palette before each test
        RiscOsColors.setPalette(RiscOsPalette.Classic)
    }

    // Palette Data Tests

    @Test
    fun `Classic palette has correct colors`() {
        val classic = RiscOsPalette.Classic

        assertEquals("Classic", classic.name)
        assertEquals(Color(0xFFBBBBBB), classic.background)
        assertEquals(Color(0xFFDDDDDD), classic.lightGray)
        assertEquals(Color(0xFFBBBBBB), classic.mediumGray)
        assertEquals(Color(0xFF888888), classic.darkGray)
        assertEquals(Color(0xFF444444), classic.veryDarkGray)
        assertEquals(Color(0xFFFFFFFF), classic.white)
        assertEquals(Color(0xFF000000), classic.black)
        assertEquals(Color(0xFF0000DD), classic.actionBlue)
        assertEquals(Color(0xFF00DD00), classic.actionGreen)
        assertEquals(Color(0xFFDD0000), classic.actionRed)
        assertEquals(Color(0xFFDDDD00), classic.actionYellow)
    }

    @Test
    fun `Aqua palette has correct colors`() {
        val aqua = RiscOsPalette.Aqua

        assertEquals("Aqua", aqua.name)
        assertEquals(Color(0xFFAABBCC), aqua.background)
        assertEquals(Color(0xFFCCDDEE), aqua.lightGray)
        assertEquals(Color(0xFFAABBCC), aqua.mediumGray)
        assertEquals(Color(0xFF778899), aqua.darkGray)
        assertEquals(Color(0xFF445566), aqua.veryDarkGray)
        assertEquals(Color(0xFFFFFFFF), aqua.white)
        assertEquals(Color(0xFF000000), aqua.black)
        assertEquals(Color(0xFF0066CC), aqua.actionBlue)
        assertEquals(Color(0xFF00AA88), aqua.actionGreen)
        assertEquals(Color(0xFFCC4444), aqua.actionRed)
        assertEquals(Color(0xFFDD9900), aqua.actionYellow)
    }

    @Test
    fun `Sand palette has correct colors`() {
        val sand = RiscOsPalette.Sand

        assertEquals("Sand", sand.name)
        assertEquals(Color(0xFFCCBBAA), sand.background)
        assertEquals(Color(0xFFEEDDCC), sand.lightGray)
        assertEquals(Color(0xFFCCBBAA), sand.mediumGray)
        assertEquals(Color(0xFF998877), sand.darkGray)
        assertEquals(Color(0xFF665544), sand.veryDarkGray)
        assertEquals(Color(0xFFFFFFFF), sand.white)
        assertEquals(Color(0xFF000000), sand.black)
        assertEquals(Color(0xFF6666AA), sand.actionBlue)
        assertEquals(Color(0xFF88AA66), sand.actionGreen)
        assertEquals(Color(0xFFCC6644), sand.actionRed)
        assertEquals(Color(0xFFDDAA44), sand.actionYellow)
    }

    @Test
    fun `Dark palette has correct colors`() {
        val dark = RiscOsPalette.Dark

        assertEquals("Dark", dark.name)
        assertEquals(Color(0xFF222222), dark.background)
        assertEquals(Color(0xFF444444), dark.lightGray)
        assertEquals(Color(0xFF222222), dark.mediumGray)
        assertEquals(Color(0xFF111111), dark.darkGray)
        assertEquals(Color(0xFF000000), dark.veryDarkGray)
        assertEquals(Color(0xFFFFFFFF), dark.white)
        assertEquals(Color(0xFF000000), dark.black)
        assertEquals(Color(0xFF6699FF), dark.actionBlue)
        assertEquals(Color(0xFF66FF99), dark.actionGreen)
        assertEquals(Color(0xFFFF6666), dark.actionRed)
        assertEquals(Color(0xFFFFDD66), dark.actionYellow)
    }

    @Test
    fun `All palettes list contains the built-in palettes`() {
        val all = RiscOsPalette.All

        assertEquals(9, all.size)
        assertTrue(all.contains(RiscOsPalette.Classic))
        assertTrue(all.contains(RiscOsPalette.Aqua))
        assertTrue(all.contains(RiscOsPalette.Sand))
        assertTrue(all.contains(RiscOsPalette.Dark))
    }

    @Test
    fun `All palettes list is in correct order`() {
        val all = RiscOsPalette.All

        assertEquals(RiscOsPalette.Classic, all[0])
        assertEquals(RiscOsPalette.Aqua, all[1])
        assertEquals(RiscOsPalette.Sand, all[2])
        assertEquals(RiscOsPalette.Dark, all[3])
    }

    // RiscOsColors Accessor Tests

    @Test
    fun `RiscOsColors starts with Classic palette by default`() {
        assertEquals(RiscOsPalette.Classic, RiscOsColors.currentPalette)
        assertEquals(Color(0xFFBBBBBB), RiscOsColors.background)
    }

    @Test
    fun `setPalette changes current palette`() {
        RiscOsColors.setPalette(RiscOsPalette.Aqua)

        assertEquals(RiscOsPalette.Aqua, RiscOsColors.currentPalette)
        assertEquals(Color(0xFFAABBCC), RiscOsColors.background)
    }

    @Test
    fun `nextPalette cycles through palettes forward`() {
        // Start with Classic
        assertEquals(RiscOsPalette.Classic, RiscOsColors.currentPalette)

        // Next should be Aqua
        RiscOsColors.nextPalette()
        assertEquals(RiscOsPalette.Aqua, RiscOsColors.currentPalette)

        // Next should be Sand
        RiscOsColors.nextPalette()
        assertEquals(RiscOsPalette.Sand, RiscOsColors.currentPalette)

        // Next should be Dark
        RiscOsColors.nextPalette()
        assertEquals(RiscOsPalette.Dark, RiscOsColors.currentPalette)

        // Advancing through the remainder wraps back around to Classic.
        repeat(RiscOsPalette.All.size - 3) { RiscOsColors.nextPalette() }
        assertEquals(RiscOsPalette.Classic, RiscOsColors.currentPalette)
    }

    @Test
    fun `previousPalette cycles through palettes backward`() {
        // Start with Classic
        assertEquals(RiscOsPalette.Classic, RiscOsColors.currentPalette)

        val all = RiscOsPalette.All

        // Previous from the first entry wraps to the last
        RiscOsColors.previousPalette()
        assertEquals(all.last(), RiscOsColors.currentPalette)

        // …then walks backwards one at a time
        RiscOsColors.previousPalette()
        assertEquals(all[all.size - 2], RiscOsColors.currentPalette)

        RiscOsColors.previousPalette()
        assertEquals(all[all.size - 3], RiscOsColors.currentPalette)

        // Walking back the rest of the way returns to Classic
        repeat(all.size - 3) { RiscOsColors.previousPalette() }
        assertEquals(RiscOsPalette.Classic, RiscOsColors.currentPalette)
    }

    @Test
    fun `color accessors return current palette colors`() {
        RiscOsColors.setPalette(RiscOsPalette.Sand)

        assertEquals(RiscOsPalette.Sand.background, RiscOsColors.background)
        assertEquals(RiscOsPalette.Sand.lightGray, RiscOsColors.lightGray)
        assertEquals(RiscOsPalette.Sand.mediumGray, RiscOsColors.mediumGray)
        assertEquals(RiscOsPalette.Sand.darkGray, RiscOsColors.darkGray)
        assertEquals(RiscOsPalette.Sand.veryDarkGray, RiscOsColors.veryDarkGray)
        assertEquals(RiscOsPalette.Sand.white, RiscOsColors.white)
        assertEquals(RiscOsPalette.Sand.black, RiscOsColors.black)
        assertEquals(RiscOsPalette.Sand.actionBlue, RiscOsColors.actionBlue)
        assertEquals(RiscOsPalette.Sand.actionGreen, RiscOsColors.actionGreen)
        assertEquals(RiscOsPalette.Sand.actionRed, RiscOsColors.actionRed)
        assertEquals(RiscOsPalette.Sand.actionYellow, RiscOsColors.actionYellow)
    }

    @Test
    fun `color accessors update when palette changes`() {
        RiscOsColors.setPalette(RiscOsPalette.Classic)
        val classicBackground = RiscOsColors.background

        RiscOsColors.setPalette(RiscOsPalette.Dark)
        val darkBackground = RiscOsColors.background

        assertNotEquals(classicBackground, darkBackground)
        assertEquals(Color(0xFFBBBBBB), classicBackground)
        assertEquals(Color(0xFF222222), darkBackground)
    }

    // Edge Cases

    @Test
    fun `multiple nextPalette calls work correctly`() {
        for (i in 1..10) {
            RiscOsColors.nextPalette()
        }

        // Starting from Classic (index 0), 10 steps lands on 10 % size
        assertEquals(RiscOsPalette.All[10 % RiscOsPalette.All.size], RiscOsColors.currentPalette)
    }

    @Test
    fun `multiple previousPalette calls work correctly`() {
        val size = RiscOsPalette.All.size

        for (i in 1..10) {
            RiscOsColors.previousPalette()
        }

        // Starting from Classic (index 0), 10 steps back lands on -10 mod size
        assertEquals(RiscOsPalette.All[((-10 % size) + size) % size], RiscOsColors.currentPalette)
    }

    @Test
    fun `mixing next and previous palette calls works correctly`() {
        RiscOsColors.setPalette(RiscOsPalette.Classic)

        RiscOsColors.nextPalette() // Aqua
        RiscOsColors.nextPalette() // Sand
        RiscOsColors.previousPalette() // Aqua
        RiscOsColors.nextPalette() // Sand
        RiscOsColors.nextPalette() // Dark

        assertEquals(RiscOsPalette.Dark, RiscOsColors.currentPalette)
    }

    @Test
    fun `setPalette can set same palette multiple times`() {
        RiscOsColors.setPalette(RiscOsPalette.Aqua)
        assertEquals(RiscOsPalette.Aqua, RiscOsColors.currentPalette)

        RiscOsColors.setPalette(RiscOsPalette.Aqua)
        assertEquals(RiscOsPalette.Aqua, RiscOsColors.currentPalette)
    }

    @Test
    fun `E-Ink palette stays legible on e-ink hardware`() {
        // E-ink renders colour as luminance over ~16 grey levels, so hue
        // carries nothing. Every foreground must clear WCAG AA (4.5:1) against
        // the background on luminance alone, or it vanishes on the device.
        val p = RiscOsPalette.EInk
        val foregrounds = mapOf(
            "black" to p.black,
            "veryDarkGray" to p.veryDarkGray,
            "darkGray" to p.darkGray,
            "actionBlue" to p.actionBlue,
            "actionGreen" to p.actionGreen,
            "actionRed" to p.actionRed,
            "actionYellow" to p.actionYellow
        )
        foregrounds.forEach { (name, colour) ->
            val ratio = contrastRatio(colour, p.background)
            assertTrue(
                "$name is $ratio:1 against the E-Ink background, below the 4.5:1 minimum",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun `E-Ink palette does not encode meaning in shade`() {
        // Keeping four action shades apart *and* above 4.5:1 on white is not
        // possible, so they are all near-black on purpose and meaning is left
        // to glyphs and labels. This pins that decision.
        val p = RiscOsPalette.EInk
        listOf(p.actionBlue, p.actionGreen, p.actionRed, p.actionYellow).forEach {
            assertTrue(
                "action colours must stay near-black so none of them reads as a hue",
                relativeLuminance(it) < 0.15
            )
        }
    }

    private fun relativeLuminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun `each palette has unique name`() {
        val names = RiscOsPalette.All.map { it.name }.toSet()
        assertEquals(RiscOsPalette.All.size, names.size) // All names should be unique
    }

    @Test
    fun `white and black are same across all palettes`() {
        val white = Color(0xFFFFFFFF)
        val black = Color(0xFF000000)

        RiscOsPalette.All.forEach { palette ->
            assertEquals(white, palette.white)
            assertEquals(black, palette.black)
        }
    }
}
