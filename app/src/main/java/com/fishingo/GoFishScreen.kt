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

// ===== Added imports for minigame =====
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random
import kotlin.math.sqrt
import androidx.compose.ui.graphics.graphicsLayer

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

    // ===== Added: UI state + pending catch =====
    var catchState by remember { mutableStateOf(CatchUiState.Idle) }
    var pendingCatch by remember { mutableStateOf<PendingCatch?>(null) }
    // forces a fresh minigame run each time you enter it
    var minigameRunId by remember { mutableIntStateOf(0) }

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
        ) { }

        // ==========================================
        //  Bottom card: Idle (test coords) OR Minigame
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

            when (catchState) {
                CatchUiState.Idle -> {
                    // ===== Your existing UI (unchanged), with button now starting minigame =====
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🧪 Test Coordinates",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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

                        Text(
                            text = "Leave empty to use GPS location",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        Button(
                            onClick = {
                                val user = currentUser ?: run {
                                    Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // 1) Freeze coords at the moment of pressing the button
                                val useManual = manualLat.isNotBlank() && manualLon.isNotBlank()
                                val (testLat, testLon) = if (useManual) {
                                    val lat = manualLat.toDoubleOrNull()
                                    val lon = manualLon.toDoubleOrNull()

                                    if (lat == null || lon == null) {
                                        Toast.makeText(context, "Invalid manual coordinates", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    lat to lon
                                } else {
                                    val loc = userLocation ?: run {
                                        Toast.makeText(context, "Location not available yet", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    loc.latitude to loc.longitude
                                }

                                // 2) Run your checks BEFORE starting minigame
                                scope.launch {
                                    val region = getRegionForLocation(
                                        context = context,
                                        latitude = testLat,
                                        longitude = testLon
                                    ) ?: run {
                                        Toast.makeText(context, "Could not determine region for this location", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

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
                                        Toast.makeText(context, "No fish data for region $region", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    // 3) Freeze everything into pendingCatch and start minigame
                                    pendingCatch = PendingCatch(
                                        userId = user.id, // <-- int, as you said
                                        region = region,
                                        locationName = locationName,
                                        latitude = testLat,
                                        longitude = testLon
                                    )
                                    minigameRunId += 1
                                    catchState = CatchUiState.MiniGame
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

                CatchUiState.MiniGame -> {
                    val pc = pendingCatch
                    if (pc == null) {
                        // Safety fallback
                        catchState = CatchUiState.Idle
                    } else {
                        FishingMinigameCard(
                            runId = minigameRunId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            holdToWinMs = 5000L,
                            timeoutMs = 22000L,
                            // tuned to feel “not too erratic”
                            pullStrength = 5.1f,
                            fishSmoothness = 0.70f,
                            safeMin = 0.40f,
                            safeMax = 0.60f,
                            onWin = {
                                // After win: run your old catch logic
                                scope.launch {
                                    performCatch(
                                        context = context,
                                        pendingCatch = pc,
                                        fishRegions = fishRegions,
                                        setPopup = { fishName, imgRes ->
                                            popupFishName = fishName
                                            popupFishImageRes = imgRes
                                            showFishPopup = true
                                        },
                                        hidePopupLater = {
                                            scope.launch {
                                                delay(5000L)
                                                showFishPopup = false
                                            }
                                        }
                                    )
                                    pendingCatch = null
                                    catchState = CatchUiState.Idle
                                }
                            },
                            onFail = {
                                Toast.makeText(context, "The fish got away!", Toast.LENGTH_SHORT).show()
                                pendingCatch = null
                                catchState = CatchUiState.Idle
                            },
                            onLetGo = {
                                // user opted out
                                pendingCatch = null
                                catchState = CatchUiState.Idle
                            }
                        )
                    }
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

// ===== Added: UI state + pending catch model =====
private enum class CatchUiState { Idle, MiniGame }

private data class PendingCatch(
    val userId: Int,
    val region: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double
)

// ===== Added: Extracted “do the catch” logic so minigame can call it on win =====
private suspend fun performCatch(
    context: Context,
    pendingCatch: PendingCatch,
    fishRegions: Map<String, List<String>>,
    setPopup: (fishName: String, imageRes: Int?) -> Unit,
    hidePopupLater: () -> Unit
) {
    val fishList = fishRegions[pendingCatch.region]
    if (fishList.isNullOrEmpty()) {
        Toast.makeText(context, "No fish data for region ${pendingCatch.region}", Toast.LENGTH_SHORT).show()
        return
    }

    val randomFish = fishList.random()

    val request = NewCatchRequest(
        fishName = randomFish,
        region = pendingCatch.region,
        locationName = pendingCatch.locationName,
        latitude = pendingCatch.latitude,
        longitude = pendingCatch.longitude,
        description = "Catch from ${pendingCatch.region}"
    )

    try {
        val response = ApiClient.catchApi.createCatch(
            userId = pendingCatch.userId,
            body = request
        )

        if (response.isSuccessful) {
            Toast.makeText(
                context,
                "You caught $randomFish at ${pendingCatch.locationName}!",
                Toast.LENGTH_LONG
            ).show()

            val info = FishInfoManager.getInfo(randomFish)
            val imgRes = info?.let { FishInfoManager.getDrawableId(context, it.image) }

            if (imgRes != null) {
                setPopup(randomFish, imgRes)
                hidePopupLater()
            }
        } else {
            Toast.makeText(context, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * ===== Minigame Card =====
 * - fish dot moves in 2D
 * - line drawn to knob
 * - only fishX pulls knob horizontally
 * - hold 5 seconds continuously inside green zone
 * - timeout at 18 seconds
 * - Let it go button
 */
@Composable
private fun FishingMinigameCard(
    runId: Int,
    modifier: Modifier = Modifier,
    holdToWinMs: Long,
    timeoutMs: Long,
    pullStrength: Float,
    fishSmoothness: Float,
    safeMin: Float,
    safeMax: Float,
    onWin: () -> Unit,
    onFail: () -> Unit,
    onLetGo: () -> Unit
) {
    // Normalized positions in 0..1
    var fishX by remember(runId) { mutableFloatStateOf(0.5f) }
    var fishY by remember(runId) { mutableFloatStateOf(0.25f) }
    var facingRight by remember(runId) { mutableStateOf(false) }
    var knobX by remember(runId) { mutableFloatStateOf(0.5f) }
    var isDragging by remember(runId) { mutableStateOf(false) }
    var knobVel by remember(runId) { mutableFloatStateOf(0f) }
    var holdMs by remember(runId) { mutableLongStateOf(0L) }
    var elapsedMs by remember(runId) { mutableLongStateOf(0L) }

    // Game loop
    LaunchedEffect(runId) {
        var last = System.currentTimeMillis()

        // New: fish chooses a target and glides toward it (big distance, smooth)
        var targetX = Random.nextFloat()
        var targetY = 0.10f + Random.nextFloat() * 0.70f // 0.10..0.80 (top area)

        while (true) {
            val now = System.currentTimeMillis()
            val dtMs = (now - last).coerceAtLeast(1)
            last = now
            elapsedMs += dtMs
            val dt = dtMs / 1000f

            // Timeout => fail
            if (elapsedMs >= timeoutMs) {
                onFail()
                return@LaunchedEffect
            }

            // ==== Fish movement (target glide) ====
            // If close to target -> pick a NEW target far away to force big sweeps
            val dxT = targetX - fishX
            val dyT = targetY - fishY
            // Update facing direction based on horizontal movement (dxT)
            // small threshold prevents flickering when almost not moving
            if (dxT > 0.002f) facingRight = true
            else if (dxT < -0.002f) facingRight = false

            val dist = kotlin.math.sqrt(dxT * dxT + dyT * dyT)
            if (dist < 0.07f) {
                // pick a new X far enough away to force big horizontal travel
                var nx: Float
                do {
                    nx = Random.nextFloat()
                } while (kotlin.math.abs(nx - fishX) < 0.35f)

                targetX = nx
                targetY = 0.10f + Random.nextFloat() * 0.70f
            }

            // Smooth glide speed (bigger => faster but still smooth)
            val glideSpeed = 1.25f
            fishX += dxT * glideSpeed * dt
            fishY += dyT * glideSpeed * dt

            // Keep fish in bounds (top area)
            fishX = fishX.coerceIn(0f, 1f)
            fishY = fishY.coerceIn(0.05f, 0.78f)

            // Pull knob horizontally toward fishX ONLY (SMOOTH: velocity-based)
            val diffX = (fishX - knobX)

            // Convert diff into a force that affects velocity
            // influence is reduced while dragging so the knob doesn't jump away from your finger
            val influence = if (isDragging) 0.11f else 1.0f


            // This makes the fish feel strong WITHOUT teleporting the knob
            knobVel += (diffX * pullStrength) * dt * 0.85f * influence

            // Gentle damping so it doesn't accelerate forever
            knobVel *= 0.90f
            // Cap knob velocity so it can't become impossible
            knobVel = knobVel.coerceIn(-1.4f, 1.4f)
            // Optional gentle "yank" (now affects velocity, not position) and only when NOT dragging
//            if (!isDragging && Random.nextFloat() < 0.025f) {
//                knobVel += kotlin.math.sign(diffX) * 0.55f
//            }

            // Apply velocity to position
            knobX += knobVel * dt

            // Clamp position + soften edge behavior
            if (knobX < 0f) {
                knobX = 0f
                knobVel = 0f
            } else if (knobX > 1f) {
                knobX = 1f
                knobVel = 0f
            }

            // Hold-to-win (continuous)
            val inside = knobX in safeMin..safeMax
            if (inside) {
                holdMs += dtMs
                if (holdMs >= holdToWinMs) {
                    onWin()
                    return@LaunchedEffect
                }
            } else {
                holdMs = 0L
            }

            delay(16)
        }
    }


    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fishing…",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0D47A1)
            )

            TextButton(onClick = onLetGo) {
                Text("Let it go")
            }
        }

        // This box contains fish, line, and slider in one coordinate space
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()

            // slider geometry inside this box
            val sliderTopPx = h * 0.68f
            val sliderHeightPx = h * 0.20f
            val knobRadiusPx = sliderHeightPx * 0.55f

            fun knobCenter(): Offset {
                val x = knobX * (w - 2 * knobRadiusPx) + knobRadiusPx
                val y = sliderTopPx + sliderHeightPx / 2f
                return Offset(x, y)
            }

            fun fishCenter(): Offset {
                // fish dot (replaceable with sprite later)
                val r = h * 0.05f
                val x = fishX * (w - 2 * r) + r
                val y = fishY * (sliderTopPx - 2 * r) + r
                return Offset(x, y)
            }

            // LINE (fish -> knob)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val start = fishCenter()
                val end = knobCenter()
                drawLine(
                    color = Color.Gray,
                    start = start,
                    end = end,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Fish sprite (replaceable)
            val fishSize = 50.dp  // tweak 32-44dp based on how chunky you want it

            Image(
                painter = painterResource(id = R.drawable.fish_sprite),
                contentDescription = "Fish",
                modifier = Modifier
                    .offset {
                        val p = fishCenter()
                        androidx.compose.ui.unit.IntOffset(
                            (p.x - (fishSize.toPx() / 2f)).toInt(),
                            (p.y - (fishSize.toPx() / 2f)).toInt()
                        )
                    }
                    .size(fishSize)
                    .graphicsLayer {
                        // IMPORTANT: your sprite faces LEFT by default
                        // face left:  scaleX = 1
                        // face right: scaleX = -1
                        scaleX = if (facingRight) -1f else 1f
                    }
            )

            // Slider track + safe zone + knob (with drag)
            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(0, sliderTopPx.toInt()) }
                    .fillMaxWidth()
                    .height(with(LocalContext.current.resources.displayMetrics) { (sliderHeightPx / density).dp })
            ) {
                // Track
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3F3F3), RoundedCornerShape(14.dp))
                        .pointerInput(runId) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val delta = dx / width
                                knobX = (knobX + delta).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    // Safe zone
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (safeMax - safeMin).coerceIn(0f, 1f))
                            .align(Alignment.CenterStart)
                            .offset {
                                val px = (safeMin * constraints.maxWidth).toInt()
                                androidx.compose.ui.unit.IntOffset(px, 0)
                            }
                            .background(Color(0xFF66BB6A).copy(alpha = 0.35f))
                    )

                    // Knob
                    val knobSize = 42.dp
                    Box(
                        modifier = Modifier
                            .size(knobSize)
                            .offset {
                                val px = (knobX * (constraints.maxWidth - knobSize.toPx())).toInt()
                                val py = ((constraints.maxHeight - knobSize.toPx()) / 2f).toInt()
                                androidx.compose.ui.unit.IntOffset(px, py)
                            }
                            .background(Color(0xFFFFA726), RoundedCornerShape(99.dp))
                            .shadow(6.dp, RoundedCornerShape(99.dp))
                    )
                }
            }
        }

        // Progress (continuous inside safe zone)
        val p = (holdMs.toFloat() / holdToWinMs.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { p },
            modifier = Modifier.fillMaxWidth()
        )

        // Timeout indicator text
        val secondsLeft = ((timeoutMs - elapsedMs).coerceAtLeast(0L) / 1000L).toInt()
        Text(
            text = "Hold in green for 5s • Time left: ${secondsLeft}s",
            fontSize = 12.sp,
            color = Color.Gray
        )
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
