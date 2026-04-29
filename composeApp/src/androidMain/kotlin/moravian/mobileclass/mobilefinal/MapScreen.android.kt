package moravian.mobileclass.mobilefinal

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.map_coordinates
import mobilefinal.composeapp.generated.resources.map_permission_button
import mobilefinal.composeapp.generated.resources.map_permission_rationale
import mobilefinal.composeapp.generated.resources.map_permission_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.osmdroid.events.MapListener
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private data class PhotoMapItem(
    val uri: Uri,
    val point: GeoPoint,
)

@Composable
actual fun MapScreen() {
    val mapConfig = koinInject<MapConfig>()
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember {
        mutableStateOf(hasLocationPermission(context))
    }
    var latitude by remember { mutableDoubleStateOf(mapConfig.defaultLatitude) }
    var longitude by remember { mutableDoubleStateOf(mapConfig.defaultLongitude) }
    var locationCentered by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // Load photo locations from EXIF in background
    val photoLocations by produceState<List<PhotoMapItem>>(initialValue = emptyList(), context) {
        value = withContext(Dispatchers.IO) { loadPhotoLocations(context) }
    }

    val selectedPhotoPreview by produceState<ImageBitmap?>(initialValue = null, selectedPhotoUri, context) {
        value = selectedPhotoUri?.let { withContext(Dispatchers.IO) { loadPhotoPreview(context, it) } }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        showMap = true
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            hasPermission = hasLocationPermission(context)
        }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val location = getBestLastKnownLocation(context)
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                locationCentered = false
            }
        }
    }

    // Animate to GPS location once when coordinates arrive
    LaunchedEffect(latitude, longitude) {
        if (!locationCentered) {
            mapViewRef.value?.controller?.animateTo(GeoPoint(latitude, longitude))
            locationCentered = true
        }
    }

    // Lifecycle handling: osmdroid REQUIRES onResume/onPause to load/stop tiles
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onPause()
            mapViewRef.value?.onDetach()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding(),
    ) {

        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.map_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(Res.string.map_permission_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(stringResource(Res.string.map_permission_button))
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().fillMaxSize()) {
            if (showMap) {
                MapViewContent(
                    latitude = latitude,
                    longitude = longitude,
                    mapConfig = mapConfig,
                    mapViewRef = mapViewRef,
                    photoLocations = photoLocations,
                    onPhotoMarkerSelected = { uri -> selectedPhotoUri = uri },
                    onMapInteracted = { selectedPhotoUri = null },
                    onMapMoved = { lat, lon ->
                        latitude = lat
                        longitude = lon
                    },
                )

                if (selectedPhotoUri != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (selectedPhotoPreview != null) {
                                Image(
                                    bitmap = selectedPhotoPreview!!,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(Res.string.map_coordinates, "%.4f".format(latitude), "%.4f".format(longitude)),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun MapViewContent(
    latitude: Double,
    longitude: Double,
    mapConfig: MapConfig,
    mapViewRef: androidx.compose.runtime.MutableState<MapView?>,
    photoLocations: List<PhotoMapItem>,
    onPhotoMarkerSelected: (Uri) -> Unit,
    onMapInteracted: () -> Unit,
    onMapMoved: (Double, Double) -> Unit,
) {
    @Suppress("COMPOSE_CALLING_UI_COMPOSABLE_FROM_COMPOSABLE")
    AndroidView(
        factory = { context ->
            // OsmDroid is configured in MobileFinalApp.onCreate(); no need to re-configure here.
            MapView(context).also { view ->
                mapViewRef.value = view

                // Log diagnostic info for network debugging
                Log.d("MapScreen", "MapView factory: Creating MapView at ($latitude, $longitude)")
                Log.d("MapScreen", "Tile cache path: ${context.cacheDir.resolve("osmdroid/tiles")}")
                Log.d("MapScreen", "Setting tile source to: Mapnik")

                view.setTileSource(TileSourceFactory.MAPNIK)
                view.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                view.setMultiTouchControls(true)
                view.controller.setZoom(mapConfig.defaultZoom)
                view.controller.setCenter(GeoPoint(latitude, longitude))
                view.isClickable = true

                // Hide preview when user taps anywhere that is not a marker.
                view.overlays.add(
                    MapEventsOverlay(
                        object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                onMapInteracted()
                                return false
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        },
                    ),
                )

                view.addMapListener(
                    object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val center = view.mapCenter as? GeoPoint ?: return false
                            onMapInteracted()
                            onMapMoved(center.latitude, center.longitude)
                            return true
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val center = view.mapCenter as? GeoPoint ?: return false
                            onMapInteracted()
                            onMapMoved(center.latitude, center.longitude)
                            return true
                        }
                    },
                )

                // Call onResume immediately so tile loading starts right away
                view.onResume()
                Log.d("MapScreen", "MapView.onResume() called, tile loading should begin")
            }
        },
        update = { view ->
            // Refresh photo markers whenever the list changes
            view.overlays.removeAll(view.overlays.filterIsInstance<Marker>().toSet())
            for (photo in photoLocations) {
                val marker = Marker(view)
                marker.position = photo.point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "📷 Photo taken here"
                marker.setOnMarkerClickListener { _, _ ->
                    onPhotoMarkerSelected(photo.uri)
                    true
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@Suppress("DEPRECATION")
private fun loadPhotoLocations(context: Context): List<PhotoMapItem> {
    val result = mutableListOf<PhotoMapItem>()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // API < 29: lat/lon stored directly in MediaStore
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE,
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val latCol = cursor.getColumnIndex(MediaStore.Images.Media.LATITUDE)
            val lonCol = cursor.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
            var count = 0
            while (cursor.moveToNext() && count < 500) {
                count++
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val lat = cursor.getDouble(latCol)
                val lon = cursor.getDouble(lonCol)
                if (lat != 0.0 || lon != 0.0) {
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    result.add(PhotoMapItem(uri = uri, point = GeoPoint(lat, lon)))
                }
            }
        }
    } else {
        // API 29+: read location via ExifInterface (requires ACCESS_MEDIA_LOCATION)
        val projection = arrayOf(MediaStore.Images.Media._ID)
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            var count = 0
            while (cursor.moveToNext() && count < 500) {
                count++
                val id = cursor.getLong(idCol)
                val rawUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id,
                )
                val uri = try {
                    MediaStore.setRequireOriginal(rawUri)
                } catch (_: UnsupportedOperationException) {
                    rawUri
                }
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val latLon = FloatArray(2)
                        if (exif.getLatLong(latLon)) {
                            result.add(
                                PhotoMapItem(
                                    uri = rawUri,
                                    point = GeoPoint(latLon[0].toDouble(), latLon[1].toDouble()),
                                ),
                            )
                        }
                    }
                } catch (_: Exception) { /* skip unreadable files */ }
            }
        }
    }

    return result
}

private fun loadPhotoPreview(
    context: Context,
    uri: Uri,
): ImageBitmap? =
    try {
        val bitmap: Bitmap? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(700, 450), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }

private fun getBestLastKnownLocation(context: Context): Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = locationManager.getProviders(true)
    var bestLocation: Location? = null

    for (provider in providers) {
        val location =
            runCatching {
                @Suppress("MissingPermission")
                locationManager.getLastKnownLocation(provider)
            }.getOrNull() ?: continue

        if (bestLocation == null || location.time > bestLocation.time) {
            bestLocation = location
        }
    }

    return bestLocation
}





