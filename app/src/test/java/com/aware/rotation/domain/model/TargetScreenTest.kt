package com.aware.rotation.domain.model

import org.junit.Assert.*
import org.junit.Test

class TargetScreenTest {

    @Test
    fun `fromId with -1 returns AllScreens`() {
        val result = TargetScreen.fromId(-1)
        assertTrue(result.isRight())
        assertEquals(TargetScreen.AllScreens, result.getOrNull())
    }

    @Test
    fun `fromId with 0 returns SpecificScreen`() {
        val result = TargetScreen.fromId(0, "Primary")
        assertTrue(result.isRight())
        val screen = result.getOrNull()
        assertTrue(screen is TargetScreen.SpecificScreen)
        assertEquals(0, screen?.id)
    }

    @Test
    fun `fromId with positive id returns SpecificScreen`() {
        val result = TargetScreen.fromId(5, "Screen 5")
        assertTrue(result.isRight())
        val screen = result.getOrNull()
        assertTrue(screen is TargetScreen.SpecificScreen)
        assertEquals(5, screen?.id)
        assertEquals("Screen 5", screen?.displayName)
    }

    @Test
    fun `fromId with negative id other than -1 returns error`() {
        val result = TargetScreen.fromId(-5)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ScreenError.InvalidDisplayId)
    }

    @Test
    fun `fromId generates default name when not provided`() {
        val result = TargetScreen.fromId(3)
        assertTrue(result.isRight())
        assertEquals("Screen 3", result.getOrNull()?.displayName)
    }

    @Test
    fun `SpecificScreen primary returns display id 0`() {
        val primary = TargetScreen.SpecificScreen.primary()
        assertEquals(0, primary.displayId)
        assertEquals("Primary Screen", primary.name)
    }

    @Test
    fun `SpecificScreen secondary returns correct display id`() {
        val secondary = TargetScreen.SpecificScreen.secondary(2)
        assertEquals(2, secondary.displayId)
        assertEquals("Secondary Screen 2", secondary.name)
    }

    @Test
    fun `AllScreens has id -1`() {
        assertEquals(-1, TargetScreen.AllScreens.id)
        assertEquals("All Screens", TargetScreen.AllScreens.displayName)
    }
}
