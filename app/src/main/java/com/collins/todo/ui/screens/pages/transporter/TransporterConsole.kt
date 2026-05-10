package com.collins.todo.ui.screens.pages.transporter

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.ui.viewinterop.AndroidView
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.SupabaseClient
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransporterConsole(
    viewModel: TransporterViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToTracking: (Int) -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToProfile: () -> Unit
) {
    val orders by viewModel.orders
    val drivers by viewModel.drivers
    val isLoading by viewModel.isLoading
    val unreadMessages by viewModel.unreadMessageCount
    
    // Sort orders to ensure the latest activity is always at the top
    val sortedOrders = orders.sortedByDescending { it.id }
    val activeOrder = sortedOrders.find { it.status == "Ongoing" || it.status == "Arrived" || it.status == "Unloading" }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Find the order for the estimate dialog - prioritized selectedOrderId
    val estimateTarget = sortedOrders.find { it.id == viewModel.selectedOrderId } ?: activeOrder

    if (viewModel.showEstimateDialog && estimateTarget != null) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.showEstimateDialog = false
                viewModel.selectedOrderId = null
            },
            title = { Text("Trip Estimate") },
            text = {
                Column {
                    Text("How many days will it take to reach the site?")
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = viewModel.estimatedDays,
                        onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.estimatedDays = it },
                        label = { Text("Estimated Days") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    estimateTarget.id?.let { id ->
                        viewModel.updateOrderStatus(id, "Ongoing", viewModel.estimatedDays.toIntOrNull() ?: 1)
                        viewModel.showEstimateDialog = false
                        viewModel.selectedOrderId = null
                        onNavigateToTracking(id)
                    }
                }) { Text("Start Trip") }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    viewModel.showEstimateDialog = false 
                    viewModel.selectedOrderId = null
                }) { Text("Cancel") } 
            }
        )
    }

    if (viewModel.showNewTripDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showNewTripDialog = false },
            title = { Text("Record New Trip", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.material,
                        onValueChange = { viewModel.material = it },
                        label = { Text("Material Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = viewModel.quantity,
                        onValueChange = { viewModel.quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = viewModel.projectSite,
                        onValueChange = { viewModel.projectSite = it },
                        label = { Text("Project Site ID") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = viewModel.supplier,
                        onValueChange = { viewModel.supplier = it },
                        label = { Text("Supplier Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = viewModel.material.isNotBlank() && viewModel.projectSite.isNotBlank(),
                    onClick = {
                        scope.launch {
                            viewModel.createTrip(
                                MaterialOrder(
                                    projectId = viewModel.projectSite.toIntOrNull() ?: 0,
                                    materialName = viewModel.material,
                                    quantity = viewModel.quantity.toDoubleOrNull() ?: 0.0,
                                    unit = "Units",
                                    unitPrice = 0.0,
                                    status = "Ongoing",
                                    requiredStage = "Current",
                                    supplierName = viewModel.supplier,
                                    organizationId = authViewModel.currentUserProfile?.organizationId
                                )
                            )
                            viewModel.material = ""; viewModel.quantity = ""; viewModel.projectSite = ""; viewModel.supplier = ""
                            viewModel.showNewTripDialog = false
                        }
                    }
                ) { Text("Create Trip") }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    viewModel.showNewTripDialog = false 
                    viewModel.material = ""; viewModel.quantity = ""; viewModel.projectSite = ""; viewModel.supplier = ""
                }) { Text("Cancel") } 
            }
        )
    }

    if (viewModel.showEditVehicleHealth) {
        LaunchedEffect(Unit) {
            viewModel.fuel = (authViewModel.currentUserProfile?.fuelLevel ?: 78).toString()
            viewModel.serviceKm = (authViewModel.currentUserProfile?.nextServiceKm ?: 1240).toString()
            viewModel.vehiclePlate = authViewModel.currentUserProfile?.vehiclePlate ?: ""
            viewModel.vehicleModel = authViewModel.currentUserProfile?.vehicleModel ?: ""
        }

        AlertDialog(
            onDismissRequest = { viewModel.showEditVehicleHealth = false },
            title = { Text("Update Vehicle Details", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.vehiclePlate,
                        onValueChange = { viewModel.vehiclePlate = it },
                        label = { Text("License Plate") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = viewModel.vehicleModel,
                        onValueChange = { viewModel.vehicleModel = it },
                        label = { Text("Model (e.g. Isuzu FSR)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = viewModel.fuel,
                        onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.fuel = it },
                        label = { Text("Fuel Level (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = viewModel.serviceKm,
                        onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.serviceKm = it },
                        label = { Text("Next Service (km)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateVehicleHealth(
                        authViewModel = authViewModel,
                        fuel = viewModel.fuel.toIntOrNull() ?: 0,
                        serviceKm = viewModel.serviceKm.toIntOrNull() ?: 0,
                        plate = viewModel.vehiclePlate,
                        model = viewModel.vehicleModel
                    )
                    viewModel.showEditVehicleHealth = false
                }) { Text("Save Changes") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEditVehicleHealth = false }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.showVehicleSetupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showVehicleSetupDialog = false },
            title = { Text("Vehicle Registration", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Register the vehicle for this transport period.", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = viewModel.vehiclePlate,
                        onValueChange = { viewModel.vehiclePlate = it },
                        label = { Text("License Plate Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = viewModel.vehicleModel,
                        onValueChange = { viewModel.vehicleModel = it },
                        label = { Text("Vehicle Model (e.g. Isuzu FSR)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateVehicleDetails(authViewModel) }) { Text("Save Vehicle") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showVehicleSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Logistics Portal", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                        Text("TRANSPORTER CONSOLE", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp) 
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearUnreadCount()
                        onNavigateToMessages()
                    }) {
                        BadgedBox(
                            badge = {
                                if (unreadMessages > 0) {
                                    Badge { Text(unreadMessages.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, "Messages")
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, "Profile")
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
                    onClick = { 
                        viewModel.vehiclePlate = authViewModel.currentUserProfile?.vehiclePlate ?: ""
                        viewModel.vehicleModel = authViewModel.currentUserProfile?.vehicleModel ?: ""
                        viewModel.showVehicleSetupDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.LocalShipping, "Vehicle Setup")
                }

                FloatingActionButton(
                    onClick = { viewModel.showNewTripDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "New Trip")
                }
                
                FloatingActionButton(
                    onClick = {
                        viewModel.clearUnreadCount()
                        onNavigateToMessages()
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape,
                    contentColor = Color.White
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadMessages > 0) {
                                Badge { Text(unreadMessages.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, "Dispatch Chat")
                    }
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
                        TransporterStatCard("Total Trips", "${sortedOrders.size}", Icons.Default.Route, Modifier.weight(1f))
                        TransporterStatCard("Earnings", "$${String.format(java.util.Locale.getDefault(), "%,.0f", sortedOrders.filter { it.status == "Delivered" || it.status == "Completed" }.sumOf { it.earnings ?: 0.0 })}", Icons.Default.Payments, Modifier.weight(1f))
                    }
                }

                if (activeOrder != null) {
                    item {
                        ActiveLoadCard(
                            order = activeOrder,
                            onGetDirections = {
                                val gmmIntentUri = "google.navigation:q=Project+Site+${activeOrder.projectId}".toUri()
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            }
                        )
                    }
                    
                    item {
                        val currentUserId = authViewModel.currentUserProfile?.id
                        val isTakenByOther = activeOrder.transporterId != null && activeOrder.transporterId != currentUserId
                        val isTakenByMe = activeOrder.transporterId == currentUserId || activeOrder.transporterId == null
                        
                        QuickActionBar(
                            currentStatus = activeOrder.status,
                            onArrived = { if (isTakenByMe) activeOrder.id?.let { viewModel.updateOrderStatus(it, "Arrived") } },
                            onUnloading = { if (isTakenByMe) activeOrder.id?.let { viewModel.updateOrderStatus(it, "Unloading") } },
                            onPOD = { if (isTakenByMe) activeOrder.id?.let { viewModel.updateOrderStatus(it, "Delivered") } },
                            onTrack = { 
                                if (isTakenByMe) {
                                    if (activeOrder.estimatedDays == null) {
                                        viewModel.showEstimateDialog = true 
                                    } else {
                                        activeOrder.id?.let { id -> onNavigateToTracking(id) }
                                    }
                                }
                            },
                            isTakenByMe = isTakenByMe && !isTakenByOther
                        )
                    }
                }

                item {
                    Text("Available Loads", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }

                val availableLoads = sortedOrders.filter { it.status == "Pending" || it.status == "Dispatched" }
                if (availableLoads.isEmpty()) {
                    if (activeOrder == null) {
                        item {
                            WaitingForLoadState()
                        }
                    } else {
                        item {
                            Text("No other loads available.", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                } else {
                    items(availableLoads.size) { index ->
                        val order = availableLoads[index]
                        AvailableLoadCard(
                            order = order,
                            isSelected = activeOrder?.id == order.id,
                            onSelect = {
                                order.id?.let { id ->
                                    viewModel.selectedOrderId = id
                                    viewModel.showEstimateDialog = true
                                }
                            }
                        )
                    }
                }

                // Vehicle Status Card
                item {
                    VehicleStatusCard(
                        fuelLevel = authViewModel.currentUserProfile?.fuelLevel ?: 78,
                        serviceKm = authViewModel.currentUserProfile?.nextServiceKm ?: 1240,
                        plate = authViewModel.currentUserProfile?.vehiclePlate,
                        model = authViewModel.currentUserProfile?.vehicleModel,
                        onEdit = { viewModel.showEditVehicleHealth = true }
                    )
                }

                item {
                    FleetSummaryCard(drivers.size) { viewModel.showDriversDialog = true }
                }
            }
        }
    }

    if (viewModel.showDriversDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDriversDialog = false },
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
                TextButton(onClick = { viewModel.showDriversDialog = false }) { Text("Close") }
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
fun VehicleStatusCard(fuelLevel: Int, serviceKm: Int, plate: String?, model: String?, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vehicle Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit Health", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            
            // Plate & Model
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (!plate.isNullOrBlank()) "$model ($plate)" else "Vehicle not registered",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalGasStation, null, tint = Color(0xFFFFA000), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fuel Level", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                        Text("$fuelLevel%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fuelLevel / 100f },
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
                Text("Next Service: in $serviceKm km", color = Color.White, fontSize = 13.sp)
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
fun AvailableLoadCard(order: MaterialOrder, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else Color.White.copy(alpha = 0.05f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory, 
                    null, 
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(order.materialName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Project: Site #${order.projectId}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                Text("${order.quantity.toInt()} ${order.unit} • $${order.earnings ?: 0.0} Earnings", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = onSelect,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("START", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
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
                if (order.earnings != null) {
                    Text("Earned: $${order.earnings}", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
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
            Text("Supplier: ${order.supplierName}", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Text("Destination: Project Site #${order.projectId}", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
            
            if (order.earnings != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Expected Earnings: $${order.earnings}",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
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
fun QuickActionBar(
    currentStatus: String,
    onArrived: () -> Unit,
    onUnloading: () -> Unit,
    onPOD: () -> Unit,
    onTrack: () -> Unit,
    isTakenByMe: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isTakenByMe) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionButton(
                    label = "ARRIVED",
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f),
                    isActive = currentStatus == "Arrived",
                    onClick = onArrived
                )
                QuickActionButton(
                    label = "UNLOADING",
                    icon = Icons.Default.Unarchive,
                    modifier = Modifier.weight(1f),
                    isActive = currentStatus == "Unloading",
                    onClick = onUnloading
                )
                QuickActionButton(
                    label = "POD",
                    icon = Icons.Default.PhotoCamera,
                    modifier = Modifier.weight(1f),
                    isActive = currentStatus == "Delivered",
                    onClick = onPOD
                )
            }
        }
        
        Button(
            onClick = onTrack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = if (!isTakenByMe) ButtonDefaults.buttonColors(containerColor = Color.Gray) else ButtonDefaults.buttonColors()
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isTakenByMe) "TRACK MOVEMENT" else "LOCKED (ALREADY TAKEN)")
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
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
            Icon(Icons.Default.Inventory, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("No active trips assigned.", color = Color.White, fontWeight = FontWeight.Bold)
        Text("New loads from your manager will appear here.", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
    }
}
