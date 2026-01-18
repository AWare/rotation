package app.rotatescreen.data.repository

import arrow.core.Either
import arrow.core.raise.either
import app.rotatescreen.data.local.dao.ScreenProfileDao
import app.rotatescreen.data.local.entity.toDomain
import app.rotatescreen.data.local.entity.toEntity
import app.rotatescreen.domain.model.OrientationError
import app.rotatescreen.domain.model.ScreenProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing screen profiles
 */
class ScreenProfileRepository(
    private val dao: ScreenProfileDao
) {
    fun getAllProfiles(): Flow<List<ScreenProfile>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    fun getActiveProfile(): Flow<ScreenProfile?> =
        dao.getActive().map { it?.toDomain() }

    suspend fun getProfile(id: String): Either<OrientationError, ScreenProfile> =
        either {
            val entity = dao.getById(id)
            entity?.toDomain() ?: raise(OrientationError.DatabaseError("Profile not found: $id"))
        }

    suspend fun saveProfile(profile: ScreenProfile): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.insert(profile.toEntity())
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to save profile: ${e.message}")
            }.bind()
        }

    suspend fun saveProfiles(profiles: List<ScreenProfile>): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.insertAll(profiles.map { it.toEntity() })
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to save profiles: ${e.message}")
            }.bind()
        }

    suspend fun deleteProfile(id: String): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deleteById(id)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to delete profile: ${e.message}")
            }.bind()
        }

    suspend fun activateProfile(id: String): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deactivateAll()
                dao.activate(id)
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to activate profile: ${e.message}")
            }.bind()
        }

    suspend fun deactivateAll(): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deactivateAll()
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to deactivate profiles: ${e.message}")
            }.bind()
        }

    suspend fun clearAll(): Either<OrientationError, Unit> =
        either {
            Either.catch {
                dao.deleteAll()
            }.mapLeft { e ->
                OrientationError.DatabaseError("Failed to clear profiles: ${e.message}")
            }.bind()
        }
}
