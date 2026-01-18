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
    fun `All palettes list contains all four palettes`() {
        val all = RiscOsPalette.All

        assertEquals(4, all.size)
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

        // Next should wrap around to Classic
        RiscOsColors.nextPalette()
        assertEquals(RiscOsPalette.Classic, RiscOsColors.currentPalette)
    }

    @Test
    fun `previousPalette cycles through palettes backward`() {
        // Start with Classic
        assertEquals(RiscOsPalette.Classic, RiscOsColors.currentPalette)

        // Previous should wrap to Dark
        RiscOsColors.previousPalette()
        assertEquals(RiscOsPalette.Dark, RiscOsColors.currentPalette)

        // Previous should be Sand
        RiscOsColors.previousPalette()
        assertEquals(RiscOsPalette.Sand, RiscOsColors.currentPalette)

        // Previous should be Aqua
        RiscOsColors.previousPalette()
        assertEquals(RiscOsPalette.Aqua, RiscOsColors.currentPalette)

        // Previous should be Classic
        RiscOsColors.previousPalette()
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

        // Should wrap around: 10 % 4 = 2, so should be Sand (index 2)
        assertEquals(RiscOsPalette.Sand, RiscOsColors.currentPalette)
    }

    @Test
    fun `multiple previousPalette calls work correctly`() {
        for (i in 1..10) {
            RiscOsColors.previousPalette()
        }

        // Should wrap around backwards: -10 % 4 = 2, so should be Sand (index 2)
        assertEquals(RiscOsPalette.Sand, RiscOsColors.currentPalette)
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
    fun `each palette has unique name`() {
        val names = RiscOsPalette.All.map { it.name }.toSet()
        assertEquals(4, names.size) // All names should be unique
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
