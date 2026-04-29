package moravian.mobileclass.mobilefinal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
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
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
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
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapTypeStandard
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPointAnnotation
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.darwin.NSObject

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun MapScreen() {
    val mapConfig = koinInject<MapConfig>()
    var latitude by remember { mutableDoubleStateOf(mapConfig.defaultLatitude) }
    var longitude by remember { mutableDoubleStateOf(mapConfig.defaultLongitude) }
    var locationCentered by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            CLLocationManager.authorizationStatus() == kCLAuthorizationStatusAuthorizedWhenInUse ||
                CLLocationManager.authorizationStatus() == kCLAuthorizationStatusAuthorizedAlways,
        )
    }

    val locationManager = remember { CLLocationManager() }
    val mapViewRef = remember { mutableStateOf<MKMapView?>(null) }
    var showMap by remember { mutableStateOf(false) }

    // Load photo locations from PHAsset in background
    val photoLocations by produceState<List<Pair<Double, Double>>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.Default) { loadPhotoLocations() }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        showMap = true
    }
    val locationDelegate =
        remember {
            LocationDelegate(
                onAuthorizationChanged = { granted ->
                    hasPermission = granted
                    if (granted) {
                        locationManager.requestLocation()
                    }
                },
                onLocation = { lat, lon ->
                    latitude = lat
                    longitude = lon
                    locationCentered = false
                    locationManager.stopUpdatingLocation()
                },
            )
        }

    val mapDelegate =
        remember {
            MapCenterDelegate(
                onCenterChanged = { lat, lon ->
                    latitude = lat
                    longitude = lon
                },
            )
        }

    DisposableEffect(Unit) {
        locationManager.delegate = locationDelegate
        if (hasPermission) {
            locationManager.requestLocation()
        } else if (CLLocationManager.authorizationStatus() == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
        }
        onDispose {
            locationManager.stopUpdatingLocation()
        }
    }

    LaunchedEffect(latitude, longitude, locationCentered) {
        if (!locationCentered) {
            mapViewRef.value?.let { mapView ->
                val center = coordinate(latitude, longitude)
                mapView.setRegion(MKCoordinateRegionMakeWithDistance(center, 1500.0, 1500.0), animated = true)
                locationCentered = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding(),
    ) {

        Text(
            text = stringResource(Res.string.map_coordinates, formatCoordinate(latitude), formatCoordinate(longitude)),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
                    onClick = { locationManager.requestWhenInUseAuthorization() },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(stringResource(Res.string.map_permission_button))
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showMap) {
                UIKitView(
                    factory = {
                        MKMapView().apply {
                            mapViewRef.value = this
                            mapType = MKMapTypeStandard
                            delegate = mapDelegate
                            showsUserLocation = hasPermission
                            val center = coordinate(latitude, longitude)
                            setRegion(MKCoordinateRegionMakeWithDistance(center, 1500.0, 1500.0), animated = false)
                        }
                    },
                    update = { mapView ->
                        mapViewRef.value = mapView
                        mapView.delegate = mapDelegate
                        mapView.showsUserLocation = hasPermission

                        // Refresh photo pins whenever the list changes
                        mapView.annotations.forEach { annotation ->
                            if (annotation is MKPointAnnotation) {
                                mapView.removeAnnotation(annotation)
                            }
                        }
                        for ((lat, lon) in photoLocations) {
                            val annotation = MKPointAnnotation(
                                coordinate = coordinate(lat, lon),
                                title = "📷",
                                subtitle = null,
                            )
                            mapView.addAnnotation(annotation)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun loadPhotoLocations(): List<Pair<Double, Double>> {
    val result = mutableListOf<Pair<Double, Double>>()
    val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options = null)
    val maxCount = minOf(assets.count().toInt(), 500)
    for (index in 0 until maxCount) {
        val asset = assets.objectAtIndex(index.toULong()) as? PHAsset ?: continue
        val location = asset.location ?: continue
        location.coordinate.useContents {
            if (latitude != 0.0 || longitude != 0.0) {
                result.add(Pair(latitude, longitude))
            }
        }
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate(
    private val onAuthorizationChanged: (Boolean) -> Unit,
    private val onLocation: (Double, Double) -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = CLLocationManager.authorizationStatus()
        val granted = status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
        onAuthorizationChanged(granted)
    }

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val lastLocation = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        lastLocation.coordinate.useContents {
            onLocation(latitude, longitude)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class MapCenterDelegate(
    private val onCenterChanged: (Double, Double) -> Unit,
) : NSObject(),
    MKMapViewDelegateProtocol {
    override fun mapView(
        mapView: MKMapView,
        regionDidChangeAnimated: Boolean,
    ) {
        mapView.centerCoordinate.useContents {
            onCenterChanged(latitude, longitude)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun coordinate(
    latitude: Double,
    longitude: Double,
) = CLLocation(latitude = latitude, longitude = longitude).coordinate

private fun formatCoordinate(value: Double): String {
    val rounded = (value.absoluteValue * 10_000).roundToLong()
    val whole = rounded / 10_000
    val fraction = (rounded % 10_000).toString().padStart(4, '0')
    val sign = if (value < 0) "-" else ""
    return "$sign$whole.$fraction"
}





