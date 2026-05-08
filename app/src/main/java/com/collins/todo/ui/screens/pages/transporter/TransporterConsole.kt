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
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransporterConsole(
    viewModel: TransporterViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToTracking: (Int) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val orders by viewModel.orders
    val drivers by viewModel.drivers
    val isLoading by viewModel.isLoading
    val activeOrder = orders.find { it.status == "Ongoing" || it.status == "Dispatched" }
    val context = LocalContext.current

    var showEstimateDialog by remember { mutableStateOf(false) }
    var showDriversDialog by remember { mutableStateOf(false) }
    var showNewTripDialog by remember { mutableStateOf(false) }
    var estimatedDays by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (showEstimateDialog && activeOrder != null) {
        AlertDialog(
            onDismissRequest = { showEstimateDialog = false },
            title = { Text("Trip Estimate") },
            text = {
                Column {
                    Text("How many days will it take to reach the site?")
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = estimatedDays,
                        onValueChange = { if (it.all { char -> char.isDigit() }) estimatedDays = it },
                        label = { Text("Estimated Days") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        // Update order status and estimated days in Supabase
                        com.collins.todo.data.repository.SupabaseClient.client.from("material_orders").update(
                            buildJsonObject {
                                put("status", "Ongoing")
                                put("estimated_days", estimatedDays.toIntOrNull() ?: 1)
                            }
                        ) {
                            filter { eq("id", activeOrder.id ?: 0) }
                        }
                        showEstimateDialog = false
                        onNavigateToTracking(activeOrder.id ?: 0)
                    }
                }) { Text("Start Trip") }
            },
            dismissButton = { TextButton(onClick = { showEstimateDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TRANSPORTER CONSOLE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, "Profile")
                    }
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = { showNewTripDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "New Trip")
                }
                
                FloatingActionButton(
                    onClick = { /* Open Chat */ },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Chat, "Dispatch Chat")
                }
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
                            onPOD = { /* Open Camera */ },
                            onTrack = { 
                                if (activeOrder?.status == "Dispatched") {
                                    showEstimateDialog = true 
                                } else {
                                    activeOrder?.id?.let { onNavigateToTracking(it) }
                                }
                            }
                        )
                    }
                }

                item {
                    FleetSummaryCard(drivers.size) { showDriversDialog = true }
                }

                item {
                    Text("Trip History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                // Show other orders as history
                val history = orders.filter { it != activeOrder }
                if (history.isEmpty() && activeOrder == null) {
                    item { Text("No trips found.", color = MaterialTheme.colorScheme.tertiary) }
                } else {
                    items(history.size) { index ->
                        val order = history[index]
                        HistoryCard(order)
                    }
                }
            }
        }
    }

    if (showDriversDialog) {
        AlertDialog(
            onDismissRequest = { showDriversDialog = false },
            title = { Text("Company Fleet", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                if (drivers.isEmpty()) {
                    Text("No other drivers found in your company.", color = MaterialTheme.colorScheme.tertiary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(drivers.size) { index ->
                            val driver = drivers[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(driver.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(driver.phoneNumber ?: "No phone", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text(driver.email, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDriversDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun FleetSummaryCard(driverCount: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Your Fleet", color = Color.White, fontWeight = FontWeight.Bold)
                Text("$driverCount drivers registered", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun HistoryCard(order: MaterialOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(order.materialName, color = Color.White, fontWeight = FontWeight.Bold)
                Text("To Site #${order.projectId}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    order.status,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
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
fun QuickActionBar(onArrived: () -> Unit, onUnloading: () -> Unit, onPOD: () -> Unit, onTrack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionButton("ARRIVED", Icons.Default.LocationOn, Modifier.weight(1f), onArrived)
            QuickActionButton("UNLOADING", Icons.Default.Unarchive, Modifier.weight(1f), onUnloading)
            QuickActionButton("POD", Icons.Default.PhotoCamera, Modifier.weight(1f), onPOD)
        }
        Button(
            onClick = onTrack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("TRACK MOVEMENT")
        }
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
