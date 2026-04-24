package moravian.mobileclass.mobilefinal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PhotoPermissionHandler(
    content: @Composable (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit
) {
    val currentStatus = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
    var hasPermission by remember {
        mutableStateOf(
            currentStatus == PHAuthorizationStatusAuthorized ||
            currentStatus == PHAuthorizationStatusLimited
        )
    }

    content(hasPermission) {
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { status ->
            dispatch_async(dispatch_get_main_queue()) {
                hasPermission = status == PHAuthorizationStatusAuthorized ||
                                status == PHAuthorizationStatusLimited
            }
        }
    }
}

