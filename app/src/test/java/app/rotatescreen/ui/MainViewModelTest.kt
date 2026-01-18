package app.rotatescreen.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.local.dao.AppOrientationDao
import app.rotatescreen.data.preferences.PreferencesManager
import app.rotatescreen.domain.model.AppOrientationSetting
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import app.rotatescreen.service.OrientationControlService
import app.rotatescreen.util.AccessibilityChecker
import app.rotatescreen.util.PermissionChecker
import arrow.core.Either
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    private lateinit var displayManager: DisplayManager
    private lateinit var database: RotationDatabase
    private lateinit var dao: AppOrientationDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android components
        application = mockk(relaxed = true)
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        displayManager = mockk(relaxed = true)
        database = mockk(relaxed = true)
        dao = mockk(relaxed = true)

        every { application.applicationContext } returns context
        every { context.packageName } returns "app.rotatescreen"
        every { context.packageManager } returns packageManager
        every { context.getSystemService(Context.DISPLAY_SERVICE) } returns displayManager

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

        // Mock display manager
        val mockDisplay = mockk<Display>(relaxed = true)
        every { mockDisplay.displayId } returns 0
        every { displayManager.displays } returns arrayOf(mockDisplay)

        // Mock static utility classes
        mockkStatic(PermissionChecker::class)
        mockkStatic(AccessibilityChecker::class)
        every { PermissionChecker.hasDrawOverlayPermission(any()) } returns Either.Right(false)
        every { AccessibilityChecker.isAccessibilityServiceEnabled(any()) } returns Either.Right(false)

        // Default mock for queryIntentActivities
        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `app enumeration excludes self package`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.example.app1", "App 1"),
            createResolveInfo("app.rotatescreen", "Rotation App"), // Should be excluded
            createResolveInfo("com.example.app2", "App 2")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps

        // Act
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert
        viewModel.installedApps.test {
            val apps = awaitItem()
            assertEquals(2, apps.size)
            assertFalse(apps.any { it.packageName == "app.rotatescreen" })
            assertTrue(apps.any { it.packageName == "com.example.app1" })
            assertTrue(apps.any { it.packageName == "com.example.app2" })
        }
    }

    @Test
    fun `app enumeration handles exceptions gracefully`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.example.app1", "App 1"),
            createResolveInfo("com.example.corrupted", "Corrupted App")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps
        every { packageManager.getApplicationInfo("com.example.corrupted", 0) } throws Exception("App corrupted")

        // Act
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert
        viewModel.installedApps.test {
            val apps = awaitItem()
            assertEquals(1, apps.size)
            assertEquals("com.example.app1", apps[0].packageName)
        }
    }

    @Test
    fun `search query filters apps case insensitively`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.example.calculator", "Calculator"),
            createResolveInfo("com.example.calendar", "Calendar"),
            createResolveInfo("com.example.camera", "Camera")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps

        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.updateSearchQuery("CAL")
        testScheduler.advanceUntilIdle()

        // Assert
        viewModel.filteredApps.test {
            val filtered = awaitItem()
            assertEquals(2, filtered.size)
            assertTrue(filtered.any { it.appName == "Calculator" })
            assertTrue(filtered.any { it.appName == "Calendar" })
            assertFalse(filtered.any { it.appName == "Camera" })
        }
    }

    @Test
    fun `apps with saved settings included even if not installed`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.example.installed", "Installed App")
        )

        val savedSettings = listOf(
            AppOrientationSetting.create(
                packageName = "com.example.installed",
                appName = "Installed App",
                orientation = ScreenOrientation.Portrait
            ),
            AppOrientationSetting.create(
                packageName = "com.example.uninstalled",
                appName = "Uninstalled App",
                orientation = ScreenOrientation.Landscape
            )
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps
        every { dao.getAll() } returns flowOf(savedSettings.map { it.toEntity() })

        // Act
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert
        viewModel.installedApps.test {
            val apps = awaitItem()
            assertEquals(2, apps.size)

            val installedApp = apps.find { it.packageName == "com.example.installed" }
            assertTrue(installedApp!!.isInstalled)

            val uninstalledApp = apps.find { it.packageName == "com.example.uninstalled" }
            assertFalse(uninstalledApp!!.isInstalled)
        }
    }

    @Test
    fun `setAppOrientation refreshes app list`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.example.app", "Test App")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps
        coEvery { dao.insert(any()) } just Runs

        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.setAppOrientation("com.example.app", "Test App", ScreenOrientation.Portrait)
        testScheduler.advanceUntilIdle()

        // Assert - verify queryIntentActivities called again after save
        verify(atLeast = 2) { packageManager.queryIntentActivities(any(), any<Int>()) }
    }

    @Test
    fun `removeAppSetting refreshes app list`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.example.app", "Test App")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps
        coEvery { dao.delete(any()) } just Runs

        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.removeAppSetting("com.example.app")
        testScheduler.advanceUntilIdle()

        // Assert
        verify(atLeast = 2) { packageManager.queryIntentActivities(any(), any<Int>()) }
    }

    @Test
    fun `duplicate packages removed with distinctBy`() = runTest {
        // Arrange - Same package with multiple launcher activities
        val launcherApps = listOf(
            createResolveInfo("com.example.app", "App Activity 1"),
            createResolveInfo("com.example.app", "App Activity 2"),
            createResolveInfo("com.example.different", "Different App")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps

        // Act
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert
        viewModel.installedApps.test {
            val apps = awaitItem()
            assertEquals(2, apps.size) // Only 2 distinct packages
            assertTrue(apps.any { it.packageName == "com.example.app" })
            assertTrue(apps.any { it.packageName == "com.example.different" })
        }
    }

    @Test
    fun `race condition handled with firstOrNull`() = runTest {
        // Arrange - Empty flow
        every { dao.getAll() } returns flowOf()

        val launcherApps = listOf(createResolveInfo("com.example.app", "Test App"))
        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps

        // Act - Should not throw NoSuchElementException
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert
        viewModel.installedApps.test {
            val apps = awaitItem()
            assertEquals(1, apps.size)
        }
    }

    @Test
    fun `DisplayManager initialization uses safe is check`() = runTest {
        // Arrange
        every { context.getSystemService(Context.DISPLAY_SERVICE) } returns displayManager

        // Act - Should not throw ClassCastException
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert
        viewModel.availableScreens.test {
            val screens = awaitItem()
            assertTrue(screens.isNotEmpty())
        }
    }

    @Test
    fun `applyOrientation handles service start exceptions`() = runTest {
        // Arrange
        every { context.startService(any()) } throws SecurityException("No permission")
        coEvery { anyConstructed<PreferencesManager>().setGlobalOrientation(any()) } just Runs

        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Act - Should not crash
        viewModel.setGlobalOrientation(ScreenOrientation.Portrait)
        testScheduler.advanceUntilIdle()

        // Assert - Exception logged, no crash
        verify { context.startService(any()) }
    }

    @Test
    fun `flashScreen handles service start exceptions`() = runTest {
        // Arrange
        every { context.startService(any()) } throws SecurityException("No permission")

        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Act - Should not crash
        viewModel.flashScreen(TargetScreen.AllScreens)
        testScheduler.advanceUntilIdle()

        // Assert - Exception logged, no crash
        verify { context.startService(any()) }
    }

    @Test
    fun `initial state has default values`() = runTest {
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScreenOrientation.Unspecified, state.globalOrientation)
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

    @Test
    fun `queryIntentActivities uses correct flags for launcher enumeration`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.android.chrome", "Chrome"),
            createResolveInfo("com.google.android.gm", "Gmail"),
            createResolveInfo("com.android.settings", "Settings"),
            createResolveInfo("com.android.calculator2", "Calculator"),
            createResolveInfo("com.android.calendar", "Calendar"),
            createResolveInfo("com.spotify.music", "Spotify"),
            createResolveInfo("com.whatsapp", "WhatsApp"),
            createResolveInfo("com.instagram.android", "Instagram")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps

        // Act
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert - Verify queryIntentActivities called with correct flags
        verify {
            packageManager.queryIntentActivities(
                match { intent ->
                    intent.action == Intent.ACTION_MAIN &&
                    intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
                },
                match { flags ->
                    // Verify flags include MATCH_DISABLED_COMPONENTS
                    (flags and PackageManager.MATCH_DISABLED_COMPONENTS) != 0
                }
            )
        }

        // Verify all apps are loaded (excluding self)
        viewModel.installedApps.test {
            val apps = awaitItem()
            assertEquals(8, apps.size)
            assertTrue(apps.any { it.packageName == "com.android.chrome" })
            assertTrue(apps.any { it.packageName == "com.spotify.music" })
        }
    }

    @Test
    fun `app enumeration includes disabled apps with MATCH_DISABLED_COMPONENTS flag`() = runTest {
        // Arrange
        val launcherApps = listOf(
            createResolveInfo("com.example.enabled", "Enabled App"),
            createResolveInfo("com.example.disabled", "Disabled App"),
            createResolveInfo("com.example.disabled_until_used", "Disabled Until Used App")
        )

        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns launcherApps

        // Act
        val viewModel = MainViewModel(application)
        testScheduler.advanceUntilIdle()

        // Assert - All apps should be included
        viewModel.installedApps.test {
            val apps = awaitItem()
            assertEquals(3, apps.size)
            assertTrue(apps.any { it.packageName == "com.example.enabled" })
            assertTrue(apps.any { it.packageName == "com.example.disabled" })
            assertTrue(apps.any { it.packageName == "com.example.disabled_until_used" })
        }
    }

    // Helper function to create mock ResolveInfo
    private fun createResolveInfo(packageName: String, label: String): ResolveInfo {
        val resolveInfo = mockk<ResolveInfo>(relaxed = true)
        val activityInfo = mockk<ActivityInfo>(relaxed = true)
        val appInfo = mockk<ApplicationInfo>(relaxed = true)

        activityInfo.packageName = packageName
        activityInfo.applicationInfo = appInfo
        resolveInfo.activityInfo = activityInfo

        every { packageManager.getApplicationInfo(packageName, 0) } returns appInfo
        every { appInfo.loadLabel(packageManager) } returns label

        return resolveInfo
    }

    // Helper to convert setting to entity for DAO mock
    private fun AppOrientationSetting.toEntity() = app.rotatescreen.data.local.entity.AppOrientationEntity(
        packageName = packageName,
        appName = appName,
        orientation = orientation.value,
        targetScreenId = targetScreen.id,
        enabled = enabled
    )
}
