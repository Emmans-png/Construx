package com.collins.todo.ui.screens.pages.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collins.todo.data.Models.UserProfile
import com.collins.todo.data.Models.DriverLocation
import com.collins.todo.data.repository.ConstructionRepository
import coil3.compose.AsyncImage
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerFleetScreen(
    onBack: () -> Unit,
    drivers: List<UserProfile>
) {
    var selectedDriver by remember { mutableStateOf<UserProfile?>(null) }
    var driverLocation by remember { mutableStateOf<DriverLocation?>(null) }
    val repository = remember { ConstructionRepository() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    LaunchedEffect(selectedDriver) {
        selectedDriver?.let {
            driverLocation = repository.getDriverLocation(it.id)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FLEET MANAGEMENT", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedDriver == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drivers) { driver ->
                        DriverFleetCard(driver) {
                            selectedDriver = driver
                        }
                    }
                }
            } else {
                DriverDetailView(
                    driver = selectedDriver!!,
                    location = driverLocation,
                    onBackToFleet = { selectedDriver = null }
                )
            }
        }
    }
}

@Composable
fun DriverFleetCard(driver: UserProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (driver.profilePictureUrl != null) {
                    AsyncImage(model = driver.profilePictureUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text(driver.username.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Role: ${driver.role}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun DriverDetailView(driver: UserProfile, location: DriverLocation?, onBackToFleet: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToFleet) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text("Driver Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                         if (driver.profilePictureUrl != null) {
                            AsyncImage(model = driver.profilePictureUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(driver.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(driver.email, color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                ProfileInfoRow(Icons.Default.Phone, "Phone", driver.phoneNumber ?: "N/A")
                ProfileInfoRow(Icons.Default.LocalGasStation, "Last Fuel Level", "${driver.fuelLevel}%")
                ProfileInfoRow(Icons.Default.Settings, "Next Service", "in ${driver.nextServiceKm} km")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("LAST KNOWN LOCATION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (location != null) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            controller.setZoom(15.0)
                            val point = GeoPoint(location.latitude, location.longitude)
                            controller.setCenter(point)
                            
                            val marker = Marker(this)
                            marker.position = point
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "Last seen: ${location.updatedAt}"
                            overlays.add(marker)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                    Text("No location data available", color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
