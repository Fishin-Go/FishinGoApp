package com.fishingo
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
    val navController= rememberNavController()

    NavHost(navController = navController,startDestination="main_menu"){
        composable("main_menu"){
            MainMenu(onGoFishClick={
                navController.navigate("go_fish")
            })
        }
        composable("go_fish"){
            GoFishScreen()
        }
    }
}


@Composable

fun GoFishScreen() {

    val context = LocalContext.current

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val activity = context as Activity

    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }



    LaunchedEffect(Unit) {

        if (ContextCompat.checkSelfPermission(

                context,

                Manifest.permission.ACCESS_FINE_LOCATION

            ) != PackageManager.PERMISSION_GRANTED ||

            ContextCompat.checkSelfPermission(

                context,

                Manifest.permission.ACCESS_COARSE_LOCATION

            ) != PackageManager.PERMISSION_GRANTED

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

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->

                if (location != null) {

                    userLocation = GeoPoint(location.latitude, location.longitude)

                }

            }

        }

    }



    // 🟢 BOX = strat superior: putem pune harta + overlay-urile (barele)

    Box(modifier = Modifier.fillMaxSize()) {



        // Harta pe stratul de bază

        AndroidView(

            factory = { ctx ->

                MapView(ctx).apply {

                    setTileSource(TileSourceFactory.MAPNIK)

                    setMultiTouchControls(true)

                    controller.setZoom(15.0)

                    userLocation?.let {

                        controller.setCenter(it)

                        val marker = Marker(this)

                        marker.position = it

                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        marker.icon = ContextCompat.getDrawable(ctx, R.drawable.ic_launcher_foreground)

                        overlayManager.add(marker)

                    }

                }

            },

            modifier = Modifier.fillMaxSize()

        )



        // Bara de sus — strat peste hartă

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(50.dp)

                .background(Color(0xFFD2B48C))

                .align(Alignment.TopCenter),

            contentAlignment = Alignment.Center

        ) {

            Text("Go Fish", fontSize = 20.sp)

        }



        // Bara de jos — strat peste hartă

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