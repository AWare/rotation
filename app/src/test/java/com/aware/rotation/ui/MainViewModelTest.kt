package com.aware.rotation.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.aware.rotation.data.local.RotationDatabase
import com.aware.rotation.data.local.dao.AppOrientationDao
import com.aware.rotation.data.preferences.PreferencesManager
import com.aware.rotation.domain.model.ScreenOrientation
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var database: RotationDatabase
    private lateinit var dao: AppOrientationDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android dependencies
        application = mockk(relaxed = true)
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        database = mockk(relaxed = true)
        dao = mockk(relaxed = true)

        every { application.applicationContext } returns context
        every { context.packageManager } returns packageManager
        every { packageManager.getInstalledApplications(any()) } returns emptyList()

        // Mock database
        mockkStatic(RotationDatabase::class)
        every { RotationDatabase.getInstance(any()) } returns database
        every { database.appOrientationDao() } returns dao
        every { dao.getAll() } returns flowOf(emptyList())
        every { dao.getAllEnabled() } returns flowOf(emptyList())

        // Mock PreferencesManager
        mockkConstructor(PreferencesManager::class)
        every { anyConstructed<PreferencesManager>().globalOrientation } returns flowOf(ScreenOrientation.Unspecified)
        every { anyConstructed<PreferencesManager>().lastTileOrientation } returns flowOf(ScreenOrientation.Unspecified)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state has default values`() = runTest {
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScreenOrientation.Unspecified, state.globalOrientation)
        assertNull(state.currentApp)
        assertTrue(state.perAppSettings.isEmpty())
        assertFalse(state.isAccessibilityServiceEnabled)
        assertFalse(state.hasDrawOverlayPermission)
    }

    @Test
    fun `updateSearchQuery updates search query flow`() = runTest {
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        viewModel.updateSearchQuery("test")
        testScheduler.advanceUntilIdle()

        assertEquals("test", viewModel.searchQuery.value)
    }

    @Test
    fun `filteredApps filters by search query`() = runTest {
        val apps = listOf(
            ApplicationInfo().apply {
                packageName = "com.test.app1"
                nonLocalizedLabel = "Test App"
            },
            ApplicationInfo().apply {
                packageName = "com.other.app"
                nonLocalizedLabel = "Other App"
            }
        )
        every { packageManager.getInstalledApplications(any()) } returns apps

        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        viewModel.filteredApps.test {
            val initial = awaitItem()
            assertEquals(2, initial.size)

            viewModel.updateSearchQuery("Test")
            testScheduler.advanceUntilIdle()

            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertEquals("Test App", filtered[0].appName)
        }
    }

    @Test
    fun `setGlobalOrientation updates state`() = runTest {
        coEvery { anyConstructed<PreferencesManager>().setGlobalOrientation(any()) } returns mockk()

        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        viewModel.setGlobalOrientation(ScreenOrientation.Portrait)
        testScheduler.advanceUntilIdle()

        coVerify { anyConstructed<PreferencesManager>().setGlobalOrientation(ScreenOrientation.Portrait) }
    }

    @Test
    fun `checkPermissions updates permission states`() = runTest {
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        viewModel.checkPermissions()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // Permissions will be false in test environment
        assertFalse(state.hasDrawOverlayPermission)
        assertFalse(state.isAccessibilityServiceEnabled)
    }
}
