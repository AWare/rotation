package com.aware.rotation.data.repository

import app.cash.turbine.test
import com.aware.rotation.data.local.dao.AppOrientationDao
import com.aware.rotation.data.local.entity.AppOrientationEntity
import com.aware.rotation.domain.model.AppOrientationSetting
import com.aware.rotation.domain.model.OrientationError
import com.aware.rotation.domain.model.ScreenOrientation
import com.aware.rotation.domain.model.TargetScreen
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OrientationRepositoryTest {

    private lateinit var dao: AppOrientationDao
    private lateinit var repository: OrientationRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repository = OrientationRepository(dao)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getAllSettings returns mapped domain models`() = runTest {
        val entities = listOf(
            AppOrientationEntity(
                packageName = "com.test1",
                appName = "Test 1",
                orientationValue = 1,
                targetScreenId = -1,
                targetScreenName = "All Screens",
                enabled = true
            ),
            AppOrientationEntity(
                packageName = "com.test2",
                appName = "Test 2",
                orientationValue = 0,
                targetScreenId = 0,
                targetScreenName = "Primary",
                enabled = false
            )
        )
        every { dao.getAll() } returns flowOf(entities)

        repository.getAllSettings().test {
            val settings = awaitItem()
            assertEquals(2, settings.size)
            assertEquals("com.test1", settings[0].packageName)
            assertEquals("com.test2", settings[1].packageName)
            awaitComplete()
        }
    }

    @Test
    fun `getEnabledSettings returns only enabled settings`() = runTest {
        val entities = listOf(
            AppOrientationEntity(
                packageName = "com.test",
                appName = "Test",
                orientationValue = 1,
                targetScreenId = -1,
                targetScreenName = "All",
                enabled = true
            )
        )
        every { dao.getAllEnabled() } returns flowOf(entities)

        repository.getEnabledSettings().test {
            val settings = awaitItem()
            assertEquals(1, settings.size)
            assertTrue(settings[0].enabled)
            awaitComplete()
        }
    }

    @Test
    fun `getSetting returns Right when setting exists`() = runTest {
        val entity = AppOrientationEntity(
            packageName = "com.test",
            appName = "Test",
            orientationValue = 1,
            targetScreenId = -1,
            targetScreenName = "All",
            enabled = true
        )
        coEvery { dao.getByPackageName("com.test") } returns entity

        val result = repository.getSetting("com.test")

        assertTrue(result.isRight())
        assertEquals("com.test", result.getOrNull()?.packageName)
    }

    @Test
    fun `getSetting returns Left when setting does not exist`() = runTest {
        coEvery { dao.getByPackageName("com.missing") } returns null

        val result = repository.getSetting("com.missing")

        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is OrientationError.DatabaseError)
    }

    @Test
    fun `saveSetting succeeds with Right`() = runTest {
        val setting = AppOrientationSetting.create(
            packageName = "com.test",
            appName = "Test"
        )
        coEvery { dao.insert(any()) } just Runs

        val result = repository.saveSetting(setting)

        assertTrue(result.isRight())
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `saveSetting returns Left on exception`() = runTest {
        val setting = AppOrientationSetting.create("com.test", "Test")
        coEvery { dao.insert(any()) } throws Exception("Database error")

        val result = repository.saveSetting(setting)

        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is OrientationError.DatabaseError)
    }

    @Test
    fun `saveSettings batch inserts all settings`() = runTest {
        val settings = listOf(
            AppOrientationSetting.create("com.test1", "Test 1"),
            AppOrientationSetting.create("com.test2", "Test 2")
        )
        coEvery { dao.insertAll(any()) } just Runs

        val result = repository.saveSettings(settings)

        assertTrue(result.isRight())
        coVerify { dao.insertAll(match { it.size == 2 }) }
    }

    @Test
    fun `deleteSetting succeeds with Right`() = runTest {
        coEvery { dao.deleteByPackageName("com.test") } just Runs

        val result = repository.deleteSetting("com.test")

        assertTrue(result.isRight())
        coVerify { dao.deleteByPackageName("com.test") }
    }

    @Test
    fun `deleteSetting returns Left on exception`() = runTest {
        coEvery { dao.deleteByPackageName(any()) } throws Exception("Error")

        val result = repository.deleteSetting("com.test")

        assertTrue(result.isLeft())
    }

    @Test
    fun `setEnabled updates enabled state`() = runTest {
        coEvery { dao.setEnabled("com.test", false) } just Runs

        val result = repository.setEnabled("com.test", false)

        assertTrue(result.isRight())
        coVerify { dao.setEnabled("com.test", false) }
    }

    @Test
    fun `clearAll deletes all settings`() = runTest {
        coEvery { dao.deleteAll() } just Runs

        val result = repository.clearAll()

        assertTrue(result.isRight())
        coVerify { dao.deleteAll() }
    }

    @Test
    fun `observeSetting emits updates`() = runTest {
        val entity = AppOrientationEntity(
            packageName = "com.test",
            appName = "Test",
            orientationValue = 1,
            targetScreenId = -1,
            targetScreenName = "All",
            enabled = true
        )
        every { dao.observeByPackageName("com.test") } returns flowOf(entity)

        repository.observeSetting("com.test").test {
            val setting = awaitItem()
            assertNotNull(setting)
            assertEquals("com.test", setting?.packageName)
            awaitComplete()
        }
    }
}
