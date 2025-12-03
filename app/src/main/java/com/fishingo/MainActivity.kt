package com.fishingo

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fishingo.ui.theme.FishinGoTheme
import com.google.android.gms.location.LocationServices
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val conf = org.osmdroid.config.Configuration.getInstance()
        val ctx = applicationContext
        conf.load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx))
        conf.userAgentValue = packageName
        conf.osmdroidTileCache = ctx.cacheDir

        UserManager.initialize(applicationContext)

        setContent {
            FishinGoTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val isLoggedIn by UserManager.isLoggedIn

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main_menu" else "auth"
    ) {
        composable("auth") {
            AuthScreen(onLoginSuccess = {
                navController.navigate("main_menu") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("main_menu") {
            MainMenu(
                onGoFishClick = { navController.navigate("go_fish") },
                onFishnetClick = { navController.navigate("inventory") },
                navController = navController
            )
        }
        composable("go_fish") { GoFishScreen() }
        composable("inventory") { FishnetScreen(navController = navController) }
    }
}