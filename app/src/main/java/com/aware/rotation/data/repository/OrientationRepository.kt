package com.aware.rotation.data.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.aware.rotation.data.local.dao.AppOrientationDao
import com.aware.rotation.data.local.entity.toDomain
import com.aware.rotation.data.local.entity.toEntity
import com.aware.rotation.domain.model.AppOrientationSetting
import com.aware.rotation.domain.model.OrientationError
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

    fun observeSetting(packageName: String): Flow<AppOrientationSetting?> =
        dao.observeByPackageName(packageName).map { it?.toDomain() }

    suspend fun getSetting(packageName: String): Either<OrientationError, AppOrientationSetting> =
        either {
            val entity = dao.getByPackageName(packageName)
            ensure(entity != null) {
                OrientationError.DatabaseError("Setting not found for $packageName")
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

    suspend fun setEnabled(packageName: String, enabled: Boolean): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.setEnabled(packageName, enabled)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to update enabled state: ${e.message}")
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
