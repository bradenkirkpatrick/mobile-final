package moravian.mobileclass.mobilefinal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.map_permission_button
import mobilefinal.composeapp.generated.resources.map_permission_rationale
import mobilefinal.composeapp.generated.resources.map_permission_title
import mobilefinal.composeapp.generated.resources.map_title
import org.jetbrains.compose.resources.stringResource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

private const val DEFAULT_LAT = 35.684
private const val DEFAULT_LON = -82.548

@Composable
actual fun MapScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember {
        mutableStateOf(hasLocationPermission(context))
    }
    var latitude by remember { mutableDoubleStateOf(DEFAULT_LAT) }
    var longitude by remember { mutableDoubleStateOf(DEFAULT_LON) }
    var locationCentered by remember { mutableStateOf(false) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

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
        Text(
            text = stringResource(Res.string.map_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )

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
            MapViewContent(latitude, longitude, mapViewRef)

            Text(
                text = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", latitude, longitude),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
            )
        }
    }
}

@Composable
private fun MapViewContent(
    latitude: Double,
    longitude: Double,
    mapViewRef: androidx.compose.runtime.MutableState<MapView?>,
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
                view.setMultiTouchControls(true)
                view.controller.setZoom(16.0)
                view.controller.setCenter(GeoPoint(latitude, longitude))
                view.isClickable = true
                
                // Call onResume immediately so tile loading starts right away
                view.onResume()
                Log.d("MapScreen", "MapView.onResume() called, tile loading should begin")
            }
        },
        update = {},
        modifier = Modifier.fillMaxSize(),
    )
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
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





