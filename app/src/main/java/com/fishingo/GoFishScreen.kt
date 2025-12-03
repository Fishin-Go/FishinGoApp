package com.fishingo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fishingo.network.ApiClient
import com.fishingo.network.NewCatchRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun GoFishScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    val mapView = remember { MapView(context) }

    // coroutine + user + fish regions
    val scope = rememberCoroutineScope()
    val currentUser by UserManager.currentUser
    val fishRegions = remember { loadFishRegions(context) }

    var clickCount by remember { mutableStateOf(0) }

    // Request permissions
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocation != PackageManager.PERMISSION_GRANTED ||
            coarseLocation != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        } else {
            locationPermissionGranted = true
        }
    }

    // Get last known location once
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = GeoPoint(it.latitude, it.longitude)
                }
            }
        }
    }

    // Listen for live location updates
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                2000L
            ).setMinUpdateDistanceMeters(1f)
                .build()

            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val location = result.lastLocation ?: return
                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    userLocation = newGeoPoint

                    mapView.controller.animateTo(newGeoPoint)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // MapView
        AndroidView(
            factory = { mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)

                val controller = this.controller

                setOnTouchListener { _, event ->
                    when (event.pointerCount) {
                        1 -> true
                        2 -> {
                            val action = event.actionMasked
                            if (action == android.view.MotionEvent.ACTION_UP ||
                                action == android.view.MotionEvent.ACTION_POINTER_UP
                            ) {
                                userLocation?.let { loc ->
                                    controller.animateTo(loc)
                                }
                            }
                            false
                        }
                        else -> true
                    }
                }
            }},
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                userLocation?.let { location ->
                    map.controller.setCenter(location)
                    map.overlays.clear()

                    val marker = Marker(map)
                    marker.position = location
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.icon = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
                    map.overlays.add(marker)

                    val locationOverlay = org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
                        org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(context),
                        map
                    )
                    locationOverlay.enableMyLocation()
                    map.overlays.add(locationOverlay)

                    locationOverlay.runOnFirstFix {
                        activity.runOnUiThread {
                            map.controller.animateTo(locationOverlay.myLocation)
                        }
                    }
                }
            }
        )

        // Top bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFFD2B48C))
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Text("🎣 Go Fish", fontSize = 24.sp, color = Color.White)
        }

        // Bottom bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFD2B48C))
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Text("FishinGo Footer", fontSize = 16.sp)
        }

        // Show how many times the button was pressed
        Text(
            text = "Test clicks: $clickCount",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-80).dp)
        )

        // Floating TEST CATCH button (centered, half-over footer)
        Button(
            onClick = {
                clickCount++

                val user = currentUser ?: return@Button

                // For now: always Transilvania
                val region = "Transilvania"
                val fishList = fishRegions[region]

                if (fishList.isNullOrEmpty()) {
                    Toast.makeText(context, "No fish list for $region", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val randomFish = fishList.random()
                val loc = userLocation

                val request = NewCatchRequest(
                    fishName = randomFish,
                    region = region,
                    locationName = "Unknown water",
                    latitude = loc?.latitude,
                    longitude = loc?.longitude,
                    description = "Test catch from Transilvania button"
                )

                scope.launch {
                    try {
                        val response = ApiClient.catchApi.createCatch(
                            userId = user.id,
                            body = request
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Catch sent! (${response.code()})",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Server error: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Network error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-25).dp) // half of 50dp footer height
        ) {
            Text("TEST CATCH")
        }
    }
}

// Helper: load region -> fish list map from res/raw/region_fish.json
private fun loadFishRegions(context: Context): Map<String, List<String>> {
    val inputStream = context.resources.openRawResource(R.raw.region_fish)
    val jsonText = inputStream.bufferedReader().use { it.readText() }

    val gson = Gson()
    val type = object : TypeToken<Map<String, List<String>>>() {}.type
    return gson.fromJson(jsonText, type)
}
