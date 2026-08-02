package app.rotatescreen.util

import android.content.Context
import android.provider.Settings
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter
import org.robolectric.shadows.ShadowSettings

/**
 * Robolectric ships no shadow for [Settings.System.canWrite], and mockk cannot
 * redefine the class once Robolectric has instrumented it ("class redefinition
 * failed: attempted to change the class modifiers"). This shadow makes the
 * WRITE_SETTINGS answer settable instead.
 *
 * It extends Robolectric's own Settings.System shadow so the put/get helpers
 * keep working.
 */
@Implements(Settings.System::class)
class ShadowWritableSystemSettings : ShadowSettings.ShadowSystem() {

    companion object {
        @JvmStatic
        var canWrite: Boolean = false

        /** When set, [canWrite] throws this instead of returning. */
        @JvmStatic
        var error: RuntimeException? = null

        @Implementation
        @JvmStatic
        fun canWrite(context: Context): Boolean {
            error?.let { throw it }
            return canWrite
        }

        // Not named `reset`: the superclass already declares a static reset().
        @Resetter
        @JvmStatic
        fun resetCanWrite() {
            canWrite = false
            error = null
        }
    }
}
