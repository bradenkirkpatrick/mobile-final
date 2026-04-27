package moravian.mobileclass.mobilefinal

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.camera_capture_button
import mobilefinal.composeapp.generated.resources.camera_permission_button
import mobilefinal.composeapp.generated.resources.camera_permission_rationale
import mobilefinal.composeapp.generated.resources.camera_permission_title
import mobilefinal.composeapp.generated.resources.camera_title
import org.jetbrains.compose.resources.stringResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun CameraScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var capturedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasPermission = granted
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { wasSaved ->
            val photoUri = pendingCaptureUri
            if (wasSaved && photoUri != null) {
                capturedImage = loadPreviewBitmap(context, photoUri)
            } else if (photoUri != null) {
                context.contentResolver.delete(photoUri, null, null)
            }
            pendingCaptureUri = null
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
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text(stringResource(Res.string.camera_permission_button))
                }
            }
        } else if (capturedImage != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    bitmap = capturedImage!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
                Button(
                    onClick = { capturedImage = null },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Retake")
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
                        val outputUri = createImageUri(context) ?: return@Button
                        pendingCaptureUri = outputUri
                        cameraLauncher.launch(outputUri)
                    },
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text(stringResource(Res.string.camera_capture_button))
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri? {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val values =
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_$timestamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MobileFinal")
            }
        }

    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}

private fun loadPreviewBitmap(context: Context, uri: Uri): ImageBitmap? =
    try {
        val bitmap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(1200, 1200), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }

