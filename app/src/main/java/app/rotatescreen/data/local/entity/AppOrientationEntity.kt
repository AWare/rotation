package app.rotatescreen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.rotatescreen.domain.model.AppOrientationSetting
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen

@Entity(tableName = "app_orientations")
data class AppOrientationEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val orientationValue: Int,
    val targetScreenId: Int,
    val targetScreenName: String,
    val enabled: Boolean = true,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Extension functions for mapping between domain and data layers using FP style
 */
fun AppOrientationEntity.toDomain(): AppOrientationSetting {
    val orientation = ScreenOrientation.fromValue(orientationValue)
        .fold({ ScreenOrientation.Unspecified }, { it })

    val targetScreen = TargetScreen.fromId(targetScreenId, targetScreenName)
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
        appName = appName,
        orientationValue = orientation.value,
        targetScreenId = targetScreen.id,
        targetScreenName = targetScreen.displayName,
        enabled = enabled
    )
