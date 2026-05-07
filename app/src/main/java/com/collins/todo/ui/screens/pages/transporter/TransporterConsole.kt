package com.collins.todo.ui.screens.pages.transporter

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.ui.screens.authentication.login.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransporterConsole(
    viewModel: TransporterViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val orders by viewModel.orders
    val isLoading by viewModel.isLoading
    val activeOrder = orders.find { it.status == "Ongoing" || it.status == "Dispatched" }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TRANSPORTER CONSOLE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                actions = {
                    IconButton(onClick = { authViewModel.signOut(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Open Chat */ },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Chat, "Dispatch Chat")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeOrder != null) {
                    item {
                        ActiveLoadCard(
                            order = activeOrder,
                            onGetDirections = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=Project+Site+${activeOrder.projectId}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            }
                        )
                    }
                    
                    item {
                        QuickActionBar(
                            onArrived = { /* Update status */ },
                            onUnloading = { /* Update status */ },
                            onPOD = { /* Open Camera */ }
                        )
                    }
                } else {
                    item {
                        WaitingForLoadState()
                    }
                }

                item {
                    Text("Trip History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                // Show other orders as history
                val history = orders.filter { it != activeOrder }
                if (history.isEmpty() && activeOrder == null) {
                    item { Text("No trips found.", color = MaterialTheme.colorScheme.tertiary) }
                }
            }
        }
    }
}

@Composable
fun ActiveLoadCard(order: MaterialOrder, onGetDirections: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(12.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text("ACTIVE LOAD", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text("${order.quantity.toInt()} ${order.unit} ${order.materialName}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("Destination: Project Site #${order.projectId}", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
            
            Spacer(Modifier.height(20.dp))
            
            Button(
                onClick = onGetDirections,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(8.dp))
                Text("GET DIRECTIONS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickActionBar(onArrived: () -> Unit, onUnloading: () -> Unit, onPOD: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickActionButton("ARRIVED", Icons.Default.LocationOn, Modifier.weight(1f), onArrived)
        QuickActionButton("UNLOADING", Icons.Default.Unarchive, Modifier.weight(1f), onUnloading)
        QuickActionButton("POD", Icons.Default.PhotoCamera, Modifier.weight(1f), onPOD)
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WaitingForLoadState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
        Spacer(Modifier.height(16.dp))
        Text("Waiting for next load...", color = MaterialTheme.colorScheme.tertiary)
    }
}
