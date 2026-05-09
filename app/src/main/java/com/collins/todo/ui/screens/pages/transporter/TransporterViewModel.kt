package com.collins.todo.ui.screens.pages.transporter

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.Models.UserProfile
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
import com.collins.todo.ui.screens.authentication.login.AuthViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TransporterViewModel : ViewModel() {
    private val repository = ConstructionRepository()

    private val _orders = mutableStateOf<List<MaterialOrder>>(emptyList())
    val orders: State<List<MaterialOrder>> = _orders

    private val _drivers = mutableStateOf<List<UserProfile>>(emptyList())
    val drivers: State<List<UserProfile>> = _drivers

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _unreadMessageCount = mutableStateOf(0)
    val unreadMessageCount: State<Int> = _unreadMessageCount

    // UI State for Dialogs
    var showEstimateDialog by mutableStateOf(false)
    var showDriversDialog by mutableStateOf(false)
    var showNewTripDialog by mutableStateOf(false)
    var showEditVehicleHealth by mutableStateOf(false)

    // Form states
    var estimatedDays by mutableStateOf("")
    var material by mutableStateOf("")
    var quantity by mutableStateOf("")
    var projectSite by mutableStateOf("")
    var supplier by mutableStateOf("")
    
    // Vehicle Health Form
    var fuel by mutableStateOf("")
    var serviceKm by mutableStateOf("")

    // Vehicle Setup
    var vehiclePlate by mutableStateOf("")
    var vehicleModel by mutableStateOf("")
    var showVehicleSetupDialog by mutableStateOf(false)

    init {
        fetchOrders()
        fetchDrivers()
        fetchUnreadCount()
        setupRealtime()
    }

    fun fetchDrivers() {
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile()
                profile?.organizationId?.let { orgId ->
                    _drivers.value = repository.getDriversByOrganization(orgId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRealtime() {
        val channel = SupabaseClient.client.channel("transporter_sync")
        
        // Listen for order changes
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "material_orders"
        }.onEach {
            fetchOrders()
        }.launchIn(viewModelScope)

        // Listen for message changes
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }.onEach {
            fetchUnreadCount()
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            channel.subscribe()
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@launch
                val messages = repository.getMessagesForUser()
                _unreadMessageCount.value = messages.count { it.receiverId == user.id && !it.isRead }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearUnreadCount() {
        _unreadMessageCount.value = 0
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val profile = repository.getUserProfile()
                
                // Fetch all orders where this driver is the transporter OR it's unassigned in their org
                val allOrders = repository.getTransporterOrders()
                
                // Filter to ensure history ONLY shows what THIS driver has handled or is handling
                // This prevents seeing other drivers' private trips
                _orders.value = allOrders.filter { 
                    it.transporterId == user?.id || it.status == "Pending" || it.status == "Dispatched"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOrderStatus(orderId: Int, newStatus: String, estimatedDays: Int? = null) {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val currentOrder = _orders.value.find { it.id == orderId } ?: return@launch
                
                repository.updateMaterialOrder(
                    currentOrder.copy(
                        status = newStatus,
                        transporterId = user?.id, // Assign the driver who updates it
                        estimatedDays = estimatedDays ?: currentOrder.estimatedDays
                    )
                )
                fetchOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createTrip(order: MaterialOrder) {
        viewModelScope.launch {
            try {
                val user = com.collins.todo.data.repository.SupabaseClient.client.auth.currentUserOrNull()
                // Ensure userId and transporterId are set for the driver creating the trip
                val orderWithUser = order.copy(
                    userId = user?.id,
                    transporterId = user?.id
                )
                repository.createMaterialOrder(orderWithUser)
                fetchOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateVehicleHealth(authViewModel: AuthViewModel, fuel: Int, serviceKm: Int, plate: String? = null, model: String? = null) {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@launch
                // Fetch the freshest profile from DB to ensure we are updating the latest data
                val currentProfile = repository.getUserProfile()
                if (currentProfile != null) {
                    val updated = currentProfile.copy(
                        id = user.id, // STICK TO THE CURRENT USER ID
                        fuelLevel = fuel, 
                        nextServiceKm = serviceKm,
                        vehiclePlate = if (!plate.isNullOrBlank()) plate else currentProfile.vehiclePlate,
                        vehicleModel = if (!model.isNullOrBlank()) model else currentProfile.vehicleModel
                    )
                    println("DEBUG_VEHICLE: Saving UNIQUE health for driver ${user.id}")
                    val result = repository.updateUserProfile(updated)
                    if (result != null) {
                        authViewModel.fetchProfile() // Sync with global Auth state
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateVehicleDetails(authViewModel: AuthViewModel) {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@launch
                val currentProfile = repository.getUserProfile()
                if (currentProfile != null) {
                    val updated = currentProfile.copy(
                        id = user.id, // STICK TO THE CURRENT USER ID
                        vehiclePlate = vehiclePlate,
                        vehicleModel = vehicleModel
                    )
                    println("DEBUG_VEHICLE: Registering UNIQUE vehicle for driver ${user.id}")
                    val result = repository.updateUserProfile(updated)
                    if (result != null) {
                        authViewModel.fetchProfile()
                        showVehicleSetupDialog = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
