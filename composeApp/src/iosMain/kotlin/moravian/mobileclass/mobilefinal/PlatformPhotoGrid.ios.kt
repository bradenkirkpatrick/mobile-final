package moravian.mobileclass.mobilefinal

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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.photos_empty_message
import mobilefinal.composeapp.generated.resources.photos_empty_title
import org.jetbrains.compose.resources.stringResource
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHCachingImageManager
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeOpportunistic
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy
import kotlin.coroutines.resume
import org.jetbrains.skia.Image as SkiaImage

private data class IosPhoto(
    val id: String,
    val asset: PHAsset,
)

private val imageManager = PHCachingImageManager()

@Composable
actual fun PlatformPhotoGrid() {
    val photos by produceState<List<IosPhoto>>(initialValue = emptyList()) {
        value = loadIosPhotos()
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
                    text = stringResource(Res.string.photos_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.photos_empty_message),
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
                        IosPhotoTile(photo)
                    }
                }
            }
        }
    }
}

@Composable
private fun IosPhotoTile(photo: IosPhoto) {
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, photo.id) {
        value = loadIosThumbnail(photo.asset)
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

@OptIn(ExperimentalForeignApi::class)
private suspend fun loadIosPhotos(): List<IosPhoto> =
    withContext(Dispatchers.Default) {
        val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options = null)
        val maxCount = minOf(assets.count().toInt(), 150)
        val photos = ArrayList<IosPhoto>(maxCount)

        for (index in 0 until maxCount) {
            val asset = assets.objectAtIndex(index.toULong()) as? PHAsset ?: continue
            photos.add(IosPhoto(id = asset.localIdentifier, asset = asset))
        }

        photos
    }

@OptIn(ExperimentalForeignApi::class)
private suspend fun loadIosThumbnail(asset: PHAsset): ImageBitmap? =
    withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val requestOptions =
                PHImageRequestOptions().apply {
                    synchronous = false
                    networkAccessAllowed = true
                    deliveryMode = PHImageRequestOptionsDeliveryModeOpportunistic
                    resizeMode = PHImageRequestOptionsResizeModeFast
                }

            val requestId =
                imageManager.requestImageForAsset(
                    asset = asset,
                    targetSize = CGSizeMake(360.0, 360.0),
                    contentMode = PHImageContentModeAspectFill,
                    options = requestOptions,
                ) { image, _ ->
                    if (continuation.isCompleted) {
                        return@requestImageForAsset
                    }

                    val data = image?.let { UIImagePNGRepresentation(it) }
                    val bitmap =
                        data
                            ?.toByteArray()
                            ?.let { bytes -> runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull() }
                    continuation.resume(bitmap)
                }

            continuation.invokeOnCancellation {
                imageManager.cancelImageRequest(requestId)
            }
        }
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) {
        return ByteArray(0)
    }

    return ByteArray(size).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}
