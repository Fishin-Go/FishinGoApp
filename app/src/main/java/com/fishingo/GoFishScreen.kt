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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.foundation.text.KeyboardOptions

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

    // Text state for manual test coordinates
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
                .background(Color(0xFFFFA726))
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
                .background(Color(0xFFFFA726))
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
        }

        // ==========================================
        //  IMPROVED: Manual lat/lon inputs + TEST button
        // ==========================================
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Text(
                    text = "🧪 Test Coordinates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D47A1)
                )

                // Row with two text fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Latitude Field
                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude", fontSize = 12.sp) },
                        placeholder = { Text("46.77", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Longitude Field
                    OutlinedTextField(
                        value = manualLon,
                        onValueChange = { manualLon = it },
                        label = { Text("Longitude", fontSize = 12.sp) },
                        placeholder = { Text("23.60", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Helper text
                Text(
                    text = "Leave empty to use GPS location",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // TEST CATCH Button
                Button(
                    onClick = {
                        val user = currentUser ?: run {
                            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val useManual = manualLat.isNotBlank() && manualLon.isNotBlank()

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

                            android.util.Log.d(
                                "WATER_TEST",
                                "Button pressed with coords: lat=$testLat, lon=$testLon"
                            )
                            val nearbyWater = WaterDatabaseManager.findNearestWater(
                                latitude = testLat,
                                longitude = testLon,
                                radiusMeters = 67.0
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
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726)
                    )
                ) {
                    Text(
                        "CATCH FISH",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================
        //  Fish popup
        // =========================
        if (showFishPopup && popupFishImageRes != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .background(
                            Color.White,
                            shape = RoundedCornerShape(16.dp)
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