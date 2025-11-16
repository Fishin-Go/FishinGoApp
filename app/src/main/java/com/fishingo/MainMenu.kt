package com.fishingo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MainMenu(
    onGoFishClick: () -> Unit,
    onFishnetClick: () -> Unit,
    navController: NavController
) {
    val currentUser by UserManager.currentUser
    var showProfileMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.clickable { showProfileMenu = true },
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(25.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.username?.first()?.uppercase() ?: "U",
                                fontSize = 18.sp,
                                color = Color(0xFF0D47A1)
                            )
                        }
                        Text(
                            text = currentUser?.username ?: "User",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text("▼", fontSize = 10.sp, color = Color.White)
                    }
                }

                DropdownMenu(
                    expanded = showProfileMenu,
                    onDismissRequest = { showProfileMenu = false },
                    modifier = Modifier
                        .width(280.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    // User Profile Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Picture Circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.username?.first()?.uppercase() ?: "U",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Username
                        Text(
                            text = currentUser?.username ?: "User",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )

                        // Email
                        Text(
                            text = currentUser?.email ?: "user@email.com",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )

                    // Account Settings
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color(0xFF616161)
                                )
                                Text(
                                    "Account Settings",
                                    fontSize = 16.sp,
                                    color = Color(0xFF212121)
                                )
                            }
                        },
                        onClick = { showProfileMenu = false },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // My Profile
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color(0xFF616161)
                                )
                                Text(
                                    "My Profile",
                                    fontSize = 16.sp,
                                    color = Color(0xFF212121)
                                )
                            }
                        },
                        onClick = { showProfileMenu = false },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )

                    // Logout
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Color(0xFFD32F2F)
                                )
                                Text(
                                    "Logout",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        },
                        onClick = {
                            showProfileMenu = false
                            UserManager.logout()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text("🎣", fontSize = 80.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "FishinGo",
                    fontSize = 48.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text("Catch 'em all!", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f))
                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onFishnetClick,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("🕸️ Fishnet", fontSize = 20.sp, color = Color(0xFF0D47A1))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onGoFishClick,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726)
                    ),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("🎣 Go Fish", fontSize = 20.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}