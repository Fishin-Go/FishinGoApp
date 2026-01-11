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
import androidx.compose.material3.*
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

    // Load this user's catches from backend
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

    // Background matching app theme: blue gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D47A1),
                        Color(0xFF1976D2),
                        Color(0xFF2196F3)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ======= HEADER: back button + title =======
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Back button
                Surface(
                    modifier = Modifier.clickable { navController.navigateUp() },
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = "🕸️ Your Fishnet",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // ======= BODY: list of cards =======
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 48.sp
                            )
                            Text(
                                text = error ?: "",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                catches.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🎣",
                                fontSize = 64.sp
                            )
                            Text(
                                text = "You haven't caught anything yet!",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Go fishing to start your collection",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        catches.forEach { catch ->
                            FishnetCard(
                                catch = catch,
                                context = context
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        // Extra padding at bottom
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ========= One card with app theme styling =========

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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // LEFT: fish image
            Surface(
                modifier = Modifier
                    .size(100.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (imageRes != null && imageRes != 0) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = catch.fishName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    } else {
                        Text(
                            text = "🐟",
                            fontSize = 40.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // RIGHT: texts (location, region, fish name, latin, description)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // catch location name (top, bold)
                Text(
                    text = catch.locationName ?: "Unknown location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // region
                Text(
                    text = catch.region,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF616161),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // fish name
                Text(
                    text = catch.fishName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0D47A1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // fish scientific name (italic)
                if (latinName.isNotBlank()) {
                    Text(
                        text = latinName,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        color = Color(0xFF757575),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // description (multi-line, smaller)
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF424242),
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}