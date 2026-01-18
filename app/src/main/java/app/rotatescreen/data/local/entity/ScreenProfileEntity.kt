package app.rotatescreen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.ScreenProfile

@Entity(tableName = "screen_profiles")
data class ScreenProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val configurationsJson: String, // JSON: {"0": 1, "1": 2} -> displayId: orientationValue
    val isActive: Boolean,
    val createdAt: Long
)

/**
 * Extension functions for mapping between domain and data layers
 */
fun ScreenProfileEntity.toDomain(): ScreenProfile {
    // Parse JSON configurations
    val configurations = try {
        val json = org.json.JSONObject(configurationsJson)
        val map = mutableMapOf<Int, ScreenOrientation>()
        json.keys().forEach { key ->
            val displayId = key.toIntOrNull() ?: return@forEach
            val orientationValue = json.getInt(key)
            ScreenOrientation.fromValue(orientationValue).fold(
                { },
                { orientation -> map[displayId] = orientation }
            )
        }
        map
    } catch (e: Exception) {
        emptyMap()
    }

    return ScreenProfile(
        id = id,
        name = name,
        description = description,
        screenConfigurations = configurations,
        isActive = isActive,
        createdAt = createdAt
    )
}

fun ScreenProfile.toEntity(): ScreenProfileEntity {
    // Convert configurations to JSON
    val json = org.json.JSONObject()
    screenConfigurations.forEach { (displayId, orientation) ->
        json.put(displayId.toString(), orientation.value)
    }

    return ScreenProfileEntity(
        id = id,
        name = name,
        description = description,
        configurationsJson = json.toString(),
        isActive = isActive,
        createdAt = createdAt
    )
}
