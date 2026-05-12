package com.collins.todo.ui.screens.pages.transporter

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.osmdroid.util.GeoPoint

class TrackingViewModel : ViewModel() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var activeOrderId: Int? = null
    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    private val _currentLocation = mutableStateOf<GeoPoint?>(null)
    val currentLocation: State<GeoPoint?> = _currentLocation

    private val _pathPoints = mutableStateListOf<GeoPoint>()
    val pathPoints: List<GeoPoint> = _pathPoints

    private val _isTracking = mutableStateOf(false)
    val isTracking: State<Boolean> = _isTracking

    private val _viewerMode = mutableStateOf(false)
    val viewerMode: State<Boolean> = _viewerMode

    fun setViewerMode(isViewer: Boolean, orderId: Int?) {
        _viewerMode.value = isViewer
        activeOrderId = orderId
        if (isViewer && orderId != null) {
            fetchLocationHistory(orderId)
            fetchCurrentLocation(orderId) // Add this to get immediate state
            startLiveUpdates(orderId)
        }
    }

    private fun fetchCurrentLocation(orderId: Int) {
        viewModelScope.launch {
            try {
                val liveData = com.collins.todo.data.repository.SupabaseClient.client.from("live_tracking")
                    .select {
                        filter { eq("order_id", orderId) }
                    }.decodeSingleOrNull<JsonObject>()
                
                liveData?.let { record ->
                    val lat = record["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@let
                    val lng = record["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@let
                    val point = GeoPoint(lat, lng)
                    _currentLocation.value = point
                    if (_pathPoints.isEmpty() || _pathPoints.last().latitude != lat || _pathPoints.last().longitude != lng) {
                        _pathPoints.add(point)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchLocationHistory(orderId: Int) {
        viewModelScope.launch {
            try {
                val history = com.collins.todo.data.repository.SupabaseClient.client.from("tracking_history")
                    .select {
                        filter { eq("order_id", orderId) }
                        order("timestamp", Order.ASCENDING)
                    }.decodeList<JsonObject>()
                
                _pathPoints.clear()
                history.forEach { json ->
                    val lat = json["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@forEach
                    val lng = json["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@forEach
                    _pathPoints.add(GeoPoint(lat, lng))
                }
                
                // Set current location to last point if available
                if (_pathPoints.isNotEmpty()) {
                    _currentLocation.value = _pathPoints.last()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startLiveUpdates(order_id: Int) {
        val channel = com.collins.todo.data.repository.SupabaseClient.client.channel("tracking_$order_id")
        realtimeChannel = channel
        
        // Listen specifically for this order's tracking updates
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "live_tracking"
        }.onEach { action ->
            val data = when (action) {
                is PostgresAction.Update -> action.record
                is PostgresAction.Insert -> action.record
                else -> null
            }

            data?.let { record ->
                val recordOrderId = record["order_id"]?.jsonPrimitive?.intOrNull
                if (recordOrderId == order_id) {
                    val lat = record["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@onEach
                    val lng = record["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@onEach
                    val point = GeoPoint(lat, lng)
                    
                    println("DEBUG_MAP: Manager received live update: $lat, $lng")
                    _currentLocation.value = point
                    
                    // Only add to path if it's a new unique point
                    if (_pathPoints.isEmpty() || _pathPoints.last().latitude != lat || _pathPoints.last().longitude != lng) {
                        _pathPoints.add(point)
                    }
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            channel.subscribe()
        }
    }

    fun startTracking(context: Context, orderId: Int?) {
        if (_isTracking.value) return
        activeOrderId = orderId

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Load existing history even for the transporter to show the trail
        orderId?.let { fetchLocationHistory(it) }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    
                    // Only update if location actually moved significantly (e.g. 5 meters) or it's the first point
                    val lastPoint = _currentLocation.value
                    if (lastPoint == null || calculateDistance(lastPoint.latitude, lastPoint.longitude, location.latitude, location.longitude) > 5.0) {
                        _currentLocation.value = geoPoint
                        _pathPoints.add(geoPoint)

                        // Sync to Supabase
                        activeOrderId?.let { id ->
                            syncLocation(id, location.latitude, location.longitude)
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _isTracking.value = true
        } catch (unlikely: SecurityException) {
            _isTracking.value = false
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun syncLocation(orderId: Int, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                
                // 1. Update Live Tracking (Realtime)
                // Use upsert on order_id to keep ONLY the latest position for live views
                com.collins.todo.data.repository.SupabaseClient.client.from("live_tracking").upsert(
                    buildJsonObject {
                        put("order_id", orderId)
                        put("latitude", lat)
                        put("longitude", lng)
                        put("timestamp", timestamp)
                    }
                )

                // 2. Insert into History (History/Breadcrumbs)
                com.collins.todo.data.repository.SupabaseClient.client.from("tracking_history").insert(
                    buildJsonObject {
                        put("order_id", orderId)
                        put("latitude", lat)
                        put("longitude", lng)
                        put("timestamp", timestamp)
                    }
                )

                // 3. Update Global Driver Location for Fleet View
                val user = com.collins.todo.data.repository.SupabaseClient.client.auth.currentUserOrNull()
                if (user != null) {
                    com.collins.todo.data.repository.ConstructionRepository().updateDriverLocation(
                        com.collins.todo.data.Models.DriverLocation(
                            driverId = user.id,
                            latitude = lat,
                            longitude = lng
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        viewModelScope.launch {
            realtimeChannel?.unsubscribe()
            realtimeChannel = null
        }
        _isTracking.value = false
        _pathPoints.clear()
        _currentLocation.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
