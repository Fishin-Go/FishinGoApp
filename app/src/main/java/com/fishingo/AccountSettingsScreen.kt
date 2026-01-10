package com.fishingo

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fishingo.network.ApiClient
import com.fishingo.network.UpdateUserRequest
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsScreen(navController: NavController) {
    val currentUser by UserManager.currentUser
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var username by remember { mutableStateOf(currentUser?.username ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                    text = "Account Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Settings Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Profile Information",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Divider(color = Color(0xFFE0E0E0))

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Change Password",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Current Password
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Current Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                Icon(
                                    if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (currentPasswordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // New Password
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "New Password") },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm New Password
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = {
                            confirmNewPassword = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Confirm New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        )
                    )

                    // Error Message
                    AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    // Success Message
                    AnimatedVisibility(visible = successMessage.isNotEmpty()) {
                        Text(
                            text = successMessage,
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save Changes Button
                    Button(
                        onClick = {
                            val user = currentUser ?: return@Button

                            when {
                                username.isEmpty() || email.isEmpty() -> {
                                    errorMessage = "Username and email cannot be empty"
                                }
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                    errorMessage = "Please enter a valid email"
                                }
                                username.length < 3 -> {
                                    errorMessage = "Username must be at least 3 characters"
                                }
                                newPassword.isNotEmpty() && currentPassword.isEmpty() -> {
                                    errorMessage = "Please enter your current password to change password"
                                }
                                newPassword.isNotEmpty() && newPassword.length < 6 -> {
                                    errorMessage = "New password must be at least 6 characters"
                                }
                                newPassword.isNotEmpty() && newPassword != confirmNewPassword -> {
                                    errorMessage = "New passwords do not match"
                                }
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val response = ApiClient.userApi.updateUser(
                                                userId = user.id,
                                                body = UpdateUserRequest(
                                                    username = username,
                                                    email = email,
                                                    currentPassword = if (currentPassword.isNotEmpty()) currentPassword else null,
                                                    newPassword = if (newPassword.isNotEmpty()) newPassword else null
                                                )
                                            )

                                            if (response.isSuccessful && response.body() != null) {
                                                val updatedUser = response.body()!!
                                                UserManager.setLoggedInUser(updatedUser)

                                                successMessage = "Account updated successfully!"
                                                errorMessage = ""

                                                // Clear password fields
                                                currentPassword = ""
                                                newPassword = ""
                                                confirmNewPassword = ""
                                            } else if (response.code() == 401) {
                                                errorMessage = "Current password is incorrect"
                                            } else if (response.code() == 409) {
                                                errorMessage = "Email is already in use"
                                            } else {
                                                errorMessage = "Update failed (${response.code()})"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Could not reach server. Please try again."
                                            Log.e("AccountSettings", "Error updating user", e)
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                "Save Changes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}