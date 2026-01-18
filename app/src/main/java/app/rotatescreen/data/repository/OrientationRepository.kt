package app.rotatescreen.data.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import app.rotatescreen.data.local.dao.AppOrientationDao
import app.rotatescreen.data.local.entity.toDomain
import app.rotatescreen.data.local.entity.toEntity
import app.rotatescreen.domain.model.AppOrientationSetting
import app.rotatescreen.domain.model.OrientationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing app orientation settings using FP style
 */
class OrientationRepository(
    private val dao: AppOrientationDao
) {
    fun getAllSettings(): Flow<List<AppOrientationSetting>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    fun getEnabledSettings(): Flow<List<AppOrientationSetting>> =
        dao.getAllEnabled().map { entities -> entities.map { it.toDomain() } }

    fun observeSetting(packageName: String): Flow<List<AppOrientationSetting>> =
        dao.observeByPackageName(packageName).map { entities -> entities.map { it.toDomain() } }

    fun observeSettingForDisplay(packageName: String, displayId: Int): Flow<AppOrientationSetting?> =
        dao.observeByPackageAndDisplay(packageName, displayId).map { it?.toDomain() }

    fun observeSettingsForDisplay(displayId: Int): Flow<List<AppOrientationSetting>> =
        dao.observeByDisplayId(displayId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getSetting(packageName: String): Either<OrientationError, List<AppOrientationSetting>> =
        either {
            val entities = dao.getByPackageName(packageName)
            ensure(entities.isNotEmpty()) {
                OrientationError.DatabaseError("Settings not found for $packageName")
            }
            entities.map { it.toDomain() }
        }

    suspend fun getSettingForDisplay(packageName: String, displayId: Int): Either<OrientationError, AppOrientationSetting> =
        either {
            val entity = dao.getByPackageAndDisplay(packageName, displayId)
            ensure(entity != null) {
                OrientationError.DatabaseError("Setting not found for $packageName on display $displayId")
            }
            entity.toDomain()
        }

    suspend fun saveSetting(setting: AppOrientationSetting): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.insert(setting.toEntity())
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to save setting: ${e.message}")
            }.bind()
        }

    suspend fun saveSettings(settings: List<AppOrientationSetting>): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.insertAll(settings.map { it.toEntity() })
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to save settings: ${e.message}")
            }.bind()
        }

    suspend fun deleteSetting(packageName: String): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deleteByPackageName(packageName)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to delete setting: ${e.message}")
            }.bind()
        }

    suspend fun deleteSettingForDisplay(packageName: String, displayId: Int): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deleteByPackageAndDisplay(packageName, displayId)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to delete setting for display: ${e.message}")
            }.bind()
        }

    suspend fun deleteSettingsForDisplay(displayId: Int): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deleteByDisplayId(displayId)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to delete settings for display: ${e.message}")
            }.bind()
        }

    suspend fun setEnabled(packageName: String, enabled: Boolean): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.setEnabled(packageName, enabled)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to update enabled state: ${e.message}")
            }.bind()
        }

    suspend fun setEnabledForDisplay(packageName: String, displayId: Int, enabled: Boolean): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.setEnabledForDisplay(packageName, displayId, enabled)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to update enabled state for display: ${e.message}")
            }.bind()
        }

    suspend fun clearAll(): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deleteAll()
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to clear settings: ${e.message}")
            }.bind()
        }
}
