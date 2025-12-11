package com.fishingo

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fishingo.network.ApiClient
import com.fishingo.network.CatchResponse
import kotlinx.coroutines.launch

@Composable
fun FishnetScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val currentUser by UserManager.currentUser

    var catches by remember { mutableStateOf<List<CatchResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Make sure fish info is loaded (latin + image + description)
    LaunchedEffect(Unit) {
        FishInfoManager.load(context)
    }

    // Load this user’s catches from backend
    LaunchedEffect(currentUser?.id) {
        val user = currentUser ?: return@LaunchedEffect
        isLoading = true
        error = null

        scope.launch {
            try {
                val response = ApiClient.catchApi.getCatches(userId = user.id)
                if (response.isSuccessful) {
                    catches = response.body().orEmpty()
                } else {
                    error = "Server error: ${response.code()}"
                    Log.e(
                        "Fishnet",
                        "Error loading catches: ${response.code()} ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                error = "Network error: ${e.message}"
                Log.e("Fishnet", "Exception while loading catches", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Background like your mockup: light gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFDFDF5),
                        Color(0xFFF3F0DC)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ======= HEADER: back button + title "YOUR FISHNET" =======
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / main-menu button (fish-shaped button in your mockup)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFFA726))
                        .clickable { navController.navigateUp() } // ← simple clickable
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "back",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "YOUR FISHNET",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A237E)
                )
            }

            // ======= BODY: list of cards =======
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading your catches...", color = Color.Gray)
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: "",
                            color = Color.Red
                        )
                    }
                }

                catches.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You haven't caught anything yet!",
                            color = Color(0xFF555555),
                            fontSize = 18.sp
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        catches.forEach { catch ->
                            FishnetCard(
                                catch = catch,
                                context = context
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ========= One card, like in your mockup =========

@Composable
private fun FishnetCard(
    catch: CatchResponse,
    context: Context
) {
    val info = FishInfoManager.getInfo(catch.fishName)
    val imageRes = info?.let { FishInfoManager.getDrawableId(context, it.image) }
    val latinName = info?.latin ?: ""
    val description = info?.description
        ?: "Fish description for ${catch.fishName} will go here."

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF8E1))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            // LEFT: fish image
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null && imageRes != 0) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = catch.fishName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    // fallback if image missing
                    Text(
                        text = "No image",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // RIGHT: texts (location, region, fish name, latin, description)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // catch location name (top, bold-ish)
                Text(
                    text = catch.locationName ?: "\"unknown location\"",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1B5E20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // "judet region"
                Text(
                    text = catch.region,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(0xFF33691E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // fish name
                Text(
                    text = catch.fishName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0D47A1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // fish scientific name (italic)
                if (latinName.isNotBlank()) {
                    Text(
                        text = latinName,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = Color(0xFF555555),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // description (multi-line, smaller)
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF444444),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
