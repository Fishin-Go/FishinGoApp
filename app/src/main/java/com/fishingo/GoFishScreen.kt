package com.fishingo

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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

    var showFishPopup by remember { mutableStateOf(false) }
    var popupFishName by remember { mutableStateOf<String?>(null) }
    var popupFishImageRes by remember { mutableStateOf<Int?>(null) }

    // 🔹 NEW: text state for manual test coordinates
    var manualLat by remember { mutableStateOf("") }
    var manualLon by remember { mutableStateOf("") }

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

    // Make sure fish info is loaded (for popup)
    LaunchedEffect(Unit) {
        FishInfoManager.load(context)
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

        // ==========================================
        //  NEW: Manual lat/lon inputs + TEST button
        //  (Everything is grouped in a Column at the bottom)
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp), // a bit above the footer bar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Small helper text so Future-You remembers what this is
            Text(
                text = "Manual test coordinates (optional)",
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Row with two text fields: latitude / longitude
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = manualLat,
                    onValueChange = { manualLat = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                TextField(
                    value = manualLon,
                    onValueChange = { manualLon = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -------------------------------------------
            // TEST CATCH button (uses manual coords if set)
            // -------------------------------------------
            Button(
                onClick = {
                    val user = currentUser ?: run {
                        Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 1) Decide which coordinates to use:
                    //    - if BOTH manual fields are non-empty and valid → use those
                    //    - otherwise → fallback to real GPS location
                    val useManual =
                        manualLat.isNotBlank() && manualLon.isNotBlank()

                    val (testLat, testLon) = if (useManual) {
                        val lat = manualLat.toDoubleOrNull()
                        val lon = manualLon.toDoubleOrNull()

                        if (lat == null || lon == null) {
                            Toast.makeText(
                                context,
                                "Invalid manual coordinates",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        lat to lon
                    } else {
                        val loc = userLocation ?: run {
                            Toast.makeText(
                                context,
                                "Location not available yet",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        loc.latitude to loc.longitude
                    }

                    scope.launch {
                        // 2) REGION FROM COUNTY
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

                        // 3) Check for water within ~67m using offline SQLite
                        android.util.Log.d(
                            "WATER_TEST",
                            "Button pressed with coords: lat=$testLat, lon=$testLon"
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

                        // 4) PICK RANDOM FISH FOR REGION
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

                        // 5) SEND CATCH TO BACKEND
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
                                // Toast as before
                                Toast.makeText(
                                    context,
                                    "You caught $randomFish at $locationName!",
                                    Toast.LENGTH_LONG
                                ).show()

                                // 🔹 look up fish info (latin name + image) from FishInfoManager
                                val info = FishInfoManager.getInfo(randomFish)
                                val imgRes = info?.let {
                                    FishInfoManager.getDrawableId(
                                        context,
                                        it.image
                                    )
                                }

                                if (imgRes != null) {
                                    popupFishName = randomFish
                                    popupFishImageRes = imgRes
                                    showFishPopup = true

                                    // hide after 5 seconds
                                    scope.launch {
                                        delay(5000L)
                                        showFishPopup = false
                                    }
                                }
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
                }
            ) {
                Text("TEST CATCH")
            }
        }

        // =========================
        //  Fish popup (unchanged)
        // =========================
        if (showFishPopup && popupFishImageRes != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)), // semi-transparent black
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .background(
                            Color.White,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    popupFishImageRes?.let { resId ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = popupFishName ?: "Caught fish",
                            modifier = Modifier
                                .size(180.dp)
                                .padding(bottom = 12.dp)
                        )
                    }

                    Text(
                        text = popupFishName ?: "Unknown fish",
                        fontSize = 20.sp,
                        color = Color(0xFF0D47A1)
                    )

                    val latin = FishInfoManager.getInfo(popupFishName ?: "")?.latin
                    if (latin != null) {
                        Text(
                            text = latin,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
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
