package moravian.mobileclass.mobilefinal

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.photos_empty_message
import mobilefinal.composeapp.generated.resources.photos_empty_title
import org.jetbrains.compose.resources.stringResource

data class LocalPhoto(
    val id: Long,
    val uri: Uri,
)

@Composable
actual fun PlatformPhotoGrid() {
    val context = LocalContext.current
    val photos by produceState<List<LocalPhoto>>(initialValue = emptyList(), context) {
        value = loadDevicePhotos(context)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding(),
    ) {
        if (photos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No Photos Found",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Your library appears empty, or photos are unavailable right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 24.dp, end = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth > maxHeight) 4 else 3
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoTile(photo = photo, context = context)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photo: LocalPhoto,
    context: Context,
) {
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, photo.uri) {
        value = loadThumbnail(context, photo.uri)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(110.dp),
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

private suspend fun loadDevicePhotos(context: Context): List<LocalPhoto> =
    withContext(Dispatchers.IO) {
        val photos = mutableListOf<LocalPhoto>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    photos.add(LocalPhoto(id = id, uri = uri))
                }
            }

        photos
    }

private suspend fun loadThumbnail(
    context: Context,
    uri: Uri,
): ImageBitmap? =
    withContext(Dispatchers.IO) {
        try {
            val bitmap: Bitmap? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(360, 360), null)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            bitmap?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
