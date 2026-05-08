package com.collins.todo.ui.screens.pages.transporter

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.repository.ConstructionRepository
import com.collins.todo.data.repository.SupabaseClient
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

    private val _drivers = mutableStateOf<List<com.collins.todo.data.Models.UserProfile>>(emptyList())
    val drivers: State<List<com.collins.todo.data.Models.UserProfile>> = _drivers

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        fetchOrders()
        fetchDrivers()
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
        val channel = SupabaseClient.client.channel("transporter_orders_sync")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "material_orders"
        }

        flow.onEach {
            fetchOrders()
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            channel.subscribe()
        }
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _orders.value = repository.getTransporterOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTrip(order: MaterialOrder) {
        viewModelScope.launch {
            try {
                val user = com.collins.todo.data.repository.SupabaseClient.client.auth.currentUserOrNull()
                repository.createMaterialOrder(order.copy(transporterId = user?.id))
                fetchOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
