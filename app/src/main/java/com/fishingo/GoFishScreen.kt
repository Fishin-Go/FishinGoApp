package com.fishingo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
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

    // -----------------------------
    //  Permissions
    // -----------------------------
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

        // -----------------------------
        //  MapView
        // -----------------------------
        AndroidView(
            factory = {
                mapView.apply {
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
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                userLocation?.let { location ->
                    map.controller.setCenter(location)
                    map.overlays.clear()

                    val marker = Marker(map)
                    marker.position = location
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
                    map.overlays.add(marker)

                    val locationOverlay =
                        org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
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

        // -------------------------------------------
        // Floating TEST CATCH button (centered)
        // -------------------------------------------
        Button(
            onClick = {
                val user = currentUser ?: run {
                    Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // ─────────────────────────────
                // OPTION A – REAL GPS (KEEPING THIS WORKING PATH COMMENTED FOR LATER)
                //
                // val loc = userLocation ?: run {
                //     Toast.makeText(context, "Location not available yet", Toast.LENGTH_SHORT).show()
                //     return@Button
                // }
                //
                // val testLat = loc.latitude
                // val testLon = loc.longitude
                //
                // ─────────────────────────────
                // OPTION B – HARDCODED TEST COORDS (CURRENTLY ACTIVE)
                // Cluj – bank of Someșul Mic (fake player position)
                val testLat =  44.3459844
                val testLon = 25.8853924

                scope.launch {
                    // 1) REGION FROM COUNTY
                    val region = getRegionForLocation(
                        context = context,
                        latitude = testLat,
                        longitude = testLon
                    ) ?: run {
                        Toast.makeText(
                            context,
                            "Could not determine region for this location",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    // 2) WATER WITHIN 50m (using Overpass + geometry)
                    // 3) Check for water within ~67m using offline SQLite
                    android.util.Log.d(
                        "WATER_TEST",
                        "Button pressed with test coords: lat=$testLat, lon=$testLon"
                    )
                    val nearbyWater = WaterDatabaseManager.findNearestWater(
                        latitude = testLat,
                        longitude = testLon,
                        radiusMeters = 67.0   // or 60.0 if you prefer stricter
                    )

                    val distance = nearbyWater?.distanceMeters ?: Double.MAX_VALUE
                    if (nearbyWater == null || distance > 67.0) {
                        Toast.makeText(
                            context,
                            "No mapped water within ~67m – move closer to a river or lake.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    val locationName = nearbyWater.name
                        ?: when {
                            nearbyWater.type != null -> "Unnamed ${nearbyWater.type}"
                            else -> "Nearby water"
                        }

                    // 3) PICK RANDOM FISH FOR REGION
                    val fishList = fishRegions[region]
                    if (fishList.isNullOrEmpty()) {
                        Toast.makeText(
                            context,
                            "No fish data for region $region",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    val randomFish = fishList.random()

                    // 4) SEND CATCH TO BACKEND
                    val request = NewCatchRequest(
                        fishName = randomFish,
                        region = region,
                        locationName = locationName,
                        latitude = testLat,
                        longitude = testLon,
                        description = "Catch from $region"
                    )

                    try {
                        val response = ApiClient.catchApi.createCatch(
                            userId = user.id,
                            body = request
                        )

                        if (response.isSuccessful) {
                            Toast.makeText(
                                context,
                                "You caught $randomFish at $locationName!",
                                Toast.LENGTH_LONG
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
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-25).dp)
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
