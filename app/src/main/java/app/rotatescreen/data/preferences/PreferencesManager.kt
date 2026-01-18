package app.rotatescreen.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import arrow.core.Either
import app.rotatescreen.domain.model.OrientationError
import app.rotatescreen.domain.model.ScreenOrientation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rotation_prefs")

/**
 * Manages app preferences using DataStore and FP style
 */
class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val GLOBAL_ORIENTATION = intPreferencesKey("global_orientation")
        val LAST_TILE_ORIENTATION = intPreferencesKey("last_tile_orientation")
    }

    val globalOrientation: Flow<ScreenOrientation> =
        context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { preferences ->
                val value = preferences[PreferencesKeys.GLOBAL_ORIENTATION]
                    ?: ScreenOrientation.Unspecified.value
                ScreenOrientation.fromValue(value).fold({ ScreenOrientation.Unspecified }, { it })
            }

    val lastTileOrientation: Flow<ScreenOrientation> =
        context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { preferences ->
                val value = preferences[PreferencesKeys.LAST_TILE_ORIENTATION]
                    ?: ScreenOrientation.Unspecified.value
                ScreenOrientation.fromValue(value).fold({ ScreenOrientation.Unspecified }, { it })
            }

    suspend fun setGlobalOrientation(orientation: ScreenOrientation): Either<OrientationError, Unit> =
        Either.catch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GLOBAL_ORIENTATION] = orientation.value
            }
            Unit
        }.mapLeft { e ->
            OrientationError.DatabaseError("Failed to save global orientation: ${e.message}")
        }

    suspend fun setLastTileOrientation(orientation: ScreenOrientation): Either<OrientationError, Unit> =
        Either.catch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_TILE_ORIENTATION] = orientation.value
            }
            Unit
        }.mapLeft { e ->
            OrientationError.DatabaseError("Failed to save tile orientation: ${e.message}")
        }

    suspend fun clear(): Either<OrientationError, Unit> =
        Either.catch {
            context.dataStore.edit { it.clear() }
            Unit
        }.mapLeft { e ->
            OrientationError.DatabaseError("Failed to clear preferences: ${e.message}")
        }
}
