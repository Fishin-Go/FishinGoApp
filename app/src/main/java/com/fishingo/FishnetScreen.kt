package com.fishingo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(navController: NavController) {
    var fishList by remember { mutableStateOf<List<CaughtFish>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun refreshFishList() {
        try {
            fishList = FishInventoryManager.getAllFish()
            hasError = false
        } catch (e: Exception) {
            hasError = true
            errorMessage = e.message ?: "Unknown error"
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        try {
            fishList = FishInventoryManager.getAllFish()
            hasError = false
        } catch (e: Exception) {
            hasError = true
            errorMessage = e.message ?: "Unknown error"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎣 Fish Inventory") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                )
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                hasError -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⚠️", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error Loading Inventory",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                FishInventoryManager.clear()
                                refreshFishList()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Text("Clear Data & Retry")
                        }
                    }
                }
                fishList.isEmpty() -> {
                    EmptyInventoryView(
                        onAddTestFish = {
                            try {
                                addTestFish()
                                refreshFishList()
                            } catch (e: Exception) {
                                hasError = true
                                errorMessage = "Failed to add fish: ${e.message}"
                                e.printStackTrace()
                            }
                        }
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Stats Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = fishList.size.toString(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                    Text("Total Fish", fontSize = 14.sp, color = Color.Gray)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = fishList.count { it.fish.rarity == FishRarity.LEGENDARY }.toString(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                    Text("Legendary", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        addTestFish()
                                        refreshFishList()
                                    } catch (e: Exception) {
                                        hasError = true
                                        errorMessage = "Failed to add fish: ${e.message}"
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ Add Fish")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    try {
                                        FishInventoryManager.clear()
                                        refreshFishList()
                                    } catch (e: Exception) {
                                        hasError = true
                                        errorMessage = "Failed to clear: ${e.message}"
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fish List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(fishList) { caughtFish ->
                                FishCardSimple(caughtFish = caughtFish)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyInventoryView(onAddTestFish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎣", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Fish Yet!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start fishing to fill your inventory",
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddTestFish,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2)
            )
        ) {
            Text("Add Test Fish", fontSize = 18.sp)
        }
    }
}

@Composable
fun FishCardSimple(caughtFish: CaughtFish) {
    val rarityColor = Color(caughtFish.fish.rarity.color.toULong())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, rarityColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Rarity Badge
                Surface(
                    color = rarityColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = caughtFish.fish.rarity.displayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Text("🐟", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = caughtFish.fish.species,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${String.format("%.1f", caughtFish.fish.weight)} kg • ${String.format("%.0f", caughtFish.fish.length)} cm",
                fontSize = 15.sp,
                color = Color(0xFF424242)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = caughtFish.fish.waterType.displayName,
                fontSize = 13.sp,
                color = Color(0xFF1976D2).copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(caughtFish.caughtAt)),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// Fixed rarity system with proper weighted probabilities
private fun getRandomRarity(): FishRarity {
    val random = Random.nextInt(100)
    return when {
        random < 5 -> FishRarity.LEGENDARY    // 5% chance
        random < 15 -> FishRarity.EPIC        // 10% chance
        random < 35 -> FishRarity.RARE        // 20% chance
        random < 65 -> FishRarity.UNCOMMON    // 30% chance
        else -> FishRarity.COMMON             // 35% chance
    }
}

private fun addTestFish() {
    val rarity = getRandomRarity()

    // Species based on rarity
    val speciesByRarity = when (rarity) {
        FishRarity.LEGENDARY -> listOf("Golden Tuna", "Dragon Koi", "Ancient Sturgeon", "Phoenix Bass")
        FishRarity.EPIC -> listOf("Giant Marlin", "Blue Whale Shark", "Rainbow Trout", "King Salmon")
        FishRarity.RARE -> listOf("Swordfish", "Mahi-Mahi", "Barracuda", "Steelhead")
        FishRarity.UNCOMMON -> listOf("Bass", "Pike", "Walleye", "Perch")
        FishRarity.COMMON -> listOf("Catfish", "Carp", "Bluegill", "Sunfish")
    }

    // Weight and length based on rarity
    val (minWeight, maxWeight, minLength, maxLength) = when (rarity) {
        FishRarity.LEGENDARY -> listOf(15.0, 50.0, 100.0, 200.0)
        FishRarity.EPIC -> listOf(10.0, 30.0, 80.0, 150.0)
        FishRarity.RARE -> listOf(5.0, 15.0, 60.0, 100.0)
        FishRarity.UNCOMMON -> listOf(2.0, 8.0, 40.0, 80.0)
        FishRarity.COMMON -> listOf(0.5, 3.0, 20.0, 50.0)
    }

    val randomFish = Fish(
        name = "Wild ${speciesByRarity.random()}",
        species = speciesByRarity.random(),
        rarity = rarity,
        waterType = WaterType.entries.random(),
        weight = Random.nextDouble(minWeight, maxWeight),
        length = Random.nextDouble(minLength, maxLength),
        description = when (rarity) {
            FishRarity.LEGENDARY -> "An incredibly rare and magnificent specimen!"
            FishRarity.EPIC -> "A truly exceptional catch!"
            FishRarity.RARE -> "A beautiful and uncommon fish!"
            FishRarity.UNCOMMON -> "A decent catch!"
            FishRarity.COMMON -> "A common but respectable fish."
        }
    )

    val newCaughtFish = CaughtFish(
        fish = randomFish,
        userId = UserManager.currentUser.value?.id ?: "guest",
        location = FishLocation(0.0, 0.0, "Test Location")
    )

    FishInventoryManager.addFish(newCaughtFish)
}