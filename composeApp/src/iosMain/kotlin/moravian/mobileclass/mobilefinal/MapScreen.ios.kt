package moravian.mobileclass.mobilefinal

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.map_permission_button
import mobilefinal.composeapp.generated.resources.map_permission_rationale
import mobilefinal.composeapp.generated.resources.map_permission_title
import mobilefinal.composeapp.generated.resources.map_title
import org.jetbrains.compose.resources.stringResource
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapTypeStandard
import platform.MapKit.MKMapView
import platform.darwin.NSObject

private const val DEFAULT_LAT = 35.684
private const val DEFAULT_LON = -82.548

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun MapScreen() {
    var latitude by remember { mutableDoubleStateOf(DEFAULT_LAT) }
    var longitude by remember { mutableDoubleStateOf(DEFAULT_LON) }
    var hasPermission by remember {
        mutableStateOf(
            CLLocationManager.authorizationStatus() == kCLAuthorizationStatusAuthorizedWhenInUse ||
                CLLocationManager.authorizationStatus() == kCLAuthorizationStatusAuthorizedAlways,
        )
    }

    val locationManager = remember { CLLocationManager() }
    val delegate =
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
                    locationManager.stopUpdatingLocation()
                },
            )
        }

    DisposableEffect(Unit) {
        locationManager.delegate = delegate
        if (hasPermission) {
            locationManager.requestLocation()
        } else if (CLLocationManager.authorizationStatus() == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
        }
        onDispose {
            locationManager.stopUpdatingLocation()
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

        Text(
            text = "Lat: %.4f, Lon: %.4f".format(latitude, longitude),
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

        UIKitView(
            factory = {
                MKMapView().apply {
                    mapType = MKMapTypeStandard
                    showsUserLocation = hasPermission
                    val center = coordinate(latitude, longitude)
                    setRegion(MKCoordinateRegionMakeWithDistance(center, 1500.0, 1500.0), animated = false)
                }
            },
            update = { mapView ->
                mapView.showsUserLocation = hasPermission
                val center = coordinate(latitude, longitude)
                mapView.setRegion(MKCoordinateRegionMakeWithDistance(center, 1500.0, 1500.0), animated = true)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
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
private fun coordinate(
    latitude: Double,
    longitude: Double,
) = CLLocation(latitude = latitude, longitude = longitude).coordinate





