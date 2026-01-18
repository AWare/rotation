package app.rotatescreen.data.local.entity

import androidx.room.Entity
import app.rotatescreen.domain.model.AppOrientationSetting
import app.rotatescreen.domain.model.AspectRatio
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen

@Entity(
    tableName = "app_orientations",
    primaryKeys = ["packageName", "targetScreenId"]
)
data class AppOrientationEntity(
    val packageName: String,
    val targetScreenId: Int,
    val appName: String,
    val orientationValue: Int,
    val targetScreenName: String,
    val aspectRatioValue: String = "LANDSCAPE",
    val enabled: Boolean = true,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Extension functions for mapping between domain and data layers using FP style
 */
fun AppOrientationEntity.toDomain(): AppOrientationSetting {
    val orientation = ScreenOrientation.fromValue(orientationValue)
        .fold({ ScreenOrientation.Unspecified }, { it })

    val aspectRatio = try {
        AspectRatio.valueOf(aspectRatioValue)
    } catch (e: Exception) {
        AspectRatio.LANDSCAPE
    }

    val targetScreen = TargetScreen.fromId(targetScreenId, targetScreenName, aspectRatio)
        .fold({ TargetScreen.AllScreens }, { it })

    return AppOrientationSetting(
        packageName = packageName,
        appName = appName,
        orientation = orientation,
        targetScreen = targetScreen,
        enabled = enabled
    )
}

fun AppOrientationSetting.toEntity(): AppOrientationEntity =
    AppOrientationEntity(
        packageName = packageName,
        targetScreenId = targetScreen.id,
        appName = appName,
        orientationValue = orientation.value,
        targetScreenName = targetScreen.displayName,
        aspectRatioValue = targetScreen.aspectRatio.name,
        enabled = enabled
    )
