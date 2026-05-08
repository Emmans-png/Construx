package com.collins.todo.ui.screens.authentication.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.collins.todo.data.repository.SupabaseClient
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@Composable
fun OrganizationEntryScreen(
    viewModel: AuthViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        AsyncImage(
            model = "https://images.unsplash.com/photo-1541975095191-04bc1ec96a40?q=80&w=2070&auto=format&fit=crop",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CONSTRUX",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "CORPORATE SETUP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 6.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Organization Info",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Define your corporate identity to get started",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.organizationName,
                        onValueChange = { viewModel.organizationName = it },
                        placeholder = { Text("Company Name", color = MaterialTheme.colorScheme.tertiary) },
                        leadingIcon = { Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.industryType,
                        onValueChange = { viewModel.industryType = it },
                        placeholder = { Text("Industry Type (e.g. Civil Engineering)", color = MaterialTheme.colorScheme.tertiary) },
                        leadingIcon = { Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.location,
                        onValueChange = { viewModel.location = it },
                        placeholder = { Text("Headquarters Location", color = MaterialTheme.colorScheme.tertiary) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (viewModel.organizationName.isNotBlank() && viewModel.location.isNotBlank()) {
                                if (viewModel.organizationId.isBlank()) {
                                    viewModel.organizationId = "ORG-${System.currentTimeMillis()}-${(1000..9999).random()}"
                                }
                                scope.launch {
                                    try {
                                        SupabaseClient.client.from("organizations").upsert(
                                            com.collins.todo.data.Models.Organization(
                                                id = viewModel.organizationId,
                                                name = viewModel.organizationName,
                                                industryType = viewModel.industryType,
                                                location = viewModel.location
                                            )
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    onNext()
                                }
                            } else {
                                viewModel.errorMessage = "Please complete all organization details"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("FINISH SETUP", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            TextButton(onClick = onBack) {
                Text("Go Back", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
