package com.collins.todo.ui.screens.pages.transporter

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
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
    val activeOrder = orders.find { it.status == "Ongoing" || it.status == "Dispatched" || it.status == "Arrived" || it.status == "Unloading" }
    val context = LocalContext.current

    var showEstimateDialog by remember { mutableStateOf(false) }
    var showDriversDialog by remember { mutableStateOf(false) }
    var showNewTripDialog by remember { mutableStateOf(false) }
    var estimatedDays by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Form states for New Trip
    var material by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var projectSite by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }

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
                        viewModel.fetchOrders()
                    }
                }) { Text("Start Trip") }
            },
            dismissButton = { TextButton(onClick = { showEstimateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showNewTripDialog) {
        AlertDialog(
            onDismissRequest = { showNewTripDialog = false },
            title = { Text("Record New Trip", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = material,
                        onValueChange = { material = it },
                        label = { Text("Material Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = projectSite,
                        onValueChange = { projectSite = it },
                        label = { Text("Project Site ID") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Supplier Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = material.isNotBlank() && projectSite.isNotBlank(),
                    onClick = {
                        scope.launch {
                            viewModel.createTrip(
                                MaterialOrder(
                                    projectId = projectSite.toIntOrNull() ?: 0,
                                    materialName = material,
                                    quantity = quantity.toDoubleOrNull() ?: 0.0,
                                    unit = "Units",
                                    unitPrice = 0.0,
                                    status = "Ongoing",
                                    requiredStage = "Current",
                                    supplierName = supplier
                                )
                            )
                            material = ""; quantity = ""; projectSite = ""; supplier = ""
                            showNewTripDialog = false
                        }
                    }
                ) { Text("Create Trip") }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    showNewTripDialog = false 
                    material = ""; quantity = ""; projectSite = ""; supplier = ""
                }) { Text("Cancel") } 
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Logistics Portal", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                        Text("TRANSPORTER CONSOLE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) 
                    }
                },
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
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Chat, "Dispatch Chat")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // Dashboard Summary Stats
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TransporterStatCard("Total Trips", "${orders.size}", Icons.Default.Route, Modifier.weight(1f))
                        TransporterStatCard("Earnings", "$${String.format("%,.0f", orders.filter { it.status == "Delivered" }.sumOf { it.quantity * 20 })}", Icons.Default.Payments, Modifier.weight(1f))
                    }
                }

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
                            onArrived = {
                                scope.launch {
                                    com.collins.todo.data.repository.SupabaseClient.client.from("material_orders").update(
                                        buildJsonObject { put("status", "Arrived") }
                                    ) { filter { eq("id", activeOrder.id ?: 0) } }
                                    viewModel.fetchOrders()
                                }
                            },
                            onUnloading = {
                                scope.launch {
                                    com.collins.todo.data.repository.SupabaseClient.client.from("material_orders").update(
                                        buildJsonObject { put("status", "Unloading") }
                                    ) { filter { eq("id", activeOrder.id ?: 0) } }
                                    viewModel.fetchOrders()
                                }
                            },
                            onPOD = {
                                scope.launch {
                                    com.collins.todo.data.repository.SupabaseClient.client.from("material_orders").update(
                                        buildJsonObject { put("status", "Delivered") }
                                    ) { filter { eq("id", activeOrder.id ?: 0) } }
                                    viewModel.fetchOrders()
                                }
                            },
                            onTrack = { 
                                if (activeOrder.status == "Dispatched") {
                                    showEstimateDialog = true 
                                } else {
                                    activeOrder.id?.let { onNavigateToTracking(it) }
                                }
                            }
                        )
                    }
                } else {
                    item {
                        WaitingForLoadState()
                    }
                }

                // Vehicle Status Card
                item {
                    VehicleStatusCard()
                }

                item {
                    FleetSummaryCard(drivers.size) { showDriversDialog = true }
                }

                item {
                    Text("Trip History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                val history = orders.filter { it.status == "Delivered" || it.status == "Completed" }
                if (history.isEmpty() && activeOrder == null) {
                    item { 
                        Text("No completed trips recorded.", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(vertical = 12.dp)) 
                    }
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
fun TransporterStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VehicleStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Vehicle Health", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalGasStation, null, tint = Color(0xFFFFA000), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fuel Level", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                        Text("78%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 0.78f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = Color(0xFFFFA000),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SettingsInputComponent, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Next Service: in 1,240 km", color = Color.White, fontSize = 13.sp)
            }
        }
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
        Box(
            modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Waiting for next load...", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        Text("Your dispatch manager will assign a trip soon.", color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}
