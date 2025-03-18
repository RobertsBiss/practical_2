package com.example.practical_2

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Default location (Valmiera)
    val defaultLocation = LatLng(57.537233, 25.412659)

    val cesuIelaViA = LatLng(57.53494954596886, 25.42434425186509)
    val terbatasIelaViA = LatLng(57.54172014823949, 25.428271086501255)
    val valleta = LatLng(57.53868023228932, 25.42360526901146)

    // Map loading state
    var isMapLoaded by remember { mutableStateOf(false) }

    // User location state
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera position state to control the map's camera
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
    }

    // Map UI settings
    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true,
                mapToolbarEnabled = true
            )
        )
    }

    // Map properties with updated location state
    val mapProperties by remember(hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = MapType.NORMAL
            )
        )
    }

    // Request location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            Log.d("MapScreen", "Location permission granted: $isGranted")
        }
    )

    // Handle lifecycle events for permission requests
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Get current location when permission is granted
    LaunchedEffect(hasLocationPermission) {
        Log.d("MapScreen", "Location permission state: $hasLocationPermission")
        if (hasLocationPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        Log.d("MapScreen", "Current location: $currentLatLng")
                        userLocation = currentLatLng // Store the user location
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } ?: run {
                        Log.d("MapScreen", "Location is null, using default")
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                    }
                }.addOnFailureListener { e ->
                    Log.e("MapScreen", "Error getting location", e)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Exception in location retrieval", e)
            }
        } else {
            Log.d("MapScreen", "No location permission, using default location")
            // Ensure map is centered on default location
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
        }
    }

    // Ensure map loads
    LaunchedEffect(Unit) {
        // Force map loaded after a delay
        delay(2000)
        isMapLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.maps_fragment_label)) },
                actions = {
                    IconButton(onClick = { navController.navigate("facts") }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View Facts"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Updated GoogleMap component with more explicit loading
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapLoaded = {
                    Log.d("MapScreen", "Map loaded successfully")
                    isMapLoaded = true
                }
            ) {
                // ViA cēsu iela
                Marker(
                    state = MarkerState(position = cesuIelaViA),
                    title = "ViA main building",
                    snippet = "ViA cēsu ielas building",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )

                // ViA tērbatas iela
                Marker(
                    state = MarkerState(position = terbatasIelaViA),
                    title = "ViA second building",
                    snippet = "ViA tērbatas ielas building",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                )

                // Valleta
                Marker(
                    state = MarkerState(position = valleta),
                    title = "Valleta",
                    snippet = "Tirzniecības centrs Valleta",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )

                // Add user location marker if available
                userLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Your Location",
                        snippet = "You are here",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )
                }
            }

            // Show loading indicator only if map is not loaded yet
            if (!isMapLoaded) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                Text(
                    "Loading map...",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}