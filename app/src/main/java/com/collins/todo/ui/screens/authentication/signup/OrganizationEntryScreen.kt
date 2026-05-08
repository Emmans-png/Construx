package com.collins.todo.ui.screens.authentication.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CONSTRUX",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-2).sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "Organization",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Text(
                text = "Setup your organization details",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 32.dp)
            )

            TextField(
                value = viewModel.organizationName,
                onValueChange = { viewModel.organizationName = it },
                label = { Text("Organization Name", color = MaterialTheme.colorScheme.tertiary) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = viewModel.industryType,
                onValueChange = { viewModel.industryType = it },
                label = { Text("Industry Type", color = MaterialTheme.colorScheme.tertiary) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = viewModel.location,
                onValueChange = { viewModel.location = it },
                label = { Text("Location", color = MaterialTheme.colorScheme.tertiary) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (viewModel.organizationName.isNotBlank() && viewModel.location.isNotBlank()) {
                        // Generate a simple unique OrgID if not already set
                        if (viewModel.organizationId.isBlank()) {
                            viewModel.organizationId = "ORG-${System.currentTimeMillis()}-${(1000..9999).random()}"
                        }
                        
                        // Sync to organizations table
                        scope.launch {
                            try {
                                println("Syncing organization: ${viewModel.organizationName}")
                                SupabaseClient.client.from("organizations").upsert(
                                    com.collins.todo.data.Models.Organization(
                                        id = viewModel.organizationId,
                                        name = viewModel.organizationName,
                                        industryType = viewModel.industryType,
                                        location = viewModel.location
                                    )
                                )
                                println("Organization synced successfully.")
                            } catch (e: Exception) {
                                println("Organization sync failed: ${e.message}")
                                e.printStackTrace()
                            }
                            onNext()
                        }
                    } else {
                        viewModel.errorMessage = "Please fill in all fields"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onBack) {
                Text("Back", color = Color.White)
            }
        }
    }
}
