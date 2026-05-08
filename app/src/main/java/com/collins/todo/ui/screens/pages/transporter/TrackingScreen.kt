package com.collins.todo.ui.screens.pages.transporter

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel,
    onBack: () -> Unit,
    orderId: Int? = null,
    isViewer: Boolean = false
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation
    val pathPoints = viewModel.pathPoints
    val isTracking by viewModel.isTracking
    val viewerMode by viewModel.viewerMode

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.startTracking(context, orderId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setViewerMode(isViewer, orderId)
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName

        if (!isViewer) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isViewer) "Live Logistics Tracking" else "Movement Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isTracking || isViewer) {
                        Text(if (isViewer) "Monitoring" else "Live", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)
                        
                        // Initial center if needed, though it will follow location
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.clear()

                    // Draw Polyline (Breadcrumbs/History)
                    if (pathPoints.isNotEmpty()) {
                        val polyline = Polyline(mapView)
                        polyline.setPoints(pathPoints)
                        polyline.outlinePaint.color = android.graphics.Color.BLUE
                        polyline.outlinePaint.strokeWidth = 10f
                        mapView.overlays.add(polyline)
                    }

                    // Draw Current Position Marker
                    currentLocation?.let { point ->
                        val marker = Marker(mapView)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = if (isViewer) "Transporter's Current Position" else "My Position"
                        
                        // Use a different icon for viewer if available
                        // marker.icon = ... 

                        mapView.overlays.add(marker)

                        // Center map on current location if it's the first time or if requested
                        // mapView.controller.animateTo(point)
                    }

                    mapView.invalidate()
                }
            )
        }
    }
}
