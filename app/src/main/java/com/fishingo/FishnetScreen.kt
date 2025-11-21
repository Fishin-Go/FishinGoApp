package com.fishingo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishnetScreen(navController: NavController) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎣 Fishnet") },
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
                        listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🛠️", fontSize = 60.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Fishnet is under construction!",
                    fontSize = 22.sp,
                    color = Color(0xFF0D47A1)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Coming soon...",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
