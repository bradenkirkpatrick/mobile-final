package moravian.mobileclass.mobilefinal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.camera_capture_button
import mobilefinal.composeapp.generated.resources.camera_permission_button
import mobilefinal.composeapp.generated.resources.camera_permission_rationale
import mobilefinal.composeapp.generated.resources.camera_permission_title
import mobilefinal.composeapp.generated.resources.camera_title
import org.jetbrains.compose.resources.stringResource
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraScreen() {
    var hasPermission by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatus(forMediaType = AVMediaTypeVideo) == AVAuthorizationStatusAuthorized,
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(Res.string.camera_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            textAlign = TextAlign.Center,
        )

        if (!hasPermission) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.camera_permission_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.camera_permission_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        AVCaptureDevice.requestAccess(forMediaType = AVMediaTypeVideo) { granted: Boolean ->
                            dispatch_async(dispatch_get_main_queue()) {
                                hasPermission = granted
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text(stringResource(Res.string.camera_permission_button))
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "📷 Camera ready",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        // Camera preview would go here
                    },
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text(stringResource(Res.string.camera_capture_button))
                }
            }
        }
    }
}
