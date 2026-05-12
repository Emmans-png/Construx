package com.collins.todo.ui.screens.pages.transporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.SupabaseClient
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransporterHomeScreen(
    viewModel: TransporterViewModel = viewModel(),
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToTracking: (Int) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val currentUser = remember { SupabaseClient.client.auth.currentUserOrNull() }
    val displayUsername = remember(currentUser) { 
        currentUser?.userMetadata?.get("username")?.toString()?.removeSurrounding("\"") ?: "Transporter"
    }

    val allOrders by viewModel.orders
    val isLoading by viewModel.isLoading

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ongoing", "Pending", "Completed")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Logistics Portal", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                        Text("CONSTRUX LOGISTICS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMessages) {
                        Icon(Icons.AutoMirrored.Filled.Message, "Messages", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, "Profile", tint = Color.White)
                    }
                    IconButton(onClick = { authViewModel.signOut(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Header Section
            TransporterHeader(displayUsername)
            
            // Stats Section
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard("Active Trips", allOrders.count { it.status == "Ongoing" || it.status == "Dispatched" }.toString(), Icons.Default.LocalShipping, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatsCard("Total Earnings", "$${String.format("%,.0f", authViewModel.currentUserProfile?.walletBalance ?: 0.0)}", Icons.Default.Payments, Color(0xFF4CAF50), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontSize = 14.sp, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.White else MaterialTheme.colorScheme.tertiary
                            ) 
                        }
                    )
                }
            }

            // Content List
            val filteredOrders = when (selectedTab) {
                0 -> allOrders.filter { it.status == "Ongoing" || it.status == "Dispatched" }
                1 -> allOrders.filter { it.status == "Pending" }
                else -> allOrders.filter { it.status == "Completed" || it.status == "Delivered" }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                if (filteredOrders.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                            Text("No ${tabs[selectedTab].lowercase()} trips found.", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                } else {
                    items(filteredOrders) { order ->
                        TripCard(order, onAction = {
                            if (order.status == "Pending" || order.status == "Dispatched") {
                                order.id?.let { onNavigateToTracking(it) }
                            } else if (order.status == "Ongoing") {
                                order.id?.let { viewModel.updateOrderStatus(it, "Delivered") }
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun TransporterHeader(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Logged in as", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
            Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun StatsCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun TripCard(order: MaterialOrder, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("${order.quantity.toInt()} ${order.unit} ${order.materialName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Order ID: #ORD-${order.id}", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                
                val statusColor = when(order.status) {
                    "Ongoing", "Dispatched" -> Color(0xFF2196F3)
                    "Pending" -> Color(0xFFFFA000)
                    "Completed", "Delivered" -> Color(0xFF4CAF50)
                    else -> Color.Gray
                }
                
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        order.status.uppercase(), 
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), 
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                TripPoint(label = "PICKUP", value = order.supplierName, icon = Icons.Default.Store, isStart = true)
                Box(modifier = Modifier.weight(1f).padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }
                TripPoint(label = "DROP-OFF", value = "Site #${order.projectId}", icon = Icons.Default.LocationOn, isStart = false)
            }
            
            if (order.status != "Completed" && order.status != "Delivered") {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        if (order.status == "Pending" || order.status == "Dispatched") "START TRIP" else "MARK AS COMPLETED",
                        fontWeight = FontWeight.Bold, 
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TripPoint(label: String, value: String, icon: ImageVector, isStart: Boolean) {
    Column(horizontalAlignment = if (isStart) Alignment.Start else Alignment.End, modifier = Modifier.width(120.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isStart) Icon(icon, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
            Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            if (!isStart) Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
        }
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
