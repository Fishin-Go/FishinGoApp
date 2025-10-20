package com.fishingo
import android.location.Location
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import org.osmdroid.util.GeoPoint
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fishingo.ui.theme.FishinGoTheme
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val conf = org.osmdroid.config.Configuration.getInstance()
        val ctx = applicationContext
        conf.load(
            ctx,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        conf.userAgentValue = packageName
        conf.osmdroidTileCache=ctx.cacheDir

        // Initialize UserManager
        UserManager.initialize(applicationContext)

        setContent {
            AppNavigator()
        }
    }
}

@Composable
fun MainMenu(onGoFishClick:()->Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment=Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text="FishinGo",
                fontSize = 35.sp,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {}){
                Text("Fishnet")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick= onGoFishClick){
                Text("Go Fish")
            }
        }
    }
}

@Composable
fun AppNavigator(){
    val navController = rememberNavController()
    val isLoggedIn by UserManager.isLoggedIn

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main_menu" else "auth"
    ){
        composable("auth"){
            AuthScreen(onLoginSuccess = {
                navController.navigate("main_menu") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("main_menu"){
            MainMenu(onGoFishClick = {
                navController.navigate("go_fish")
            })
        }
        composable("go_fish"){
            GoFishScreen(navController = navController)
        }
    }
}

@Composable
fun ProfileTopBar(navController: androidx.navigation.NavController) {
    val currentUser by UserManager.currentUser
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFFD2B48C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Go Fish",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Box {
                Button(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B7355)
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = currentUser?.username ?: "User",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (expanded) "▲" else "▼",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    // Profile Picture
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Color(0xFF1E88E5),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.username?.first()?.uppercase() ?: "U",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentUser?.username ?: "User",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Divider()

                    DropdownMenuItem(
                        text = { Text("Account Settings") },
                        onClick = {
                            expanded = false
                            // TODO: Navigate to settings
                        },
                        leadingIcon = {
                            Text("⚙️", fontSize = 20.sp)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Profile") },
                        onClick = {
                            expanded = false
                            // TODO: Navigate to profile
                        },
                        leadingIcon = {
                            Text("👤", fontSize = 20.sp)
                        }
                    )

                    Divider()

                    DropdownMenuItem(
                        text = {
                            Text(
                                "Logout",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            expanded = false
                            UserManager.logout()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        leadingIcon = {
                            Text("🚪", fontSize = 20.sp)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GoFishScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val activity = context as Activity
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    val mapView = remember { MapView(context) }

    // Cerere permisiuni
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

    // Obținerea locației
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = GeoPoint(it.latitude, it.longitude)
                }
            }
        }
    }

    // Adăugăm un efect care ascultă live locația
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                2000 // actualizare la fiecare 2 secunde
            ).setMinUpdateDistanceMeters(1f) // dacă te miști măcar 1 metru
                .build()

            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val location = result.lastLocation ?: return
                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    userLocation = newGeoPoint

                    // Centrează harta pe noua locație
                    mapView.controller.animateTo(newGeoPoint)
                }
            }

            // Important: folosim context.mainLooper ca să nu crape UI-ul
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // MapView în Compose
        AndroidView(
            factory = { mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)

                // Corecție: folosim controller local pentru a evita "unresolved reference"
                val controller = this.controller

                setOnTouchListener { _, event ->
                    when (event.pointerCount) {
                        1 -> true // blocăm pan complet cu un deget
                        2 -> {
                            val action = event.actionMasked
                            if (action == android.view.MotionEvent.ACTION_UP ||
                                action == android.view.MotionEvent.ACTION_POINTER_UP
                            ) {
                                // când utilizatorul termină zoom-ul, recentrăm pe marker
                                userLocation?.let { loc ->
                                    // folosim controller local (nu dă eroare)
                                    controller.animateTo(loc)
                                }
                            }
                            false // lăsăm zoom-ul să funcționeze normal
                        }
                        else -> true
                    }
                }
            }},
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                userLocation?.let { location ->
                    // Centrează harta
                    map.controller.setCenter(location)
                    map.overlays.clear()

                    // Marker personal
                    val marker = Marker(map)
                    marker.position = location
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.icon = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
                    map.overlays.add(marker)

                    // Overlay locație curentă
                    val locationOverlay = org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
                        org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(context),
                        map
                    )
                    locationOverlay.enableMyLocation()
                    map.overlays.add(locationOverlay)

                    // Centrează automat pe prima locație detectată
                    locationOverlay.runOnFirstFix {
                        activity.runOnUiThread {
                            map.controller.animateTo(locationOverlay.myLocation)
                        }
                    }
                }
            }
        )

        // Bara de sus - UPDATED WITH PROFILE
        ProfileTopBar(navController = navController)

        // Bara de jos
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
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview(){
    MainMenu(onGoFishClick = {})
}